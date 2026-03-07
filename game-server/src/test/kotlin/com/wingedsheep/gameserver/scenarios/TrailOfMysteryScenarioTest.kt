package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Trail of Mystery.
 *
 * Card reference:
 * - Trail of Mystery ({1}{G}): Enchantment
 *   Whenever a face-down creature you control enters, you may search your library for
 *   a basic land card, reveal it, put it into your hand, then shuffle.
 *   Whenever a permanent you control is turned face up, if it's a creature, it gets
 *   +2/+2 until end of turn.
 */
class TrailOfMysteryScenarioTest : ScenarioTestBase() {

    private fun findCardsInLibrary(game: TestGame, playerNumber: Int, cardName: String): List<EntityId> {
        val playerId = if (playerNumber == 1) game.player1Id else game.player2Id
        return game.state.getLibrary(playerId).filter { entityId ->
            game.state.getEntity(entityId)?.get<CardComponent>()?.name == cardName
        }
    }

    init {
        context("Trail of Mystery") {

            test("searches for basic land when face-down creature enters") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Trail of Mystery")
                    .withCardInHand(1, "Pine Walker")
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Pine Walker face-down for {3}
                val cardId = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Pine Walker"
                }
                val castResult = game.execute(CastSpell(game.player1Id, cardId, castFaceDown = true))
                withClue("Cast face-down should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                // Trail of Mystery triggers - answer "yes" to the may ability
                withClue("Should have pending may decision") {
                    game.hasPendingDecision() shouldBe true
                }
                game.answerYesNo(true)

                // Select a basic land from the library search
                withClue("Should have pending search decision") {
                    game.hasPendingDecision() shouldBe true
                }
                val forestsInLibrary = findCardsInLibrary(game, 1, "Forest")
                game.selectCards(listOf(forestsInLibrary.first()))

                // Verify the Forest is now in hand
                withClue("Forest should be in hand after search") {
                    game.isInHand(1, "Forest") shouldBe true
                }
            }

            test("does not trigger when face-up creature enters") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Trail of Mystery")
                    .withCardInHand(1, "Alpine Grizzly")
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Alpine Grizzly normally (face up)
                game.castSpell(1, "Alpine Grizzly")
                game.resolveStack()

                // Stack should be empty - no trigger from Trail of Mystery
                withClue("Stack should be empty - Trail of Mystery should not trigger for face-up creatures") {
                    game.state.stack.size shouldBe 0
                }
            }

            test("gives +2/+2 when creature is turned face up") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Trail of Mystery")
                    .withCardInHand(1, "Pine Walker")
                    .withLandsOnBattlefield(1, "Forest", 10)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Pine Walker face-down for {3}
                val cardId = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Pine Walker"
                }
                val castResult = game.execute(CastSpell(game.player1Id, cardId, castFaceDown = true))
                withClue("Cast face-down should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                // Trail of Mystery trigger for face-down entering - decline the search
                game.answerYesNo(false)

                // Find the face-down creature
                val faceDownId = game.state.getBattlefield().find { entityId ->
                    game.state.getEntity(entityId)?.has<FaceDownComponent>() == true
                }
                withClue("Face-down creature should be on battlefield") {
                    faceDownId shouldNotBe null
                }

                // Turn face up for {4}{G}
                val turnUpResult = game.execute(TurnFaceUp(game.player1Id, faceDownId!!))
                withClue("Turn face-up should succeed: ${turnUpResult.error}") {
                    turnUpResult.error shouldBe null
                }

                // Resolve triggers (Pine Walker untap + Trail of Mystery +2/+2)
                game.resolveStack()

                // Pine Walker should be 7/7 (5/5 base + 2/2 from Trail of Mystery)
                val projected = game.state.projectedState
                val pineWalkerId = game.findPermanent("Pine Walker")
                withClue("Pine Walker should be on battlefield") {
                    pineWalkerId shouldNotBe null
                }
                val power = projected.getPower(pineWalkerId!!)
                val toughness = projected.getToughness(pineWalkerId)
                withClue("Pine Walker should be 7/7 with Trail of Mystery buff") {
                    power shouldBe 7
                    toughness shouldBe 7
                }
            }
        }
    }
}
