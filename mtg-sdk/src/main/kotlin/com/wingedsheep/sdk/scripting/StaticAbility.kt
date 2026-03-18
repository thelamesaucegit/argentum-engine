package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.text.TextReplaceable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Static abilities provide continuous effects that don't use the stack.
 * These include effects from enchantments, equipment, and other permanents.
 *
 * Static abilities are data objects - application is handled by the ECS
 * layer system (StateProjector) which calculates the projected game state.
 *
 * Concrete subtypes are organized into categorized files:
 * - KeywordStaticAbilities.kt - keyword grants
 * - AbilityGrantStaticAbilities.kt - triggered/activated ability grants
 * - StatsStaticAbilities.kt - power/toughness modifications
 * - CombatStaticAbilities.kt - attack/block restrictions, combat damage
 * - BlockingStaticAbilities.kt - blocking evasion and restrictions
 * - CostStaticAbilities.kt - spell cost modifications
 * - TypeStaticAbilities.kt - type/subtype/color changes
 * - ProtectionStaticAbilities.kt - protection and targeting restrictions
 * - SpellStaticAbilities.kt - casting permissions and spell modifications
 * - MiscStaticAbilities.kt - miscellaneous static abilities
 */
@Serializable
sealed interface StaticAbility : TextReplaceable<StaticAbility> {
    val description: String
}

/**
 * Target for static abilities (what the ability affects).
 */
@Serializable
sealed interface StaticTarget {
    @SerialName("AttachedCreature")
    @Serializable
    data object AttachedCreature : StaticTarget

    @SerialName("SourceCreature")
    @Serializable
    data object SourceCreature : StaticTarget

    @SerialName("Controller")
    @Serializable
    data object Controller : StaticTarget

    @SerialName("AllControlledCreatures")
    @Serializable
    data object AllControlledCreatures : StaticTarget

    @SerialName("SpecificCard")
    @Serializable
    data class SpecificCard(val entityId: EntityId) : StaticTarget
}
