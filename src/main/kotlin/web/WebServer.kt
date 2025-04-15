package web

import io.ktor.application.*
import io.ktor.features.ContentNegotiation
import io.ktor.html.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.html.*
import vector.VectorSearch

/**
 * @author i.bekmansurov
 */
internal interface WebServer {

    fun start()
}

internal class WebServerImpl(
    private val vectorSearch: VectorSearch,
) : WebServer {

    override fun start() {
        embeddedServer(Netty, port = PORT) {
            install(ContentNegotiation)

            routing {
                get(HOME_PAGE) {
                    call.respondHtml {
                        head {
                            title { +HOME_PAGE_TILE }
                            link(href = CSS_FILE_PATH, rel = REL, type = CSS_TYPE)
                        }
                        body {
                            div(classes = CONTAINER_CLASS) {
                                h1 { +HEADING_HOME_PAGE }
                                form(action = SEARCH_PAGE, method = FormMethod.get) {
                                    input(name = QUERY_PARAM, type = InputType.text) {
                                        attributes[PLACEHOLDER_CLASS] = PLACEHOLDER_TEXT
                                    }
                                    button(type = ButtonType.submit) { +BUTTON_TEXT_SEARCH }
                                }
                            }
                        }
                    }
                }

                get(SEARCH_PAGE) {
                    val query = call.parameters[QUERY_PARAM].orEmpty()
                    if (query.isBlank()) {
                        call.respondText(text = ERROR_EMPTY_QUERY, status = HttpStatusCode.BadRequest)
                        return@get
                    }

                    val results = vectorSearch.search(query)

                    call.respondHtml {
                        head {
                            title { +"$query - $SEARCH_PAGE_TITLE" }
                            link(href = CSS_FILE_PATH, rel = REL, type = CSS_TYPE)
                        }
                        body {
                            div(classes = CONTAINER_CLASS) {
                                h1 { +"$HEADING_SEARCH_PAGE $query" }
                                if (results.isEmpty()) {
                                    p { +MESSAGE_NO_RESULTS }
                                } else {
                                    ul {
                                        results.forEach { result ->
                                            li {
                                                a(href = result.url) { +result.url }
                                                +formatSimilarity(result.cosineSimilarity)
                                            }
                                        }
                                    }
                                }
                                p {
                                    a(href = HOME_PAGE) { +LINK_TEXT_BACK_TO_SEARCH }
                                }
                            }
                        }
                    }
                }

                static(CSS_RESOURCES_DIR) {
                    resources(CSS_RESOURCES)
                }
            }
        }.start(wait = true)
    }

    private companion object {

        const val PORT = 8080

        const val HOME_PAGE = "/"
        const val SEARCH_PAGE = "/search"

        const val CSS_RESOURCES_DIR = "/css"
        const val CSS_RESOURCES = "css"
        const val CSS_FILE_PATH = "/css/styles.css"
        const val CSS_TYPE = "text/css"
        const val REL = "stylesheet"

        const val HOME_PAGE_TILE = "Google"
        const val SEARCH_PAGE_TITLE = "Поиск в Google"
        const val HEADING_HOME_PAGE = "Google"
        const val HEADING_SEARCH_PAGE = "Результаты поиска для запроса:"
        const val PLACEHOLDER_TEXT = "Введите запрос"
        const val BUTTON_TEXT_SEARCH = "Найти"
        const val ERROR_EMPTY_QUERY = "Запрос не может быть пустым"
        const val MESSAGE_NO_RESULTS = "Ничего не найдено"
        const val LINK_TEXT_BACK_TO_SEARCH = "Вернуться к поиску"

        const val QUERY_PARAM = "query"

        const val CONTAINER_CLASS = "container"
        const val PLACEHOLDER_CLASS = "placeholder"

        private fun formatSimilarity(similarity: Double): String = " (${"%.4f".format(similarity)})"
    }
}
