package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class KastralTheWindcrestedTest : ScenarioTestBase() {

    private fun TestGame.chooseMode(modeIndex: Int) {
        val decision = getPendingDecision()
        decision shouldNotBe null
        decision as ChooseOptionDecision
        submitDecision(OptionChosenResponse(decision.id, modeIndex))
    }

    /**
     * Helper to advance through combat damage and stop when a decision appears
     * or when we reach the target phase/step.
     */
    private fun TestGame.passUntilDecisionOrPhase(phase: Phase, step: Step) {
        var iterations = 0
        while ((state.phase != phase || state.step != step) &&
               state.pendingDecision == null && iterations < 50) {
            val priorityPlayer = state.priorityPlayerId ?: break
            passPriority()
            iterations++
        }
    }

    init {
        test("Kastral triggers once when multiple Birds deal combat damage - draw mode") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Kastral, the Windcrested")
                .withCardOnBattlefield(1, "Storm Crow")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withActivePlayer(1)
                .build()

            val handSizeBefore = game.handSize(1)

            // Attack with both Birds
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf(
                "Kastral, the Windcrested" to 2,
                "Storm Crow" to 2
            ))
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareNoBlockers()

            // Advance through combat damage; trigger fires and goes on stack
            // Then resolves and presents modal choice
            game.passUntilDecisionOrPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            // Should get exactly one modal choice (batched trigger, not two)
            game.hasPendingDecision() shouldBe true
            game.chooseMode(2) // Draw a card

            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            // Should have drawn exactly 1 card (trigger fired once, not twice)
            game.handSize(1) shouldBe handSizeBefore + 1
        }

        test("Kastral mode 1 - put Bird from hand onto battlefield with finality counter") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Kastral, the Windcrested")
                .withCardInHand(1, "Storm Crow")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withActivePlayer(1)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Kastral, the Windcrested" to 2))
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareNoBlockers()

            game.passUntilDecisionOrPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            // Choose mode 0: Put a Bird from hand/graveyard onto battlefield with finality
            game.hasPendingDecision() shouldBe true
            game.chooseMode(0)

            // Select Storm Crow from the gathered collection
            if (game.hasPendingDecision()) {
                val crows = game.findCardsInHand(1, "Storm Crow")
                game.selectCards(crows)
            }

            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            game.isOnBattlefield("Storm Crow") shouldBe true
            game.isInHand(1, "Storm Crow") shouldBe false

            // Verify finality counter
            val crowId = game.findPermanent("Storm Crow")!!
            val counters = game.state.getEntity(crowId)?.get<CountersComponent>()
            counters?.getCount(CounterType.FINALITY) shouldBe 1
        }

        test("Kastral mode 2 - put +1/+1 counter on each Bird") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Kastral, the Windcrested")
                .withCardOnBattlefield(1, "Storm Crow")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withActivePlayer(1)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf(
                "Kastral, the Windcrested" to 2,
                "Storm Crow" to 2
            ))
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareNoBlockers()

            game.passUntilDecisionOrPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            // Choose mode 1: Put a +1/+1 counter on each Bird you control
            game.hasPendingDecision() shouldBe true
            game.chooseMode(1)

            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            // Both Birds should have +1/+1 counters
            val kastralId = game.findPermanent("Kastral, the Windcrested")!!
            val kastralCounters = game.state.getEntity(kastralId)?.get<CountersComponent>()
            kastralCounters?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1

            val crowId = game.findPermanent("Storm Crow")!!
            val crowCounters = game.state.getEntity(crowId)?.get<CountersComponent>()
            crowCounters?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
        }

        test("Kastral does not trigger when non-Bird creature deals combat damage") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Kastral, the Windcrested")
                .withCardOnBattlefield(1, "Glory Seeker")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withActivePlayer(1)
                .build()

            val handSizeBefore = game.handSize(1)

            // Only attack with the non-Bird Glory Seeker
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Glory Seeker" to 2))
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareNoBlockers()

            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            // No trigger should have fired
            game.handSize(1) shouldBe handSizeBefore
        }
    }
}
