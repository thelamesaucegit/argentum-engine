package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect
import com.wingedsheep.engine.mechanics.layers.FloatingEffectData
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CantBlockGroupEffect
import kotlin.reflect.KClass

/**
 * Executor for CantBlockGroupEffect.
 * "Creatures can't block this turn." / "[filter] creatures can't block this turn."
 *
 * Creates a floating effect with SetCantBlock that dynamically applies to all creatures
 * matching the filter. Per Rule 611.2c, rule-modifying effects like "can't block" apply
 * to all matching objects including those entering the battlefield after the effect resolves.
 */
class CantBlockGroupExecutor : EffectExecutor<CantBlockGroupEffect> {

    override val effectType: KClass<CantBlockGroupEffect> = CantBlockGroupEffect::class

    override fun execute(
        state: GameState,
        effect: CantBlockGroupEffect,
        context: EffectContext
    ): ExecutionResult {
        val floatingEffect = ActiveFloatingEffect(
            id = EntityId.generate(),
            effect = FloatingEffectData(
                layer = Layer.ABILITY,
                sublayer = null,
                modification = SerializableModification.SetCantBlock,
                affectedEntities = emptySet(),
                dynamicGroupFilter = effect.filter
            ),
            duration = effect.duration,
            sourceId = context.sourceId,
            sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name },
            controllerId = context.controllerId,
            timestamp = System.currentTimeMillis()
        )

        val newState = state.copy(
            floatingEffects = state.floatingEffects + floatingEffect
        )

        return ExecutionResult.success(newState)
    }
}
