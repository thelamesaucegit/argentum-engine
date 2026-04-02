package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Bushy Bodyguard
 * {1}{G}
 * Creature — Squirrel Warrior
 * 2/1
 *
 * Offspring {2}
 * When this creature enters, you may forage. If you do, put two +1/+1 counters on it.
 * (To forage, exile three cards from your graveyard or sacrifice a Food.)
 */
val BushyBodyguard = card("Bushy Bodyguard") {
    manaCost = "{1}{G}"
    typeLine = "Creature — Squirrel Warrior"
    power = 2
    toughness = 1
    oracleText = "Offspring {2} (You may pay an additional {2} as you cast this spell. If you do, when this creature enters, create a 1/1 token copy of it.)\nWhen this creature enters, you may forage. If you do, put two +1/+1 counters on it. (To forage, exile three cards from your graveyard or sacrifice a Food.)"

    // Offspring modeled as Kicker
    keywordAbility(KeywordAbility.Kicker(ManaCost.parse("{2}")))

    // Offspring ETB: create token copy when kicked
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = WasKicked
        effect = Effects.CreateTokenCopyOfSelf(overridePower = 1, overrideToughness = 1)
    }

    // When this creature enters, you may forage. If you do, put two +1/+1 counters on it.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = MayEffect(
            effect = ModalEffect.chooseOne(
                // Mode 1: Exile 3 cards from your graveyard
                Mode.noTarget(
                    CompositeEffect(
                        listOf(
                            GatherCardsEffect(
                                source = CardSource.FromZone(Zone.GRAVEYARD, Player.You),
                                storeAs = "graveCards"
                            ),
                            SelectFromCollectionEffect(
                                from = "graveCards",
                                selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(3)),
                                storeSelected = "exileCards",
                                prompt = "Choose 3 cards from your graveyard to exile (forage)"
                            ),
                            MoveCollectionEffect(
                                from = "exileCards",
                                destination = CardDestination.ToZone(Zone.EXILE)
                            ),
                            Effects.AddCounters("+1/+1", 2, EffectTarget.Self)
                        )
                    ),
                    "Exile three cards from your graveyard"
                ),
                // Mode 2: Sacrifice a Food
                Mode.noTarget(
                    CompositeEffect(
                        listOf(
                            SacrificeEffect(
                                filter = GameObjectFilter.Any.withSubtype("Food"),
                                count = 1
                            ),
                            Effects.AddCounters("+1/+1", 2, EffectTarget.Self)
                        )
                    ),
                    "Sacrifice a Food"
                )
            ),
            description_override = "You may forage"
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "166"
        artist = "Andrea Piparo"
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0de60cf7-fa82-4b6f-9f88-6590fba5c863.jpg?1721426775"
    }
}
