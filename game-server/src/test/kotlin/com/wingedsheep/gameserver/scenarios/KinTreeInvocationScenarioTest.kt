package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Kin-Tree Invocation.
 *
 * Card reference:
 * - Kin-Tree Invocation ({B}{G}): Sorcery
 *   "Create an X/X black and green Spirit Warrior creature token,
 *    where X is the greatest toughness among creatures you control."
 */
class KinTreeInvocationScenarioTest : ScenarioTestBase() {

    init {
        context("Kin-Tree Invocation token creation") {
            test("creates token with X equal to greatest toughness among creatures you control") {
                // Woolly Loxodon is a 6/7 — greatest toughness should be 7
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Kin-Tree Invocation")
                    .withCardOnBattlefield(1, "Glory Seeker") // 2/2
                    .withCardOnBattlefield(1, "Woolly Loxodon") // 6/7
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Kin-Tree Invocation")
                withClue("Cast should succeed") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                val tokens = game.findAllPermanents("Spirit Warrior Token")
                withClue("Should have created one Spirit Warrior token") {
                    tokens.size shouldBe 1
                }

                val clientState = game.getClientState(1)
                val tokenInfo = clientState.cards[tokens.first()]
                withClue("Token should be 7/7 (greatest toughness among creatures)") {
                    tokenInfo shouldNotBe null
                    tokenInfo!!.power shouldBe 7
                    tokenInfo.toughness shouldBe 7
                }
            }

            test("creates 0/0 token when no creatures are controlled") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Kin-Tree Invocation")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Kin-Tree Invocation")
                withClue("Cast should succeed") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                // 0/0 token should die to state-based actions
                val tokens = game.findAllPermanents("Spirit Warrior Token")
                withClue("0/0 token should have died to state-based actions") {
                    tokens.size shouldBe 0
                }
            }
        }
    }
}
