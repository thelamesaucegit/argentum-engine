package com.wingedsheep.gameserver.ai

import com.wingedsheep.gameserver.ai.decision.AiDecisionHandler
import com.wingedsheep.gameserver.ai.decision.AiDecisionHandlerRegistry
import com.wingedsheep.gameserver.dto.*
import com.wingedsheep.gameserver.protocol.LegalActionInfo
import com.wingedsheep.engine.core.*
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId

/**
 * Converts game state, legal actions, and pending decisions into concise text
 * that an LLM can reason about.
 *
 * Entity IDs are mapped to short numeric labels, and legal actions to letter labels.
 */
class GameStateFormatter(
    private val decisionHandlerRegistry: AiDecisionHandlerRegistry = AiDecisionHandlerRegistry()
) {

    /**
     * Format a full state update for the LLM.
     */
    fun format(
        state: ClientGameState,
        legalActions: List<LegalActionInfo>,
        pendingDecision: PendingDecision?
    ): String {
        val sb = StringBuilder()

        // Build entity ID label map for readable references
        val entityLabels = buildEntityLabels(state)

        sb.appendLine("=== GAME STATE ===")
        sb.appendLine("Turn ${state.turnNumber} | Phase: ${state.currentPhase} | Step: ${state.currentStep}")
        sb.appendLine("Active: ${if (state.activePlayerId == state.viewingPlayerId) "You" else "Opponent"} | Priority: ${if (state.priorityPlayerId == state.viewingPlayerId) "You" else "Opponent"}")
        sb.appendLine()

        // Format players
        val you = state.players.find { it.playerId == state.viewingPlayerId }
        val opponent = state.players.find { it.playerId != state.viewingPlayerId }

        if (you != null) {
            sb.appendLine("-- YOU --")
            formatPlayer(sb, you, state, entityLabels, isYou = true)
        }
        sb.appendLine()
        if (opponent != null) {
            sb.appendLine("-- OPPONENT --")
            formatPlayer(sb, opponent, state, entityLabels, isYou = false)
        }

        // Format combat state
        if (state.combat != null) {
            sb.appendLine()
            formatCombat(sb, state.combat, state, entityLabels)
        }

        // Format stack
        val stackZone = state.zones.find { it.zoneId.zoneType == Zone.STACK }
        if (stackZone != null && stackZone.cardIds.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("=== STACK (top first) ===")
            for (cardId in stackZone.cardIds.reversed()) {
                val card = state.cards[cardId] ?: continue
                val label = entityLabels[cardId] ?: cardId.value
                val description = card.stackText ?: card.oracleText.takeIf { it.isNotBlank() }
                sb.appendLine("  [$label] ${card.name}${description?.let { " — $it" } ?: ""}")
            }
        }

        // Format pending decision or legal actions
        if (pendingDecision != null) {
            sb.appendLine()
            formatDecision(sb, pendingDecision, state, entityLabels)
        } else if (legalActions.isNotEmpty()) {
            sb.appendLine()
            formatLegalActions(sb, legalActions, state, entityLabels)
        }

        return sb.toString()
    }

    /**
     * Format a mulligan decision with full card info.
     */
    fun formatMulligan(
        cards: List<MulliganCardDisplay>,
        mulliganCount: Int,
        isOnThePlay: Boolean
    ): String {
        val sb = StringBuilder()
        sb.appendLine("=== MULLIGAN DECISION ===")
        sb.appendLine("You are on the ${if (isOnThePlay) "play" else "draw"}.")
        sb.appendLine("Mulligan count: $mulliganCount (keeping ${7 - mulliganCount} cards)")
        sb.appendLine()
        sb.appendLine("Your hand:")
        for ((i, card) in cards.withIndex()) {
            sb.append("  [${i + 1}] ${card.name}")
            if (card.manaCost != null) sb.append(" ${card.manaCost}")
            if (card.typeLine != null) sb.append(" — ${card.typeLine}")
            if (card.power != null && card.toughness != null) sb.append(" ${card.power}/${card.toughness}")
            sb.appendLine()
        }
        sb.appendLine()
        sb.appendLine("Choose: [A] Keep hand, [B] Mulligan")
        return sb.toString()
    }

    /**
     * Format a choose-bottom-cards decision with full card info.
     */
    fun formatChooseBottomCards(
        cards: List<MulliganCardDisplay>,
        cardIds: List<EntityId>,
        count: Int
    ): String {
        val sb = StringBuilder()
        sb.appendLine("=== CHOOSE CARDS TO PUT ON BOTTOM ===")
        sb.appendLine("You must put $count card(s) on the bottom of your library.")
        sb.appendLine()
        sb.appendLine("Your hand:")
        for ((i, card) in cards.withIndex()) {
            sb.append("  [${actionLetter(i)}] ${card.name}")
            if (card.manaCost != null) sb.append(" ${card.manaCost}")
            if (card.typeLine != null) sb.append(" — ${card.typeLine}")
            if (card.power != null && card.toughness != null) sb.append(" ${card.power}/${card.toughness}")
            sb.appendLine()
        }
        sb.appendLine()
        sb.appendLine("Reply with the letter(s) of the $count card(s) to put on bottom.")
        return sb.toString()
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private fun buildEntityLabels(state: ClientGameState): Map<EntityId, String> {
        val labels = mutableMapOf<EntityId, String>()
        var counter = 1
        // Label all visible cards
        for ((id, _) in state.cards) {
            labels[id] = counter.toString()
            counter++
        }
        // Label players
        for (player in state.players) {
            if (player.playerId !in labels) {
                labels[player.playerId] = "P${if (player.playerId == state.viewingPlayerId) "1" else "2"}"
            }
        }
        return labels
    }

    private fun formatPlayer(
        sb: StringBuilder,
        player: ClientPlayer,
        state: ClientGameState,
        labels: Map<EntityId, String>,
        isYou: Boolean
    ) {
        sb.appendLine("Life: ${player.life} | Hand: ${player.handSize} | Library: ${player.librarySize}")

        // Mana pool (floating mana)
        if (isYou && player.manaPool != null && !player.manaPool.isEmpty) {
            val pool = player.manaPool
            val parts = mutableListOf<String>()
            if (pool.white > 0) parts.add("{W}x${pool.white}")
            if (pool.blue > 0) parts.add("{U}x${pool.blue}")
            if (pool.black > 0) parts.add("{B}x${pool.black}")
            if (pool.red > 0) parts.add("{R}x${pool.red}")
            if (pool.green > 0) parts.add("{G}x${pool.green}")
            if (pool.colorless > 0) parts.add("{C}x${pool.colorless}")
            sb.appendLine("Mana pool: ${parts.joinToString(", ")}")
        }

        // Battlefield
        val battlefield = state.zones
            .filter { it.zoneId.zoneType == Zone.BATTLEFIELD && it.zoneId.ownerId == player.playerId }
            .flatMap { it.cardIds }
            .mapNotNull { id -> state.cards[id]?.let { id to it } }

        if (battlefield.isNotEmpty()) {
            sb.appendLine("Battlefield:")
            // Show all permanents individually (no land grouping) so each has a label
            for ((id, card) in battlefield) {
                val label = labels[id] ?: "?"
                sb.append("  [$label] ${card.name}")
                if (card.power != null && card.toughness != null) {
                    sb.append(" ${card.power}/${card.toughness}")
                    if (card.damage != null && card.damage > 0) sb.append(" (${card.damage} dmg)")
                }
                if (card.isTapped) sb.append(" (tapped)")
                if (card.hasSummoningSickness) sb.append(" (summoning sick)")
                if (card.isFaceDown) sb.append(" (face-down)")
                val keywords = card.keywords
                if (keywords.isNotEmpty()) sb.append(" [${keywords.joinToString(", ") { it.name.lowercase() }}]")
                if (card.counters.isNotEmpty()) {
                    sb.append(" {${card.counters.entries.joinToString(", ") { "${it.value} ${it.key.name.lowercase()}" }}}")
                }
                if (card.attachments.isNotEmpty()) {
                    val attachNames = card.attachments.mapNotNull { state.cards[it]?.name }
                    if (attachNames.isNotEmpty()) sb.append(" equipped: ${attachNames.joinToString(", ")}")
                }
                // Oracle text for non-vanilla, non-land permanents (skip if face-down)
                if (!card.isFaceDown && card.oracleText.isNotBlank() && "Land" !in card.cardTypes) {
                    sb.append(" — \"${card.oracleText}\"")
                }
                sb.appendLine()
            }
        }

        // Hand (only for "you")
        if (isYou) {
            val handZone = state.zones
                .find { it.zoneId.zoneType == Zone.HAND && it.zoneId.ownerId == player.playerId }
            val handCards = handZone?.cardIds?.mapNotNull { id -> state.cards[id]?.let { id to it } } ?: emptyList()
            if (handCards.isNotEmpty()) {
                sb.appendLine("Hand:")
                for ((id, card) in handCards) {
                    val label = labels[id] ?: "?"
                    sb.append("  [$label] ${card.name} ${card.manaCost}")
                    sb.append(" — ${card.typeLine}")
                    if (card.power != null && card.toughness != null) sb.append(" ${card.power}/${card.toughness}")
                    if (card.keywords.isNotEmpty()) sb.append(" [${card.keywords.joinToString(", ") { it.name.lowercase() }}]")
                    if (card.oracleText.isNotBlank()) sb.append(" — \"${card.oracleText}\"")
                    sb.appendLine()
                }
            }
        }

        // Graveyard
        if (player.graveyardSize > 0) {
            val graveyardZone = state.zones
                .find { it.zoneId.zoneType == Zone.GRAVEYARD && it.zoneId.ownerId == player.playerId }
            val graveyardCards = graveyardZone?.cardIds?.mapNotNull { id ->
                state.cards[id]?.let { card ->
                    val stats = if (card.power != null) " ${card.power}/${card.toughness}" else ""
                    "${card.name}$stats"
                }
            } ?: emptyList()
            if (graveyardCards.isNotEmpty()) {
                sb.appendLine("Graveyard (${player.graveyardSize}): ${graveyardCards.joinToString(", ")}")
            } else {
                sb.appendLine("Graveyard: ${player.graveyardSize} cards")
            }
        }

        // Exile with card names
        if (player.exileSize > 0) {
            val exileZone = state.zones
                .find { it.zoneId.zoneType == Zone.EXILE && it.zoneId.ownerId == player.playerId }
            val exileCards = exileZone?.cardIds?.mapNotNull { id ->
                state.cards[id]?.name
            } ?: emptyList()
            if (exileCards.isNotEmpty()) {
                sb.appendLine("Exile (${player.exileSize}): ${exileCards.joinToString(", ")}")
            } else {
                sb.appendLine("Exile: ${player.exileSize} cards")
            }
        }
    }

    private fun formatCombat(
        sb: StringBuilder,
        combat: ClientCombatState,
        state: ClientGameState,
        labels: Map<EntityId, String>
    ) {
        sb.appendLine("=== COMBAT ===")
        if (combat.attackers.isNotEmpty()) {
            sb.appendLine("Attackers:")
            for (attacker in combat.attackers) {
                val card = state.cards[attacker.creatureId]
                val label = labels[attacker.creatureId] ?: "?"
                val stats = if (card != null) " ${card.power}/${card.toughness}" else ""
                val keywords = card?.keywords?.takeIf { it.isNotEmpty() }?.let { kws ->
                    " [${kws.joinToString(", ") { it.name.lowercase() }}]"
                } ?: ""
                val blockerNames = attacker.blockedBy.mapNotNull { state.cards[it]?.name }
                val blockedStr = if (blockerNames.isNotEmpty()) " blocked by: ${blockerNames.joinToString(", ")}" else " (unblocked)"
                sb.appendLine("  [$label] ${attacker.creatureName}$stats$keywords$blockedStr")
            }
        }
        if (combat.blockers.isNotEmpty()) {
            sb.appendLine("Blockers:")
            for (blocker in combat.blockers) {
                val card = state.cards[blocker.creatureId]
                val label = labels[blocker.creatureId] ?: "?"
                val stats = if (card != null) " ${card.power}/${card.toughness}" else ""
                val keywords = card?.keywords?.takeIf { it.isNotEmpty() }?.let { kws ->
                    " [${kws.joinToString(", ") { it.name.lowercase() }}]"
                } ?: ""
                val attackerName = state.cards[blocker.blockingAttacker]?.name ?: "?"
                sb.appendLine("  [$label] ${blocker.creatureName}$stats$keywords blocking $attackerName")
            }
        }
    }

    private fun formatLegalActions(
        sb: StringBuilder,
        actions: List<LegalActionInfo>,
        state: ClientGameState,
        labels: Map<EntityId, String>
    ) {
        sb.appendLine("=== LEGAL ACTIONS ===")
        for ((i, action) in actions.withIndex()) {
            val letter = actionLetter(i)
            sb.append("[$letter] ${action.description}")
            if (action.manaCostString != null) sb.append(" ${action.manaCostString}")
            if (!action.isAffordable) sb.append(" (can't afford)")

            // Phase 3: Inline targets for LLM selection
            if (action.requiresTargets && action.targetRequirements != null) {
                val allTargets = action.targetRequirements.flatMap { req ->
                    req.validTargets?.map { tid ->
                        val card = state.cards[tid]
                        val label = labels[tid] ?: tid.value
                        if (card != null) {
                            val stats = if (card.power != null) " ${card.power}/${card.toughness}" else ""
                            Triple(tid, "[$label] ${card.name}$stats", req.description)
                        } else {
                            Triple(tid, "[$label] player", req.description)
                        }
                    } ?: emptyList()
                }

                if (allTargets.size >= 2) {
                    sb.appendLine()
                    sb.append("    Choose target: ")
                    sb.appendLine(allTargets.mapIndexed { j, (_, name, _) ->
                        "[${j + 1}] $name"
                    }.joinToString(", "))
                    sb.append("    Reply \"$letter${1}\" for ${allTargets.first().second}")
                } else if (allTargets.size == 1) {
                    sb.append(" — target: ${allTargets.first().second}")
                }
            }
            sb.appendLine()
        }
    }

    private fun formatDecision(
        sb: StringBuilder,
        decision: PendingDecision,
        state: ClientGameState,
        labels: Map<EntityId, String>
    ) {
        sb.appendLine("=== DECISION REQUIRED ===")
        sb.appendLine(decision.prompt)
        sb.appendLine()

        // Delegate to the handler registry
        val handler = decisionHandlerRegistry.getHandler(decision)
        if (handler != null) {
            @Suppress("UNCHECKED_CAST")
            (handler as AiDecisionHandler<PendingDecision>).format(sb, decision, state, labels)
        }
    }

    companion object {
        fun actionLetter(index: Int): String {
            if (index < 26) return ('A' + index).toString()
            // For more than 26 options, use AA, AB, etc.
            return "A${('A' + (index - 26))}"
        }

        fun letterToIndex(letter: String): Int? {
            val upper = letter.uppercase().trim()
            if (upper.length == 1 && upper[0] in 'A'..'Z') {
                return upper[0] - 'A'
            }
            if (upper.length == 2 && upper[0] == 'A' && upper[1] in 'A'..'Z') {
                return 26 + (upper[1] - 'A')
            }
            return null
        }
    }
}

/**
 * Card display info for mulligan/bottom-cards decisions.
 */
data class MulliganCardDisplay(
    val name: String,
    val manaCost: String? = null,
    val typeLine: String? = null,
    val power: Int? = null,
    val toughness: Int? = null
)
