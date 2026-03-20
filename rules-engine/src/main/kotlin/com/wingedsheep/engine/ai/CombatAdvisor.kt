package com.wingedsheep.engine.ai

import com.wingedsheep.engine.ai.evaluation.BoardEvaluator
import com.wingedsheep.engine.ai.evaluation.BoardPresence
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.EntityId

/**
 * Specialized advisor for attack and block decisions.
 *
 * Uses pure heuristic analysis (NOT simulation) because the simulator can't
 * resolve through combat phases — it would see "creatures tapped, no damage
 * dealt" and always prefer not attacking.
 */
class CombatAdvisor(
    private val simulator: GameSimulator,
    private val evaluator: BoardEvaluator
) {
    /**
     * Build a DeclareAttackers action choosing which creatures to send in.
     */
    fun chooseAttackers(
        state: GameState,
        legalAction: LegalAction,
        playerId: EntityId
    ): GameAction {
        val projected = state.projectedState
        val validAttackers = legalAction.validAttackers ?: emptyList()
        val defendingPlayers = legalAction.validAttackTargets ?: emptyList()

        if (validAttackers.isEmpty() || defendingPlayers.isEmpty()) {
            return DeclareAttackers(playerId, emptyMap())
        }

        val opponentId = state.getOpponent(playerId) ?: defendingPlayers.first()
        val opponentLife = state.getEntity(opponentId)?.get<LifeTotalComponent>()?.life ?: 20
        val opponentCreatures = getOpponentUntappedCreatures(state, projected, playerId)

        // Check if we have lethal — alpha strike
        val totalPower = validAttackers.sumOf { (projected.getPower(it) ?: 0).coerceAtLeast(0) }
        if (totalPower >= opponentLife) {
            return DeclareAttackers(playerId, validAttackers.associateWith { opponentId })
        }

        val attackerMap = mutableMapOf<EntityId, EntityId>()
        for (entityId in validAttackers) {
            if (shouldAttack(state, projected, entityId, opponentCreatures, opponentLife)) {
                attackerMap[entityId] = opponentId
            }
        }

        return DeclareAttackers(playerId, attackerMap)
    }

    /**
     * Build a DeclareBlockers action choosing which creatures block which attackers.
     */
    fun chooseBlockers(
        state: GameState,
        legalAction: LegalAction,
        playerId: EntityId
    ): GameAction {
        val projected = state.projectedState
        val validBlockers = legalAction.validBlockers ?: emptyList()
        val mandatory = legalAction.mandatoryBlockerAssignments ?: emptyMap()

        if (validBlockers.isEmpty()) {
            return DeclareBlockers(playerId, emptyMap())
        }

        val attackers = getAttackingCreatures(state)
        if (attackers.isEmpty()) {
            return DeclareBlockers(playerId, emptyMap())
        }

        val myLife = state.getEntity(playerId)?.get<LifeTotalComponent>()?.life ?: 20
        val incomingDamage = attackers.sumOf { (projected.getPower(it) ?: 0).coerceAtLeast(0) }
        val isLethal = incomingDamage >= myLife

        val blockerMap = mutableMapOf<EntityId, List<EntityId>>()

        // Handle mandatory blockers first
        val assignedBlockers = mutableSetOf<EntityId>()
        val blockedAttackers = mutableSetOf<EntityId>()
        for ((blockerId, mustBlockAttackers) in mandatory) {
            if (mustBlockAttackers.isNotEmpty()) {
                blockerMap[blockerId] = listOf(mustBlockAttackers.first())
                assignedBlockers.add(blockerId)
                blockedAttackers.add(mustBlockAttackers.first())
            }
        }

        if (isLethal) {
            assignBlocksForSurvival(state, projected, validBlockers, attackers, assignedBlockers, blockedAttackers, blockerMap)
        } else {
            assignBlocksForProfit(state, projected, validBlockers, attackers, assignedBlockers, blockedAttackers, blockerMap)
        }

        return DeclareBlockers(playerId, blockerMap)
    }

    // ── Attack decision ──────────────────────────────────────────────────

    private fun shouldAttack(
        state: GameState,
        projected: ProjectedState,
        entityId: EntityId,
        opponentCreatures: List<EntityId>,
        opponentLife: Int
    ): Boolean {
        val power = projected.getPower(entityId) ?: 0
        val toughness = projected.getToughness(entityId) ?: 0
        val keywords = projected.getKeywords(entityId)
        val card = state.getEntity(entityId)?.get<CardComponent>()
        val myValue = card?.let { BoardPresence.permanentValue(state, projected, entityId, it) } ?: 0.0

        if (power <= 0) return false

        // ── Always attack: no downside ──
        if (Keyword.VIGILANCE.name in keywords) return true
        if (Keyword.INDESTRUCTIBLE.name in keywords) return true

        // ── No blockers: always attack ──
        if (opponentCreatures.isEmpty()) return true

        // ── Evasion: attack unless they have relevant blockers ──
        if (Keyword.FLYING.name in keywords) {
            val hasRelevantBlocker = opponentCreatures.any { blocker ->
                val bk = projected.getKeywords(blocker)
                Keyword.FLYING.name in bk || Keyword.REACH.name in bk
            }
            if (!hasRelevantBlocker) return true
        }

        // ── Menace: need 2+ blockers ──
        if (Keyword.MENACE.name in keywords && opponentCreatures.size <= 1) return true

        // ── Deathtouch: always trades up, so almost always attack ──
        if (Keyword.DEATHTOUCH.name in keywords) return true

        // ── First strike advantage ──
        if (Keyword.FIRST_STRIKE.name in keywords || Keyword.DOUBLE_STRIKE.name in keywords) return true

        // ── Low life opponent: be aggressive ──
        if (opponentLife <= 8) return true

        // ── Core heuristic: attack if the expected value is positive ──
        // The opponent will block with their best blocker for this creature.
        // We want to attack if: (damage dealt or good trade) outweighs (risk of losing creature).
        val bestBlocker = opponentCreatures.minByOrNull { blocker ->
            // Find the opponent's "cheapest effective blocker" — can kill us for minimum cost
            val bPower = projected.getPower(blocker) ?: 0
            val bToughness = projected.getToughness(blocker) ?: 0
            val bKeywords = projected.getKeywords(blocker)
            val canKillUs = bPower >= toughness || Keyword.DEATHTOUCH.name in bKeywords
            val blockerValue = creatureValue(state, projected, blocker)

            if (canKillUs) blockerValue  // they'd trade this creature to kill us
            else Double.MAX_VALUE         // can't kill us — not a relevant blocker
        }

        if (bestBlocker != null) {
            val bPower = projected.getPower(bestBlocker) ?: 0
            val bToughness = projected.getToughness(bestBlocker) ?: 0
            val bKeywords = projected.getKeywords(bestBlocker)
            val canKillUs = bPower >= toughness || Keyword.DEATHTOUCH.name in bKeywords
            val weKillThem = power >= bToughness || Keyword.DEATHTOUCH.name in keywords
            val blockerValue = creatureValue(state, projected, bestBlocker)

            if (!canKillUs) {
                // They can't kill us by blocking → always attack (we deal damage or force a bad block)
                return true
            }

            if (weKillThem && blockerValue >= myValue * 0.8) {
                // We trade and their creature is about as valuable → good trade
                return true
            }

            // They can kill us and trading down → don't attack (unless we need pressure)
            // But still attack if we have enough backup
            val myCreatureCount = projected.getBattlefieldControlledBy(state.turnOrder.find { it != state.getOpponent(state.turnOrder[0]) } ?: return false)
                .count { projected.isCreature(it) }
            if (myCreatureCount >= 3) return true // we have board presence, trade is fine
        }

        // Default: attack. In MTG, being passive usually loses.
        return true
    }

    // ── Blocking strategies ──────────────────────────────────────────────

    private fun assignBlocksForSurvival(
        state: GameState,
        projected: ProjectedState,
        validBlockers: List<EntityId>,
        attackers: List<EntityId>,
        assignedBlockers: MutableSet<EntityId>,
        blockedAttackers: MutableSet<EntityId>,
        blockerMap: MutableMap<EntityId, List<EntityId>>
    ) {
        val available = validBlockers.filter { it !in assignedBlockers }
        val unblocked = attackers.filter { it !in blockedAttackers }

        // Block biggest threats first (highest power)
        val sortedAttackers = unblocked.sortedByDescending { projected.getPower(it) ?: 0 }

        for (attacker in sortedAttackers) {
            if ((projected.getPower(attacker) ?: 0) <= 0) continue

            val blocker = available
                .filter { it !in assignedBlockers }
                .minByOrNull { creatureValue(state, projected, it) }

            if (blocker != null) {
                blockerMap[blocker] = listOf(attacker)
                assignedBlockers.add(blocker)
                blockedAttackers.add(attacker)
            }
        }
    }

    private fun assignBlocksForProfit(
        state: GameState,
        projected: ProjectedState,
        validBlockers: List<EntityId>,
        attackers: List<EntityId>,
        assignedBlockers: MutableSet<EntityId>,
        blockedAttackers: MutableSet<EntityId>,
        blockerMap: MutableMap<EntityId, List<EntityId>>
    ) {
        val unblocked = attackers.filter { it !in blockedAttackers }

        for (attacker in unblocked) {
            val aPower = projected.getPower(attacker) ?: 0
            val aToughness = projected.getToughness(attacker) ?: 0
            val aKeywords = projected.getKeywords(attacker)
            val aHasDeathtouch = Keyword.DEATHTOUCH.name in aKeywords
            val attackerValue = creatureValue(state, projected, attacker)

            val blocker = validBlockers
                .filter { it !in assignedBlockers }
                .filter { blockerId ->
                    val bPower = projected.getPower(blockerId) ?: 0
                    val bToughness = projected.getToughness(blockerId) ?: 0
                    val bKeywords = projected.getKeywords(blockerId)
                    val bHasDeathtouch = Keyword.DEATHTOUCH.name in bKeywords
                    val blockerValue = creatureValue(state, projected, blockerId)

                    val weKillThem = bPower >= aToughness || bHasDeathtouch
                    val weSurvive = bToughness > aPower && !aHasDeathtouch

                    // Block if: we kill them and survive, or we kill them and they're more valuable
                    (weKillThem && weSurvive) || (weKillThem && attackerValue > blockerValue)
                }
                .minByOrNull { creatureValue(state, projected, it) }

            if (blocker != null) {
                blockerMap[blocker] = listOf(attacker)
                assignedBlockers.add(blocker)
                blockedAttackers.add(attacker)
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun getOpponentUntappedCreatures(
        state: GameState,
        projected: ProjectedState,
        playerId: EntityId
    ): List<EntityId> {
        val opponentId = state.getOpponent(playerId) ?: return emptyList()
        return projected.getBattlefieldControlledBy(opponentId).filter { entityId ->
            projected.isCreature(entityId) &&
                state.getEntity(entityId)?.has<TappedComponent>() != true
        }
    }

    private fun getAttackingCreatures(state: GameState): List<EntityId> {
        return state.getBattlefield().filter { entityId ->
            state.getEntity(entityId)?.has<com.wingedsheep.engine.state.components.combat.AttackingComponent>() == true
        }
    }

    private fun creatureValue(state: GameState, projected: ProjectedState, entityId: EntityId): Double {
        val card = state.getEntity(entityId)?.get<CardComponent>() ?: return 0.0
        return BoardPresence.permanentValue(state, projected, entityId, card)
    }
}
