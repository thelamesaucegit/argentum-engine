package com.wingedsheep.sdk.scripting

import kotlinx.serialization.Serializable
import java.util.concurrent.atomic.AtomicLong

/**
 * Unique identifier for an ability instance.
 */
@JvmInline
@Serializable
value class AbilityId(val value: String) {
    companion object {
        private val counter = AtomicLong(0)

        fun generate(): AbilityId = AbilityId("ability_${counter.incrementAndGet()}")

        /**
         * Create a deterministic AbilityId for a Class level-up ability.
         * Uses a fixed prefix so the engine can match the ability when activated.
         */
        fun classLevelUp(targetLevel: Int): AbilityId = AbilityId("class_level_up_$targetLevel")
    }
}
