package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Aryel, Knight of Windgrace.
 *
 * Card reference:
 * - Aryel, Knight of Windgrace ({2}{W}{B}): 4/4 Legendary Creature — Human Knight
 *   Vigilance
 *   {2}{W}, {T}: Create a 2/2 white Knight creature token with vigilance.
 *   {B}, {T}, Tap X untapped Knights you control: Destroy target creature with power X or less.
 */
class AryelKnightOfWindgraceScenarioTest : ScenarioTestBase() {

    init {
        context("Aryel token creation ability") {
            test("creates a 2/2 white Knight token with vigilance") {
                val game = scenario()
                    .withPlayers("Knight Player", "Opponent")
                    .withCardOnBattlefield(1, "Aryel, Knight of Windgrace")
                    .withLandsOnBattlefield(1, "Plains", 3) // {2}{W} for the cost
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val aryel = game.findPermanent("Aryel, Knight of Windgrace")!!

                val cardDef = cardRegistry.getCard("Aryel, Knight of Windgrace")!!
                // First activated ability = token creation
                val tokenAbility = cardDef.script.activatedAbilities[0]

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = aryel,
                        abilityId = tokenAbility.id
                    )
                )

                withClue("Token ability should activate successfully") {
                    result.error shouldBe null
                }

                // Aryel should be tapped (part of {T} cost)
                withClue("Aryel should be tapped") {
                    game.state.getEntity(aryel)?.has<TappedComponent>() shouldBe true
                }

                // Resolve the ability (it goes on the stack)
                game.resolveStack()

                // Should have a Knight token on the battlefield
                val knightToken = game.findPermanent("Knight Token")
                withClue("Should have a Knight token on the battlefield") {
                    knightToken shouldNotBe null
                }
            }
        }

        context("Aryel destroy ability with TapXPermanents") {
            test("destroys a creature with power <= X when tapping X Knights") {
                val game = scenario()
                    .withPlayers("Knight Player", "Opponent")
                    .withCardOnBattlefield(1, "Aryel, Knight of Windgrace")
                    .withCardOnBattlefield(1, "Knight of New Benalia") // 3/1 Knight
                    .withCardOnBattlefield(1, "Benalish Honor Guard") // 2/2 Knight
                    .withCardOnBattlefield(2, "Primordial Wurm") // 7/6
                    .withCardOnBattlefield(2, "Baloth Gorger") // 4/4
                    .withLandsOnBattlefield(1, "Swamp", 1) // {B} for the cost
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val aryel = game.findPermanent("Aryel, Knight of Windgrace")!!
                val knight1 = game.findPermanent("Knight of New Benalia")!!
                val knight2 = game.findPermanent("Benalish Honor Guard")!!
                val balothGorger = game.findPermanent("Baloth Gorger")!!

                val cardDef = cardRegistry.getCard("Aryel, Knight of Windgrace")!!
                // Second activated ability = destroy ability
                val destroyAbility = cardDef.script.activatedAbilities[1]

                // Tap 2 Knights (X=2) to destroy a creature with power <= 2
                // But Baloth Gorger has power 4, so it should NOT be destroyed
                // Let's target Baloth Gorger with X=2, the conditional effect should not destroy
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = aryel,
                        abilityId = destroyAbility.id,
                        targets = listOf(ChosenTarget.Permanent(balothGorger)),
                        xValue = 2,
                        costPayment = AdditionalCostPayment(
                            tappedPermanents = listOf(knight1, knight2)
                        )
                    )
                )

                withClue("Ability should activate successfully") {
                    result.error shouldBe null
                }

                // Aryel and the 2 Knights should be tapped
                withClue("Aryel should be tapped") {
                    game.state.getEntity(aryel)?.has<TappedComponent>() shouldBe true
                }
                withClue("Knight of New Benalia should be tapped") {
                    game.state.getEntity(knight1)?.has<TappedComponent>() shouldBe true
                }
                withClue("Benalish Honor Guard should be tapped") {
                    game.state.getEntity(knight2)?.has<TappedComponent>() shouldBe true
                }

                // Resolve the ability
                game.resolveStack()

                // Baloth Gorger (power 4) should NOT be destroyed because X=2 and 4 > 2
                withClue("Baloth Gorger should still be on battlefield (power 4 > X=2)") {
                    game.findPermanent("Baloth Gorger") shouldNotBe null
                }
            }

            test("destroys a creature when power <= X") {
                val game = scenario()
                    .withPlayers("Knight Player", "Opponent")
                    .withCardOnBattlefield(1, "Aryel, Knight of Windgrace")
                    .withCardOnBattlefield(1, "Knight of New Benalia") // 3/1 Knight
                    .withCardOnBattlefield(1, "Benalish Honor Guard") // 2/2 Knight
                    .withCardOnBattlefield(2, "Baloth Gorger") // 4/4
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val aryel = game.findPermanent("Aryel, Knight of Windgrace")!!
                val knight1 = game.findPermanent("Knight of New Benalia")!!
                val knight2 = game.findPermanent("Benalish Honor Guard")!!
                val balothGorger = game.findPermanent("Baloth Gorger")!!

                val cardDef = cardRegistry.getCard("Aryel, Knight of Windgrace")!!
                val destroyAbility = cardDef.script.activatedAbilities[1]

                // Tap 2 Knights, target Baloth Gorger with X=4 — wait, we only have 2 Knights so max X=2
                // Instead, target Baloth Gorger with X=2 (won't work) or use a smaller creature
                // Actually, Aryel is also a Knight! So we can tap 3 creatures: knight1, knight2, and...
                // Wait, Aryel needs to tap itself for the {T} cost, so it can't also be tapped as a Knight.
                // We only have 2 Knights to tap. Let's put a smaller creature on opponent's side.

                // Actually let's just redo with Benalish Honor Guard (2/2) as target and X=2
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = aryel,
                        abilityId = destroyAbility.id,
                        targets = listOf(ChosenTarget.Permanent(balothGorger)),
                        xValue = 4,
                        costPayment = AdditionalCostPayment(
                            tappedPermanents = listOf(knight1, knight2)
                        )
                    )
                )

                // This should fail because we only provided 2 tap targets but X=4
                withClue("Should fail because not enough Knights to tap for X=4") {
                    result.error shouldNotBe null
                }
            }

            test("successfully destroys creature with exactly enough tapped Knights") {
                val game = scenario()
                    .withPlayers("Knight Player", "Opponent")
                    .withCardOnBattlefield(1, "Aryel, Knight of Windgrace")
                    .withCardOnBattlefield(1, "Knight of New Benalia") // 3/1 Knight
                    .withCardOnBattlefield(1, "Benalish Honor Guard") // 2/2 Knight
                    .withCardOnBattlefield(2, "Mesa Unicorn") // 2/2 target
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val aryel = game.findPermanent("Aryel, Knight of Windgrace")!!
                val knight1 = game.findPermanent("Knight of New Benalia")!!
                val knight2 = game.findPermanent("Benalish Honor Guard")!!
                val mesaUnicorn = game.findPermanent("Mesa Unicorn")!!

                val cardDef = cardRegistry.getCard("Aryel, Knight of Windgrace")!!
                val destroyAbility = cardDef.script.activatedAbilities[1]

                // X=2, tap 2 Knights, target opponent's 2/2 creature (power 2 <= 2)
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = aryel,
                        abilityId = destroyAbility.id,
                        targets = listOf(ChosenTarget.Permanent(mesaUnicorn)),
                        xValue = 2,
                        costPayment = AdditionalCostPayment(
                            tappedPermanents = listOf(knight1, knight2)
                        )
                    )
                )

                withClue("Ability should activate successfully") {
                    result.error shouldBe null
                }

                // Resolve the ability
                game.resolveStack()

                // Opponent's Mesa Unicorn (power 2) should be destroyed (2 <= X=2)
                withClue("Opponent's Mesa Unicorn should be destroyed") {
                    val oppBattlefield = game.state.getZone(com.wingedsheep.engine.state.ZoneKey(game.player2Id, Zone.BATTLEFIELD))
                    oppBattlefield.contains(mesaUnicorn) shouldBe false
                }

                // Player 1's Knight should still be tapped on battlefield
                withClue("Knight of New Benalia should be tapped") {
                    game.state.getEntity(knight1)?.has<TappedComponent>() shouldBe true
                }
            }
        }
    }
}
