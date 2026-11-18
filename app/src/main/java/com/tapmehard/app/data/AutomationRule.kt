package com.tapmehard.app.data

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class ActionType {
    TOGGLE_FLASHLIGHT,
    PLAY_PAUSE_MEDIA,
    TAKE_SCREENSHOT,
    LAUNCH_APP
}

@Serializable
data class AutomationRule(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val sequence: String = "..",
    val customIntervals: List<Long>? = null,
    val actionType: ActionType,
    val targetPackage: String? = null,
    val targetAppName: String? = null,
    val enabled: Boolean = true
) {
    /**
     * Backward-compatible tap count derived directly from sequence length.
     */
    val tapCount: Int
        get() = sequence.length

    init {
        require(isValidSequence(sequence)) {
            "Guard failure: Sequence '$sequence' is invalid. " +
            "Sequence should always be in morse code format that is '-' and '.' (hyphen and dot)."
        }
    }

    companion object {
        private val MORSE_GUARD_REGEX = Regex("^[.-]+$")

        /**
         * Guard validation: returns true ONLY if the sequence is non-empty
         * and contains exclusively '-' (hyphen) and '.' (dot).
         */
        fun isValidSequence(seq: String): Boolean {
            return seq.isNotEmpty() && MORSE_GUARD_REGEX.matches(seq)
        }

        /**
         * Enforces the sequence guard. Throws [IllegalArgumentException] if violated.
         */
        fun validateSequenceGuard(seq: String): String {
            val trimmed = seq.trim()
            require(isValidSequence(trimmed)) {
                "Sequence guard violated: '$seq' is not in Morse code format. Must contain only '-' and '.'."
            }
            return trimmed
        }
    }
}
