package tf_idf

import Utils.WEB_PAGES_PATH
import Utils.lemmatize
import Utils.replaceHtmlExtension
import Utils.tokenizeAndCleanText
import org.jsoup.Jsoup
import java.io.File
import kotlin.math.ln

/**
 * @author i.bekmansurov
 */
internal interface TfIdfCalculator {

    fun calculateTfIdf()
}

internal class TfIdfCalculatorImpl(
    pagesPath: String = WEB_PAGES_PATH,
    private val outputPrefix: String = "output",
) : TfIdfCalculator {

    private val pages = File(pagesPath).listFiles()?.toList() ?: emptyList()

    private data class DocumentAnalysis(
        val tokens: List<String>,
        val tokenFrequency: Map<String, Int>,
        val lemmaFrequency: Map<String, Int>,
    )

    override fun calculateTfIdf() {
        val analysisResults = pages.map { analyzeDocument(it) }

        val tokenPagesCount = countPageOccurrences(analysisResults) { it.tokenFrequency.keys }
        val lemmaPagesCount = countPageOccurrences(analysisResults) { it.lemmaFrequency.keys }

        analysisResults.forEachIndexed { index, (tokens, tokenFrequency, lemmaFrequency) ->
            val file = pages[index]
            writeTfIdf(file, tokenFrequency, tokens.size, tokenPagesCount, "token")
            writeTfIdf(file, lemmaFrequency, tokens.size, lemmaPagesCount, "lemma")
        }
    }

    private fun analyzeDocument(file: File): DocumentAnalysis {
        val text = Jsoup.parse(file.readText()).text()
        val tokens = tokenizeAndCleanText(text)
        return DocumentAnalysis(
            tokens = tokens,
            tokenFrequency = tokens.groupingBy { it }.eachCount(),
            lemmaFrequency = tokens.groupingBy { lemmatize(it) }.eachCount(),
        )
    }

    private fun countPageOccurrences(
        results: List<DocumentAnalysis>,
        featureSelector: (DocumentAnalysis) -> Set<String>,
    ): Map<String, Int> {
        return results
            .flatMap { featureSelector(it) }
            .groupingBy { it }
            .eachCount()
    }

    /**
     * TF(t, d) = количество_вхождений_термина_t_в_документ_d / общее_количество_слов_в_документе_d
     * ~ как часто термин встречается в документе
     *
     * TF-IDF(t, d) = TF(t, d) * IDF(t)
     * ~ важность термина в документе относительно всей коллекции
     */
    private fun writeTfIdf(
        file: File,
        frequencies: Map<String, Int>,
        totalTokens: Int,
        pageCounts: Map<String, Int>,
        prefix: String
    ) {
        val outputPath = "$outputPrefix/tfidf_${prefix}s/${prefix}_tfidf_${file.name.replaceHtmlExtension()}"

        val lines = frequencies.map { (token, count) ->
            val tf = count.toDouble() / totalTokens
            val idf = calculateIdf(token, pageCounts)
            val tfIdf = tf * idf
            "$token ${idf.format(DIGITS)} ${tfIdf.format(DIGITS)}"
        }

        File(outputPath).apply {
            parentFile?.mkdirs()
        }.writeText(lines.joinToString("\n"))
    }

    /**
     * IDF(t) = log(общее_количество_документов / количество_документов_содержащих_термин_t)
     * ~ мера редкости термина во всей коллекции документов
     */
    private fun calculateIdf(token: String, pageCounts: Map<String, Int>): Double {
        val pagesWithToken = pageCounts[token] ?: 0
        return if (pagesWithToken == 0) 0.0 else ln(pages.size.toDouble() / pagesWithToken)
    }

    private fun Double.format(digits: Int) = "%.${digits}f".format(this)

    private companion object {

        const val DIGITS = 4
    }
}
