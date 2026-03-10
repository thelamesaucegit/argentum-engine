package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Sidisi, Brood Tyrant.
 *
 * Sidisi, Brood Tyrant: {1}{B}{G}{U}
 * Legendary Creature — Snake Shaman 3/3
 * Whenever Sidisi, Brood Tyrant enters the battlefield or attacks, mill three cards.
 * Whenever one or more creature cards are put into your graveyard from your library,
 * create a 2/2 black Zombie creature token.
 */
class SidisiBroodTyrantScenarioTest : ScenarioTestBase() {

    init {
        context("Sidisi ETB mill trigger") {

            test("mills three cards when entering the battlefield - no creatures milled") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Sidisi, Brood Tyrant")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardOnBattlefield(1, "Swamp")
                    .withCardOnBattlefield(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Sidisi, Brood Tyrant")
                game.resolveStack()

                game.graveyardSize(1) shouldBe 3
                game.isOnBattlefield("Sidisi, Brood Tyrant") shouldBe true
                // No zombie token because no creatures were milled
                game.findAllPermanents("Zombie Token").size shouldBe 0
            }

            test("creates zombie token when creature is milled") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Sidisi, Brood Tyrant")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardOnBattlefield(1, "Swamp")
                    .withCardOnBattlefield(1, "Forest")
                    .withCardInLibrary(1, "Hill Giant")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Sidisi, Brood Tyrant")
                game.resolveStack()

                game.findAllPermanents("Zombie Token").size shouldBe 1
            }

            test("creates only one zombie even when multiple creatures are milled") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Sidisi, Brood Tyrant")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardOnBattlefield(1, "Swamp")
                    .withCardOnBattlefield(1, "Forest")
                    .withCardInLibrary(1, "Hill Giant")
                    .withCardInLibrary(1, "Hill Giant")
                    .withCardInLibrary(1, "Hill Giant")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Sidisi, Brood Tyrant")
                game.resolveStack()

                // Only one zombie should be created (batching trigger)
                game.findAllPermanents("Zombie Token").size shouldBe 1
            }
        }

        context("Sidisi attack mill trigger") {

            test("mills three cards and creates zombie when attacking") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Sidisi, Brood Tyrant")
                    .withCardInLibrary(1, "Hill Giant")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Sidisi, Brood Tyrant" to 2))
                game.resolveStack()

                game.graveyardSize(1) shouldBe 3
                game.findAllPermanents("Zombie Token").size shouldBe 1
            }

            test("no zombie when only non-creatures milled from attack") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Sidisi, Brood Tyrant")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Sidisi, Brood Tyrant" to 2))
                game.resolveStack()

                game.graveyardSize(1) shouldBe 3
                game.findAllPermanents("Zombie Token").size shouldBe 0
            }
        }
    }
}
