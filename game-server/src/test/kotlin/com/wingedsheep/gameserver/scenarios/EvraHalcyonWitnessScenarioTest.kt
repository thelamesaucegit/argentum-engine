package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Evra, Halcyon Witness's activated ability:
 * "{4}: Exchange your life total with Evra, Halcyon Witness's power."
 */
class EvraHalcyonWitnessScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Evra, Halcyon Witness") {

            test("exchange life total with power - life becomes 4, power becomes 20") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Evra, Halcyon Witness")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val evraId = game.findPermanent("Evra, Halcyon Witness")!!
                val cardDef = cardRegistry.getCard("Evra, Halcyon Witness")!!
                val exchangeAbility = cardDef.script.activatedAbilities[0]

                // Verify starting state: 20 life, 4 power
                val startingLife = game.state.getEntity(game.player1Id)?.get<LifeTotalComponent>()?.life
                withClue("Starting life should be 20") {
                    startingLife shouldBe 20
                }

                withClue("Starting power should be 4") {
                    stateProjector.getProjectedPower(game.state, evraId) shouldBe 4
                }

                // Activate exchange ability
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = evraId,
                        abilityId = exchangeAbility.id
                    )
                )

                game.resolveStack()

                // After exchange: life = 4 (Evra's former power), power = 20 (former life)
                val newLife = game.state.getEntity(game.player1Id)?.get<LifeTotalComponent>()?.life
                withClue("Life total should be 4 after exchange") {
                    newLife shouldBe 4
                }

                withClue("Evra's power should be 20 after exchange") {
                    stateProjector.getProjectedPower(game.state, evraId) shouldBe 20
                }

                // Toughness should be unchanged
                withClue("Evra's toughness should still be 4") {
                    stateProjector.getProjectedToughness(game.state, evraId) shouldBe 4
                }
            }

            test("double exchange returns to original values") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Evra, Halcyon Witness")
                    .withLandsOnBattlefield(1, "Plains", 8) // enough for two activations
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val evraId = game.findPermanent("Evra, Halcyon Witness")!!
                val cardDef = cardRegistry.getCard("Evra, Halcyon Witness")!!
                val exchangeAbility = cardDef.script.activatedAbilities[0]

                // First exchange: life 20 → 4, power 4 → 20
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = evraId,
                        abilityId = exchangeAbility.id
                    )
                )
                game.resolveStack()

                // Second exchange: life 4 → 20, power 20 → 4
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = evraId,
                        abilityId = exchangeAbility.id
                    )
                )
                game.resolveStack()

                val finalLife = game.state.getEntity(game.player1Id)?.get<LifeTotalComponent>()?.life
                withClue("Life total should be 20 after double exchange") {
                    finalLife shouldBe 20
                }

                withClue("Evra's power should be 4 after double exchange") {
                    stateProjector.getProjectedPower(game.state, evraId) shouldBe 4
                }
            }
        }
    }
}
