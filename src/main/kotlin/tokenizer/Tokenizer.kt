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
    private val typos = getTypos()
    private val pageTokens = mutableMapOf<String, MutableSet<String>>()
    private val pageLemmas = mutableMapOf<String, MutableMap<String, MutableSet<String>>>()

    override fun writeTokens() {
        File(WEB_PAGES_PATH).listFiles()?.forEach { file ->
            val html = file.readText()
            val text = Jsoup.parse(html).text()
            val tokens = tokenizeAndCleanText(text)
            pageTokens[file.name] = tokens.toMutableSet()
        }
        writeListOfTokens()
    }

    override fun writeLemmas() {
        pageTokens.forEach { (pageName, tokens) ->
            val lemmas = mutableMapOf<String, MutableSet<String>>()
            tokens.forEach { token ->
                lemmas.getOrPut(lemmatizeToken(token)) { mutableSetOf() }.add(token)
            }
            pageLemmas[pageName] = lemmas
        }
        writeListOfLemmas()
    }

    private fun tokenizeAndCleanText(text: String): List<String> {
        return text.split("\\s+".toRegex())
            .asSequence()
            .map { cleanWord(it) }
            .filter { isValidWord(it) }
            .toList()
    }

    private fun cleanWord(word: String): String {
        return word.replace("[^а-яА-Я-]".toRegex(), EMPTY_STRING)
            .lowercase()
    }

    private fun isValidWord(word: String): Boolean {
        return word.isNotBlank() && word.length > 2 && word !in stopWords && word !in typos
    }

    private fun writeListOfTokens() {
        pageTokens.forEach { (pageName, tokens) ->
            val outputFilePath = "$TOKENS_OUTPUT_PATH/токены_${pageName.replaceHtmlExtension()}"
            val tokensText = tokens.joinToString(NEWLINE)
            writeToFile(outputFilePath, tokensText)
        }
    }

    private fun writeListOfLemmas() {
        pageLemmas.forEach { (pageName, lemmas) ->
            val outputFilePath = "$LEMMAS_OUTPUT_PATH/леммы_${pageName.replaceHtmlExtension()}"
            val lemmasText = lemmas.entries.joinToString(NEWLINE) { (lemma, tokens) ->
                "$lemma: ${tokens.joinToString(SPACE)}"
            }
            writeToFile(outputFilePath, lemmasText)
        }
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

    private fun getTypos(): Set<String> {
        return File(TYPOS_FILE_PATH).readLines()
            .map { word -> word.trim().lowercase() }
            .filter { word -> word.isNotBlank() }
            .toSet()
    }

    private fun writeToFile(filePath: String, text: String) {
        File(filePath).apply {
            if (exists()) {
                writeText(EMPTY_STRING)
            } else {
                parentFile?.mkdirs()
                createNewFile()
            }
            writeText(text)
        }
    }

    private fun String.replaceHtmlExtension(): String {
        return this.replace(".html", ".txt")
    }

    private companion object {

        const val STOPWORDS_FILE_PATH = "src/main/kotlin/tokenizer/stopwords.txt"
        const val TYPOS_FILE_PATH = "src/main/kotlin/tokenizer/typos.txt"
        const val LEMMAS_OUTPUT_PATH = "output/lemmas"
        const val TOKENS_OUTPUT_PATH = "output/tokens"
        const val WEB_PAGES_PATH = "output/pages"

        const val EMPTY_STRING = ""
        const val NEWLINE = "\n"
        const val SPACE = " "
    }
}
