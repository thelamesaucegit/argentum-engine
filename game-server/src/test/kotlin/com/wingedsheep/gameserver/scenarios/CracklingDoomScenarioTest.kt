package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class CracklingDoomScenarioTest : ScenarioTestBase() {

    init {
        context("Crackling Doom") {
            test("deals 2 damage to opponent and forces sacrifice of greatest power creature") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Crackling Doom")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardOnBattlefield(2, "Grizzly Bears")    // 2/2
                    .withCardOnBattlefield(2, "Hill Giant")       // 3/3
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Crackling Doom (no targets)
                game.castSpell(1, "Crackling Doom")
                game.resolveStack()

                // Opponent should have taken 2 damage (20 - 2 = 18)
                game.getLifeTotal(2) shouldBe 18

                // Hill Giant (greatest power = 3) should be sacrificed automatically
                game.findPermanent("Hill Giant") shouldBe null
                // Grizzly Bears should survive
                game.findPermanent("Grizzly Bears") shouldNotBe null
            }

            test("opponent chooses when multiple creatures tied for greatest power") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Crackling Doom")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardOnBattlefield(2, "Hill Giant")       // 3/3
                    .withCardOnBattlefield(2, "Raging Minotaur")  // 3/3
                    .withCardOnBattlefield(2, "Grizzly Bears")    // 2/2
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Crackling Doom")
                game.resolveStack()

                // At this point, opponent should be prompted to choose between
                // Hill Giant and Raging Minotaur (both power 3)
                val hillGiantId = game.findPermanent("Hill Giant")!!

                // Select Hill Giant to sacrifice
                game.selectCards(listOf(hillGiantId))

                // Opponent took 2 damage
                game.getLifeTotal(2) shouldBe 18
                // Hill Giant sacrificed, Raging Minotaur and Grizzly Bears survive
                game.findPermanent("Hill Giant") shouldBe null
                game.findPermanent("Raging Minotaur") shouldNotBe null
                game.findPermanent("Grizzly Bears") shouldNotBe null
            }

            test("does nothing if opponent controls no creatures") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Crackling Doom")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Crackling Doom")
                game.resolveStack()

                // Opponent should still take 2 damage even without creatures
                game.getLifeTotal(2) shouldBe 18
            }
        }
    }
}
