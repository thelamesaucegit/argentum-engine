package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Thorn Elemental.
 *
 * Thorn Elemental: {5}{G}{G} 7/7 Creature — Elemental
 * "You may have Thorn Elemental assign its combat damage as though it weren't blocked."
 *
 * These tests verify:
 * 1. When blocked and player chooses "yes", damage goes to defending player
 * 2. When blocked and player chooses "no", damage goes to blockers normally
 * 3. When unblocked, no decision is presented and damage goes to defending player
 */
class ThornElementalScenarioTest : ScenarioTestBase() {

    init {
        context("Thorn Elemental assign-as-unblocked ability") {

            test("when blocked and player chooses to assign as unblocked, damage goes to defending player") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Thorn Elemental")  // 7/7
                    .withCardOnBattlefield(2, "Grizzly Bears")    // 2/2
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val thornId = game.findPermanent("Thorn Elemental")!!
                val bearsId = game.findPermanent("Grizzly Bears")!!
                val startingLife = game.getLifeTotal(2)

                // Declare Thorn Elemental as attacker
                game.execute(DeclareAttackers(game.player1Id, mapOf(thornId to game.player2Id)))

                // Advance to declare blockers
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Block with Grizzly Bears
                game.execute(DeclareBlockers(game.player2Id, mapOf(bearsId to listOf(thornId))))

                // Pass priority until combat damage — a YesNoDecision should appear
                // asking whether to assign damage as though unblocked
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)

                // Should have a yes/no decision
                withClue("Should have a pending YesNo decision") {
                    game.state.pendingDecision shouldNotBe null
                    game.state.pendingDecision!!::class.simpleName shouldBe "YesNoDecision"
                }

                // Choose to assign damage to the defending player
                game.answerYesNo(true)

                // Advance to postcombat main
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // Defending player should have taken 7 damage
                withClue("Defending player should take 7 damage") {
                    game.getLifeTotal(2) shouldBe startingLife - 7
                }

                // Grizzly Bears survives (no damage assigned to it)
                withClue("Grizzly Bears should survive") {
                    game.findPermanent("Grizzly Bears") shouldBe bearsId
                }

                // Thorn Elemental takes 2 damage from Grizzly Bears but survives (7 toughness)
                withClue("Thorn Elemental should survive") {
                    game.findPermanent("Thorn Elemental") shouldBe thornId
                }
            }

            test("when blocked and player chooses to assign to blockers, damage goes to blockers normally") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Thorn Elemental")  // 7/7
                    .withCardOnBattlefield(2, "Grizzly Bears")    // 2/2
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val thornId = game.findPermanent("Thorn Elemental")!!
                val bearsId = game.findPermanent("Grizzly Bears")!!
                val startingLife = game.getLifeTotal(2)

                // Declare Thorn Elemental as attacker
                game.execute(DeclareAttackers(game.player1Id, mapOf(thornId to game.player2Id)))

                // Advance to declare blockers
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Block with Grizzly Bears
                game.execute(DeclareBlockers(game.player2Id, mapOf(bearsId to listOf(thornId))))

                // Advance to postcombat main — autoResolveDecision answers "no" to the
                // YesNoDecision, so damage is assigned to blockers normally
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // Defending player should NOT take damage (Thorn Elemental was blocked)
                withClue("Defending player should take no damage") {
                    game.getLifeTotal(2) shouldBe startingLife
                }

                // Grizzly Bears should be dead (7 damage vs 2 toughness)
                withClue("Grizzly Bears should be dead") {
                    game.findPermanent("Grizzly Bears") shouldBe null
                }

                // Thorn Elemental takes 2 damage from Grizzly Bears but survives
                withClue("Thorn Elemental should survive") {
                    game.findPermanent("Thorn Elemental") shouldBe thornId
                }
            }

            test("when unblocked, no decision is presented and damage goes to defending player") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Thorn Elemental")  // 7/7
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val thornId = game.findPermanent("Thorn Elemental")!!
                val startingLife = game.getLifeTotal(2)

                // Declare Thorn Elemental as attacker
                game.execute(DeclareAttackers(game.player1Id, mapOf(thornId to game.player2Id)))

                // Advance through combat — no decision should appear
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // Defending player should take 7 damage
                withClue("Defending player should take 7 damage from unblocked Thorn Elemental") {
                    game.getLifeTotal(2) shouldBe startingLife - 7
                }
            }
        }
    }
}
