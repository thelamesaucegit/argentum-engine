package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Agate Assault
 * {2}{R}
 * Sorcery
 *
 * Choose one —
 * • Agate Assault deals 4 damage to target creature. If that creature would die
 *   this turn, exile it instead.
 * • Exile target artifact.
 *
 * Note: The "exile instead of die" replacement effect on mode 1 is approximated
 * as just dealing 4 damage. The exile-on-death is rarely mechanically relevant.
 */
val AgateAssault = card("Agate Assault") {
    manaCost = "{2}{R}"
    typeLine = "Sorcery"
    oracleText = "Choose one —\n• Agate Assault deals 4 damage to target creature. If that creature " +
        "would die this turn, exile it instead.\n• Exile target artifact."

    spell {
        effect = ModalEffect.chooseOne(
            // Mode 1: Deal 4 damage to target creature
            Mode.withTarget(
                Effects.DealDamage(4, EffectTarget.ContextTarget(0)),
                Targets.Creature,
                "Deal 4 damage to target creature"
            ),
            // Mode 2: Exile target artifact
            Mode.withTarget(
                Effects.Exile(EffectTarget.ContextTarget(0)),
                Targets.Artifact,
                "Exile target artifact"
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "122"
        artist = "Slawomir Maniak"
        flavorText = "\"Only an amateur crushes their enemies with a *cold* rock.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/d/7dd9946b-515e-4e0d-9da2-711e126e9fa6.jpg?1721426563"
    }
}
