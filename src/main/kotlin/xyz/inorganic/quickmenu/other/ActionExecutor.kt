package xyz.inorganic.quickmenu.other

import xyz.inorganic.quickmenu.data.ActionButtonData
import xyz.inorganic.quickmenu.data.command_actions.KeybindActionData
import xyz.inorganic.quickmenu.data.command_actions.KeyPressMode
import xyz.inorganic.quickmenu.data.command_actions.MacroActionData
import xyz.inorganic.quickmenu.data.command_actions.SleepActionData
import xyz.inorganic.quickmenu.macros.MacroExecutor

object ActionExecutor {
    private class Execution(val button: ActionButtonData) {
        var index = 0
        var remainingTicks = 0
        val heldKeys = mutableSetOf<String>()
    }

    private val executions = mutableListOf<Execution>()

    fun submit(button: ActionButtonData) {
        val removed = executions.filter { it.button === button }
        executions.removeAll { it.button === button }
        removed.forEach { releaseHeld(it) }
        val exec = Execution(button)
        executions.add(exec)
        runForward(exec)
        if (exec.index >= button.actions.size) {
            executions.remove(exec)
            releaseHeld(exec)
        }
    }

    fun advance() {
        val iterator = executions.iterator()
        while (iterator.hasNext()) {
            val exec = iterator.next()
            if (exec.remainingTicks > 0) {
                exec.remainingTicks--
            } else {
                runForward(exec)
            }
            if (exec.index >= exec.button.actions.size) {
                iterator.remove()
                releaseHeld(exec)
            }
        }
    }

    fun cancel(button: ActionButtonData) {
        MacroExecutor.stop(button)
        val removed = executions.filter { it.button === button }
        executions.removeAll { it.button === button }
        removed.forEach { releaseHeld(it) }
    }

    fun cancelAll() {
        val removed = executions.toList()
        executions.clear()
        removed.forEach { MacroExecutor.stop(it.button); releaseHeld(it) }
    }

    private fun releaseHeld(exec: Execution) {
        for (translationKey in exec.heldKeys) {
            KeybindHandler.releaseKey(translationKey)
        }
        exec.heldKeys.clear()
    }

    private fun runForward(exec: Execution) {
        val actions = exec.button.actions
        while (exec.index < actions.size) {
            val action = actions[exec.index]
            exec.index++
            if (action is SleepActionData) {
                if (action.ticks > 0) {
                    exec.remainingTicks = action.ticks
                    return
                }
            } else {
                if (action is MacroActionData) {
                    MacroExecutor.toggle(exec.button, action)
                    continue
                }
                action.run()
                if (action is KeybindActionData && action.mode == KeyPressMode.PRESS) {
                    exec.heldKeys.add(action.translationKey)
                }
            }
        }
    }
}