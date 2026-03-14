package com.wingedsheep.gameserver.ai.decision.handlers

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.gameserver.ai.AiResponseParser
import com.wingedsheep.gameserver.ai.GameStateFormatter
import com.wingedsheep.gameserver.ai.decision.AiDecisionHandler
import com.wingedsheep.gameserver.dto.ClientGameState
import com.wingedsheep.sdk.model.EntityId
import kotlin.reflect.KClass

class ChooseTargetsHandler : AiDecisionHandler<ChooseTargetsDecision> {
    override val decisionType: KClass<ChooseTargetsDecision> = ChooseTargetsDecision::class

    override fun autoResolve(decision: ChooseTargetsDecision): DecisionResponse {
        throw UnsupportedOperationException()
    }

    override fun format(
        sb: StringBuilder,
        decision: ChooseTargetsDecision,
        state: ClientGameState,
        labels: Map<EntityId, String>
    ) {
        for (req in decision.targetRequirements) {
            sb.appendLine("Target ${req.index + 1}: ${req.description} (choose ${req.minTargets}-${req.maxTargets})")
            val validIds = decision.legalTargets[req.index] ?: emptyList()
            for ((j, tid) in validIds.withIndex()) {
                val card = state.cards[tid]
                val label = labels[tid] ?: tid.value
                val letter = GameStateFormatter.actionLetter(j)
                if (card != null) {
                    val owner = if (card.controllerId == state.viewingPlayerId) "your" else "opponent's"
                    val stats = if (card.power != null) " ${card.power}/${card.toughness}" else ""
                    sb.appendLine("  [$letter] [$label] $owner ${card.name}$stats")
                } else {
                    val playerName = if (tid == state.viewingPlayerId) "you" else "opponent"
                    sb.appendLine("  [$letter] [$label] $playerName")
                }
            }
        }
    }

    override fun parse(
        response: String,
        decision: ChooseTargetsDecision,
        state: ClientGameState,
        parser: AiResponseParser
    ): DecisionResponse? {
        return if (decision.targetRequirements.size == 1) {
            val req = decision.targetRequirements[0]
            val validTargets = decision.legalTargets[req.index] ?: return null
            val index = parser.parseActionChoice(response, validTargets.size - 1) ?: return null
            TargetsResponse(
                decisionId = decision.id,
                selectedTargets = mapOf(req.index to listOf(validTargets[index]))
            )
        } else {
            val result = mutableMapOf<Int, List<EntityId>>()
            for (req in decision.targetRequirements) {
                val validTargets = decision.legalTargets[req.index] ?: continue
                val index = parser.parseActionChoice(response, validTargets.size - 1)
                if (index != null) {
                    result[req.index] = listOf(validTargets[index])
                }
            }
            if (result.isNotEmpty()) {
                TargetsResponse(decisionId = decision.id, selectedTargets = result)
            } else null
        }
    }

    override fun heuristic(decision: ChooseTargetsDecision, state: ClientGameState): DecisionResponse {
        val myId = state.viewingPlayerId
        val targets = decision.targetRequirements.associate { req ->
            val valid = decision.legalTargets[req.index] ?: emptyList()
            // Prefer opponent's creatures for targeting heuristic (most targets are harmful)
            val opponentTargets = valid.filter { tid ->
                val card = state.cards[tid]
                card != null && card.controllerId != myId
            }
            val preferred = if (opponentTargets.isNotEmpty()) opponentTargets else valid
            // Pick highest-power targets first
            val sorted = preferred.sortedByDescending { tid -> state.cards[tid]?.power ?: 0 }
            req.index to sorted.take(req.minTargets)
        }
        return TargetsResponse(decisionId = decision.id, selectedTargets = targets)
    }
}
