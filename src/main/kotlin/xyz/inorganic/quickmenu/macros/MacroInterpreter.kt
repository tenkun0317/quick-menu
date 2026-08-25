package xyz.inorganic.quickmenu.macros

import net.minecraft.client.Minecraft
import net.minecraft.core.registries.BuiltInRegistries
import xyz.inorganic.quickmenu.other.KeybindHandler

enum class StepResult {
    CONTINUE_DEFAULT,
    WAITING,
    FINISHED,
    ERROR
}

object MacroInterpreter {

    class Frame(val program: MacroProgram) {
        var pc = 0
        val pendingLoops = mutableMapOf<Int, Long>()
    }

    class MacroInstance(
        val program: MacroProgram,
        val ownerName: String,
        val vars: MutableMap<String, String> = mutableMapOf()
    ) {
        val frames = ArrayDeque<Frame>()
        var waitRemaining = 0L
        var alive = true
        var error: MacroError? = null

        init {
            frames.addLast(Frame(program))
        }

        fun topFrame(): Frame = frames.last()
    }

    fun executeOne(instance: MacroInstance): StepResult {
        val frame = instance.topFrame()
        val program = frame.program

        if (frame.pc >= program.count) {
            if (instance.frames.size == 1) {
                instance.alive = false
                return StepResult.FINISHED
            }
            instance.frames.removeLast()
            return StepResult.CONTINUE_DEFAULT
        }

        val idx = frame.pc
        val stmt = program.statements[idx]

        return when (stmt) {
            is MacroCondStmt -> {
                val takeBranch = stmt is MacroElseStmt || evalCondition(stmt.condition, instance)
                if (takeBranch) {
                    frame.pc = idx + 1
                } else {
                    frame.pc = program.nextCond[idx]
                }
                StepResult.CONTINUE_DEFAULT
            }
            is MacroEndifStmt -> {
                frame.pc = program.advanceTo[idx]
                StepResult.CONTINUE_DEFAULT
            }
            is MacroLoopStmt -> {
                val end = program.loopEnds[idx]
                val remaining = frame.pendingLoops[idx]
                when {
                    remaining == null -> {
                        val times = stmt.times
                        when {
                            times == null -> {
                                frame.pendingLoops[idx] = -1L
                                frame.pc = idx + 1
                            }
                            times == 0L -> frame.pc = end + 1
                            else -> {
                                frame.pendingLoops[idx] = times - 1
                                frame.pc = idx + 1
                            }
                        }
                    }
                    remaining == 0L -> {
                        frame.pendingLoops.remove(idx)
                        frame.pc = end + 1
                    }
                    remaining == -1L -> frame.pc = idx + 1
                    else -> {
                        frame.pendingLoops[idx] = remaining - 1
                        frame.pc = idx + 1
                    }
                }
                StepResult.CONTINUE_DEFAULT
            }
            is MacroEndLoopStmt -> {
                val loopStart = program.loopStartOfEnd[idx]
                if (loopStart == -1) {
                    fail(instance, stmt.line, "対応する loop が見つかりません")
                    StepResult.ERROR
                } else {
                    frame.pc = loopStart
                    StepResult.CONTINUE_DEFAULT
                }
            }
            is MacroBreakStmt -> {
                val loopIdx = program.innerLoopStart[idx]
                if (loopIdx == -1) {
                    fail(instance, stmt.line, "break はループの外では使えません")
                    StepResult.ERROR
                } else {
                    frame.pc = program.loopEnds[loopIdx] + 1
                    StepResult.CONTINUE_DEFAULT
                }
            }
            is MacroJumpStmt -> {
                val target = program.jumpTargets[idx]
                if (target == -1) {
                    fail(instance, stmt.line, "ジャンプ先のラベルが見つかりません")
                    StepResult.ERROR
                } else {
                    frame.pc = target
                    StepResult.CONTINUE_DEFAULT
                }
            }
            is MacroLabelStmt -> {
                frame.pc = program.advanceTo[idx]
                StepResult.CONTINUE_DEFAULT
            }
            is MacroExitStmt -> {
                instance.alive = false
                StepResult.FINISHED
            }
            is MacroWaitStmt -> {
                frame.pc = program.advanceTo[idx]
                instance.waitRemaining = stmt.ticks
                StepResult.WAITING
            }
            is MacroCmdStmt -> {
                frame.pc = program.advanceTo[idx]
                runCommand(stmt.text)
                StepResult.CONTINUE_DEFAULT
            }
            is MacroChatStmt -> {
                frame.pc = program.advanceTo[idx]
                runChat(stmt.text)
                StepResult.CONTINUE_DEFAULT
            }
            is MacroKeyStmt -> {
                frame.pc = program.advanceTo[idx]
                KeybindHandler.pressKey(stmt.translationKey)
                StepResult.CONTINUE_DEFAULT
            }
            is MacroSetStmt -> {
                frame.pc = program.advanceTo[idx]
                instance.vars[stmt.name] = stmt.value
                StepResult.CONTINUE_DEFAULT
            }
            is MacroRunStmt -> {
                val target = MacroRegistry.resolve(stmt.name)
                if (target == null) {
                    fail(instance, stmt.line, "マクロが見つかりません: ${stmt.name}")
                    StepResult.ERROR
                } else {
                    frame.pc = program.advanceTo[idx]
                    MacroExecutor.pushRun(instance, target)
                    StepResult.CONTINUE_DEFAULT
                }
            }
        }
    }

    private fun fail(instance: MacroInstance, line: Int, message: String) {
        instance.alive = false
        instance.error = MacroError(line, message)
    }

    fun evalCondition(condition: MacroCondition?, instance: MacroInstance): Boolean {
        if (condition == null) return false
        return when (condition) {
            is KeyCondition -> {
                val mapping = KeybindHandler.getFromTranslationKey(condition.translationKey)
                mapping?.isDown == true
            }
            is ItemCondition -> evalItem(condition.registryId)
            is CompareCondition -> {
                val leftValue = when (condition.variable.lowercase()) {
                    "health" -> Minecraft.getInstance().player?.health?.toDouble()
                    else -> instance.vars[condition.variable]?.toDoubleOrNull()
                }
                if (leftValue == null) false else compareNumbers(leftValue, condition.operator, condition.value)
            }
            is NotCondition -> !evalCondition(condition.inner, instance)
            is AndCondition -> evalCondition(condition.left, instance) && evalCondition(condition.right, instance)
            is OrCondition -> evalCondition(condition.left, instance) || evalCondition(condition.right, instance)
        }
    }

    private fun compareNumbers(left: Double, operator: String, right: Double): Boolean {
        return when (operator) {
            ">" -> left > right
            "<" -> left < right
            ">=" -> left >= right
            "<=" -> left <= right
            "==" -> left == right
            "!=" -> left != right
            else -> false
        }
    }

    private fun evalItem(registryId: String): Boolean {
        val player = Minecraft.getInstance().player ?: return false
        val item = player.mainHandItem.item
        val key = BuiltInRegistries.ITEM.getKey(item)
        if (key == null) return false
        val path = key.path
        return "$key" == registryId || path == registryId.substringAfter(":")
    }

    private fun runCommand(text: String) {
        val client = Minecraft.getInstance()
        val player = client.player ?: return
        val trimmed = if (text.startsWith("/")) text.substring(1) else text
        if (trimmed.isEmpty()) return
        player.connection.sendCommand(trimmed)
    }

    private fun runChat(text: String) {
        val client = Minecraft.getInstance()
        val player = client.player ?: return
        val message = if (text.length > 256) text.substring(0, 256) else text
        player.connection.sendChat(message)
    }
}