package tokenizer

import Utils.WEB_PAGES_PATH
import Utils.lemmatize
import Utils.replaceHtmlExtension
import Utils.tokenizeAndCleanText
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
                lemmas.getOrPut(lemmatize(token)) { mutableSetOf() }.add(token)
            }
            pageLemmas[pageName] = lemmas
        }
        writeListOfLemmas()
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

    private companion object {

        const val LEMMAS_OUTPUT_PATH = "output/lemmas"
        const val TOKENS_OUTPUT_PATH = "output/tokens"

        const val EMPTY_STRING = ""
        const val NEWLINE = "\n"
        const val SPACE = " "
    }
}
