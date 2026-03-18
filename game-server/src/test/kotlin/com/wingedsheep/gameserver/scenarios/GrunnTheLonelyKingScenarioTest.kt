package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Grunn, the Lonely King.
 *
 * Tests:
 * - "Whenever Grunn attacks alone, double its power and toughness until end of turn."
 * - New trigger: AttacksAlone (AttackEvent with alone = true)
 * - New DynamicAmount: SourceToughness
 */
class GrunnTheLonelyKingScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    init {
        context("Grunn, the Lonely King - attacks alone trigger") {

            test("attacking alone doubles power and toughness") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Grunn, the Lonely King")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                    .build()

                // Move to declare attackers
                game.passPriority() // P1 passes in begin combat
                game.passPriority() // P2 passes in begin combat

                // Attack with Grunn alone
                game.declareAttackers(mapOf(
                    "Grunn, the Lonely King" to 2
                ))

                // Resolve the "attacks alone" triggered ability
                game.resolveStack()

                // Grunn should now be 10/10 (5+5 / 5+5)
                val grunnId = game.findPermanent("Grunn, the Lonely King")!!
                val projected = projector.project(game.state)
                projected.getPower(grunnId) shouldBe 10
                projected.getToughness(grunnId) shouldBe 10
            }

            test("attacking with another creature does not trigger") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Grunn, the Lonely King")
                    .withCardOnBattlefield(1, "Llanowar Elves")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                    .build()

                // Move to declare attackers
                game.passPriority() // P1 passes in begin combat
                game.passPriority() // P2 passes in begin combat

                // Attack with both creatures
                game.declareAttackers(mapOf(
                    "Grunn, the Lonely King" to 2,
                    "Llanowar Elves" to 2
                ))

                // No trigger should fire — Grunn should still be 5/5
                val grunnId = game.findPermanent("Grunn, the Lonely King")!!
                val projected = projector.project(game.state)
                projected.getPower(grunnId) shouldBe 5
                projected.getToughness(grunnId) shouldBe 5
            }
        }
    }
}
