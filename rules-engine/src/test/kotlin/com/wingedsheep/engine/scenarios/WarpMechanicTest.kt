package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.WarpedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.WarpExiledComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class WarpMechanicTest : FunSpec({

    val warpCreature = card("Warp Test Creature") {
        manaCost = "{3}{R}{R}"
        typeLine = "Creature — Elemental"
        power = 4
        toughness = 3
        warp = "{1}{R}"
        keywords(Keyword.HASTE)
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(warpCreature))
        return driver
    }

    fun GameTestDriver.gotoMainPhase() {
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("warp creature can be cast for its warp cost") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.gotoMainPhase()

        val player = driver.activePlayer!!
        val cardId = driver.putCardInHand(player, "Warp Test Creature")
        driver.giveMana(player, Color.RED, 2)

        val result = driver.submit(
            CastSpell(
                playerId = player,
                cardId = cardId,
                useAlternativeCost = true,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        result.isSuccess shouldBe true
        driver.stackSize shouldBe 1
    }

    test("warped creature enters battlefield with WarpedComponent") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.gotoMainPhase()

        val player = driver.activePlayer!!
        val cardId = driver.putCardInHand(player, "Warp Test Creature")
        driver.giveMana(player, Color.RED, 2)

        driver.submit(
            CastSpell(
                playerId = player,
                cardId = cardId,
                useAlternativeCost = true,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        driver.bothPass()

        val permanent = driver.findPermanent(player, "Warp Test Creature")
        permanent shouldNotBe null
        driver.state.getEntity(permanent!!)?.has<WarpedComponent>() shouldBe true
    }

    test("warped creature is exiled at beginning of next end step") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.gotoMainPhase()

        val player = driver.activePlayer!!
        val cardId = driver.putCardInHand(player, "Warp Test Creature")
        driver.giveMana(player, Color.RED, 2)

        driver.submit(
            CastSpell(
                playerId = player,
                cardId = cardId,
                useAlternativeCost = true,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        driver.bothPass()

        driver.findPermanent(player, "Warp Test Creature") shouldNotBe null

        // Advance to end step — delayed trigger should exile it
        driver.passPriorityUntil(Step.END)
        // The delayed trigger fires as a triggered ability on the stack — resolve it
        driver.bothPass()

        // Creature should no longer be on battlefield
        driver.findPermanent(player, "Warp Test Creature") shouldBe null

        // Card should be in exile with WarpExiledComponent
        driver.getExileCardNames(player) shouldBe listOf("Warp Test Creature")
        val exiledCardId = driver.getExile(player).first()
        driver.state.getEntity(exiledCardId)?.has<WarpExiledComponent>() shouldBe true
    }

    test("warped creature can be re-cast from exile using warp cost") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.gotoMainPhase()

        val player = driver.activePlayer!!
        val cardId = driver.putCardInHand(player, "Warp Test Creature")
        driver.giveMana(player, Color.RED, 2)

        driver.submit(
            CastSpell(
                playerId = player,
                cardId = cardId,
                useAlternativeCost = true,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        driver.bothPass()
        driver.passPriorityUntil(Step.END)
        driver.bothPass() // Resolve the warp exile trigger

        // Find the exiled card
        val exiledCardId = driver.getExile(player).first {
            driver.state.getEntity(it)?.get<CardComponent>()?.name == "Warp Test Creature"
        }

        // Advance through the rest of turn 1 and opponent's turn 2
        // Currently at end step of turn 1 (warp trigger already resolved)
        driver.passPriorityUntil(Step.END) // Ensure we're at end step
        driver.bothPass() // Finish turn 1 → turn 2 (opponent)
        driver.passPriorityUntil(Step.END) // Go through opponent's turn
        driver.bothPass() // Finish turn 2 → turn 3 (player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN) // Arrive at player's main phase

        // Give mana for warp cost again
        driver.giveMana(player, Color.RED, 2)

        // Cast from exile using warp
        val result = driver.submit(
            CastSpell(
                playerId = player,
                cardId = exiledCardId,
                useAlternativeCost = true,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        result.isSuccess shouldBe true

        // Resolve — creature should be on battlefield again
        driver.bothPass()
        driver.findPermanent(player, "Warp Test Creature") shouldNotBe null
    }

    test("spellWarpedThisTurn is set when a spell is warped") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.gotoMainPhase()

        val player = driver.activePlayer!!
        val cardId = driver.putCardInHand(player, "Warp Test Creature")
        driver.giveMana(player, Color.RED, 2)

        driver.state.spellWarpedThisTurn shouldBe false

        driver.submit(
            CastSpell(
                playerId = player,
                cardId = cardId,
                useAlternativeCost = true,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )

        driver.state.spellWarpedThisTurn shouldBe true
    }

    test("creature cast for normal cost does not get WarpedComponent") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.gotoMainPhase()

        val player = driver.activePlayer!!
        val cardId = driver.putCardInHand(player, "Warp Test Creature")
        driver.giveMana(player, Color.RED, 5)

        driver.submit(
            CastSpell(
                playerId = player,
                cardId = cardId,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        driver.bothPass()

        val permanent = driver.findPermanent(player, "Warp Test Creature")
        permanent shouldNotBe null
        driver.state.getEntity(permanent!!)?.has<WarpedComponent>() shouldBe false
        driver.state.spellWarpedThisTurn shouldBe false
    }
})
