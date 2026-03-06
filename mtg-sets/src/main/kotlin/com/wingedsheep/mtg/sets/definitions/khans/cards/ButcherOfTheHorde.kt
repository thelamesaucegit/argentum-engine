package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Butcher of the Horde
 * {1}{R}{W}{B}
 * Creature — Demon
 * 5/4
 * Flying
 * Sacrifice another creature: Butcher of the Horde gains your choice of vigilance, lifelink,
 * or haste until end of turn.
 */
val ButcherOfTheHorde = card("Butcher of the Horde") {
    manaCost = "{1}{R}{W}{B}"
    typeLine = "Creature — Demon"
    power = 5
    toughness = 4
    oracleText = "Flying\nSacrifice another creature: This creature gains your choice of vigilance, lifelink, or haste until end of turn."

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.SacrificeAnother(GameObjectFilter.Creature)
        effect = Effects.GrantKeyword(Keyword.VIGILANCE, EffectTarget.Self)
        description = "Sacrifice another creature: This creature gains vigilance until end of turn."
    }

    activatedAbility {
        cost = Costs.SacrificeAnother(GameObjectFilter.Creature)
        effect = Effects.GrantKeyword(Keyword.LIFELINK, EffectTarget.Self)
        description = "Sacrifice another creature: This creature gains lifelink until end of turn."
    }

    activatedAbility {
        cost = Costs.SacrificeAnother(GameObjectFilter.Creature)
        effect = Effects.GrantKeyword(Keyword.HASTE, EffectTarget.Self)
        description = "Sacrifice another creature: This creature gains haste until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "168"
        artist = "Karl Kopinski"
        flavorText = "Mardu ones provide the Butcher with an unending supply of fresh souls."
        imageUri = "https://cards.scryfall.io/normal/front/4/c/4c76027b-9c8d-4b59-b35d-01857e4ca1a4.jpg?1562786278"
    }
}
