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
                val name = card?.name ?: "Player"
                val letter = GameStateFormatter.actionLetter(j)
                sb.appendLine("  [$letter] [$label] $name${card?.let { " ${it.power ?: ""}/${it.toughness ?: ""}" } ?: ""}")
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
        val targets = decision.targetRequirements.associate { req ->
            val valid = decision.legalTargets[req.index] ?: emptyList()
            req.index to valid.take(req.minTargets)
        }
        return TargetsResponse(decisionId = decision.id, selectedTargets = targets)
    }
}
