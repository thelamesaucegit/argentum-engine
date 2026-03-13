package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Grand Warlord Radha.
 *
 * Card reference:
 * - Grand Warlord Radha ({2}{R}{G}): Legendary Creature — Elf Warrior 3/4
 *   Haste
 *   Whenever one or more creatures you control attack, add that much mana in any
 *   combination of {R} and/or {G}. Until end of turn, you don't lose this mana as
 *   steps and phases end.
 */
class GrandWarlordRadhaScenarioTest : ScenarioTestBase() {

    init {
        context("Grand Warlord Radha") {

            test("attacking with multiple creatures adds mana (choose all red)") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Grand Warlord Radha")
                    .withCardOnBattlefield(1, "Llanowar Elves")
                    .withCardOnBattlefield(1, "Goblin Chainwhirler")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                    .build()

                // Move to declare attackers
                game.passPriority() // P1 passes in begin combat
                game.passPriority() // P2 passes in begin combat

                // Attack with all 3 creatures
                game.declareAttackers(mapOf(
                    "Grand Warlord Radha" to 2,
                    "Llanowar Elves" to 2,
                    "Goblin Chainwhirler" to 2
                ))

                // Resolve Radha's triggered ability — should pause for mana distribution
                game.resolveStack()

                val decision = game.state.pendingDecision
                withClue("Should have a number decision for mana distribution") {
                    decision shouldNotBe null
                }

                // Colors are sorted by WUBRG ordinal: RED first, GREEN second
                // Choose 3 red (all red, 0 green)
                game.chooseNumber(3)

                // Verify mana pool
                val pool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()!!
                pool.red shouldBe 3
                pool.green shouldBe 0
            }

            test("attacking with multiple creatures adds mana (split between colors)") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Grand Warlord Radha")
                    .withCardOnBattlefield(1, "Llanowar Elves")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                    .build()

                // Move to declare attackers
                game.passPriority()
                game.passPriority()

                // Attack with 2 creatures
                game.declareAttackers(mapOf(
                    "Grand Warlord Radha" to 2,
                    "Llanowar Elves" to 2
                ))

                // Resolve triggered ability
                game.resolveStack()

                // Colors sorted by WUBRG ordinal: RED first, GREEN second
                // Choose 1 red, rest (1) goes to green
                game.chooseNumber(1)

                val pool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()!!
                pool.red shouldBe 1
                pool.green shouldBe 1
            }

            test("Radha has haste and can attack with summoning sickness") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Grand Warlord Radha", summoningSickness = true)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                    .build()

                // Move to declare attackers
                game.passPriority()
                game.passPriority()

                // Radha should be able to attack even with summoning sickness (haste)
                val result = game.declareAttackers(mapOf("Grand Warlord Radha" to 2))
                withClue("Radha should be able to attack (haste)") {
                    result.error shouldBe null
                }
            }
        }
    }
}
