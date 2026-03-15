package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Haphazard Bombardment.
 *
 * Card reference:
 * - Haphazard Bombardment ({5}{R}): Enchantment
 *   When Haphazard Bombardment enters the battlefield, choose four nonenchantment permanents
 *   you don't control and put an aim counter on each of them.
 *   At the beginning of your end step, if two or more permanents you don't control have an
 *   aim counter on them, destroy one of those permanents at random.
 */
class HaphazardBombardmentScenarioTest : ScenarioTestBase() {

    private fun TestGame.getAimCounters(cardName: String): Int {
        val entityId = findPermanent(cardName) ?: return 0
        return state.getEntity(entityId)
            ?.get<CountersComponent>()
            ?.getCount(CounterType.AIM) ?: 0
    }

    init {
        context("Haphazard Bombardment ETB - choose four and place aim counters") {

            test("places aim counters on chosen nonenchantment permanents opponent controls") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Haphazard Bombardment")
                    .withCardOnBattlefield(2, "Glory Seeker")      // creature
                    .withCardOnBattlefield(2, "Mammoth Spider")    // creature
                    .withCardOnBattlefield(2, "Fire Elemental")    // creature
                    .withCardOnBattlefield(2, "Juggernaut")        // artifact creature
                    .withCardOnBattlefield(2, "Serra Angel")       // 5th creature - forces choice
                    .withLandsOnBattlefield(1, "Mountain", 6)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Haphazard Bombardment
                val castResult = game.castSpell(1, "Haphazard Bombardment")
                withClue("Cast should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Resolve — enchantment enters, ETB trigger resolves, selection decision is created
                game.resolveStack()

                // ETB trigger fires — should have a decision to choose 4 permanents
                withClue("Should have pending selection for ETB") {
                    game.hasPendingDecision() shouldBe true
                }

                // Select four of the five opponent creatures
                val targets = listOf("Glory Seeker", "Mammoth Spider", "Fire Elemental", "Juggernaut")
                    .map { game.findPermanent(it)!! }
                game.selectCards(targets)

                // Verify aim counters were placed on chosen permanents
                withClue("Glory Seeker should have an aim counter") {
                    game.getAimCounters("Glory Seeker") shouldBe 1
                }
                withClue("Mammoth Spider should have an aim counter") {
                    game.getAimCounters("Mammoth Spider") shouldBe 1
                }
                withClue("Fire Elemental should have an aim counter") {
                    game.getAimCounters("Fire Elemental") shouldBe 1
                }
                withClue("Juggernaut should have an aim counter") {
                    game.getAimCounters("Juggernaut") shouldBe 1
                }
                // Serra Angel should NOT have an aim counter (wasn't chosen)
                withClue("Serra Angel should NOT have an aim counter") {
                    game.getAimCounters("Serra Angel") shouldBe 0
                }
            }

            test("auto-selects all when exactly 4 nonenchantment permanents exist") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Haphazard Bombardment")
                    .withCardOnBattlefield(2, "Glory Seeker")      // creature
                    .withCardOnBattlefield(2, "Mammoth Spider")    // creature
                    .withCardOnBattlefield(2, "Fire Elemental")    // creature
                    .withCardOnBattlefield(2, "Juggernaut")        // artifact creature
                    .withLandsOnBattlefield(1, "Mountain", 6)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Haphazard Bombardment
                game.castSpell(1, "Haphazard Bombardment")

                // Resolve — auto-selects all 4 since exactly 4 exist
                game.resolveStack()

                // Verify aim counters were placed on all four
                withClue("Glory Seeker should have an aim counter") {
                    game.getAimCounters("Glory Seeker") shouldBe 1
                }
                withClue("Mammoth Spider should have an aim counter") {
                    game.getAimCounters("Mammoth Spider") shouldBe 1
                }
                withClue("Fire Elemental should have an aim counter") {
                    game.getAimCounters("Fire Elemental") shouldBe 1
                }
                withClue("Juggernaut should have an aim counter") {
                    game.getAimCounters("Juggernaut") shouldBe 1
                }
            }

            test("chooses all available if fewer than four nonenchantment permanents exist") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Haphazard Bombardment")
                    .withCardOnBattlefield(2, "Glory Seeker")      // creature
                    .withCardOnBattlefield(2, "Mammoth Spider")    // creature
                    .withLandsOnBattlefield(1, "Mountain", 6)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Haphazard Bombardment")
                withClue("Cast should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Resolve the enchantment
                game.resolveStack()

                // With only 2 nonenchantment permanents, all should be auto-selected (ChooseExactly handles this)
                // Resolve the ETB trigger
                game.resolveStack()

                // Both should have aim counters
                withClue("Glory Seeker should have an aim counter") {
                    game.getAimCounters("Glory Seeker") shouldBe 1
                }
                withClue("Mammoth Spider should have an aim counter") {
                    game.getAimCounters("Mammoth Spider") shouldBe 1
                }
            }
        }

        context("Haphazard Bombardment end step trigger - random destruction") {

            test("destroys one permanent at random at end step when 2+ have aim counters") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Haphazard Bombardment")
                    .withCardOnBattlefield(2, "Glory Seeker")
                    .withCardOnBattlefield(2, "Mammoth Spider")
                    .withCardOnBattlefield(2, "Fire Elemental")
                    .withCardOnBattlefield(2, "Juggernaut")
                    .withLandsOnBattlefield(1, "Mountain", 6)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast and resolve — auto-selects all 4 (exactly 4 available)
                game.castSpell(1, "Haphazard Bombardment")
                game.resolveStack()

                // Advance to end step — trigger goes on stack
                game.passUntilPhase(Phase.ENDING, Step.END)
                // Resolve the end step trigger
                if (game.state.stack.isNotEmpty()) {
                    game.resolveStack()
                }

                // One of the four should have been destroyed (3 remain on battlefield)
                val remainingCount = listOf("Glory Seeker", "Mammoth Spider", "Fire Elemental", "Juggernaut")
                    .count { game.isOnBattlefield(it) }

                withClue("One permanent should have been destroyed, leaving 3 on battlefield") {
                    remainingCount shouldBe 3
                }
            }

            test("does not trigger when fewer than 2 permanents have aim counters") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Haphazard Bombardment")
                    .withCardOnBattlefield(2, "Glory Seeker")  // only 1 nonenchantment permanent
                    .withLandsOnBattlefield(1, "Mountain", 6)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast and resolve
                game.castSpell(1, "Haphazard Bombardment")
                game.resolveStack()

                // Only 1 permanent gets aim counter (auto-selected)
                game.resolveStack()

                withClue("Glory Seeker should have an aim counter") {
                    game.getAimCounters("Glory Seeker") shouldBe 1
                }

                // Advance to end step — should NOT trigger destruction (only 1 aim counter)
                game.passUntilPhase(Phase.ENDING, Step.END)

                withClue("Glory Seeker should still be on battlefield (trigger condition not met)") {
                    game.isOnBattlefield("Glory Seeker") shouldBe true
                }
            }
        }
    }
}
