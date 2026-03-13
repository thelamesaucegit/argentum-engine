package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DeflectDamageSourceChoiceContinuation
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.DeflectNextDamageFromChosenSourceEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for DeflectNextDamageFromChosenSourceEffect.
 *
 * Presents the controller with a choice of all game objects that could be damage sources
 * (permanents on the battlefield + spells on the stack), then creates a floating effect
 * that prevents the next damage from the chosen source and deals that much to the
 * source's controller.
 */
class DeflectNextDamageFromChosenSourceExecutor : EffectExecutor<DeflectNextDamageFromChosenSourceEffect> {

    override val effectType: KClass<DeflectNextDamageFromChosenSourceEffect> =
        DeflectNextDamageFromChosenSourceEffect::class

    override fun execute(
        state: GameState,
        effect: DeflectNextDamageFromChosenSourceEffect,
        context: EffectContext
    ): ExecutionResult {
        val controllerId = context.controllerId

        // Gather all possible sources: permanents on battlefield + spells on stack
        val sourceIds = mutableListOf<EntityId>()

        for (entityId in state.getBattlefield()) {
            if (state.getEntity(entityId)?.get<CardComponent>() != null) {
                sourceIds.add(entityId)
            }
        }

        for (entityId in state.stack) {
            if (state.getEntity(entityId)?.get<CardComponent>() != null) {
                sourceIds.add(entityId)
            }
        }

        if (sourceIds.isEmpty()) {
            return ExecutionResult.success(state)
        }

        val decisionId = UUID.randomUUID().toString()

        val decision = SelectCardsDecision(
            id = decisionId,
            playerId = controllerId,
            prompt = "Choose a source of damage",
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }
            ),
            options = sourceIds,
            minSelections = 1,
            maxSelections = 1,
            useTargetingUI = true
        )

        val continuation = DeflectDamageSourceChoiceContinuation(
            decisionId = decisionId,
            controllerId = controllerId,
            sourceId = context.sourceId,
            sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }
        )

        val newState = state.withPendingDecision(decision).pushContinuation(continuation)
        return ExecutionResult.paused(newState, decision)
    }
}
