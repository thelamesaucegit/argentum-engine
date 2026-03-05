package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Group Iteration Effects
// =============================================================================

/**
 * Apply an effect to every entity matching a group filter.
 *
 * The inner [effect] is executed once per matched entity.
 * Within the inner effect, [com.wingedsheep.sdk.scripting.targets.EffectTarget.Self] resolves to the
 * current iteration entity rather than the source permanent.
 *
 * @property filter Which entities are affected
 * @property effect The effect to apply to each matched entity
 * @property noRegenerate If true, affected entities cannot be regenerated
 * @property simultaneous If true (default), the group is snapshotted before effects apply
 */
@SerialName("ForEachInGroup")
@Serializable
data class ForEachInGroupEffect(
    val filter: GroupFilter,
    val effect: Effect,
    val noRegenerate: Boolean = false,
    val simultaneous: Boolean = true
) : Effect {
    override val description: String = buildString {
        append(effect.description.replaceFirstChar { it.uppercase() })
        append(" ")
        append(filter.description.replaceFirstChar { it.lowercase() })
        if (noRegenerate) append(". They can't be regenerated")
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newFilter = filter.applyTextReplacement(replacer)
        val newEffect = effect.applyTextReplacement(replacer)
        return if (newFilter !== filter || newEffect !== effect)
            copy(filter = newFilter, effect = newEffect) else this
    }
}