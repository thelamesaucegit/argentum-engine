package com.wingedsheep.gameserver.ai.decision.handlers

import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.gameserver.ai.AiResponseParser
import com.wingedsheep.gameserver.ai.decision.AiDecisionHandler
import com.wingedsheep.gameserver.dto.ClientGameState
import com.wingedsheep.sdk.model.EntityId
import kotlin.reflect.KClass

class YesNoHandler : AiDecisionHandler<YesNoDecision> {
    override val decisionType: KClass<YesNoDecision> = YesNoDecision::class

    override fun autoResolve(decision: YesNoDecision): DecisionResponse {
        throw UnsupportedOperationException()
    }

    override fun format(
        sb: StringBuilder,
        decision: YesNoDecision,
        state: ClientGameState,
        labels: Map<EntityId, String>
    ) {
        sb.appendLine("[A] ${decision.yesText}")
        sb.appendLine("[B] ${decision.noText}")
    }

    override fun parse(
        response: String,
        decision: YesNoDecision,
        state: ClientGameState,
        parser: AiResponseParser
    ): DecisionResponse? {
        val choice = parser.parseYesNo(response) ?: return null
        return YesNoResponse(decisionId = decision.id, choice = choice)
    }

    override fun heuristic(decision: YesNoDecision, state: ClientGameState): DecisionResponse {
        return YesNoResponse(decisionId = decision.id, choice = true)
    }
}
