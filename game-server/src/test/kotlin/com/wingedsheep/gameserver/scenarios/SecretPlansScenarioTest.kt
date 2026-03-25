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
 * Scenario tests for Secret Plans.
 *
 * Card reference:
 * - Secret Plans ({G}{U}): Enchantment
 *   Face-down creatures you control get +0/+1.
 *   Whenever a permanent you control is turned face up, draw a card.
 */
class SecretPlansScenarioTest : ScenarioTestBase() {

    private fun TestGame.findCardInHand(playerNumber: Int, cardName: String): EntityId {
        val playerId = if (playerNumber == 1) player1Id else player2Id
        return state.getHand(playerId).first { entityId ->
            state.getEntity(entityId)?.get<CardComponent>()?.name == cardName
        }
    }

    init {
        context("Secret Plans") {

            test("face-down creatures you control get +0/+1") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Secret Plans")
                    .withCardInHand(1, "Pine Walker")
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Pine Walker face-down for {3}
                val cardId = game.findCardInHand(1, "Pine Walker")
                val castResult = game.execute(CastSpell(game.player1Id, cardId, castFaceDown = true))
                withClue("Cast face-down should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                // Face-down creature should be 2/3 (2/2 base + 0/+1 from Secret Plans)
                val faceDownId = game.state.getBattlefield().find { entityId ->
                    game.state.getEntity(entityId)?.has<FaceDownComponent>() == true
                }
                withClue("Face-down creature should be on battlefield") {
                    faceDownId shouldNotBe null
                }

                val projected = game.state.projectedState
                val power = projected.getPower(faceDownId!!)
                val toughness = projected.getToughness(faceDownId)
                withClue("Face-down creature should be 2/3 (2/2 + 0/+1 from Secret Plans)") {
                    power shouldBe 2
                    toughness shouldBe 3
                }
            }

            test("draws a card when your creature is turned face up") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Secret Plans")
                    .withCardInHand(1, "Pine Walker")
                    .withLandsOnBattlefield(1, "Forest", 10)
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialHandSize = game.handSize(1)

                // Cast Pine Walker face-down for {3}
                val cardId = game.findCardInHand(1, "Pine Walker")
                val castResult = game.execute(CastSpell(game.player1Id, cardId, castFaceDown = true))
                withClue("Cast face-down should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                // Hand size should be one less (cast Pine Walker from hand)
                val handSizeAfterCast = game.handSize(1)
                withClue("Hand should be one less after casting") {
                    handSizeAfterCast shouldBe initialHandSize - 1
                }

                // Turn face up for {4}{G}
                val faceDownId = game.state.getBattlefield().find { entityId ->
                    game.state.getEntity(entityId)?.has<FaceDownComponent>() == true
                }
                withClue("Face-down creature should be on battlefield") {
                    faceDownId shouldNotBe null
                }

                val turnUpResult = game.execute(TurnFaceUp(game.player1Id, faceDownId!!))
                withClue("Turn face-up should succeed: ${turnUpResult.error}") {
                    turnUpResult.error shouldBe null
                }
                game.resolveStack()

                // Should have drawn a card from Secret Plans trigger
                withClue("Should have drawn a card from Secret Plans trigger") {
                    game.handSize(1) shouldBe handSizeAfterCast + 1
                }
            }

            test("does NOT draw a card when opponent's creature is turned face up") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Secret Plans")
                    .withCardInHand(2, "Woolly Loxodon")
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withLandsOnBattlefield(2, "Forest", 10)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val p1HandSize = game.handSize(1)

                // Opponent casts Woolly Loxodon face-down for {3}
                val cardId = game.findCardInHand(2, "Woolly Loxodon")
                val castResult = game.execute(CastSpell(game.player2Id, cardId, castFaceDown = true))
                withClue("Cast face-down should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                // Turn face up for {5}{G}
                val faceDownId = game.state.getBattlefield().find { entityId ->
                    game.state.getEntity(entityId)?.has<FaceDownComponent>() == true
                }
                withClue("Face-down creature should be on battlefield") {
                    faceDownId shouldNotBe null
                }

                val turnUpResult = game.execute(TurnFaceUp(game.player2Id, faceDownId!!))
                withClue("Turn face-up should succeed: ${turnUpResult.error}") {
                    turnUpResult.error shouldBe null
                }
                game.resolveStack()

                // Player 1 should NOT have drawn a card - Secret Plans only triggers for YOUR creatures
                withClue("Player 1 should NOT have drawn a card from opponent's creature turning face up") {
                    game.handSize(1) shouldBe p1HandSize
                }
            }

            test("opponent's face-down creatures do NOT get +0/+1") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Secret Plans")
                    .withCardInHand(2, "Woolly Loxodon")
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withLandsOnBattlefield(2, "Forest", 10)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Opponent casts Woolly Loxodon face-down for {3}
                val cardId = game.findCardInHand(2, "Woolly Loxodon")
                val castResult = game.execute(CastSpell(game.player2Id, cardId, castFaceDown = true))
                withClue("Cast face-down should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                // Opponent's face-down creature should be 2/2 (NOT 2/3)
                val faceDownId = game.state.getBattlefield().find { entityId ->
                    game.state.getEntity(entityId)?.has<FaceDownComponent>() == true
                }
                withClue("Face-down creature should be on battlefield") {
                    faceDownId shouldNotBe null
                }

                val projected = game.state.projectedState
                val power = projected.getPower(faceDownId!!)
                val toughness = projected.getToughness(faceDownId)
                withClue("Opponent's face-down creature should be 2/2 (no Secret Plans bonus)") {
                    power shouldBe 2
                    toughness shouldBe 2
                }
            }
        }
    }
}
