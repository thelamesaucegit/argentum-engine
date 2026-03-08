package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Pearl Lake Ancient.
 *
 * Card reference:
 * - Pearl Lake Ancient ({5}{U}{U}): Creature — Leviathan 6/7
 *   Flash, can't be countered, prowess
 *   "Return three lands you control to their owner's hand: Return Pearl Lake Ancient to its owner's hand."
 */
class PearlLakeAncientScenarioTest : ScenarioTestBase() {

    init {
        context("Pearl Lake Ancient - bounce self by returning 3 lands") {

            test("returns self to hand when paying with 3 lands") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Pearl Lake Ancient")
                    .withCardOnBattlefield(1, "Island")
                    .withCardOnBattlefield(1, "Island")
                    .withCardOnBattlefield(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ancient = game.findPermanent("Pearl Lake Ancient")!!
                val islands = game.state.getBattlefield().filter { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Island"
                }

                withClue("Should have 3 islands on battlefield") {
                    islands.size shouldBe 3
                }

                val cardDef = cardRegistry.getCard("Pearl Lake Ancient")!!
                val ability = cardDef.script.activatedAbilities.first()

                val costPayment = AdditionalCostPayment(
                    bouncedPermanents = islands
                )

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = ancient,
                        abilityId = ability.id,
                        costPayment = costPayment
                    )
                )

                withClue("Activation should succeed") {
                    result.error shouldBe null
                }

                // Resolve the ability
                game.resolveStack()

                // Pearl Lake Ancient should be in hand
                val hand = game.state.getHand(game.player1Id)
                val ancientInHand = hand.any { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Pearl Lake Ancient"
                }
                withClue("Pearl Lake Ancient should be in hand") {
                    ancientInHand shouldBe true
                }

                // All 3 islands should be in hand
                val landsInHand = hand.count { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Island"
                }
                withClue("Three islands should be in hand") {
                    landsInHand shouldBe 3
                }

                // No creatures should remain on battlefield
                val p1Battlefield = game.state.getZone(ZoneKey(game.player1Id, Zone.BATTLEFIELD))
                val creaturesOnBattlefield = p1Battlefield.filter { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.typeLine?.isCreature == true
                }
                withClue("No creatures should remain on battlefield") {
                    creaturesOnBattlefield.size shouldBe 0
                }
            }

            test("cannot activate with fewer than 3 lands") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Pearl Lake Ancient")
                    .withCardOnBattlefield(1, "Island")
                    .withCardOnBattlefield(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ancient = game.findPermanent("Pearl Lake Ancient")!!
                val islands = game.state.getBattlefield().filter { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Island"
                }

                withClue("Should have 2 islands on battlefield") {
                    islands.size shouldBe 2
                }

                val cardDef = cardRegistry.getCard("Pearl Lake Ancient")!!
                val ability = cardDef.script.activatedAbilities.first()

                // Try to pay with only 2 lands
                val costPayment = AdditionalCostPayment(
                    bouncedPermanents = islands
                )

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = ancient,
                        abilityId = ability.id,
                        costPayment = costPayment
                    )
                )

                withClue("Activation should fail with only 2 lands") {
                    result.error shouldNotBe null
                }
            }
        }
    }
}
