package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Chief of the Scale.
 *
 * Card reference:
 * - Chief of the Scale ({W}{B}): 2/3 Creature — Human Warrior
 *   Other Warrior creatures you control get +0/+1.
 *
 * Tests:
 * 1. Other Warrior creatures you control get +0/+1
 * 2. Chief of the Scale does not buff itself
 * 3. Non-Warrior creatures you control are not affected
 * 4. Opponent's Warrior creatures are not affected
 */
class ChiefOfTheScaleScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Chief of the Scale lord effects") {

            test("Other Warrior creatures you control get +0/+1") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Chief of the Scale")
                    .withCardOnBattlefield(1, "Disowned Ancestor") // Spirit Warrior, 0/4
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ancestor = game.findPermanent("Disowned Ancestor")!!
                val projected = stateProjector.project(game.state)

                withClue("Disowned Ancestor (Spirit Warrior 0/4) should be 0/5 with +0/+1 bonus") {
                    projected.getPower(ancestor) shouldBe 0
                    projected.getToughness(ancestor) shouldBe 5
                }
            }

            test("Chief of the Scale does not buff itself") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Chief of the Scale")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val chief = game.findPermanent("Chief of the Scale")!!
                val projected = stateProjector.project(game.state)

                withClue("Chief of the Scale (2/3) should remain 2/3 — says 'other'") {
                    projected.getPower(chief) shouldBe 2
                    projected.getToughness(chief) shouldBe 3
                }
            }

            test("Non-Warrior creatures you control are not affected") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Chief of the Scale")
                    .withCardOnBattlefield(1, "Grizzly Bears") // Bear, 2/2
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val projected = stateProjector.project(game.state)

                withClue("Grizzly Bears (Bear 2/2) should remain 2/2 — not a Warrior") {
                    projected.getPower(bears) shouldBe 2
                    projected.getToughness(bears) shouldBe 2
                }
            }

            test("Opponent's Warrior creatures are not affected") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Chief of the Scale")
                    .withCardOnBattlefield(2, "Disowned Ancestor") // Opponent's Warrior, 0/4
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ancestor = game.findPermanent("Disowned Ancestor")!!
                val projected = stateProjector.project(game.state)

                withClue("Opponent's Disowned Ancestor (Spirit Warrior 0/4) should remain 0/4 — not yours") {
                    projected.getPower(ancestor) shouldBe 0
                    projected.getToughness(ancestor) shouldBe 4
                }
            }
        }
    }
}
