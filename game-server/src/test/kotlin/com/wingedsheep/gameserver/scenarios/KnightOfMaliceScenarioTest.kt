package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Knight of Malice.
 *
 * Knight of Malice is a 2/2 with:
 * - First strike
 * - Hexproof from white (can't be targeted by white spells/abilities opponents control)
 * - Gets +1/+0 as long as any player controls a white permanent
 *
 * Cards used:
 * - Knight of Malice ({1}{B}, 2/2 Human Knight)
 * - Blessed Light ({4}{W}, exile target creature or enchantment) — white spell
 * - Cast Down ({1}{B}, destroy target nonlegendary creature) — black spell
 * - Mesa Unicorn (white creature, used as a white permanent for conditional bonus)
 */
class KnightOfMaliceScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Hexproof from white") {

            test("opponent's white spell cannot target Knight of Malice") {
                val game = scenario()
                    .withPlayers("Owner", "Opponent")
                    .withCardOnBattlefield(1, "Knight of Malice")
                    .withCardInHand(2, "Blessed Light")
                    .withLandsOnBattlefield(2, "Plains", 5)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val knightId = game.findPermanent("Knight of Malice")!!

                val castResult = game.castSpell(2, "Blessed Light", knightId)
                withClue("White spell from opponent should not be able to target creature with hexproof from white") {
                    castResult.error shouldNotBe null
                }
            }

            test("opponent's non-white spell CAN target Knight of Malice") {
                val game = scenario()
                    .withPlayers("Owner", "Opponent")
                    .withCardOnBattlefield(1, "Knight of Malice")
                    .withCardInHand(2, "Cast Down")
                    .withLandsOnBattlefield(2, "Swamp", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val knightId = game.findPermanent("Knight of Malice")!!

                val castResult = game.castSpell(2, "Cast Down", knightId)
                withClue("Black spell from opponent should be able to target creature with hexproof from white: ${castResult.error}") {
                    castResult.error shouldBe null
                }
            }

            test("owner's white spell CAN target own Knight of Malice") {
                // Hexproof from white only prevents opponents from targeting.
                // The controller can still target their own creature with white spells.
                val game = scenario()
                    .withPlayers("Owner", "Opponent")
                    .withCardOnBattlefield(1, "Knight of Malice")
                    .withCardInHand(1, "Blessed Light")
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val knightId = game.findPermanent("Knight of Malice")!!

                val castResult = game.castSpell(1, "Blessed Light", knightId)
                withClue("Owner's white spell should be able to target own creature with hexproof from white: ${castResult.error}") {
                    castResult.error shouldBe null
                }
            }
        }

        context("Conditional +1/+0 bonus") {

            test("gets +1/+0 when any player controls a white permanent") {
                // Mesa Unicorn is a white creature — Knight should be 3/2
                val game = scenario()
                    .withPlayers("Owner", "Opponent")
                    .withCardOnBattlefield(1, "Knight of Malice")
                    .withCardOnBattlefield(2, "Mesa Unicorn")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val knightId = game.findPermanent("Knight of Malice")!!
                val projected = stateProjector.project(game.state)

                withClue("Knight of Malice should be 3/2 when a white permanent is on the battlefield") {
                    projected.getPower(knightId) shouldBe 3
                    projected.getToughness(knightId) shouldBe 2
                }
            }

            test("does not get +1/+0 when no white permanents exist") {
                // No white permanents — Knight should be base 2/2
                val game = scenario()
                    .withPlayers("Owner", "Opponent")
                    .withCardOnBattlefield(1, "Knight of Malice")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val knightId = game.findPermanent("Knight of Malice")!!
                val projected = stateProjector.project(game.state)

                withClue("Knight of Malice should be 2/2 when no white permanents exist") {
                    projected.getPower(knightId) shouldBe 2
                    projected.getToughness(knightId) shouldBe 2
                }
            }

            test("gets +1/+0 when owner controls a white permanent") {
                // Owner's own white permanent should also trigger the bonus
                val game = scenario()
                    .withPlayers("Owner", "Opponent")
                    .withCardOnBattlefield(1, "Knight of Malice")
                    .withCardOnBattlefield(1, "Mesa Unicorn")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val knightId = game.findPermanent("Knight of Malice")!!
                val projected = stateProjector.project(game.state)

                withClue("Knight of Malice should be 3/2 when owner controls a white permanent") {
                    projected.getPower(knightId) shouldBe 3
                    projected.getToughness(knightId) shouldBe 2
                }
            }
        }
    }
}
