package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Demonic Vigor.
 *
 * Card reference:
 * - Demonic Vigor ({B}): Enchantment — Aura
 *   "Enchant creature"
 *   "Enchanted creature gets +1/+1."
 *   "When enchanted creature dies, return that card to its owner's hand."
 */
class DemonicVigorScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Demonic Vigor aura with death trigger") {

            test("enchanted creature returns to hand when destroyed") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Cabal Evangel") // 2/2 creature
                    .withCardInHand(1, "Demonic Vigor")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInHand(2, "Cast Down") // {1}{B} destroy nonlegendary
                    .withLandsOnBattlefield(2, "Swamp", 2)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val creatureId = game.findPermanent("Cabal Evangel")!!

                // Cast Demonic Vigor targeting Cabal Evangel
                val castResult = game.castSpell(1, "Demonic Vigor", creatureId)
                withClue("Demonic Vigor should cast successfully") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                withClue("Demonic Vigor should be on the battlefield") {
                    game.isOnBattlefield("Demonic Vigor") shouldBe true
                }

                // Pass priority to opponent
                game.passPriority()

                // Opponent casts Cast Down on the enchanted creature
                val destroyResult = game.castSpell(2, "Cast Down", creatureId)
                withClue("Cast Down should cast successfully") {
                    destroyResult.error shouldBe null
                }

                // Resolve Cast Down — creature dies, Demonic Vigor triggers
                game.resolveStack()

                // Demonic Vigor's trigger should have returned the creature to hand
                withClue("Cabal Evangel should be returned to Player's hand") {
                    game.isInHand(1, "Cabal Evangel") shouldBe true
                }
                withClue("Cabal Evangel should not be in graveyard") {
                    game.isInGraveyard(1, "Cabal Evangel") shouldBe false
                }
                withClue("Demonic Vigor should be in graveyard (aura falls off)") {
                    game.isInGraveyard(1, "Demonic Vigor") shouldBe true
                }
            }

            test("enchanted creature gives +1/+1 while on battlefield") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Cabal Evangel") // 2/2
                    .withCardInHand(1, "Demonic Vigor")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val creatureId = game.findPermanent("Cabal Evangel")!!

                // Cast Demonic Vigor targeting Cabal Evangel
                game.castSpell(1, "Demonic Vigor", creatureId)
                game.resolveStack()

                // Creature should be 3/3 (2/2 base + 1/1 from aura)
                val projected = stateProjector.project(game.state)
                withClue("Cabal Evangel power should be 3 (2 base + 1 from Demonic Vigor)") {
                    projected.getPower(creatureId) shouldBe 3
                }
                withClue("Cabal Evangel toughness should be 3 (2 base + 1 from Demonic Vigor)") {
                    projected.getToughness(creatureId) shouldBe 3
                }
            }
        }
    }
}
