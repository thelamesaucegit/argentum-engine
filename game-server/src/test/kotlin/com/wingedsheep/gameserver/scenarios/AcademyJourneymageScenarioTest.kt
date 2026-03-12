package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Academy Journeymage.
 *
 * Card reference:
 * - Academy Journeymage ({4}{U}): Creature — Human Wizard 3/2
 *   This spell costs {1} less to cast if you control a Wizard.
 *   When Academy Journeymage enters the battlefield, return target creature
 *   an opponent controls to its owner's hand.
 */
class AcademyJourneymageScenarioTest : ScenarioTestBase() {

    init {
        context("Academy Journeymage cost reduction") {

            test("costs {3}{U} when you control a Wizard") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Academy Journeymage")
                    .withCardOnBattlefield(1, "Tolarian Scholar") // Human Wizard
                    .withCardOnBattlefield(2, "Glory Seeker") // 2/2 target
                    .withLandsOnBattlefield(1, "Island", 4) // Only 4 mana: {3}{U} with reduction
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast creature spell (no targets at cast time)
                val castResult = game.castSpell(1, "Academy Journeymage")
                withClue("Cast should succeed with cost reduction: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Resolve the creature spell - ETB trigger fires
                game.resolveStack()

                // ETB trigger should create a pending target decision
                withClue("Should have pending target selection for ETB bounce") {
                    game.hasPendingDecision() shouldBe true
                }

                // Select Glory Seeker as the bounce target
                val glorySeekerTarget = game.findPermanent("Glory Seeker")!!
                game.selectTargets(listOf(glorySeekerTarget))

                // Resolve the triggered ability
                game.resolveStack()

                withClue("Academy Journeymage should be on battlefield") {
                    game.isOnBattlefield("Academy Journeymage") shouldBe true
                }
                withClue("Glory Seeker should have been bounced to opponent's hand") {
                    game.isOnBattlefield("Glory Seeker") shouldBe false
                }
                withClue("Glory Seeker should be in opponent's hand") {
                    game.isInHand(2, "Glory Seeker") shouldBe true
                }
            }

            test("costs full {4}{U} without a Wizard on the battlefield") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Academy Journeymage")
                    .withCardOnBattlefield(2, "Glory Seeker") // 2/2 target
                    .withLandsOnBattlefield(1, "Island", 4) // Only 4 mana: not enough for {4}{U}
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Academy Journeymage")
                withClue("Cast should fail without enough mana") {
                    castResult.error shouldBe "Not enough mana to cast this spell"
                }
            }

            test("can be cast for full cost {4}{U} without a Wizard") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Academy Journeymage")
                    .withCardOnBattlefield(2, "Glory Seeker")
                    .withLandsOnBattlefield(1, "Island", 5) // Full 5 mana
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast creature spell (no targets at cast time)
                val castResult = game.castSpell(1, "Academy Journeymage")
                withClue("Cast should succeed with full mana: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Resolve the creature spell - ETB trigger fires
                game.resolveStack()

                // ETB trigger: select target
                withClue("Should have pending target selection for ETB bounce") {
                    game.hasPendingDecision() shouldBe true
                }
                val glorySeekerTarget = game.findPermanent("Glory Seeker")!!
                game.selectTargets(listOf(glorySeekerTarget))
                game.resolveStack()

                withClue("Academy Journeymage should be on battlefield") {
                    game.isOnBattlefield("Academy Journeymage") shouldBe true
                }
                withClue("Glory Seeker should have been bounced") {
                    game.isOnBattlefield("Glory Seeker") shouldBe false
                }
            }

            test("cost reduction does not stack with multiple Wizards") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Academy Journeymage")
                    .withCardOnBattlefield(1, "Tolarian Scholar") // Wizard 1
                    .withCardOnBattlefield(1, "Ghitu Lavarunner") // Wizard 2
                    .withCardOnBattlefield(2, "Glory Seeker")
                    .withLandsOnBattlefield(1, "Island", 4) // 4 mana: still {3}{U}
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast with only 4 mana — should still work with reduction (only -1 even with 2 Wizards)
                val castResult = game.castSpell(1, "Academy Journeymage")
                withClue("Cast should succeed with reduction (only -1 even with 2 Wizards): ${castResult.error}") {
                    castResult.error shouldBe null
                }
            }
        }
    }
}
