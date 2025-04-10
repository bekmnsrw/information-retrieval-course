import crawler.Crawler
import crawler.CrawlerImpl
import inverted_index.BooleanSearch
import inverted_index.BooleanSearchImpl
import inverted_index.InvertedIndex
import inverted_index.InvertedIndexImpl
import kotlinx.coroutines.runBlocking
import tf_idf.TfIdfCalculator
import tf_idf.TfIdfCalculatorImpl
import tokenizer.Tokenizer
import tokenizer.TokenizerImpl

/**
 * @author i.bekmansurov
 */
fun main() = runBlocking {

    /**
     * Replace with appropriate task number from [Task.entries] and run [main] function
     */
    val task = Task._4

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
        Task._3 -> {
            val invertedIndex: InvertedIndex = InvertedIndexImpl()
            invertedIndex.buildInvertedIndex()
            invertedIndex.writeInvertedIndex()

            val booleanSearch: BooleanSearch = BooleanSearchImpl(
                pageLemmas = invertedIndex.pageLemmas,
                invertedIndex = invertedIndex.invertedIndex,
            )

            while (true) {
                println("\nВведите поисковый запрос (или 'exit' для выхода):")
                val query = readlnOrNull()?.trim().orEmpty()

                if (query.equals("exit", ignoreCase = true)) {
                    println("Завершение работы поисковой системы")
                    break
                }

                if (query.isBlank()) {
                    println("Ошибка: пустой запрос")
                    continue
                }

                try {
                    val result = booleanSearch.search(query)
                    if (result.isEmpty()) {
                        println("По запросу '$query' ничего не найдено")
                    } else {
                        println("Найдено ${result.size} документов:")
                        result.forEachIndexed { index, fileName ->
                            println("${index + 1}. $fileName")
                        }
                    }
                } catch (e: Exception) {
                    println("Ошибка при обработке запроса: ${e.message}")
                }
            }
        }
        Task._4 -> {
            val tfIdfCalculator: TfIdfCalculator = TfIdfCalculatorImpl(
                /** Uncomment for testing **/
                // pagesPath = Utils.WEB_PAGES_TEST_PATH,
                // outputPrefix = Utils.OUTPUT_TEST_PREFIX,
            )
            tfIdfCalculator.calculateTfIdf()
        }
        Task._5 -> {}
        Task._6 -> {}
    }
}

internal enum class Task {

    _1, // Crawler
    _2, // Tokenizer
    _3, // Inverted Index
    _4, // TF-IDF
    _5, //
    _6, //
}
