package com.wingedsheep.gameserver.ai.decision.handlers

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.gameserver.ai.AiResponseParser
import com.wingedsheep.gameserver.ai.GameStateFormatter
import com.wingedsheep.gameserver.ai.decision.AiDecisionHandler
import com.wingedsheep.gameserver.dto.ClientGameState
import com.wingedsheep.sdk.model.EntityId
import kotlin.reflect.KClass

class ChooseOptionHandler : AiDecisionHandler<ChooseOptionDecision> {
    override val decisionType: KClass<ChooseOptionDecision> = ChooseOptionDecision::class

    override fun autoResolve(decision: ChooseOptionDecision): DecisionResponse {
        throw UnsupportedOperationException()
    }

    override fun format(
        sb: StringBuilder,
        decision: ChooseOptionDecision,
        state: ClientGameState,
        labels: Map<EntityId, String>
    ) {
        for ((j, option) in decision.options.withIndex()) {
            sb.appendLine("  [${GameStateFormatter.actionLetter(j)}] $option")
        }
    }

    override fun parse(
        response: String,
        decision: ChooseOptionDecision,
        state: ClientGameState,
        parser: AiResponseParser
    ): DecisionResponse? {
        val index = parser.parseActionChoice(response, decision.options.size - 1) ?: return null
        return OptionChosenResponse(decisionId = decision.id, optionIndex = index)
    }

    override fun heuristic(decision: ChooseOptionDecision, state: ClientGameState): DecisionResponse {
        return OptionChosenResponse(decisionId = decision.id, optionIndex = 0)
    }
}
