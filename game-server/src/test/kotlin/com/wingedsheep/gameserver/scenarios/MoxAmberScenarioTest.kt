package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Mox Amber.
 *
 * Card reference:
 * - Mox Amber ({0}): Legendary Artifact
 *   "{T}: Add one mana of any color among legendary creatures and planeswalkers you control."
 */
class MoxAmberScenarioTest : ScenarioTestBase() {

    init {
        context("Mox Amber mana ability") {
            test("adds mana matching the color of a legendary creature you control") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Mox Amber")
                    .withCardOnBattlefield(1, "Adeliz, the Cinder Wind") // U/R legendary creature
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val moxAmber = game.findPermanent("Mox Amber")!!
                val cardDef = cardRegistry.getCard("Mox Amber")!!
                val ability = cardDef.script.activatedAbilities.first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = moxAmber,
                        abilityId = ability.id,
                        manaColorChoice = Color.RED
                    )
                )

                withClue("Ability should activate successfully") {
                    result.isSuccess shouldBe true
                }

                val manaPool = result.newState.getEntity(game.player1Id)?.get<ManaPoolComponent>()
                withClue("Should have 1 red mana") {
                    manaPool shouldNotBe null
                    manaPool!!.red shouldBe 1
                }
            }

            test("adds blue mana when controlling a blue legendary creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Mox Amber")
                    .withCardOnBattlefield(1, "Adeliz, the Cinder Wind") // U/R legendary creature
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val moxAmber = game.findPermanent("Mox Amber")!!
                val cardDef = cardRegistry.getCard("Mox Amber")!!
                val ability = cardDef.script.activatedAbilities.first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = moxAmber,
                        abilityId = ability.id,
                        manaColorChoice = Color.BLUE
                    )
                )

                withClue("Ability should activate successfully") {
                    result.isSuccess shouldBe true
                }

                val manaPool = result.newState.getEntity(game.player1Id)?.get<ManaPoolComponent>()
                withClue("Should have 1 blue mana") {
                    manaPool shouldNotBe null
                    manaPool!!.blue shouldBe 1
                }
            }

            test("produces no mana with no legendary creatures or planeswalkers") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Mox Amber")
                    .withCardOnBattlefield(1, "Serra Angel") // Not legendary
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val moxAmber = game.findPermanent("Mox Amber")!!
                val cardDef = cardRegistry.getCard("Mox Amber")!!
                val ability = cardDef.script.activatedAbilities.first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = moxAmber,
                        abilityId = ability.id,
                        manaColorChoice = Color.WHITE
                    )
                )

                withClue("Ability should still activate (taps the Mox)") {
                    result.isSuccess shouldBe true
                }

                val manaPool = result.newState.getEntity(game.player1Id)?.get<ManaPoolComponent>()
                withClue("Should have no mana (no legendary creatures/planeswalkers)") {
                    if (manaPool != null) {
                        manaPool.white shouldBe 0
                        manaPool.blue shouldBe 0
                        manaPool.black shouldBe 0
                        manaPool.red shouldBe 0
                        manaPool.green shouldBe 0
                    }
                }
            }

            test("cannot produce a color not among legendary permanents") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Mox Amber")
                    .withCardOnBattlefield(1, "Adeliz, the Cinder Wind") // U/R only
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val moxAmber = game.findPermanent("Mox Amber")!!
                val cardDef = cardRegistry.getCard("Mox Amber")!!
                val ability = cardDef.script.activatedAbilities.first()

                // Request green, but only U/R are available
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = moxAmber,
                        abilityId = ability.id,
                        manaColorChoice = Color.GREEN
                    )
                )

                withClue("Ability should activate successfully") {
                    result.isSuccess shouldBe true
                }

                val manaPool = result.newState.getEntity(game.player1Id)?.get<ManaPoolComponent>()
                withClue("Should not produce green mana — falls back to first available color") {
                    manaPool shouldNotBe null
                    manaPool!!.green shouldBe 0
                    // Should produce one of the available colors (U or R)
                    (manaPool.blue + manaPool.red) shouldBe 1
                }
            }

            test("does not count opponent's legendary creatures") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Mox Amber")
                    .withCardOnBattlefield(2, "Adeliz, the Cinder Wind") // Opponent's legendary
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val moxAmber = game.findPermanent("Mox Amber")!!
                val cardDef = cardRegistry.getCard("Mox Amber")!!
                val ability = cardDef.script.activatedAbilities.first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = moxAmber,
                        abilityId = ability.id,
                        manaColorChoice = Color.RED
                    )
                )

                withClue("Ability should activate") {
                    result.isSuccess shouldBe true
                }

                val manaPool = result.newState.getEntity(game.player1Id)?.get<ManaPoolComponent>()
                withClue("Should produce no mana — opponent's legendary doesn't count") {
                    if (manaPool != null) {
                        manaPool.white shouldBe 0
                        manaPool.blue shouldBe 0
                        manaPool.black shouldBe 0
                        manaPool.red shouldBe 0
                        manaPool.green shouldBe 0
                    }
                }
            }
        }
    }
}
