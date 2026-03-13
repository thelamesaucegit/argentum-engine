package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Garna, the Bloodflame.
 *
 * Card reference:
 * - Garna, the Bloodflame ({3}{B}{R}): Legendary Creature — Human Warrior 3/3
 *   Flash
 *   When Garna enters the battlefield, return to your hand all creature cards
 *   in your graveyard that were put there from anywhere this turn.
 *   Other creatures you control have haste.
 */
class GarnaTheBloodflameScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Garna, the Bloodflame") {

            test("ETB returns creature cards put into graveyard this turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Cabal Evangel") // 2/2 creature
                    .withCardInHand(1, "Cast Down") // {1}{B} destroy nonlegendary
                    .withCardInHand(1, "Garna, the Bloodflame")
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val creatureId = game.findPermanent("Cabal Evangel")!!

                // Player 1 destroys their own Cabal Evangel
                val destroyResult = game.castSpell(1, "Cast Down", creatureId)
                withClue("Cast Down should cast successfully") {
                    destroyResult.error shouldBe null
                }
                game.resolveStack()

                withClue("Cabal Evangel should be in graveyard") {
                    game.isInGraveyard(1, "Cabal Evangel") shouldBe true
                }

                // Now cast Garna
                val garnaResult = game.castSpell(1, "Garna, the Bloodflame")
                withClue("Garna should cast successfully") {
                    garnaResult.error shouldBe null
                }
                game.resolveStack()

                withClue("Garna should be on the battlefield") {
                    game.isOnBattlefield("Garna, the Bloodflame") shouldBe true
                }
                withClue("Cabal Evangel should be returned to hand (put into graveyard this turn)") {
                    game.isInHand(1, "Cabal Evangel") shouldBe true
                }
                withClue("Cabal Evangel should not be in graveyard anymore") {
                    game.isInGraveyard(1, "Cabal Evangel") shouldBe false
                }
            }

            test("ETB does not return creature cards already in graveyard from previous turns") {
                // Turn number 2 so cards placed in graveyard during setup (at turn 2)
                // won't match the current turn. We then advance to turn 3 conceptually
                // by setting withTurnNumber(3).
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInGraveyard(1, "Cabal Evangel") // Will have GraveyardEntryTurnComponent(1)
                    .withCardInHand(1, "Garna, the Bloodflame")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .withTurnNumber(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val garnaResult = game.castSpell(1, "Garna, the Bloodflame")
                withClue("Garna should cast successfully") {
                    garnaResult.error shouldBe null
                }
                game.resolveStack()

                withClue("Garna should be on the battlefield") {
                    game.isOnBattlefield("Garna, the Bloodflame") shouldBe true
                }
                withClue("Cabal Evangel should still be in graveyard (was there from previous turns)") {
                    game.isInGraveyard(1, "Cabal Evangel") shouldBe true
                }
            }

            test("other creatures you control have haste") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Garna, the Bloodflame")
                    .withCardOnBattlefield(1, "Cabal Evangel")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val evangelId = game.findPermanent("Cabal Evangel")!!
                val garnaId = game.findPermanent("Garna, the Bloodflame")!!

                val projected = stateProjector.project(game.state)
                withClue("Cabal Evangel should have haste from Garna") {
                    projected.hasKeyword(evangelId, Keyword.HASTE) shouldBe true
                }
                withClue("Garna herself should NOT have haste from her own ability (other creatures only)") {
                    projected.hasKeyword(garnaId, Keyword.HASTE) shouldBe false
                }
            }
        }
    }
}
