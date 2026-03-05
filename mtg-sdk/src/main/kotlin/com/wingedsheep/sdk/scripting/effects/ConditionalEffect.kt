package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Conditional Effect
// =============================================================================

/**
 * An effect that only happens if a condition is met.
 */
@SerialName("Conditional")
@Serializable
data class ConditionalEffect(
    val condition: Condition,
    val effect: Effect,
    val elseEffect: Effect? = null
) : Effect {
    override val description: String = buildString {
        append(condition.description.replaceFirstChar { it.uppercase() })
        append(", ")
        append(effect.description.replaceFirstChar { it.lowercase() })
        if (elseEffect != null) {
            append(". Otherwise, ")
            append(elseEffect.description.replaceFirstChar { it.lowercase() })
        }
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newCondition = condition.applyTextReplacement(replacer)
        val newEffect = effect.applyTextReplacement(replacer)
        val newElseEffect = elseEffect?.applyTextReplacement(replacer)
        return if (newCondition !== condition || newEffect !== effect || newElseEffect !== elseEffect)
            copy(condition = newCondition, effect = newEffect, elseEffect = newElseEffect) else this
    }
}
