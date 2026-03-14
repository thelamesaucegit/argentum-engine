package com.wingedsheep.gameserver.ai.decision.handlers

import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.NumberChosenResponse
import com.wingedsheep.gameserver.ai.AiResponseParser
import com.wingedsheep.gameserver.ai.decision.AiDecisionHandler
import com.wingedsheep.gameserver.dto.ClientGameState
import com.wingedsheep.sdk.model.EntityId
import kotlin.reflect.KClass

class ChooseNumberHandler : AiDecisionHandler<ChooseNumberDecision> {
    override val decisionType: KClass<ChooseNumberDecision> = ChooseNumberDecision::class

    override fun autoResolve(decision: ChooseNumberDecision): DecisionResponse {
        throw UnsupportedOperationException()
    }

    override fun format(
        sb: StringBuilder,
        decision: ChooseNumberDecision,
        state: ClientGameState,
        labels: Map<EntityId, String>
    ) {
        sb.appendLine("Choose a number between ${decision.minValue} and ${decision.maxValue}.")
        sb.appendLine("Reply with the number.")
    }

    override fun parse(
        response: String,
        decision: ChooseNumberDecision,
        state: ClientGameState,
        parser: AiResponseParser
    ): DecisionResponse? {
        val number = parser.parseNumber(response, decision.minValue, decision.maxValue) ?: return null
        return NumberChosenResponse(decisionId = decision.id, number = number)
    }

    override fun heuristic(decision: ChooseNumberDecision, state: ClientGameState): DecisionResponse {
        return NumberChosenResponse(decisionId = decision.id, number = decision.minValue)
    }
}
