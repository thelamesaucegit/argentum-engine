package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect

/**
 * Fungal Infection
 * {B}
 * Instant
 * Target creature gets -1/-1 until end of turn. Create a 1/1 green Saproling creature token.
 */
val FungalInfection = card("Fungal Infection") {
    manaCost = "{B}"
    typeLine = "Instant"
    oracleText = "Target creature gets -1/-1 until end of turn. Create a 1/1 green Saproling creature token."

    spell {
        val t = target("target", Targets.Creature)
        effect = Effects.ModifyStats(-1, -1, t)
            .then(
                CreateTokenEffect(
                    count = 1,
                    power = 1,
                    toughness = 1,
                    colors = setOf(Color.GREEN),
                    creatureTypes = setOf("Saproling"),
                    imageUri = "https://cards.scryfall.io/normal/front/3/4/34032448-fe31-44c7-845c-37fea0b8e762.jpg?1767955055"
                )
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "94"
        artist = "Filip Burburan"
        flavorText = "\"To thallids, the whole world is just a pile of mulch to grow saprolings in.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/3/a3f6baa5-666d-40b0-82e8-b0df10ff3cdd.jpg?1562740631"
    }
}
