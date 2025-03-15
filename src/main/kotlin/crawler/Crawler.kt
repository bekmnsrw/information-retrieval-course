package crawler

import org.jsoup.Jsoup
import java.io.File
import kotlinx.coroutines.*
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream

/**
 * @author i.bekmansurov
 */
internal interface Crawler {

    fun loadWebPages()
}

internal class CrawlerImpl : Crawler {

    override fun loadWebPages() {
        val outputDir = File(OUTPUT_DIR_NAME).apply { if (!exists()) mkdir() }
        val pagesDir = File(outputDir, PAGES_DIR_NAME).apply { if (!exists()) mkdir() }
        val indexFile = File(outputDir, INDEX_FILE_NAME).apply { writeText(EMPTY_STRING) }

        runBlocking {
            val coroutineContext = Dispatchers.IO + SupervisorJob()
            val coroutineScope = CoroutineScope(coroutineContext)

            coroutineScope.launch {
                WebPages.URLS
                    .mapIndexed { index, url ->
                        launch {
                            loadWebPageWithRetry(url, index + 1, pagesDir, indexFile)
                        }
                    }
                    .joinAll()
            }.apply {
                invokeOnCompletion {
                    println("Все страницы скачаны")
                    val archiveFile = File(outputDir, ZIP_ARCHIVE_NAME)
                    createZipArchive(pagesDir, archiveFile)
                    println("Архив создан: ${archiveFile.absolutePath}")
                }
                join()
            }
        }
    }

    private suspend fun loadWebPageWithRetry(
        url: String,
        index: Int,
        pagesDir: File,
        indexFile: File,
        maxRetries: Int = 3,
        retryDelay: Long = 1000,
    ) {
        var retryCount = 0
        while (retryCount < maxRetries) {
            runCatching {
                val doc = Jsoup.connect(url).get()
                val fileName = buildPageFileName(index)
                File(pagesDir, fileName).apply {
                    writeText(doc.html())
                    indexFile.appendText("$index $url\n")
                }
            }
                .onSuccess { file ->
                    println("Скачано: $url -> ${file.name}")
                    return
            }
                .onFailure { error ->
                    retryCount++
                    println("Ошибка при скачивании $url (попытка $retryCount из $maxRetries): ${error.message}")
                    if (retryCount < maxRetries) {
                        delay(retryDelay)
                    }
            }
        }

        println("Не удалось скачать $url после $maxRetries попыток")
    }

    private fun buildPageFileName(index: Int): String {
        /**
         * Example output: выкачка_1.html
         */
        return PAGE_FILE_NAME_PREFIX + index + HTML_SUFFIX
    }

    private fun createZipArchive(sourceDir: File, zipFile: File) {
        ZipArchiveOutputStream(zipFile.outputStream()).use { zipOutput ->
            sourceDir.walkTopDown()
                .filter { it.isFile }
                .forEach { file ->
                    val entry = ZipArchiveEntry(file, file.relativeTo(sourceDir).path)
                    zipOutput.putArchiveEntry(entry)
                    file.inputStream().use { it.copyTo(zipOutput) }
                    zipOutput.closeArchiveEntry()
                }
        }
    }

    private companion object {

        const val OUTPUT_DIR_NAME = "output"
        const val PAGES_DIR_NAME = "pages"

        const val EMPTY_STRING = ""

        const val INDEX_FILE_NAME = "index.txt"
        const val ZIP_ARCHIVE_NAME = "pages.zip"

        const val PAGE_FILE_NAME_PREFIX = "выкачка_"
        const val HTML_SUFFIX = ".html"
    }
}