package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Bloodstone Goblin's triggered ability:
 * "Whenever you cast a spell, if that spell was kicked,
 *  Bloodstone Goblin gets +1/+1 and gains menace until end of turn."
 */
class BloodstoneGoblinScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Bloodstone Goblin") {

            test("casting a kicked spell triggers +1/+1 and menace") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Bloodstone Goblin")
                    .withCardInHand(1, "Untamed Kavu")
                    .withLandsOnBattlefield(1, "Forest", 5) // {1}{G} + {3} kicker
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val playerId = game.player1Id
                val hand = game.state.getHand(playerId)
                val kavuId = hand.find { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Untamed Kavu"
                }!!

                // Cast Untamed Kavu with kicker - this should trigger Bloodstone Goblin
                game.execute(CastSpell(playerId, kavuId, wasKicked = true))

                // Resolve the triggered ability (Bloodstone Goblin's +1/+1 and menace)
                game.resolveStack()

                val goblinId = game.findPermanent("Bloodstone Goblin")!!
                val projected = stateProjector.project(game.state)

                withClue("Bloodstone Goblin should be 3/3 after triggered ability") {
                    stateProjector.getProjectedPower(game.state, goblinId) shouldBe 3
                    stateProjector.getProjectedToughness(game.state, goblinId) shouldBe 3
                }

                withClue("Bloodstone Goblin should have menace") {
                    projected.hasKeyword(goblinId, Keyword.MENACE) shouldBe true
                }
            }

            test("casting an unkicked spell does not trigger") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Bloodstone Goblin")
                    .withCardInHand(1, "Untamed Kavu")
                    .withLandsOnBattlefield(1, "Forest", 2) // just {1}{G}, no kicker
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Untamed Kavu without kicker
                game.castSpell(1, "Untamed Kavu")

                // Resolve the spell (no triggered ability should fire)
                game.resolveStack()

                val goblinId = game.findPermanent("Bloodstone Goblin")!!
                val projected = stateProjector.project(game.state)

                withClue("Bloodstone Goblin should still be 2/2") {
                    stateProjector.getProjectedPower(game.state, goblinId) shouldBe 2
                    stateProjector.getProjectedToughness(game.state, goblinId) shouldBe 2
                }

                withClue("Bloodstone Goblin should not have menace") {
                    projected.hasKeyword(goblinId, Keyword.MENACE) shouldBe false
                }
            }
        }
    }
}
