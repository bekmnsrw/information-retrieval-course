package inverted_index

import inverted_index.Command.*
import inverted_index.Operator.*

/**
 * @author i.bekmansurov
 */
internal interface BooleanSearch {

    fun search(query: String): Set<String>
}

internal class BooleanSearchImpl(
    private val pageLemmas: MutableMap<String, MutableMap<String, MutableSet<String>>>,
    private val invertedIndex: MutableMap<String, MutableSet<String>>,
) : BooleanSearch {

    private val tokenToLemmas: Map<String, Set<String>> by lazy {
        val map = mutableMapOf<String, MutableSet<String>>()
        pageLemmas.values.forEach { lemmasMap ->
            lemmasMap.forEach { (lemma, tokens) ->
                tokens.forEach { token ->
                    map.getOrPut(token) { mutableSetOf() }.add(lemma)
                }
            }
        }
        map.mapKeys { it.key.lowercase() }
    }

    override fun search(query: String): Set<String> {
        val tokens = parseQuery(query)
        val expandedTokens = expandTokensToLemmas(tokens)
        val rpn = convertToRPN(expandedTokens)
        return evaluateRPN(rpn)
    }

    private fun parseQuery(query: String): List<String> {
        val regex = Regex("""(\b(${AND.value}|${OR.value}|${NOT.value})\b|[${L_BR.value}${R_BR.value}]|[^\s${L_BR.value}${R_BR.value}]+)""")
        return regex.findAll(query)
            .map { it.value.trim().uppercase() }
            .toList()
    }

    private fun expandTokensToLemmas(tokens: List<String>): List<String> {
        return tokens.flatMap { token ->
            when (token.uppercase()) {
                AND.value, OR.value, NOT.value -> listOf(token.uppercase())
                L_BR.value, R_BR.value -> listOf(token.uppercase())
                else -> {
                    val lemmas = tokenToLemmas[token.lowercase()] ?: emptySet()
                    lemmas.takeIf { it.isNotEmpty() }?.toList() ?: listOf(token.lowercase())
                }
            }
        }
    }

    /**
     * Пример преобразования для запроса "A AND (B OR NOT C)"
     *
     * Шаги обработки:
     *   1. Входные токены: ["A", "AND", "(", "B", "OR", "NOT", "C", ")"]
     *   2. Процесс конвертации:
     *     Токен | Действие                | Стек              | Выход
     *     -----------------------------------------------------------------------------
     *     A     | Пушим в выход           | []                | [A]
     *     AND   | Пушим в стек            | [AND]             | [A]
     *     (     | Пушим в стек            | [AND, (]          | [A]
     *     B     | Пушим в выход           | [AND, (]          | [A, B]
     *     OR    | Пушим в стек            | [AND, (, OR]      | [A, B]
     *     NOT   | Пушим в стек            | [AND, (, OR, NOT] | [A, B]
     *     C     | Пушим в выход           | [AND, (, OR, NOT] | [A, B, C]
     *     )     | Выталкиваем до (        | [AND]             | [A, B, C, NOT, OR]
     *     Конец | Выталкиваем остаток     | []                | [A, B, C, NOT, OR, AND]
     *
     *   3. Результат RPN: [A, B, C, NOT, OR, AND]
     */
    private fun convertToRPN(tokens: List<String>): List<String> {
        val output = mutableListOf<String>()
        val stack = ArrayDeque<String>()
        val precedence = mapOf(NOT.value to 3, AND.value to 2, OR.value to 1)

        // Обработка каждого токена в запросе
        tokens.forEach { token ->
            when (token) {
                L_BR.value -> {
                    // Помещаем в стек для последующей обработки группы
                    stack.addFirst(token)
                }

                R_BR.value -> {
                    // Выталкиваем операторы из стека в output, пока не встретим '('
                    while (stack.isNotEmpty() && stack.first() != L_BR.value) {
                        output.add(stack.removeFirst())
                    }
                    // Удаляем саму '(' из стека
                    stack.removeFirst()
                }

                AND.value, OR.value, NOT.value -> {
                    // Выталкиваем из стека операторы с более высоким или равным приоритетом
                    while (
                        stack.isNotEmpty() &&
                        stack.first() != L_BR.value && // Останавливаемся на '('
                        requireNotNull(precedence[token]) <= requireNotNull(precedence[stack.first()]) // Сравниваем приоритеты
                    ) {
                        output.add(stack.removeFirst())
                    }
                    // Помещаем текущий оператор в стек
                    stack.addFirst(token)
                }

                else -> {
                    // Сразу добавляем в результат
                    output.add(token.lowercase())
                }
            }
        }

        while (stack.isNotEmpty()) {
            output.add(stack.removeFirst())
        }

        return output
    }

    /**
     * Пример вычисления для RPN-выражения [A, B, C, NOT, OR, AND]
     *
     * Данные:
     * - A → [doc1, doc3]
     * - B → [doc1, doc2]
     * - C → [doc3, doc4]
     * - Все документы: [doc1, doc2, doc3, doc4]
     *
     * Шаги вычисления:
     *   | Токен | Действие              | Стек (множества документов)
     *   |-------|-----------------------|---------------------------------------------
     *   | A     | Добавить A            | [ [doc1, doc3] ]
     *   | B     | Добавить B            | [ [doc1, doc3], [doc1, doc2] ]
     *   | C     | Добавить C            | [ [doc1, doc3], [doc1, doc2], [doc3, doc4] ]
     *   | NOT   | NOT C = все - C       | [ [doc1, doc3], [doc1, doc2], [doc1, doc2] ]
     *   | OR    | B OR (NOT C)          | [ [doc1, doc3], [doc1, doc2 ∪ doc1, doc2] ]
     *   |       |                       | → [ [doc1, doc3], [doc1, doc2] ]
     *   | AND   | A AND (B OR NOT C)    | [ [doc1, doc3] ∩ [doc1, doc2] ] → [doc1]
     *
     * Результат: [doc1]
     */
    private fun evaluateRPN(rpn: List<String>): Set<String> {
        val stack = ArrayDeque<Set<String>>()

        rpn.forEach { token ->
            when (token) {
                AND.value -> {
                    val right = stack.removeFirst()
                    val left = stack.removeFirst()
                    stack.addFirst(left intersect right)
                }
                OR.value -> {
                    val right = stack.removeFirst()
                    val left = stack.removeFirst()
                    stack.addFirst(left union right)
                }
                NOT.value -> {
                    val operand = stack.removeFirst()
                    stack.addFirst(getAllDocuments() - operand)
                }
                else -> {
                    stack.addFirst(invertedIndex[token] ?: emptySet())
                }
            }
        }

        return stack.firstOrNull() ?: emptySet()
    }

    private fun getAllDocuments(): Set<String> {
        return pageLemmas.keys.toSet()
    }
}
