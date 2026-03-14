package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Precognition Field.
 *
 * Precognition Field: {3}{U}
 * Enchantment
 * You may look at the top card of your library any time.
 * You may cast instant and sorcery spells from the top of your library.
 * {3}: Exile the top card of your library.
 */
class PrecognitionFieldScenarioTest : ScenarioTestBase() {

    init {
        test("can cast instant from top of library with Precognition Field") {
            val game = ScenarioBuilder()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Precognition Field")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withCardInLibrary(1, "Charge")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Mountain")
                .build()

            val topCardId = game.state.getLibrary(game.player1Id).first()
            game.state.getEntity(topCardId)?.get<CardComponent>()?.name shouldBe "Charge"

            val result = game.execute(CastSpell(game.player1Id, topCardId))
            result.error shouldBe null
        }

        test("can cast sorcery from top of library with Precognition Field") {
            val game = ScenarioBuilder()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Precognition Field")
                .withLandsOnBattlefield(1, "Island", 3)
                .withCardInLibrary(1, "Divination")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Mountain")
                .build()

            val topCardId = game.state.getLibrary(game.player1Id).first()

            val result = game.execute(CastSpell(game.player1Id, topCardId))
            result.error shouldBe null
        }

        test("cannot cast creature from top of library with Precognition Field") {
            val game = ScenarioBuilder()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Precognition Field")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardInLibrary(1, "Knight of New Benalia")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Mountain")
                .build()

            val topCardId = game.state.getLibrary(game.player1Id).first()

            val result = game.execute(CastSpell(game.player1Id, topCardId))
            result.error shouldNotBe null
        }

        test("cannot play land from top of library with Precognition Field") {
            val game = ScenarioBuilder()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Precognition Field")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Mountain")
                .build()

            val topCardId = game.state.getLibrary(game.player1Id).first()

            val result = game.execute(PlayLand(game.player1Id, topCardId))
            result.error shouldNotBe null
        }

        test("activated ability exiles top card of library") {
            val game = ScenarioBuilder()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Precognition Field")
                .withLandsOnBattlefield(1, "Island", 3)
                .withCardInLibrary(1, "Knight of New Benalia")
                .withCardInLibrary(1, "Charge")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Mountain")
                .build()

            val precFieldId = game.findPermanent("Precognition Field")!!
            val topCardId = game.state.getLibrary(game.player1Id).first()
            val topCardName = game.state.getEntity(topCardId)?.get<CardComponent>()?.name
            topCardName shouldBe "Knight of New Benalia"

            // Get the activated ability ID from the card definition
            val ability = cardRegistry.getCard("Precognition Field")!!.script.activatedAbilities.first()

            // Activate {3}: Exile the top card of your library
            val result = game.execute(
                ActivateAbility(game.player1Id, precFieldId, ability.id)
            )
            result.error shouldBe null

            // Both pass to resolve the ability
            game.execute(PassPriority(game.player1Id))
            game.execute(PassPriority(game.player2Id))

            // The creature should now be in exile
            game.state.getExile(game.player1Id).contains(topCardId) shouldBe true

            // Charge should now be on top
            val newTopId = game.state.getLibrary(game.player1Id).first()
            val newTopName = game.state.getEntity(newTopId)?.get<CardComponent>()?.name
            newTopName shouldBe "Charge"
        }
    }
}
