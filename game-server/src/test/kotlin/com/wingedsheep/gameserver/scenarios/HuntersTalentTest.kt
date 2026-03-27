package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AbilityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Hunter's Talent.
 *
 * Hunter's Talent {1}{G}
 * Enchantment — Class
 *
 * Level 1: When this Class enters, target creature you control deals damage
 * equal to its power to target creature you don't control.
 *
 * Level 2 ({1}{G}): Whenever you attack, target attacking creature gets +1/+0
 * and gains trample until end of turn.
 *
 * Level 3 ({3}{G}): At the beginning of your end step, if you control a creature
 * with power 4 or greater, draw a card.
 */
class HuntersTalentTest : ScenarioTestBase() {

    init {
        context("Hunter's Talent Level 1 — ETB bite effect") {
            test("creature you control deals damage equal to its power to enemy creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Hunter's Talent")
                    .withCardOnBattlefield(1, "Quaketusk Boar") // 4/2
                    .withCardOnBattlefield(2, "Hired Claw") // 1/2
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Hunter's Talent
                val castResult = game.castSpell(1, "Hunter's Talent")
                withClue("Should cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                // ETB trigger fires — select targets: creature you control + creature opponent controls
                val boarId = game.findPermanent("Quaketusk Boar")!!
                val hiredClawId = game.findPermanent("Hired Claw")!!
                val decisionId = game.state.pendingDecision?.id
                    ?: error("Expected target selection decision for ETB trigger")
                game.submitDecision(TargetsResponse(decisionId, mapOf(0 to listOf(boarId), 1 to listOf(hiredClawId))))
                game.resolveStack()

                // Hired Claw (1/2) takes 4 damage from Quaketusk Boar (4/2) — should die
                withClue("Hired Claw should be killed by 4 damage") {
                    game.isOnBattlefield("Hired Claw") shouldBe false
                }

                // Hunter's Talent should be on the battlefield at level 1
                val talentId = game.findPermanent("Hunter's Talent")!!
                val classComponent = game.state.getEntity(talentId)?.get<ClassLevelComponent>()
                withClue("Hunter's Talent should be at level 1") {
                    classComponent shouldNotBe null
                    classComponent?.currentLevel shouldBe 1
                }
            }
        }

        context("Hunter's Talent Level 2 — level up and attack trigger") {
            test("can level up to 2 and trigger on attack") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Hunter's Talent")
                    .withCardOnBattlefield(1, "Quaketusk Boar", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val talentId = game.findPermanent("Hunter's Talent")!!

                // Activate level-up ability (costs {1}{G})
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = talentId,
                        abilityId = AbilityId.classLevelUp(2)
                    )
                )
                withClue("Level-up should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                // Verify level is now 2
                val classComponent = game.state.getEntity(talentId)?.get<ClassLevelComponent>()
                withClue("Should be at level 2") {
                    classComponent?.currentLevel shouldBe 2
                }

                // Now attack with Quaketusk Boar to trigger level 2 ability
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                val attackResult = game.declareAttackers(mapOf("Quaketusk Boar" to 2))
                withClue("Attack should succeed: ${attackResult.error}") {
                    attackResult.error shouldBe null
                }

                // Level 2 triggers: "Whenever you attack, target attacking creature gets +1/+0 and trample"
                // Auto-select Quaketusk Boar as only attacking creature
                game.resolveStack()

                // Quaketusk Boar should now be 5/2 (4 + 1 from level 2) with trample
                val boarId = game.findPermanent("Quaketusk Boar")!!
                val projected = game.state.projectedState
                withClue("Quaketusk Boar should have 5 power (+1/+0 from level 2)") {
                    projected.getPower(boarId) shouldBe 5
                }
            }
        }

        context("Hunter's Talent level-up restrictions") {
            test("cannot level up at instant speed") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Hunter's Talent")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(2) // not active player
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val talentId = game.findPermanent("Hunter's Talent")!!

                // Try to level up — should fail because it's not Player 1's turn (sorcery speed)
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = talentId,
                        abilityId = AbilityId.classLevelUp(2)
                    )
                )
                withClue("Level-up should fail (not sorcery speed)") {
                    result.error shouldNotBe null
                }
            }

            test("cannot skip from level 1 to level 3") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Hunter's Talent")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val talentId = game.findPermanent("Hunter's Talent")!!

                // Try to level up to 3 directly — should fail
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = talentId,
                        abilityId = AbilityId.classLevelUp(3)
                    )
                )
                withClue("Cannot skip to level 3") {
                    result.error shouldNotBe null
                }
            }
        }
    }
}
