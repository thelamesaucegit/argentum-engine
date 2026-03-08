package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Briber's Purse.
 *
 * Card reference:
 * - Briber's Purse ({X}): Artifact
 *   "Briber's Purse enters the battlefield with X gem counters on it.
 *    {1}, {T}, Remove a gem counter from Briber's Purse:
 *    Target creature can't attack or block this turn."
 */
class BribersPurseScenarioTest : ScenarioTestBase() {

    private fun TestGame.addGemCounters(name: String, count: Int) {
        val entityId = findPermanent(name)!!
        state = state.updateEntity(entityId) { container ->
            val counters = container.get<CountersComponent>() ?: CountersComponent()
            container.with(counters.withAdded(CounterType.GEM, count))
        }
    }

    init {
        context("Briber's Purse - ETB with gem counters") {

            test("cast with X=3 should enter with 3 gem counters") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Briber's Purse")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardOnBattlefield(1, "Mountain") // 4th land for library draw protection
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Briber's Purse with X=3 (costs {3})
                val castResult = game.castXSpell(1, "Briber's Purse", xValue = 3)
                withClue("Briber's Purse should be cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                // Verify it's on the battlefield with 3 gem counters
                withClue("Briber's Purse should be on battlefield") {
                    game.isOnBattlefield("Briber's Purse") shouldBe true
                }

                val purseId = game.findPermanent("Briber's Purse")!!
                val counters = game.state.getEntity(purseId)?.get<CountersComponent>()
                withClue("Should have 3 gem counters") {
                    counters?.getCount(CounterType.GEM) shouldBe 3
                }
            }

            test("cast with X=0 should enter with no gem counters") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Briber's Purse")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Briber's Purse with X=0 (free)
                val castResult = game.castXSpell(1, "Briber's Purse", xValue = 0)
                withClue("Briber's Purse should be cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                val purseId = game.findPermanent("Briber's Purse")!!
                val counters = game.state.getEntity(purseId)?.get<CountersComponent>()
                val gemCount = counters?.getCount(CounterType.GEM) ?: 0
                withClue("Should have 0 gem counters") {
                    gemCount shouldBe 0
                }
            }
        }

        context("Briber's Purse - Activated ability") {

            test("activated ability removes gem counter and prevents creature from attacking") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Briber's Purse")
                    .withCardOnBattlefield(1, "Mountain")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Add 2 gem counters to Briber's Purse
                game.addGemCounters("Briber's Purse", 2)

                val purseId = game.findPermanent("Briber's Purse")!!
                val bearsId = game.findPermanent("Grizzly Bears")!!
                val cardDef = cardRegistry.getCard("Briber's Purse")!!
                val ability = cardDef.script.activatedAbilities.first()

                // Activate ability targeting Grizzly Bears
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = purseId,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(bearsId))
                    )
                )
                withClue("Ability should activate successfully: ${result.error}") {
                    result.error shouldBe null
                }

                // Resolve the ability
                game.resolveStack()

                // Verify gem counter was removed (should have 1 left)
                val counters = game.state.getEntity(purseId)?.get<CountersComponent>()
                withClue("Should have 1 gem counter after activation") {
                    counters?.getCount(CounterType.GEM) shouldBe 1
                }

                // Verify Grizzly Bears can't attack (has a floating "can't attack" effect)
                val projected = game.state.projectedState
                withClue("Grizzly Bears should not be able to attack") {
                    projected.cantAttack(bearsId) shouldBe true
                }
                withClue("Grizzly Bears should not be able to block") {
                    projected.cantBlock(bearsId) shouldBe true
                }
            }

            test("ability cannot be activated without gem counters") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Briber's Purse")
                    .withCardOnBattlefield(1, "Mountain")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // No gem counters on Briber's Purse

                val purseId = game.findPermanent("Briber's Purse")!!
                val bearsId = game.findPermanent("Grizzly Bears")!!
                val cardDef = cardRegistry.getCard("Briber's Purse")!!
                val ability = cardDef.script.activatedAbilities.first()

                // Try to activate ability — should fail
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = purseId,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(bearsId))
                    )
                )
                withClue("Ability should fail due to missing gem counters") {
                    (result.error != null) shouldBe true
                }
            }
        }
    }
}
