package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Control Change Effects
// =============================================================================

/**
 * Gain control of target permanent.
 * "Gain control of target permanent."
 *
 * Used by Blatant Thievery and similar control-stealing effects.
 */
@SerialName("GainControl")
@Serializable
data class GainControlEffect(
    val target: EffectTarget,
    val duration: Duration = Duration.Permanent
) : Effect {
    override val description: String = buildString {
        append("gain control of ${target.description}")
        if (duration != Duration.Permanent && duration.description.isNotEmpty()) {
            append(" ${duration.description}")
        }
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Gain control of target permanent for the active player (whoever's turn it is).
 * Unlike GainControlEffect which gives control to the ability's controller,
 * this gives control to the current active player.
 *
 * Used by Risky Move: "At the beginning of each player's upkeep, that player
 * gains control of Risky Move."
 */
@SerialName("GainControlByActivePlayer")
@Serializable
data class GainControlByActivePlayerEffect(
    val target: EffectTarget = EffectTarget.Self
) : Effect {
    override val description: String = "that player gains control of ${target.description}"

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Gain control of a permanent based on who controls the most creatures of a subtype.
 * The player with strictly more creatures of the given subtype than all other players
 * gains control of the target permanent.
 *
 * Used by Thoughtbound Primoc: "At the beginning of your upkeep, if a player controls
 * more Wizards than each other player, that player gains control of Thoughtbound Primoc."
 */
@SerialName("GainControlByMostOfSubtype")
@Serializable
data class GainControlByMostOfSubtypeEffect(
    val subtype: Subtype,
    val target: EffectTarget = EffectTarget.Self
) : Effect {
    override val description: String =
        "the player who controls the most ${subtype.value}s gains control of ${target.description}"

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val new = replacer.replaceSubtype(subtype)
        return if (new == subtype) this else copy(subtype = new)
    }
}

/**
 * Give control of a permanent to a targeted player.
 * Unlike GainControlEffect (which always gives control to the ability's controller),
 * this effect gives control to a player resolved from a target.
 *
 * Used by Custody Battle: "target opponent gains control of this creature unless you sacrifice a land."
 *
 * @property permanent Which permanent changes control (default: enchanted creature)
 * @property newController Which player gains control (default: first target, expected to be a player)
 */
@SerialName("GiveControlToTargetPlayer")
@Serializable
data class GiveControlToTargetPlayerEffect(
    val permanent: EffectTarget = EffectTarget.EnchantedCreature,
    val newController: EffectTarget = EffectTarget.ContextTarget(0),
    val duration: Duration = Duration.Permanent
) : Effect {
    override val description: String = "target opponent gains control of ${permanent.description}"

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Exchange control of two target creatures.
 * "You may exchange control of target creature you control and target creature an opponent controls."
 *
 * Creates two floating effects at Layer.CONTROL:
 * 1. Target A (yours) → opponent gains control
 * 2. Target B (opponent's) → you gain control
 *
 * @property target1 The creature you control (becomes opponent's)
 * @property target2 The creature an opponent controls (becomes yours)
 */
@SerialName("ExchangeControl")
@Serializable
data class ExchangeControlEffect(
    val target1: EffectTarget = EffectTarget.ContextTarget(0),
    val target2: EffectTarget = EffectTarget.ContextTarget(1)
) : Effect {
    override val description: String =
        "Exchange control of ${target1.description} and ${target2.description}"

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}
