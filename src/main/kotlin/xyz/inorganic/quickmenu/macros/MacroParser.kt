package xyz.inorganic.quickmenu.macros

sealed class MacroStatement {
    abstract val line: Int
}

class MacroCmdStmt(override val line: Int, val text: String) : MacroStatement()
class MacroChatStmt(override val line: Int, val text: String) : MacroStatement()
class MacroKeyStmt(override val line: Int, val translationKey: String) : MacroStatement()
class MacroRunStmt(override val line: Int, val name: String) : MacroStatement()
class MacroSetStmt(override val line: Int, val name: String, val value: String) : MacroStatement()
class MacroWaitStmt(override val line: Int, val ticks: Long) : MacroStatement()
class MacroBreakStmt(override val line: Int) : MacroStatement()
class MacroExitStmt(override val line: Int) : MacroStatement()
class MacroLabelStmt(override val line: Int, val name: String) : MacroStatement()
class MacroJumpStmt(override val line: Int, val target: String) : MacroStatement()

sealed class MacroCondStmt(override val line: Int, val condition: MacroCondition?) : MacroStatement()

class MacroIfStmt(line: Int, condition: MacroCondition?) : MacroCondStmt(line, condition)
class MacroElifStmt(line: Int, condition: MacroCondition?) : MacroCondStmt(line, condition)
class MacroElseStmt(line: Int) : MacroCondStmt(line, null)
class MacroEndifStmt(override val line: Int) : MacroStatement()

class MacroLoopStmt(override val line: Int, val times: Long?) : MacroStatement()
class MacroEndLoopStmt(override val line: Int) : MacroStatement()

sealed interface MacroCondition

class KeyCondition(val translationKey: String) : MacroCondition
class ItemCondition(val registryId: String) : MacroCondition
class CompareCondition(val variable: String, val operator: String, val value: Double) : MacroCondition
class NotCondition(val inner: MacroCondition) : MacroCondition
class AndCondition(val left: MacroCondition, val right: MacroCondition) : MacroCondition
class OrCondition(val left: MacroCondition, val right: MacroCondition) : MacroCondition

class MacroError(val line: Int, val message: String) {
    fun displayString(): String = "行 $line: $message"
}

class MacroProgram(
    val statements: List<MacroStatement>,
    val nextCond: IntArray,
    val advanceTo: IntArray,
    val loopEnds: IntArray,
    val loopStartOfEnd: IntArray,
    val innerLoopStart: IntArray,
    val jumpTargets: IntArray
) {
    val count: Int = statements.size
}

object MacroParser {

    fun parse(script: String): Pair<MacroProgram?, List<MacroError>> {
        val errors = mutableListOf<MacroError>()
        val statements = mutableListOf<MacroStatement>()

        val loopStack = mutableListOf<Int>()
        val ifHeadStack = mutableListOf<Int>()
        val chainConds = mutableMapOf<Int, MutableList<Int>>()
        val chainEndifs = mutableMapOf<Int, Int>()
        val labels = mutableMapOf<String, Int>()

        val lines = script.split("\n")

        lines.forEachIndexed { index, rawLine ->
            val line = index + 1
            val trimmed = stripComment(rawLine).trim()
            if (trimmed.isEmpty()) return@forEachIndexed

            val keyword = trimmed.token().lowercase()

            fun add(stmt: MacroStatement) = statements.add(stmt)

            when (keyword) {
                "cmd" -> add(MacroCmdStmt(line, trimmed.rest()))
                "chat" -> add(MacroChatStmt(line, trimmed.rest()))
                "key" -> {
                    val rest = trimmed.rest()
                    if (rest.isEmpty()) errors.add(MacroError(line, "キーバインド名がありません"))
                    else add(MacroKeyStmt(line, rest))
                }
                "run" -> {
                    val rest = trimmed.rest()
                    if (rest.isEmpty()) errors.add(MacroError(line, "マクロ名がありません"))
                    else add(MacroRunStmt(line, rest))
                }
                "set" -> {
                    val parts = trimmed.substringAfter("set").trim().split(Regex("\\s+"), limit = 2)
                    if (parts.size < 2) {
                        errors.add(MacroError(line, "set は「set <名前> <値>」の形式で指定してください"))
                    } else if (!parts[0].matches(Regex("[A-Za-z_][A-Za-z0-9_]*"))) {
                        errors.add(MacroError(line, "変数名が正しくありません: ${parts[0]}"))
                    } else {
                        add(MacroSetStmt(line, parts[0], parts[1]))
                    }
                }
                "wait" -> {
                    val ticks = parseWaitTicks(trimmed.rest())
                    if (ticks == null) errors.add(MacroError(line, "wait 時間の形式が正しくありません"))
                    else add(MacroWaitStmt(line, ticks))
                }
                "loop" -> {
                    val rest = trimmed.rest()
                    var times: Long? = null
                    if (rest.isNotEmpty()) {
                        val parsed = rest.toLongOrNull()
                        if (parsed == null) {
                            errors.add(MacroError(line, "ループ回数は数値で指定してください: $rest"))
                        } else {
                            times = parsed
                        }
                    }
                    add(MacroLoopStmt(line, times))
                    loopStack.add(statements.size - 1)
                }
                "endloop" -> {
                    if (loopStack.isEmpty()) {
                        errors.add(MacroError(line, "対応する loop がありません"))
                    } else {
                        loopStack.removeAt(loopStack.size - 1)
                    }
                    add(MacroEndLoopStmt(line))
                }
                "end" -> {
                    if (loopStack.isEmpty()) {
                        errors.add(MacroError(line, "対応する loop がありません"))
                    } else {
                        loopStack.removeAt(loopStack.size - 1)
                    }
                    add(MacroEndLoopStmt(line))
                }
                "if" -> {
                    val cond = parseCondition(trimmed.rest(), errors, line)
                    add(MacroIfStmt(line, cond))
                    chainConds[statements.size - 1] = mutableListOf(statements.size - 1)
                    ifHeadStack.add(statements.size - 1)
                }
                "elif" -> {
                    if (ifHeadStack.isEmpty()) {
                        errors.add(MacroError(line, "対応する if がありません"))
                    } else {
                        val cond = parseCondition(trimmed.rest(), errors, line)
                        add(MacroElifStmt(line, cond))
                        chainConds[ifHeadStack.last()]?.add(statements.size - 1)
                    }
                }
                "else" -> {
                    if (ifHeadStack.isEmpty()) {
                        errors.add(MacroError(line, "対応する if がありません"))
                    } else {
                        add(MacroElseStmt(line))
                        chainConds[ifHeadStack.last()]?.add(statements.size - 1)
                    }
                }
                "endif" -> {
                    if (ifHeadStack.isEmpty()) {
                        errors.add(MacroError(line, "対応する if がありません"))
                    } else {
                        val head = ifHeadStack.removeAt(ifHeadStack.size - 1)
                        chainEndifs[head] = statements.size
                        add(MacroEndifStmt(line))
                    }
                }
                "break" -> {
                    if (loopStack.isEmpty()) {
                        errors.add(MacroError(line, "break はループの外では使えません"))
                    }
                    add(MacroBreakStmt(line))
                }
                "exit" -> add(MacroExitStmt(line))
                "label:" -> {
                    val name = trimmed.substringAfter("label:").trim()
                    if (name.isEmpty()) {
                        errors.add(MacroError(line, "ラベル名がありません"))
                    } else {
                        labels[name] = statements.size
                        add(MacroLabelStmt(line, name))
                    }
                }
                "jump" -> {
                    val rest = trimmed.rest()
                    if (rest.isEmpty()) {
                        errors.add(MacroError(line, "ジャンプ先のラベル名がありません"))
                    } else {
                        add(MacroJumpStmt(line, rest))
                    }
                }
                else -> errors.add(MacroError(line, "不明なコマンド: $keyword"))
            }
        }

        if (loopStack.isNotEmpty()) {
            errors.add(MacroError(lines.size, "閉じられていない loop があります"))
        }
        if (ifHeadStack.isNotEmpty()) {
            errors.add(MacroError(lines.size, "閉じられていない if があります"))
        }

        if (errors.isNotEmpty()) return null to errors

        val n = statements.size
        val nextCond = IntArray(n) { -1 }
        val advanceTo = IntArray(n) { it + 1 }
        val loopEnds = IntArray(n) { -1 }
        val loopStartOfEnd = IntArray(n) { -1 }
        val innerLoopStart = IntArray(n) { -1 }
        val jumpTargets = IntArray(n) { -1 }

        val loopStack2 = mutableListOf<Int>()
        statements.indices.forEach { i ->
            when (statements[i]) {
                is MacroLoopStmt -> loopStack2.add(i)
                is MacroEndLoopStmt -> {
                    if (loopStack2.isNotEmpty()) {
                        val start = loopStack2.removeAt(loopStack2.size - 1)
                        loopEnds[start] = i
                        loopStartOfEnd[i] = start
                    }
                }
                else -> {}
            }
            if (loopStack2.isNotEmpty()) {
                innerLoopStart[i] = loopStack2.last()
            }
        }

        chainConds.forEach { (head, conds) ->
            val endifIdx = chainEndifs[head]!!
            for (i in conds.indices) {
                val condIdx = conds[i]
                val next = if (i + 1 < conds.size) conds[i + 1] else endifIdx
                nextCond[condIdx] = next
                val bodyTail = next - 1
                if (bodyTail != condIdx) {
                    advanceTo[bodyTail] = endifIdx + 1
                }
            }
        }

        val errorCountBeforeJumps = errors.size
        statements.indices.forEach { index ->
            if (statements[index] is MacroJumpStmt) {
                val target = (statements[index] as MacroJumpStmt).target
                val labelIdx = labels[target]
                if (labelIdx != null) {
                    jumpTargets[index] = labelIdx
                } else {
                    errors.add(MacroError(statements[index].line, "ラベルが見つかりません: $target"))
                }
            }
        }

        if (errors.size != errorCountBeforeJumps) return null to errors

        return MacroProgram(
            statements, nextCond, advanceTo, loopEnds, loopStartOfEnd, innerLoopStart, jumpTargets
        ) to errors
    }

    fun previewLine(script: String): String {
        return script.lines().firstOrNull {
            val t = stripComment(it).trim()
            t.isNotEmpty() && !t.token().equals("label:", ignoreCase = true)
        }?.trim() ?: "(空のマクロ)"
    }

    fun stripComment(line: String): String {
        val hash = line.indexOf('#')
        return if (hash >= 0) line.substring(0, hash) else line
    }

    private fun String.token(): String {
        val idx = indexOf(' ')
        return if (idx == -1) this else substring(0, idx)
    }

    private fun String.rest(): String {
        val idx = indexOf(' ')
        return if (idx == -1) "" else substring(idx + 1).trim()
    }

    private fun parseWaitTicks(text: String): Long? {
        val t = text.trim()
        if (t.isEmpty()) return null
        val numberStr = t.dropLast(1)
        val number = numberStr.toDoubleOrNull() ?: return null
        return when {
            t.endsWith("t") -> number.toLong().coerceAtLeast(0)
            t.endsWith("s") -> (number * 20).toLong().coerceAtLeast(0)
            else -> null
        }
    }

    private fun parseCondition(text: String, errors: MutableList<MacroError>, line: Int): MacroCondition? {
        val cond = ConditionParser(text).parseCondition()
        if (cond == null) {
            errors.add(MacroError(line, "条件の解析に失敗しました: ${text.trim()}"))
        }
        return cond
    }
}

private data class CondToken(val text: String, val isNumber: Boolean)

private class ConditionParser(private val text: String) {

    private val tokens = tokenize(text)
    private var pos = 0

    fun parseCondition(): MacroCondition? {
        val cond = parseOr() ?: return null
        return if (pos == tokens.size) cond else null
    }

    private fun parseOr(): MacroCondition? {
        var left = parseAnd() ?: return null
        while (pos < tokens.size && tokens[pos].text == "||") {
            pos++
            val right = parseAnd() ?: return null
            left = OrCondition(left, right)
        }
        return left
    }

    private fun parseAnd(): MacroCondition? {
        var left = parseNot()
        while (pos < tokens.size && tokens[pos].text == "&&") {
            pos++
            val right = parseNot()
            if (left != null && right != null) {
                left = AndCondition(left, right)
            } else {
                return null
            }
        }
        return left
    }

    private fun parseNot(): MacroCondition? {
        if (pos < tokens.size && tokens[pos].text == "!") {
            pos++
            val inner = parseNot() ?: return null
            return NotCondition(inner)
        }
        return parsePrimary()
    }

    private fun parsePrimary(): MacroCondition? {
        if (pos >= tokens.size) return null

        if (tokens[pos].text == "(") {
            pos++
            val inner = parseOr() ?: return null
            if (pos >= tokens.size || tokens[pos].text != ")") return null
            pos++
            return inner
        }

        val first = tokens[pos].text

        if (first.startsWith("key:")) {
            pos++
            return KeyCondition(first.substring(4))
        }
        if (first.startsWith("item:")) {
            pos++
            return ItemCondition(first.substring(5))
        }

        val opToken = tokens.getOrNull(pos + 1)
        if (opToken != null && opToken.text in COMPARISON_OPERATORS) {
            val value = tokens.getOrNull(pos + 2)?.text?.toDoubleOrNull() ?: return null
            pos += 3
            return CompareCondition(first, opToken.text, value)
        }

        return null
    }

    companion object {
        private val COMPARISON_OPERATORS = setOf("<=", ">=", "==", "!=", "<", ">")

        private fun tokenize(text: String): List<CondToken> {
            val result = mutableListOf<CondToken>()
            val s = text.trim()
            var i = 0
            while (i < s.length) {
                val c = s[i]
                when {
                    c.isWhitespace() -> i++
                    c == '(' || c == ')' -> {
                        result.add(CondToken(c.toString(), false))
                        i++
                    }
                    s.startsWith("&&", i) || s.startsWith("||", i) || s.startsWith("!=", i) ||
                        s.startsWith("<=", i) || s.startsWith(">=", i) || s.startsWith("==", i) -> {
                        result.add(CondToken(s.substring(i, i + 2), false))
                        i += 2
                    }
                    c == '!' || c == '<' || c == '>' || c == '=' || c == '&' || c == '|' -> {
                        result.add(CondToken(c.toString(), false))
                        i++
                    }
                    c == '-' || c == '.' -> {
                        val start = i
                        while (i < s.length && (s[i].isDigit() || s[i] == '.' || s[i] == '-' || s[i] == '_')) i++
                        if (i == start) {
                            result.add(CondToken(c.toString(), false))
                            i++
                        } else {
                            result.add(CondToken(s.substring(start, i), true))
                        }
                    }
                    c.isDigit() -> {
                        val start = i
                        while (i < s.length && (s[i].isDigit() || s[i] == '.' || s[i] == '_')) i++
                        result.add(CondToken(s.substring(start, i), true))
                    }
                    else -> {
                        val start = i
                        while (i < s.length && !s[i].isWhitespace() && "()<>!=&|".indexOf(s[i]) == -1) i++
                        if (i == start) {
                            result.add(CondToken(c.toString(), false))
                            i++
                        } else {
                            result.add(CondToken(s.substring(start, i), false))
                        }
                    }
                }
            }
            return result
        }
    }
}