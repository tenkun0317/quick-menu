package xyz.inorganic.quickmenu.data.command_actions

import kotlinx.serialization.Serializable

@Serializable
class SleepActionData(
    val ticks: Int = 20
) : ActionData() {
    override val type: String = "sleep"
    override val value: String = ticks.toString()

    override val typeString: String = "SLEEP"
    override fun getDisplayString(): String = formatSeconds(ticks) + "s"

    override fun run() {}

    companion object {
        const val TICKS_PER_SECOND = 20

        fun formatSeconds(ticks: Int): String {
            val seconds = ticks.toDouble() / TICKS_PER_SECOND
            return if (seconds == seconds.toInt().toDouble()) {
                seconds.toInt().toString()
            } else {
                (Math.round(seconds * 100.0) / 100.0).toString()
            }
        }

        fun fromSecondsText(text: String): SleepActionData {
            val seconds = text.toDoubleOrNull() ?: return SleepActionData(0)
            val ticks = Math.round(seconds * TICKS_PER_SECOND).toInt().coerceAtLeast(0)
            return SleepActionData(ticks)
        }
    }
}