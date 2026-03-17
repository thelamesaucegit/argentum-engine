package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class TriumphOfGerrardScenarioTest : ScenarioTestBase() {

    init {
        context("Triumph of Gerrard Saga") {

            test("Chapter I puts +1/+1 counter on creature with greatest power") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Triumph of Gerrard")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardOnBattlefield(1, "Grizzly Bears")     // 2/2
                    .withCardOnBattlefield(1, "Hill Giant")         // 3/3
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Triumph of Gerrard — triggers Chapter I on ETB
                game.castSpell(1, "Triumph of Gerrard")
                game.resolveStack()

                // Chapter I should have triggered — target Hill Giant (greatest power 3)
                if (game.state.pendingDecision != null) {
                    val hillGiantId = game.findPermanent("Hill Giant")!!
                    game.selectTargets(listOf(hillGiantId))
                }

                // Resolve the chapter I ability
                game.resolveStack()

                // Hill Giant should now have a +1/+1 counter
                val hillGiantId = game.findPermanent("Hill Giant")!!
                val counters = game.state.getEntity(hillGiantId)?.get<CountersComponent>()
                counters shouldNotBe null
                counters!!.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
            }

            test("Chapter I only allows targeting creature with greatest power") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Triumph of Gerrard")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardOnBattlefield(1, "Grizzly Bears")     // 2/2
                    .withCardOnBattlefield(1, "Hill Giant")         // 3/3
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Triumph of Gerrard — triggers Chapter I on ETB
                game.castSpell(1, "Triumph of Gerrard")
                game.resolveStack()

                // Chapter I trigger requires targeting — verify Grizzly Bears is NOT a legal target
                val decision = game.state.pendingDecision
                decision shouldNotBe null

                val hillGiantId = game.findPermanent("Hill Giant")!!
                val bearsId = game.findPermanent("Grizzly Bears")!!

                // Legal targets should include Hill Giant (power 3) but not Grizzly Bears (power 2)
                val chooseDecision = decision as ChooseTargetsDecision
                val legalTargets = chooseDecision.legalTargets.values.flatten()
                legalTargets.contains(hillGiantId) shouldBe true
                legalTargets.contains(bearsId) shouldBe false
            }

            test("Chapter III grants flying, first strike, and lifelink until end of turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Triumph of Gerrard")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardOnBattlefield(1, "Hill Giant")         // 3/3
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Triumph of Gerrard — triggers Chapter I on ETB
                game.castSpell(1, "Triumph of Gerrard")
                game.resolveStack()

                // Chapter I — target Hill Giant
                if (game.state.pendingDecision != null) {
                    val hillGiantId = game.findPermanent("Hill Giant")!!
                    game.selectTargets(listOf(hillGiantId))
                }
                game.resolveStack()

                // Advance to next P1 turn for Chapter II
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN) // P2's turn
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN) // P1's turn again

                // Chapter II — target Hill Giant (now 4/4 with the +1/+1 counter)
                if (game.state.pendingDecision != null) {
                    val hillGiantId = game.findPermanent("Hill Giant")!!
                    game.selectTargets(listOf(hillGiantId))
                }
                game.resolveStack()

                // Advance to next P1 turn for Chapter III
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN) // P2's turn
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN) // P1's turn again

                // Chapter III — target Hill Giant
                if (game.state.pendingDecision != null) {
                    val hillGiantId = game.findPermanent("Hill Giant")!!
                    game.selectTargets(listOf(hillGiantId))
                }
                game.resolveStack()

                // Hill Giant should now have flying, first strike, and lifelink
                val hillGiantId = game.findPermanent("Hill Giant")!!
                val projected = game.state.projectedState
                projected.hasKeyword(hillGiantId, Keyword.FLYING) shouldBe true
                projected.hasKeyword(hillGiantId, Keyword.FIRST_STRIKE) shouldBe true
                projected.hasKeyword(hillGiantId, Keyword.LIFELINK) shouldBe true
            }
        }
    }
}
