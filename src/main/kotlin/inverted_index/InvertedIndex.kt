package inverted_index

import java.io.File

/**
 * @author i.bekmansurov
 */
internal interface InvertedIndex {

    /**
     * @property pageLemmas Леммы документов в формате: [имя документа → [лемма → токены]]
     */
    val pageLemmas: MutableMap<String, MutableMap<String, MutableSet<String>>>

    /**
     * @property invertedIndex Инвертированный индекс в формате: [лемма → документы]
     */
    val invertedIndex: MutableMap<String, MutableSet<String>>

    fun buildInvertedIndex()

    fun writeInvertedIndex()
}

internal class InvertedIndexImpl : InvertedIndex {

    override val pageLemmas = mutableMapOf<String, MutableMap<String, MutableSet<String>>>()
    override val invertedIndex = mutableMapOf<String, MutableSet<String>>()

    init {
        readLemmas()
    }

    override fun buildInvertedIndex() {
        pageLemmas.forEach { (pageName, lemmasMap) ->
            lemmasMap.keys.forEach { lemma ->
                invertedIndex.getOrPut(lemma) { mutableSetOf() }.add(pageName)
            }
        }
    }

    override fun writeInvertedIndex() {
        val outputFile = File(INVERTED_INDEX_PATH)
        val content = invertedIndex.entries.joinToString("\n") { (lemma, docs) ->
            "$lemma: ${docs.joinToString(", ")}"
        }
        outputFile.parentFile?.mkdirs()
        outputFile.writeText(content)
    }

    private fun readLemmas() {
        val lemmasDir = File(LEMMAS_OUTPUT_PATH)

        lemmasDir.listFiles()?.forEach { file ->
            val pageName = file.name
                .removePrefix("леммы_")
                .removeSuffix(".txt")
                .plus(".html")

            val lemmas = mutableMapOf<String, MutableSet<String>>()

            file.readLines().forEach { line ->
                val (lemma, tokens) = line.split(": ")
                lemmas[lemma.trim()] = tokens.split(" ").toMutableSet()
            }

            pageLemmas[pageName] = lemmas
        }
    }

    private companion object {

        const val LEMMAS_OUTPUT_PATH = "output/lemmas"
        const val INVERTED_INDEX_PATH = "output/inverted_index.txt"
    }
}