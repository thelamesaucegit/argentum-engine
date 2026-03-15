package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Naban, Dean of Iteration.
 *
 * Card reference:
 * - Naban, Dean of Iteration ({1}{U}): 2/1 Legendary Creature — Human Wizard
 *   If a Wizard entering the battlefield under your control causes a triggered ability
 *   of a permanent you control to trigger, that ability triggers an additional time.
 */
class NabanDeanOfIterationScenarioTest : ScenarioTestBase() {

    init {
        context("Naban, Dean of Iteration") {

            test("Wizard ETB trigger fires an additional time with Naban on battlefield") {
                // Naban is on the battlefield, cast Academy Journeymage (Wizard with ETB bounce)
                // Opponent has two creatures — the trigger should fire twice, bouncing two creatures
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Naban, Dean of Iteration")
                    .withCardInHand(1, "Academy Journeymage")
                    .withCardOnBattlefield(2, "Serra Angel")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Island", 5)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Academy Journeymage (costs {4}{U}, but {1} less with Wizard = {3}{U})
                val castResult = game.castSpell(1, "Academy Journeymage")
                withClue("Cast should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Resolve creature spell — ETB trigger fires
                game.resolveStack()

                // First trigger: select Serra Angel as target
                withClue("Should have pending decision for first ETB trigger target") {
                    game.hasPendingDecision() shouldBe true
                }
                val angelId = game.findPermanent("Serra Angel")!!
                game.selectTargets(listOf(angelId))

                // Resolve first trigger
                game.resolveStack()

                // Second trigger (additional from Naban): select Grizzly Bears as target
                withClue("Should have pending decision for second ETB trigger target (Naban doubling)") {
                    game.hasPendingDecision() shouldBe true
                }
                val bearsId = game.findPermanent("Grizzly Bears")!!
                game.selectTargets(listOf(bearsId))

                // Resolve second trigger
                game.resolveStack()

                // Both creatures should have been bounced
                withClue("Serra Angel should not be on the battlefield") {
                    game.isOnBattlefield("Serra Angel") shouldBe false
                }
                withClue("Grizzly Bears should not be on the battlefield") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }
            }

            test("non-Wizard ETB trigger does NOT get doubled by Naban") {
                // Naban on battlefield, cast a non-Wizard creature with ETB
                // The trigger should only fire once
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Naban, Dean of Iteration")
                    .withCardInHand(1, "Aven Sentry") // non-Wizard creature
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Aven Sentry — it has no ETB trigger, just a vanilla creature
                val castResult = game.castSpell(1, "Aven Sentry")
                withClue("Cast should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Resolve creature spell
                game.resolveStack()

                // No pending decision — no extra triggers
                withClue("Should have no pending decision (no ETB trigger to double)") {
                    game.hasPendingDecision() shouldBe false
                }
            }

            test("without Naban, Wizard ETB trigger fires only once") {
                // No Naban on battlefield — Academy Journeymage trigger fires once
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Academy Journeymage")
                    .withCardOnBattlefield(2, "Serra Angel")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Island", 5)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Academy Journeymage
                val castResult = game.castSpell(1, "Academy Journeymage")
                withClue("Cast should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Resolve creature spell — ETB trigger fires
                game.resolveStack()

                // First trigger: select Serra Angel
                withClue("Should have pending decision for ETB trigger target") {
                    game.hasPendingDecision() shouldBe true
                }
                val angelId = game.findPermanent("Serra Angel")!!
                game.selectTargets(listOf(angelId))

                // Resolve trigger
                game.resolveStack()

                // Only one creature should be bounced — no second trigger
                withClue("Serra Angel should not be on the battlefield") {
                    game.isOnBattlefield("Serra Angel") shouldBe false
                }
                withClue("Grizzly Bears should still be on the battlefield (only one trigger)") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
            }
        }
    }
}
