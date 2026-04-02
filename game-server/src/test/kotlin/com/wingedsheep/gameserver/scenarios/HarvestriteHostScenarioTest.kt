package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class HarvestriteHostScenarioTest : ScenarioTestBase() {

    init {
        context("Harvestrite Host - ability resolution tracking") {
            test("first resolution gives +1/+0 but no card draw") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Harvestrite Host")
                    .withCardInHand(1, "Pawpatch Recruit") // {G} Rabbit
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.handSize(1)

                // Cast Pawpatch Recruit (a Rabbit) → triggers Harvestrite Host
                game.castSpell(1, "Pawpatch Recruit")
                game.resolveStack()

                // Trigger fires, select Harvestrite Host as target
                val hostId = game.findPermanent("Harvestrite Host")!!
                game.selectTargets(listOf(hostId))
                game.resolveStack()

                // After first trigger: Harvestrite Host should be 4/3
                val clientState = game.getClientState(1)
                val hostInfo = clientState.cards[hostId]
                withClue("Harvestrite Host should exist in client state") {
                    hostInfo shouldNotBe null
                }
                withClue("Harvestrite Host should have 4 power (+1/+0)") {
                    hostInfo!!.power shouldBe 4
                }
                withClue("Harvestrite Host toughness unchanged") {
                    hostInfo!!.toughness shouldBe 3
                }
                // No card draw on first resolution
                withClue("No card drawn on first resolution") {
                    game.handSize(1) shouldBe handBefore - 1 // -1 for casting Pawpatch Recruit
                }
            }

            test("second resolution draws a card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Harvestrite Host")
                    .withCardInHand(1, "Pawpatch Recruit") // {G} Rabbit #1
                    .withCardInHand(1, "Druid of the Spade") // {2}{G} Rabbit #2
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val hostId = game.findPermanent("Harvestrite Host")!!

                // Cast first Rabbit ({G}) → triggers Harvestrite Host
                game.castSpell(1, "Pawpatch Recruit")
                game.resolveStack()
                game.selectTargets(listOf(hostId))
                game.resolveStack()

                val handAfterFirst = game.handSize(1)

                // Cast second Rabbit ({2}{G}) → triggers Harvestrite Host (second resolution)
                game.castSpell(1, "Druid of the Spade")
                game.resolveStack()
                game.selectTargets(listOf(hostId))
                game.resolveStack()

                // After second trigger: Harvestrite Host should be 5/3
                val clientState = game.getClientState(1)
                withClue("After 2nd trigger, power should be 5") {
                    clientState.cards[hostId]!!.power shouldBe 5
                }

                // Should have drawn 1 card from second resolution
                val handAfterSecond = game.handSize(1)
                withClue("Drew 1 card from second resolution") {
                    handAfterSecond shouldBe handAfterFirst - 1 + 1 // -1 for cast, +1 for draw
                }
            }

            test("Harvestrite Host entering triggers its own ability") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Harvestrite Host")
                    .withCardOnBattlefield(1, "Pawpatch Recruit") // a Rabbit to target
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Harvestrite Host (it's a Rabbit, so self-ETB triggers)
                game.castSpell(1, "Harvestrite Host")
                game.resolveStack()

                // ETB trigger fires, target Pawpatch Recruit
                val recruitId = game.findPermanent("Pawpatch Recruit")!!
                game.selectTargets(listOf(recruitId))
                game.resolveStack()

                // Pawpatch Recruit should have gotten +1/+0
                val clientState = game.getClientState(1)
                val recruitInfo = clientState.cards[recruitId]
                withClue("Pawpatch Recruit should have 3 power (+1/+0)") {
                    recruitInfo!!.power shouldBe 3 // 2/2 base + 1/0
                }
            }
        }
    }
}
