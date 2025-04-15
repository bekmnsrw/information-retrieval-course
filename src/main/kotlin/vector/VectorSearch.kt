package vector

import Utils.lemmatize
import Utils.tokenizeAndCleanText
import java.io.File
import kotlin.math.sqrt

/**
 * @author i.bekmansurov
 */
internal interface VectorSearch {

    fun search(query: String): List<SearchResult>
}

internal class VectorSearchImpl : VectorSearch {

    private val lemmaToIdf = mutableMapOf<String, Double>()
    private val pageIdToPageVector = mutableMapOf<String, PageVector>()
    private val pageIdToUrl = mutableMapOf<String, String>()

    init {
        loadResources()
    }

    private fun loadResources() {
        File(INDEX_PATH).forEachLine { line ->
            val (id, url) = line.split(SPACE, limit = 2)
            pageIdToUrl[id] = url
        }

        File(TF_IDF_LEMMAS_PATH).listFiles()?.forEach { file ->
            file.forEachLine { line ->
                val (lemma, idf, _) = line.split(SPACE)
                lemmaToIdf.putIfAbsent(lemma, idf.toDouble())
            }
        }

        File(TF_IDF_LEMMAS_PATH).listFiles()?.forEach { file ->
            val pageTfIdf = file.readLines().associate { line ->
                val (lemma, _, tfIdf) = line.split(SPACE)
                lemma to tfIdf.toDouble()
            }
            val norm = calculateNorm(pageTfIdf.values)
            val pageId = file.name.getPageId()
            pageIdToPageVector[pageId] = PageVector(pageTfIdf, norm)
        }
    }

    private fun createQueryVector(query: String): QueryVector? {
        val tokens = tokenizeAndCleanText(query)
        val lemmas = tokens
            .map { token -> lemmatize(token) }
            .ifEmpty { return null }

        val lemmaCounts = lemmas.groupingBy { it }.eachCount()
        val total = lemmas.size.toDouble()

        val queryTf = lemmaCounts.mapValues { (_, count) -> count / total }
        val queryTfIdf = queryTf.mapValues { (lemma, tf) -> tf * (lemmaToIdf[lemma] ?: ZERO_SIMILARITY) }

        val queryNorm = calculateNorm(queryTfIdf.values)

        return if (queryNorm == ZERO_SIMILARITY) null else QueryVector(queryTfIdf, queryNorm)
    }

    override fun search(query: String): List<SearchResult> {
        val queryVector = createQueryVector(query) ?: return emptyList()

        val searchResults = pageIdToPageVector.mapNotNull { (id, pageVector) ->
            val url = pageIdToUrl[id] ?: run { return@mapNotNull null }
            val cosineSimilarity = calculateCosineSimilarity(
                queryTfIdf = queryVector.tfIdf,
                pageTfIdf = pageVector.tfIdf,
                queryNorm = queryVector.norm,
                pageNorm = pageVector.norm,
            )
            SearchResult(url, cosineSimilarity)
        }

        return searchResults
            .filter { result -> result.cosineSimilarity > ZERO_SIMILARITY }
            .sortedByDescending { result -> result.cosineSimilarity }
    }

    private fun calculateNorm(values: Collection<Double>): Double = sqrt(values.sumOf { it * it })

    private fun calculateCosineSimilarity(
        queryTfIdf: Map<String, Double>,
        pageTfIdf: Map<String, Double>,
        queryNorm: Double,
        pageNorm: Double,
    ): Double {
        if (pageNorm == ZERO_SIMILARITY) return ZERO_SIMILARITY

        val dotProduct = queryTfIdf.entries.sumOf { (lemma, tfIdf) ->
            (pageTfIdf[lemma] ?: ZERO_SIMILARITY) * tfIdf
        }

        return dotProduct / (pageNorm * queryNorm)
    }

    private fun String.getPageId(): String = this
        .removePrefix(LEMMA_FILE_PREFIX)
        .removeSuffix(LEMMA_FILE_SUFFIX)

    private companion object {

        const val INDEX_PATH = "output/index.txt"
        const val TF_IDF_LEMMAS_PATH = "output/tfidf_lemmas"
        const val SPACE = " "
        const val LEMMA_FILE_PREFIX = "lemma_tfidf_выкачка_"
        const val LEMMA_FILE_SUFFIX = ".txt"
        const val ZERO_SIMILARITY = 0.0
    }
}
