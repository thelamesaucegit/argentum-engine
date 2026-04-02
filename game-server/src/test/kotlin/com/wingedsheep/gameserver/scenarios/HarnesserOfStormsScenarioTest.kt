package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Harnesser of Storms.
 *
 * Harnesser of Storms {2}{R}
 * Creature — Otter Wizard
 * 1/4
 *
 * Whenever you cast a noncreature or Otter spell, you may exile the top card
 * of your library. Until end of turn, you may play that card.
 * This ability triggers only once each turn.
 */
class HarnesserOfStormsScenarioTest : ScenarioTestBase() {

    init {
        context("Harnesser of Storms trigger - noncreature or Otter") {
            test("triggers when casting a noncreature spell") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Harnesser of Storms")
                    .withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Shock", 2)
                // Trigger goes on stack; resolve until MayEffect decision
                game.resolveStack()
                game.answerYesNo(true)
                game.resolveStack()

                game.state.getExile(game.player1Id).size shouldBe 1
            }

            test("triggers when casting an Otter creature spell") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Harnesser of Storms")
                    .withCardInHand(1, "Stormcatch Mentor")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Stormcatch Mentor")
                game.resolveStack()
                game.answerYesNo(true)
                game.resolveStack()

                game.state.getExile(game.player1Id).size shouldBe 1
            }

            test("does NOT trigger when casting a non-Otter creature spell") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Harnesser of Storms")
                    .withCardInHand(1, "Glory Seeker")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Glory Seeker")
                game.resolveStack()

                game.state.getExile(game.player1Id).size shouldBe 0
            }

            test("triggers only once per turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Harnesser of Storms")
                    .withCardInHand(1, "Shock")
                    .withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // First noncreature spell — triggers
                game.castSpellTargetingPlayer(1, "Shock", 2)
                game.resolveStack()
                game.answerYesNo(true)
                game.resolveStack()

                game.state.getExile(game.player1Id).size shouldBe 1

                // Second noncreature spell — should NOT trigger (once per turn)
                game.castSpellTargetingPlayer(1, "Shock", 2)
                game.resolveStack()

                // Still only one card in exile
                game.state.getExile(game.player1Id).size shouldBe 1
            }
        }
    }
}
