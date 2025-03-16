package tokenizer

import com.github.demidko.aot.WordformMeaning.lookupForMeanings
import org.jsoup.Jsoup
import java.io.File

/**
 * @author i.bekmansurov
 */
internal interface Tokenizer {

    fun writeTokens()

    fun writeLemmas()
}

internal class TokenizerImpl : Tokenizer {

    private val stopWords = getStopWords()
    private val _tokens = mutableSetOf<String>()
    private val _lemmas = mutableMapOf<String, MutableSet<String>>()

    override fun writeTokens() {
        File(WEB_PAGES_PATH).listFiles()?.forEach { file ->
            val html = file.readText()
            val text = Jsoup.parse(html).text()
            val tokens = tokenizeAndCleanText(text)
            _tokens.addAll(tokens)
        }
        writeListOfTokens()
    }

    override fun writeLemmas() {
        _tokens.forEach { token ->
            _lemmas.getOrPut(lemmatizeToken(token)) { mutableSetOf() }.add(token)
        }
        writeListOfLemmas()
    }

    private fun tokenizeAndCleanText(text: String): List<String> {
        return text.split("\\s+".toRegex())
            .asSequence()
            .map { word -> word.replace("[^а-яА-Я]".toRegex(), EMPTY_STRING) }
            .filter { word -> word.isNotBlank() }
            .filter { word -> word.length > 2 }
            .map { word -> word.lowercase() }
            .filter { word -> word !in stopWords }
            .toList()
    }

    private fun writeListOfTokens() {
        val tokens = _tokens.joinToString(NEWLINE)
        writeToFile(TOKENS_OUTPUT_PATH, tokens)
    }

    private fun writeListOfLemmas() {
        val lemmas = _lemmas.entries.joinToString(NEWLINE) { (lemma, tokens) ->
            "$lemma: ${tokens.joinToString(SPACE)}"
        }
        writeToFile(LEMMAS_OUTPUT_PATH, lemmas)
    }

    private fun lemmatizeToken(token: String): String {
        return lookupForMeanings(token)
            .map { meaning -> meaning.lemma.toString() }
            .firstOrNull() ?: token
    }

    private fun getStopWords(): Set<String> {
        return File(STOPWORDS_FILE_PATH).readLines()
            .map { word -> word.trim().lowercase() }
            .filter { word -> word.isNotBlank() }
            .toSet()
    }

    private fun writeToFile(filePath: String, text: String) {
        File(filePath).apply {
            if (exists()) {
                writeText("")
            } else {
                parentFile?.mkdirs()
                createNewFile()
            }
            writeText(text)
        }
    }

    private companion object {

        const val STOPWORDS_FILE_PATH = "src/main/kotlin/tokenizer/stopwords.txt"
        const val LEMMAS_OUTPUT_PATH = "output/lemmas.txt"
        const val TOKENS_OUTPUT_PATH = "output/tokens.txt"
        const val WEB_PAGES_PATH = "output/pages"

        const val EMPTY_STRING = ""
        const val NEWLINE = "\n"
        const val SPACE = " "
    }
}
