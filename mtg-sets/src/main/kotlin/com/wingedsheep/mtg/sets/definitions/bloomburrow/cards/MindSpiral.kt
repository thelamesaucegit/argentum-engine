package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.effects.OptionalCostEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Mind Spiral
 * {4}{U}
 * Sorcery
 *
 * Gift a tapped Fish (You may promise an opponent a gift as you cast this spell.
 * If you do, they create a tapped 1/1 blue Fish creature token before its other effects.)
 *
 * Target player draws three cards. If the gift was promised, tap target creature
 * an opponent controls and put a stun counter on it.
 */
val MindSpiral = card("Mind Spiral") {
    manaCost = "{4}{U}"
    typeLine = "Sorcery"
    oracleText = "Gift a tapped Fish (You may promise an opponent a gift as you cast this spell. If you do, they create a tapped 1/1 blue Fish creature token before its other effects.)\nTarget player draws three cards. If the gift was promised, tap target creature an opponent controls and put a stun counter on it."

    val drawEffect = Effects.DrawCards(3, EffectTarget.ContextTarget(0))

    spell {
        val player = target("player", Targets.Player)
        effect = OptionalCostEffect(
            cost = CreateTokenEffect(
                count = DynamicAmount.Fixed(1),
                power = 1,
                toughness = 1,
                colors = setOf(Color.BLUE),
                creatureTypes = setOf("Fish"),
                controller = EffectTarget.PlayerRef(Player.EachOpponent),
                tapped = true,
                imageUri = "https://cards.scryfall.io/normal/front/d/e/de0d6700-49f0-4233-97ba-cef7821c30ed.jpg?1721431109"
            ),
            ifPaid = drawEffect
                .then(Effects.SelectTarget(Targets.CreatureOpponentControls, "stunTarget"))
                .then(Effects.Tap(EffectTarget.PipelineTarget("stunTarget")))
                .then(Effects.AddCounters("stun", 1, EffectTarget.PipelineTarget("stunTarget"))),
            ifNotPaid = drawEffect
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "59"
        artist = "Filip Burburan"
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7e24fe6a-607b-49b8-9fca-cecb1e40de7f.jpg?1721426147"

        ruling("2024-07-26", "You may target a creature that is already tapped with Mind Spiral. If the target creature is already tapped as Mind Spiral resolves, you will still put a stun counter on it.")
        ruling("2024-07-26", "For instants and sorceries with gift, the gift is given to the appropriate opponent as part of the resolution of the spell. This happens before any of the spell's other effects would take place.")
    }
}
