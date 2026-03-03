package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Kheru Dreadmaw.
 *
 * Card reference:
 * - Kheru Dreadmaw ({4}{B}): Creature — Zombie Crocodile 4/4
 *   Defender
 *   {1}{G}, Sacrifice another creature: You gain life equal to the sacrificed creature's toughness.
 */
class KheruDreadmawScenarioTest : ScenarioTestBase() {

    init {
        context("Kheru Dreadmaw activated ability") {
            test("gains life equal to sacrificed creature's toughness") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Kheru Dreadmaw")
                    .withCardOnBattlefield(1, "Alpine Grizzly") // 4/2 creature
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val dreadmawId = game.findPermanent("Kheru Dreadmaw")!!
                val grizzlyId = game.findPermanent("Alpine Grizzly")!!

                val cardDef = cardRegistry.getCard("Kheru Dreadmaw")!!
                val ability = cardDef.script.activatedAbilities.first()

                // Activate: {1}{G}, sacrifice Alpine Grizzly (toughness 2)
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = dreadmawId,
                        abilityId = ability.id,
                        costPayment = AdditionalCostPayment(
                            sacrificedPermanents = listOf(grizzlyId)
                        )
                    )
                )

                withClue("Ability should activate successfully: ${result.error}") {
                    result.error shouldBe null
                }

                // Alpine Grizzly should be sacrificed
                withClue("Alpine Grizzly should be in graveyard") {
                    game.isOnBattlefield("Alpine Grizzly") shouldBe false
                    game.isInGraveyard(1, "Alpine Grizzly") shouldBe true
                }

                // Resolve the ability
                game.resolveStack()

                // Should gain 2 life (Alpine Grizzly's toughness)
                withClue("Player should gain 2 life from sacrificing Alpine Grizzly (toughness 2)") {
                    game.getLifeTotal(1) shouldBe 22
                }
            }

            test("cannot sacrifice itself (excludeSelf)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Kheru Dreadmaw")
                    // No other creatures - should not be able to activate
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val dreadmawId = game.findPermanent("Kheru Dreadmaw")!!

                val cardDef = cardRegistry.getCard("Kheru Dreadmaw")!!
                val ability = cardDef.script.activatedAbilities.first()

                // Try to sacrifice itself - should fail
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = dreadmawId,
                        abilityId = ability.id,
                        costPayment = AdditionalCostPayment(
                            sacrificedPermanents = listOf(dreadmawId)
                        )
                    )
                )

                withClue("Ability should fail when trying to sacrifice self") {
                    result.error shouldBe "Cannot pay ability cost"
                }

                // Kheru Dreadmaw should still be on battlefield
                withClue("Kheru Dreadmaw should still be on battlefield") {
                    game.isOnBattlefield("Kheru Dreadmaw") shouldBe true
                }
            }
        }
    }
}
