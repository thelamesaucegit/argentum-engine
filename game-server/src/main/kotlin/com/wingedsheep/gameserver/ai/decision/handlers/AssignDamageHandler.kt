package com.wingedsheep.gameserver.ai.decision.handlers

import com.wingedsheep.engine.core.AssignDamageDecision
import com.wingedsheep.engine.core.DamageAssignmentResponse
import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.gameserver.ai.AiResponseParser
import com.wingedsheep.gameserver.ai.GameStateFormatter
import com.wingedsheep.gameserver.ai.decision.AiDecisionHandler
import com.wingedsheep.gameserver.dto.ClientGameState
import com.wingedsheep.sdk.model.EntityId
import kotlin.reflect.KClass

class AssignDamageHandler : AiDecisionHandler<AssignDamageDecision> {
    override val decisionType: KClass<AssignDamageDecision> = AssignDamageDecision::class

    override fun canAutoResolve(decision: AssignDamageDecision): Boolean = true

    override fun autoResolve(decision: AssignDamageDecision): DecisionResponse {
        return DamageAssignmentResponse(decisionId = decision.id, assignments = decision.defaultAssignments)
    }

    override fun format(
        sb: StringBuilder,
        decision: AssignDamageDecision,
        state: ClientGameState,
        labels: Map<EntityId, String>
    ) {
        sb.appendLine("Assign ${decision.availablePower} damage:")
        for ((j, tid) in decision.orderedTargets.withIndex()) {
            val name = state.cards[tid]?.name ?: "Creature"
            val min = decision.minimumAssignments[tid] ?: 0
            sb.appendLine("  [${GameStateFormatter.actionLetter(j)}] $name (min: $min)")
        }
        if (decision.hasTrample && decision.defenderId != null) {
            sb.appendLine("  Remaining goes to defending player (trample)")
        }
    }

    override fun parse(
        response: String,
        decision: AssignDamageDecision,
        state: ClientGameState,
        parser: AiResponseParser
    ): DecisionResponse {
        return DamageAssignmentResponse(decisionId = decision.id, assignments = decision.defaultAssignments)
    }

    override fun heuristic(decision: AssignDamageDecision, state: ClientGameState): DecisionResponse {
        return DamageAssignmentResponse(decisionId = decision.id, assignments = decision.defaultAssignments)
    }
}
