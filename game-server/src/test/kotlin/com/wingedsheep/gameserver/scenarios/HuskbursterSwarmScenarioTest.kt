package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Huskburster Swarm.
 *
 * Huskburster Swarm {7}{B}
 * Creature — Elemental Insect
 * 6/6
 *
 * This spell costs {1} less to cast for each creature card you own in exile
 * and in your graveyard.
 * Menace, deathtouch
 */
class HuskbursterSwarmScenarioTest : ScenarioTestBase() {

    init {
        context("Huskburster Swarm cost reduction") {
            test("costs less with creatures in graveyard") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Huskburster Swarm")
                    // 3 creatures in graveyard = costs {4}{B} instead of {7}{B}
                    .withCardInGraveyard(1, "Glory Seeker")
                    .withCardInGraveyard(1, "Glory Seeker")
                    .withCardInGraveyard(1, "Glory Seeker")
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Should be castable with 5 lands (costs {4}{B} with 3 creatures in graveyard)
                val result = game.castSpell(1, "Huskburster Swarm")
                result.error shouldBe null

                game.resolveStack()

                // Should be on battlefield
                game.findPermanent("Huskburster Swarm") shouldNotBe null
            }

            test("costs less with creatures in exile") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Huskburster Swarm")
                    // 3 creatures in exile = costs {4}{B} instead of {7}{B}
                    .withCardInExile(1, "Glory Seeker")
                    .withCardInExile(1, "Glory Seeker")
                    .withCardInExile(1, "Glory Seeker")
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Should be castable with 5 lands
                val result = game.castSpell(1, "Huskburster Swarm")
                result.error shouldBe null

                game.resolveStack()

                game.findPermanent("Huskburster Swarm") shouldNotBe null
            }

            test("counts both exile and graveyard together") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Huskburster Swarm")
                    // 2 in graveyard + 2 in exile = 4 creatures = costs {3}{B}
                    .withCardInGraveyard(1, "Glory Seeker")
                    .withCardInGraveyard(1, "Glory Seeker")
                    .withCardInExile(1, "Glory Seeker")
                    .withCardInExile(1, "Glory Seeker")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Should be castable with 4 lands (costs {3}{B})
                val result = game.castSpell(1, "Huskburster Swarm")
                result.error shouldBe null

                game.resolveStack()

                game.findPermanent("Huskburster Swarm") shouldNotBe null
            }

            test("not castable without enough reduction") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Huskburster Swarm")
                    // Only 1 creature in graveyard = costs {6}{B}, need 7 lands
                    .withCardInGraveyard(1, "Glory Seeker")
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Should NOT be castable with 5 lands (costs {6}{B})
                val result = game.castSpell(1, "Huskburster Swarm")
                result.error shouldNotBe null
            }
        }
    }
}
