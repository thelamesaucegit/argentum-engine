package com.wingedsheep.engine.mechanics.combat.rules

import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId

/**
 * Context for attack restriction checks.
 */
data class AttackCheckContext(
    val state: GameState,
    val projected: ProjectedState,
    val attackerId: EntityId,
    val attackingPlayer: EntityId,
    val cardRegistry: CardRegistry?
)

/**
 * Per-creature attack restriction: checks whether a creature is eligible to attack
 * regardless of which player/planeswalker it's attacking.
 *
 * Returns an error message if the creature CANNOT attack, null if this rule allows it.
 */
interface AttackRestrictionRule {
    fun check(ctx: AttackCheckContext): String?
}

/**
 * Per-defender attack restriction: checks whether a creature can attack a specific defender.
 * Used for mechanics like CantAttackUnless and CantBeAttackedWithout where the defender matters.
 *
 * Returns an error message if the creature CANNOT attack this defender, null if allowed.
 */
interface AttackDefenderRule {
    fun check(ctx: AttackCheckContext, defenderId: EntityId): String?

    /**
     * Returns true if the creature is restricted from attacking ALL opponents.
     * Default implementation checks [check] against every opponent.
     * Used by [getValidAttackers] for must-attack requirement validation.
     */
    fun restrictsAllDefenders(ctx: AttackCheckContext): Boolean {
        val opponents = ctx.state.turnOrder.filter { it != ctx.attackingPlayer }
        return opponents.all { opponentId -> check(ctx, opponentId) != null }
    }
}
