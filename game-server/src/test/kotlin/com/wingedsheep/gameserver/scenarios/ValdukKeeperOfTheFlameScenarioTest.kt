package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import io.kotest.matchers.shouldBe

class ValdukKeeperOfTheFlameScenarioTest : ScenarioTestBase() {

    private fun ScenarioTestBase.TestGame.countTokensOnBattlefield(playerNumber: Int, tokenName: String): Int {
        val playerId = if (playerNumber == 1) player1Id else player2Id
        return state.getBattlefield(playerId).count { entityId ->
            val container = state.getEntity(entityId) ?: return@count false
            val card = container.get<CardComponent>() ?: return@count false
            container.has<TokenComponent>() && card.name == tokenName
        }
    }

    private fun ScenarioTestBase.TestGame.countTokensInExile(playerNumber: Int, tokenName: String): Int {
        val playerId = if (playerNumber == 1) player1Id else player2Id
        return state.getExile(playerId).count { entityId ->
            val container = state.getEntity(entityId) ?: return@count false
            val card = container.get<CardComponent>() ?: return@count false
            container.has<TokenComponent>() && card.name == tokenName
        }
    }

    /**
     * Attach an equipment/aura that's already on the battlefield to a creature.
     * This manually wires up the AttachedToComponent and AttachmentsComponent.
     */
    private fun ScenarioTestBase.TestGame.attachTo(attachmentName: String, creatureName: String) {
        val attachmentId = findPermanent(attachmentName)!!
        val creatureId = findPermanent(creatureName)!!

        // Add AttachedToComponent to the attachment
        var attachmentContainer = state.getEntity(attachmentId)!!
        attachmentContainer = attachmentContainer.with(AttachedToComponent(creatureId))
        state = state.withEntity(attachmentId, attachmentContainer)

        // Add/update AttachmentsComponent on the creature
        var creatureContainer = state.getEntity(creatureId)!!
        val existing = creatureContainer.get<AttachmentsComponent>()
        val newAttachments = (existing?.attachedIds ?: emptyList()) + attachmentId
        creatureContainer = creatureContainer.with(AttachmentsComponent(newAttachments))
        state = state.withEntity(creatureId, creatureContainer)
    }

    init {
        context("Valduk, Keeper of the Flame") {
            test("creates tokens at beginning of combat equal to number of attachments") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Valduk, Keeper of the Flame")
                    .withCardOnBattlefield(1, "Short Sword")
                    .withCardOnBattlefield(1, "Frenzied Rage")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Manually attach the equipment and aura to Valduk
                game.attachTo("Short Sword", "Valduk, Keeper of the Flame")
                game.attachTo("Frenzied Rage", "Valduk, Keeper of the Flame")

                // Advance to beginning of combat - triggers Valduk's ability
                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)

                // Resolve Valduk's triggered ability
                game.resolveStack()

                // Should have created 2 Elemental tokens (one per attachment)
                game.countTokensOnBattlefield(1, "Elemental Token") shouldBe 2
            }

            test("creates no tokens when no attachments") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Valduk, Keeper of the Flame")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Advance to beginning of combat
                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)

                // Resolve Valduk's triggered ability (creates 0 tokens)
                game.resolveStack()

                // No tokens should be created
                game.countTokensOnBattlefield(1, "Elemental Token") shouldBe 0
            }

            test("tokens are exiled at the beginning of the next end step") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Valduk, Keeper of the Flame")
                    .withCardOnBattlefield(1, "Short Sword")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Attach equipment
                game.attachTo("Short Sword", "Valduk, Keeper of the Flame")

                // Advance to beginning of combat
                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                game.resolveStack()

                // Token should be on battlefield
                game.countTokensOnBattlefield(1, "Elemental Token") shouldBe 1

                // Advance to end step - delayed trigger should exile the token
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                // Token should no longer be on the battlefield
                game.countTokensOnBattlefield(1, "Elemental Token") shouldBe 0
            }
        }
    }
}
