package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Tap/Untap Effects
// =============================================================================

/**
 * Tap/Untap target effect.
 * "Tap target creature" or "Untap target creature"
 */
@SerialName("TapUntap")
@Serializable
data class TapUntapEffect(
    val target: EffectTarget,
    val tap: Boolean = true
) : Effect {
    override val description: String = "${if (tap) "Tap" else "Untap"} ${target.description}"
    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Tap or untap all entities in a named collection.
 * Used for effects that let a player choose permanents to tap/untap from a selection.
 * "Untap up to two lands" (after Gather → Select → TapUntapCollection)
 */
@SerialName("TapUntapCollection")
@Serializable
data class TapUntapCollectionEffect(
    val collectionName: String,
    val tap: Boolean = true
) : Effect {
    override val description: String = "${if (tap) "Tap" else "Untap"} each permanent in $collectionName"
    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Tap up to X target creatures.
 * Used for Tidal Surge: "Tap up to three target creatures without flying."
 * Note: The targeting filter is specified in the spell's TargetCreature, not here.
 */
@SerialName("TapTargetCreatures")
@Serializable
data class TapTargetCreaturesEffect(
    val maxTargets: Int
) : Effect {
    override val description: String = "Tap up to $maxTargets target creature${if (maxTargets > 1) "s" else ""}"

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}
