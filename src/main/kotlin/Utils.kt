import com.github.demidko.aot.WordformMeaning.lookupForMeanings
import java.io.File

/**
 * @author i.bekmansurov
 */
internal object Utils {

    const val WEB_PAGES_PATH = "output/pages"
    const val WEB_PAGES_TEST_PATH = "output/test/pages"
    const val OUTPUT_PREFIX = "output"
    const val OUTPUT_TEST_PREFIX = "output/test"

    fun String.replaceHtmlExtension(): String {
        return this.replace(HTML, TXT)
    }

    fun lemmatize(token: String): String {
        return lookupForMeanings(token)
            .map { meaning -> meaning.lemma.toString() }
            .firstOrNull() ?: token
    }

    fun tokenizeAndCleanText(text: String): List<String> {
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
        return word.isNotBlank() &&
                word.length > 2 &&
                word !in getStopWords() &&
                word !in getTypos()
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

    private const val STOPWORDS_FILE_PATH = "src/main/kotlin/tokenizer/stopwords.txt"
    private const val TYPOS_FILE_PATH = "src/main/kotlin/tokenizer/typos.txt"

    private const val HTML = ".html"
    private const val TXT = ".txt"
    private const val EMPTY_STRING = ""
}