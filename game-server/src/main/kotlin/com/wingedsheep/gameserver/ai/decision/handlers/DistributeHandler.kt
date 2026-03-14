package com.wingedsheep.gameserver.ai.decision.handlers

import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.DistributeDecision
import com.wingedsheep.engine.core.DistributionResponse
import com.wingedsheep.gameserver.ai.AiResponseParser
import com.wingedsheep.gameserver.ai.GameStateFormatter
import com.wingedsheep.gameserver.ai.decision.AiDecisionHandler
import com.wingedsheep.gameserver.dto.ClientGameState
import com.wingedsheep.sdk.model.EntityId
import kotlin.reflect.KClass

class DistributeHandler : AiDecisionHandler<DistributeDecision> {
    override val decisionType: KClass<DistributeDecision> = DistributeDecision::class

    override fun autoResolve(decision: DistributeDecision): DecisionResponse {
        throw UnsupportedOperationException()
    }

    override fun format(
        sb: StringBuilder,
        decision: DistributeDecision,
        state: ClientGameState,
        labels: Map<EntityId, String>
    ) {
        sb.appendLine("Distribute ${decision.totalAmount} among targets (min ${decision.minPerTarget} each):")
        for ((j, tid) in decision.targets.withIndex()) {
            val card = state.cards[tid]
            val name = card?.name ?: "Player"
            val label = labels[tid] ?: tid.value
            sb.appendLine("  [${GameStateFormatter.actionLetter(j)}] [$label] $name")
        }
        sb.appendLine("Reply with amounts per target (e.g., \"A:2, B:1\").")
    }

    override fun parse(
        response: String,
        decision: DistributeDecision,
        state: ClientGameState,
        parser: AiResponseParser
    ): DecisionResponse? {
        val dist = parser.parseDistribution(response, decision.targets.size, decision.totalAmount)
            ?: return null
        val entityDist = dist.entries.associate { (idx, amount) -> decision.targets[idx] to amount }
        return DistributionResponse(decisionId = decision.id, distribution = entityDist)
    }

    override fun heuristic(decision: DistributeDecision, state: ClientGameState): DecisionResponse {
        val perTarget = decision.totalAmount / decision.targets.size
        val remainder = decision.totalAmount % decision.targets.size
        val dist = decision.targets.withIndex().associate { (i, tid) ->
            tid to (perTarget + if (i < remainder) 1 else 0)
        }
        return DistributionResponse(decisionId = decision.id, distribution = dist)
    }
}
