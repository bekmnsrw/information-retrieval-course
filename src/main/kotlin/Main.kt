import crawler.CrawlerImpl
import kotlinx.coroutines.runBlocking

/**
 * @author i.bekmansurov
 */
fun main() = runBlocking {
    val crawler = CrawlerImpl()
    crawler.loadWebPages()
}