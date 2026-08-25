package xyz.inorganic.quickmenu.macros

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import xyz.inorganic.quickmenu.QuickMenu
import xyz.inorganic.quickmenu.data.ActionButtonData
import xyz.inorganic.quickmenu.data.command_actions.MacroActionData
import xyz.inorganic.quickmenu.macros.MacroInterpreter.Frame
import xyz.inorganic.quickmenu.macros.MacroInterpreter.MacroInstance

/**
 * マクロ実行管理シングルトン。
 * - トグル式: 同じボタンで開始/停止。
 * - 全クライアントティックで各インスタンスを最大 macroMaxStepsPerTick ステップ前進させる。
 * - 安全対策: 最大実行時間(macroMaxRunSeconds)超過で強制停止 + チャット警告、緊急停止。
 */
object MacroExecutor {

    private data class Entry(
        val owner: ActionButtonData?,
        val instance: MacroInstance,
        val startTick: Long
    ) {
        val ownerName: String
            get() = owner?.name ?: "test"
    }

    private val entries = mutableListOf<Entry>()

    private var tickCounter = 0L

    private val maxStepsPerTick: Int
        get() = QuickMenu.CONFIG.macroMaxStepsPerTick

    private val maxRunTicks: Long
        get() = QuickMenu.CONFIG.macroMaxRunSeconds * 20L

    private val maxNesting: Int
        get() = QuickMenu.CONFIG.macroMaxNesting

    fun toggle(owner: ActionButtonData, action: MacroActionData) {
        val existing = entries.firstOrNull { it.owner === owner }
        if (existing != null) {
            stopEntry(existing)
        } else {
            start(owner, action.script)
        }
    }

    fun testRun(script: String) {
        entries.removeAll { it.owner == null }
        start(null, script)
    }

    fun stopTest() {
        entries.removeAll { it.owner == null }
    }

    fun testRunning(): Boolean {
        return entries.any { it.owner == null }
    }

    fun stop(owner: ActionButtonData) {
        val existing = entries.firstOrNull { it.owner === owner }
        if (existing != null) stopEntry(existing)
    }

    private fun start(owner: ActionButtonData?, script: String) {
        val (program, errors) = MacroParser.parse(script)
        if (program == null) {
            errors.firstOrNull()?.let { sendMessage("マクロの解析エラー: ${it.displayString()}") }
            return
        }
        val instance = MacroInstance(program, owner?.name ?: "test")
        entries.add(Entry(owner, instance, tickCounter))
    }

    private fun stopEntry(entry: Entry) {
        entry.instance.alive = false
        entry.instance.error = null
        entries.remove(entry)
    }

    fun isRunning(owner: ActionButtonData): Boolean {
        return entries.any { it.owner === owner }
    }

    val runningCount: Int
        get() = entries.count { it.owner != null }

    fun advanceAll() {
        tickCounter++
        if (entries.isEmpty()) return
        val iterator = entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val instance = entry.instance
            if (!instance.alive) {
                iterator.remove()
                continue
            }
            if (tickCounter - entry.startTick > maxRunTicks) {
                instance.alive = false
                iterator.remove()
                sendMessage("マクロ「${entry.ownerName}」が実行時間上限を超えたため強制停止しました")
                continue
            }
            step(instance)
            if (!instance.alive) {
                instance.error?.let { sendMessage("マクロ「${entry.ownerName}」エラー: ${it.displayString()}") }
                iterator.remove()
            }
        }
    }

    private fun step(instance: MacroInstance) {
        if (instance.waitRemaining > 0) {
            instance.waitRemaining--
            return
        }
        var steps = 0
        while (steps < maxStepsPerTick && instance.alive && instance.waitRemaining == 0L) {
            val result = executeOneGuarded(instance)
            if (result != StepResult.CONTINUE_DEFAULT) break
            steps++
        }
    }

    private fun executeOneGuarded(instance: MacroInstance): StepResult {
        return try {
            MacroInterpreter.executeOne(instance)
        } catch (e: Exception) {
            instance.alive = false
            instance.error = MacroError(0, "実行時エラー: ${e.message}")
            StepResult.ERROR
        }
    }

    /**
     * run <マクロ名> のサブマクロ展開。ネスト上限を超えたら失敗。
     */
    fun pushRun(instance: MacroInstance, target: MacroActionData): Boolean {
        if (instance.frames.size >= maxNesting) {
            instance.alive = false
            instance.error = MacroError(0, "マクロのネストが深すぎます (上限 $maxNesting)")
            return false
        }
        val (program, errors) = MacroParser.parse(target.script)
        if (program == null) {
            instance.alive = false
            instance.error = errors.firstOrNull() ?: MacroError(0, "サブマクロの解析に失敗しました")
            return false
        }
        instance.frames.addLast(Frame(program))
        return true
    }

    fun emergencyStop() {
        if (entries.isEmpty()) return
        entries.clear()
        sendMessage("マクロをすべて停止しました")
    }

    fun clear() = entries.clear()

    private fun sendMessage(text: String) {
        val client = Minecraft.getInstance()
        client.player?.sendSystemMessage(Component.literal("[QuickMenu] $text"))
    }
}