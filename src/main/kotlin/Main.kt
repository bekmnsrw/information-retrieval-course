import crawler.Crawler
import crawler.CrawlerImpl
import kotlinx.coroutines.runBlocking
import tokenizer.Tokenizer
import tokenizer.TokenizerImpl

/**
 * @author i.bekmansurov
 */
fun main() = runBlocking {

    /**
     * Replace with appropriate task number from [Task.entries] and run [main] function
     */
    val task = Task._2

    when (task) {
        Task._1 -> {
            val crawler: Crawler = CrawlerImpl()
            crawler.loadWebPages()
        }
        Task._2 -> {
            val tokenizer: Tokenizer = TokenizerImpl()
            tokenizer.writeTokens()
            tokenizer.writeLemmas()
        }
        Task._3 -> {}
        Task._4 -> {}
        Task._5 -> {}
        Task._6 -> {}
    }
}

internal enum class Task(val taskName: String) {

    _1("Crawler"),
    _2("Tokenizer"),
    _3(""),
    _4(""),
    _5(""),
    _6(""),
}
