package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContainIgnoringCase

/**
 * Scenario tests for Brave the Sands (KTK #5).
 *
 * Brave the Sands {1}{W}
 * Enchantment
 * Creatures you control have vigilance.
 * Each creature you control can block an additional creature each combat.
 */
class BraveTheSandsScenarioTest : ScenarioTestBase() {

    init {
        context("Vigilance granting") {
            test("creatures you control have vigilance") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Brave the Sands")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val bearsId = game.findPermanent("Grizzly Bears")!!
                val projected = game.state.projectedState

                withClue("Bears should have vigilance from Brave the Sands") {
                    projected.hasKeyword(bearsId, com.wingedsheep.sdk.core.Keyword.VIGILANCE) shouldBe true
                }
            }
        }

        context("Block additional creature") {
            test("creature can block two attackers with Brave the Sands") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Devoted Hero")
                    .withCardOnBattlefield(1, "Devoted Hero")
                    .withCardOnBattlefield(2, "Brave the Sands")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val heroes = game.findAllPermanents("Devoted Hero")
                val bearsId = game.findPermanent("Grizzly Bears")!!

                game.execute(
                    DeclareAttackers(game.player1Id, mapOf(
                        heroes[0] to game.player2Id,
                        heroes[1] to game.player2Id
                    ))
                )

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Grizzly Bears blocks BOTH attackers (1 base + 1 additional from Brave the Sands)
                val blockResult = game.execute(
                    DeclareBlockers(
                        game.player2Id,
                        mapOf(bearsId to listOf(heroes[0], heroes[1]))
                    )
                )

                withClue("Should be able to block two attackers with Brave the Sands: ${blockResult.error}") {
                    blockResult.error shouldBe null
                }
            }

            test("creature cannot block three attackers with one Brave the Sands") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Devoted Hero")
                    .withCardOnBattlefield(1, "Devoted Hero")
                    .withCardOnBattlefield(1, "Devoted Hero")
                    .withCardOnBattlefield(2, "Brave the Sands")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val heroes = game.findAllPermanents("Devoted Hero")
                val bearsId = game.findPermanent("Grizzly Bears")!!

                game.execute(
                    DeclareAttackers(game.player1Id, mapOf(
                        heroes[0] to game.player2Id,
                        heroes[1] to game.player2Id,
                        heroes[2] to game.player2Id
                    ))
                )

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Try to block all three - should fail (max 2 with one Brave the Sands)
                val blockResult = game.execute(
                    DeclareBlockers(
                        game.player2Id,
                        mapOf(bearsId to listOf(heroes[0], heroes[1], heroes[2]))
                    )
                )

                withClue("Should NOT be able to block three with only one Brave the Sands") {
                    blockResult.error shouldNotBe null
                    blockResult.error!! shouldContainIgnoringCase "can only block 2 creature"
                }
            }

            test("without Brave the Sands, creature cannot block two attackers") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Devoted Hero")
                    .withCardOnBattlefield(1, "Devoted Hero")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val heroes = game.findAllPermanents("Devoted Hero")
                val bearsId = game.findPermanent("Grizzly Bears")!!

                game.execute(
                    DeclareAttackers(game.player1Id, mapOf(
                        heroes[0] to game.player2Id,
                        heroes[1] to game.player2Id
                    ))
                )

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                val blockResult = game.execute(
                    DeclareBlockers(
                        game.player2Id,
                        mapOf(bearsId to listOf(heroes[0], heroes[1]))
                    )
                )

                withClue("Normal creature should not block two attackers") {
                    blockResult.error shouldNotBe null
                    blockResult.error!! shouldContainIgnoringCase "can only block one creature"
                }
            }
        }
    }
}
