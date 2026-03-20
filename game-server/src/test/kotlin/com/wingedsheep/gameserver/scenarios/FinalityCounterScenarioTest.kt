package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import io.kotest.matchers.shouldBe

/**
 * Tests for the finality counter death replacement mechanic.
 *
 * If a permanent with a finality counter on it would die (go from battlefield
 * to graveyard), it is exiled instead.
 */
class FinalityCounterScenarioTest : ScenarioTestBase() {

    init {
        test("Creature with finality counter is exiled instead of dying when destroyed") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Glory Seeker")
                .withCardInHand(2, "Shock")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Mountain")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withActivePlayer(2)
                .build()

            // Add finality counter to Glory Seeker via direct state manipulation
            val glorySeekerId = game.findPermanent("Glory Seeker")!!
            game.state = game.state.updateEntity(glorySeekerId) { container ->
                val counters = container.get<CountersComponent>() ?: CountersComponent()
                container.with(counters.withAdded(CounterType.FINALITY, 1))
            }

            // Shock the Glory Seeker (2 damage to a 2/2)
            game.castSpell(2, "Shock", glorySeekerId)
            game.resolveStack()

            // Glory Seeker should NOT be in graveyard (it has finality counter)
            game.isInGraveyard(1, "Glory Seeker") shouldBe false

            // Glory Seeker should NOT be on battlefield
            game.isOnBattlefield("Glory Seeker") shouldBe false

            // Glory Seeker should be in exile
            val exileZone = game.state.getZone(ZoneKey(game.player1Id, Zone.EXILE))
            val inExile = exileZone.any { entityId ->
                game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Glory Seeker"
            }
            inExile shouldBe true
        }

        test("Creature without finality counter dies normally") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Glory Seeker")
                .withCardInHand(2, "Shock")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Mountain")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withActivePlayer(2)
                .build()

            val glorySeekerId = game.findPermanent("Glory Seeker")!!

            game.castSpell(2, "Shock", glorySeekerId)
            game.resolveStack()

            // Without finality counter, it should go to graveyard normally
            game.isInGraveyard(1, "Glory Seeker") shouldBe true
        }
    }
}
