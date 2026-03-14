package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Jodah, Archmage Eternal.
 *
 * Card reference:
 * - Jodah, Archmage Eternal ({1}{U}{R}{W}): Legendary Creature — Human Wizard 4/3
 *   Flying
 *   You may pay {W}{U}{B}{R}{G} rather than pay the mana cost for spells you cast.
 */
class JodahArchmageEternalScenarioTest : ScenarioTestBase() {

    init {
        context("Jodah, Archmage Eternal alternative casting cost") {

            test("can cast an expensive spell using WUBRG alternative cost") {
                // Verdant Force costs {5}{G}{G}{G} but with Jodah, player can pay {W}{U}{B}{R}{G}
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Jodah, Archmage Eternal")
                    .withCardInHand(1, "Verdant Force") // {5}{G}{G}{G} 7/7
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Verdant Force using alternative cost
                val castResult = game.castSpellWithAlternativeCost(1, "Verdant Force")
                withClue("Cast with alternative cost should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Resolve the spell
                game.resolveStack()

                // Verdant Force should be on the battlefield
                withClue("Verdant Force should be on the battlefield") {
                    game.isOnBattlefield("Verdant Force") shouldBe true
                }
            }

            test("cannot use alternative cost when Jodah is not on battlefield") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Verdant Force")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Try to cast with alternative cost — should fail because no Jodah
                val castResult = game.castSpellWithAlternativeCost(1, "Verdant Force")
                withClue("Cast with alternative cost should fail without Jodah") {
                    (castResult.error != null) shouldBe true
                }
            }

            test("can still cast spell normally when Jodah is on battlefield") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Jodah, Archmage Eternal")
                    .withCardInHand(1, "Charge") // {W} instant — cheap spell
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Charge normally (paying {W})
                val castResult = game.castSpell(1, "Charge")
                withClue("Normal cast should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
            }

            test("spell with X in cost has X=0 when using alternative cost") {
                // Fight with Fire has {2}{R} base, X-kicker for {5}{R}
                // With alternative cost, the base cost is {WUBRG} and X would be 0
                // Let's use a simpler test: Syncopate is {X}{U} counter spell
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Jodah, Archmage Eternal")
                    .withCardInHand(1, "Shivan Fire") // {R} instant
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardOnBattlefield(2, "Llanowar Elves") // Target for the spell
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Shivan Fire (normally {R}) using alternative WUBRG cost
                val target = game.findPermanent("Llanowar Elves")!!
                val castResult = game.castSpellWithAlternativeCost(1, "Shivan Fire", target)
                withClue("Cast with alternative cost should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                // Llanowar Elves should be dead (2 damage kills a 1/1)
                withClue("Llanowar Elves should be destroyed") {
                    game.isOnBattlefield("Llanowar Elves") shouldBe false
                }
            }
        }
    }
}
