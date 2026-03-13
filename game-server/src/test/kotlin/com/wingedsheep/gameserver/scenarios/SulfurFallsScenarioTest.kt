package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

class SulfurFallsScenarioTest : ScenarioTestBase() {

    init {
        context("Sulfur Falls") {
            test("enters tapped when you control no Island or Mountain") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Sulfur Falls")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cardId = game.state.getHand(game.player1Id).find { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Sulfur Falls"
                }!!

                val result = game.execute(PlayLand(game.player1Id, cardId))
                withClue("Playing Sulfur Falls should succeed") {
                    result.error shouldBe null
                }

                val permanentId = game.findPermanent("Sulfur Falls")!!
                val isTapped = game.state.getEntity(permanentId)?.get<TappedComponent>() != null
                withClue("Sulfur Falls should enter tapped with no Island or Mountain") {
                    isTapped shouldBe true
                }
            }

            test("enters untapped when you control an Island") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Sulfur Falls")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cardId = game.state.getHand(game.player1Id).find { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Sulfur Falls"
                }!!

                val result = game.execute(PlayLand(game.player1Id, cardId))
                withClue("Playing Sulfur Falls should succeed") {
                    result.error shouldBe null
                }

                val permanentId = game.findPermanent("Sulfur Falls")!!
                val isTapped = game.state.getEntity(permanentId)?.get<TappedComponent>() != null
                withClue("Sulfur Falls should enter untapped with an Island") {
                    isTapped shouldBe false
                }
            }

            test("enters untapped when you control a Mountain") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Sulfur Falls")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cardId = game.state.getHand(game.player1Id).find { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Sulfur Falls"
                }!!

                val result = game.execute(PlayLand(game.player1Id, cardId))
                withClue("Playing Sulfur Falls should succeed") {
                    result.error shouldBe null
                }

                val permanentId = game.findPermanent("Sulfur Falls")!!
                val isTapped = game.state.getEntity(permanentId)?.get<TappedComponent>() != null
                withClue("Sulfur Falls should enter untapped with a Mountain") {
                    isTapped shouldBe false
                }
            }

            test("enters tapped when you control no lands") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Sulfur Falls")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cardId = game.state.getHand(game.player1Id).find { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Sulfur Falls"
                }!!

                val result = game.execute(PlayLand(game.player1Id, cardId))
                withClue("Playing Sulfur Falls should succeed") {
                    result.error shouldBe null
                }

                val permanentId = game.findPermanent("Sulfur Falls")!!
                val isTapped = game.state.getEntity(permanentId)?.get<TappedComponent>() != null
                withClue("Sulfur Falls should enter tapped with no lands") {
                    isTapped shouldBe true
                }
            }
        }
    }
}
