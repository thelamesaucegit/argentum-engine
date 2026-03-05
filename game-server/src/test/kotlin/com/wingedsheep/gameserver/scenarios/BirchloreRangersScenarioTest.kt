package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Birchlore Rangers.
 *
 * Card reference:
 * - Birchlore Rangers (G): 1/1 Creature — Elf Druid Ranger
 *   "Tap two untapped Elves you control: Add one mana of any color."
 *   Morph {G}
 */
class BirchloreRangersScenarioTest : ScenarioTestBase() {

    private val manaSolver = ManaSolver(cardRegistry)

    init {
        context("Birchlore Rangers tap two Elves ability") {
            test("adds one mana of the chosen color when tapping two Elves") {
                val game = scenario()
                    .withPlayers("Elf Player", "Opponent")
                    .withCardOnBattlefield(1, "Birchlore Rangers")
                    .withCardOnBattlefield(1, "Elvish Warrior")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val rangers = game.findPermanent("Birchlore Rangers")!!
                val warrior = game.findPermanent("Elvish Warrior")!!

                val cardDef = cardRegistry.getCard("Birchlore Rangers")!!
                val ability = cardDef.script.activatedAbilities.first()

                val costPayment = AdditionalCostPayment(
                    tappedPermanents = listOf(rangers, warrior)
                )

                val activateResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = rangers,
                        abilityId = ability.id,
                        costPayment = costPayment,
                        manaColorChoice = Color.RED
                    )
                )

                withClue("Ability should activate successfully") {
                    activateResult.error shouldBe null
                }

                // Both Elves should be tapped
                withClue("Birchlore Rangers should be tapped") {
                    game.state.getEntity(rangers)?.has<TappedComponent>() shouldBe true
                }
                withClue("Elvish Warrior should be tapped") {
                    game.state.getEntity(warrior)?.has<TappedComponent>() shouldBe true
                }

                // Mana pool should have 1 red mana
                val manaPool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()!!
                withClue("Should have 1 red mana in pool") {
                    manaPool.red shouldBe 1
                }
                withClue("Should have no other colored mana") {
                    manaPool.green shouldBe 0
                    manaPool.blue shouldBe 0
                    manaPool.white shouldBe 0
                    manaPool.black shouldBe 0
                }
            }

            test("can produce any color of mana") {
                val game = scenario()
                    .withPlayers("Elf Player", "Opponent")
                    .withCardOnBattlefield(1, "Birchlore Rangers")
                    .withCardOnBattlefield(1, "Elvish Warrior")
                    .withCardOnBattlefield(1, "Wirewood Elf")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val rangers = game.findPermanent("Birchlore Rangers")!!
                val warrior = game.findPermanent("Elvish Warrior")!!
                val wirewood = game.findPermanent("Wirewood Elf")!!

                val cardDef = cardRegistry.getCard("Birchlore Rangers")!!
                val ability = cardDef.script.activatedAbilities.first()

                // First activation: produce blue mana by tapping Rangers + Warrior
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = rangers,
                        abilityId = ability.id,
                        costPayment = AdditionalCostPayment(tappedPermanents = listOf(rangers, warrior)),
                        manaColorChoice = Color.BLUE
                    )
                )

                val manaPool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()!!
                withClue("Should have 1 blue mana") {
                    manaPool.blue shouldBe 1
                }
            }

            test("cannot activate with fewer than two Elves") {
                val game = scenario()
                    .withPlayers("Elf Player", "Opponent")
                    .withCardOnBattlefield(1, "Birchlore Rangers")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val rangers = game.findPermanent("Birchlore Rangers")!!

                val cardDef = cardRegistry.getCard("Birchlore Rangers")!!
                val ability = cardDef.script.activatedAbilities.first()

                val costPayment = AdditionalCostPayment(
                    tappedPermanents = listOf(rangers)
                )

                val activateResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = rangers,
                        abilityId = ability.id,
                        costPayment = costPayment,
                        manaColorChoice = Color.GREEN
                    )
                )

                withClue("Ability should fail with only one Elf") {
                    activateResult.error shouldNotBe null
                }
            }

            test("non-Elf creatures cannot be tapped for the cost") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Birchlore Rangers")
                    .withCardOnBattlefield(1, "Goblin Piledriver")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val rangers = game.findPermanent("Birchlore Rangers")!!
                val goblin = game.findPermanent("Goblin Piledriver")!!

                val cardDef = cardRegistry.getCard("Birchlore Rangers")!!
                val ability = cardDef.script.activatedAbilities.first()

                val costPayment = AdditionalCostPayment(
                    tappedPermanents = listOf(rangers, goblin)
                )

                val activateResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = rangers,
                        abilityId = ability.id,
                        costPayment = costPayment,
                        manaColorChoice = Color.GREEN
                    )
                )

                withClue("Ability should fail when tapping a non-Elf") {
                    activateResult.error shouldNotBe null
                }
            }

            test("already tapped Elves cannot be used") {
                val game = scenario()
                    .withPlayers("Elf Player", "Opponent")
                    .withCardOnBattlefield(1, "Birchlore Rangers")
                    .withCardOnBattlefield(1, "Elvish Warrior", tapped = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val rangers = game.findPermanent("Birchlore Rangers")!!
                val warrior = game.findPermanent("Elvish Warrior")!!

                val cardDef = cardRegistry.getCard("Birchlore Rangers")!!
                val ability = cardDef.script.activatedAbilities.first()

                val costPayment = AdditionalCostPayment(
                    tappedPermanents = listOf(rangers, warrior)
                )

                val activateResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = rangers,
                        abilityId = ability.id,
                        costPayment = costPayment,
                        manaColorChoice = Color.GREEN
                    )
                )

                withClue("Ability should fail when one Elf is already tapped") {
                    activateResult.error shouldNotBe null
                }
            }
        }

        context("ManaSolver accounts for TapPermanents mana abilities") {
            test("getAvailableManaCount includes Birchlore Rangers mana from non-source Elves") {
                val game = scenario()
                    .withPlayers("Elf Player", "Opponent")
                    .withCardOnBattlefield(1, "Birchlore Rangers")
                    .withCardOnBattlefield(1, "Elvish Warrior")
                    .withCardOnBattlefield(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Forest = 1 mana, Birchlore Rangers tapping 2 Elves = 1 mana → total 2
                val available = manaSolver.getAvailableManaCount(game.state, game.player1Id)
                withClue("Should count Forest (1) + TapPermanents bonus (1) = 2 mana") {
                    available shouldBe 2
                }
            }

            test("canPay returns true when TapPermanents provides needed mana") {
                val game = scenario()
                    .withPlayers("Elf Player", "Opponent")
                    .withCardOnBattlefield(1, "Birchlore Rangers")
                    .withCardOnBattlefield(1, "Elvish Warrior")
                    .withCardOnBattlefield(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cost {1}{G}: Forest provides {G}, TapPermanents provides {1}
                withClue("Should be able to pay {1}{G} with Forest + Birchlore ability") {
                    manaSolver.canPay(game.state, game.player1Id, ManaCost.parse("{1}{G}")) shouldBe true
                }
            }

            test("canPay returns true for off-color costs when TapPermanents produces any color") {
                val game = scenario()
                    .withPlayers("Elf Player", "Opponent")
                    .withCardOnBattlefield(1, "Birchlore Rangers")
                    .withCardOnBattlefield(1, "Elvish Warrior")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // No lands at all, but TapPermanents produces any color
                withClue("Should be able to pay {U} with Birchlore any-color mana") {
                    manaSolver.canPay(game.state, game.player1Id, ManaCost.parse("{U}")) shouldBe true
                }
            }

            test("canPay returns false when not enough TapPermanents mana") {
                val game = scenario()
                    .withPlayers("Elf Player", "Opponent")
                    .withCardOnBattlefield(1, "Birchlore Rangers")
                    .withCardOnBattlefield(1, "Elvish Warrior")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Only 1 TapPermanents activation (1 mana), need 2
                withClue("Should not be able to pay {U}{U} with only 1 TapPermanents mana") {
                    manaSolver.canPay(game.state, game.player1Id, ManaCost.parse("{U}{U}")) shouldBe false
                }
            }

            test("does not double-count mana dork Elves as TapPermanents targets") {
                val game = scenario()
                    .withPlayers("Elf Player", "Opponent")
                    .withCardOnBattlefield(1, "Birchlore Rangers")
                    .withCardOnBattlefield(1, "Wirewood Elf") // Elf that taps for {G}
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Wirewood Elf is a regular mana source (1 mana).
                // Rangers is not a regular source (TapPermanents cost, not Tap).
                // Only 1 non-source Elf (Rangers), need 2 for TapPermanents → no bonus.
                // Total = 1 (Wirewood Elf)
                val available = manaSolver.getAvailableManaCount(game.state, game.player1Id)
                withClue("Should count Wirewood Elf (1) only, no TapPermanents bonus") {
                    available shouldBe 1
                }
            }

            test("multiple TapPermanents activations with enough Elves") {
                val game = scenario()
                    .withPlayers("Elf Player", "Opponent")
                    .withCardOnBattlefield(1, "Birchlore Rangers")
                    .withCardOnBattlefield(1, "Elvish Warrior")
                    .withCardOnBattlefield(1, "Elvish Warrior")
                    .withCardOnBattlefield(1, "Elvish Warrior")
                    .withCardOnBattlefield(1, "Elvish Warrior")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // 5 Elves total (all non-source): 5/2 = 2 activations → 2 bonus mana
                val available = manaSolver.getAvailableManaCount(game.state, game.player1Id)
                withClue("Should count 2 TapPermanents bonus from 5 non-source Elves (5/2=2)") {
                    available shouldBe 2
                }
            }
        }
    }
}
