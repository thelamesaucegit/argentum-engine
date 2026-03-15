package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import io.kotest.matchers.shouldBe

class TianaShipsCaretakerScenarioTest : ScenarioTestBase() {

    /**
     * Attach an equipment/aura that's already on the battlefield to a creature.
     */
    private fun TestGame.attachTo(attachmentName: String, creatureName: String) {
        val attachmentId = findPermanent(attachmentName)!!
        val creatureId = findPermanent(creatureName)!!

        var attachmentContainer = state.getEntity(attachmentId)!!
        attachmentContainer = attachmentContainer.with(AttachedToComponent(creatureId))
        state = state.withEntity(attachmentId, attachmentContainer)

        var creatureContainer = state.getEntity(creatureId)!!
        val existing = creatureContainer.get<AttachmentsComponent>()
        val newAttachments = (existing?.attachedIds ?: emptyList()) + attachmentId
        creatureContainer = creatureContainer.with(AttachmentsComponent(newAttachments))
        state = state.withEntity(creatureId, creatureContainer)
    }

    init {
        context("Tiana, Ship's Caretaker") {
            test("returns destroyed Aura to hand at end step") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Tiana, Ship's Caretaker")
                    .withCardOnBattlefield(1, "Arcane Flight")
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withCardInHand(2, "Invoke the Divine")
                    .withLandsOnBattlefield(2, "Plains", 3)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .withPriorityPlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Attach the aura to Glory Seeker
                game.attachTo("Arcane Flight", "Glory Seeker")

                // Opponent destroys Arcane Flight with Invoke the Divine
                val auraId = game.findPermanent("Arcane Flight")!!
                game.castSpell(2, "Invoke the Divine", auraId)
                game.resolveStack()

                // Arcane Flight is in graveyard
                game.isInGraveyard(1, "Arcane Flight") shouldBe true

                // Tiana's triggered ability - MayEffect asks yes/no
                game.answerYesNo(true)
                game.resolveStack()

                // Arcane Flight is still in graveyard (delayed trigger hasn't fired yet)
                game.isInGraveyard(1, "Arcane Flight") shouldBe true

                // Advance to end step - delayed trigger fires
                game.passUntilPhase(Phase.ENDING, Step.END)
                if (game.state.stack.isNotEmpty()) {
                    game.resolveStack()
                }

                // Arcane Flight should be back in hand
                game.isInHand(1, "Arcane Flight") shouldBe true
                game.isInGraveyard(1, "Arcane Flight") shouldBe false
            }

            test("returns destroyed Equipment to hand at end step") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Tiana, Ship's Caretaker")
                    .withCardOnBattlefield(1, "Short Sword")
                    .withCardInHand(2, "Invoke the Divine")
                    .withLandsOnBattlefield(2, "Plains", 3)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .withPriorityPlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Opponent destroys Short Sword
                val swordId = game.findPermanent("Short Sword")!!
                game.castSpell(2, "Invoke the Divine", swordId)
                game.resolveStack()

                // Short Sword is in graveyard
                game.isInGraveyard(1, "Short Sword") shouldBe true

                // Tiana's triggered ability - MayEffect
                game.answerYesNo(true)
                game.resolveStack()

                // Advance to end step
                game.passUntilPhase(Phase.ENDING, Step.END)
                if (game.state.stack.isNotEmpty()) {
                    game.resolveStack()
                }

                // Short Sword should be back in hand
                game.isInHand(1, "Short Sword") shouldBe true
            }

            test("declining may effect leaves card in graveyard") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Tiana, Ship's Caretaker")
                    .withCardOnBattlefield(1, "Short Sword")
                    .withCardInHand(2, "Invoke the Divine")
                    .withLandsOnBattlefield(2, "Plains", 3)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .withPriorityPlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Opponent destroys Short Sword
                val swordId = game.findPermanent("Short Sword")!!
                game.castSpell(2, "Invoke the Divine", swordId)
                game.resolveStack()

                // Decline the may effect
                game.answerYesNo(false)
                game.resolveStack()

                // Advance to end step
                game.passUntilPhase(Phase.ENDING, Step.END)

                // Short Sword should still be in graveyard
                game.isInGraveyard(1, "Short Sword") shouldBe true
                game.isInHand(1, "Short Sword") shouldBe false
            }
        }
    }
}
