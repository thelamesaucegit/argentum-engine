package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Sarkhan, the Dragonspeaker.
 *
 * Tests cover:
 * - Starting loyalty on ETB
 * - +1: Become a 4/4 red Dragon creature with flying, indestructible, haste
 * - -3: Deal 4 damage to target creature
 * - Once-per-turn loyalty ability restriction
 * - SBA: planeswalker with 0 loyalty dies
 */
class SarkhanTheDragonspeakerScenarioTest : ScenarioTestBase() {

    init {
        context("Sarkhan, the Dragonspeaker") {

            test("+1 ability turns Sarkhan into a 4/4 Dragon creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Sarkhan, the Dragonspeaker")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val sarkhanId = game.findPermanent("Sarkhan, the Dragonspeaker")!!

                // Manually add starting loyalty counters (normally done on ETB via StackResolver)
                game.state = game.state.updateEntity(sarkhanId) { c ->
                    val counters = c.get<CountersComponent>() ?: CountersComponent()
                    c.with(counters.withAdded(CounterType.LOYALTY, 4))
                }

                // Get the +1 ability (first loyalty ability)
                val cardDef = cardRegistry.getCard("Sarkhan, the Dragonspeaker")!!
                val plusOneAbility = cardDef.script.activatedAbilities[0]

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = sarkhanId,
                        abilityId = plusOneAbility.id
                    )
                )

                withClue("Activation should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                // Verify Sarkhan is now a 4/4 creature
                val projected = game.state.projectedState

                withClue("Sarkhan should be a creature") {
                    projected.isCreature(sarkhanId) shouldBe true
                }
                withClue("Sarkhan should have power 4") {
                    projected.getPower(sarkhanId) shouldBe 4
                }
                withClue("Sarkhan should have toughness 4") {
                    projected.getToughness(sarkhanId) shouldBe 4
                }
                withClue("Sarkhan should have flying") {
                    projected.getKeywords(sarkhanId).contains(Keyword.FLYING.name) shouldBe true
                }
                withClue("Sarkhan should have indestructible") {
                    projected.getKeywords(sarkhanId).contains(Keyword.INDESTRUCTIBLE.name) shouldBe true
                }
                withClue("Sarkhan should have haste") {
                    projected.getKeywords(sarkhanId).contains(Keyword.HASTE.name) shouldBe true
                }

                // Verify loyalty counter was increased by 1 (4 + 1 = 5)
                val counters = game.state.getEntity(sarkhanId)?.get<CountersComponent>()
                withClue("Sarkhan should have 5 loyalty counters") {
                    counters?.getCount(CounterType.LOYALTY) shouldBe 5
                }
            }

            test("-3 ability deals 4 damage to target creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Sarkhan, the Dragonspeaker")
                    .withCardOnBattlefield(2, "Alpine Grizzly") // 4/2
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val sarkhanId = game.findPermanent("Sarkhan, the Dragonspeaker")!!
                val grizzlyId = game.findPermanent("Alpine Grizzly")!!

                // Add starting loyalty counters
                game.state = game.state.updateEntity(sarkhanId) { c ->
                    val counters = c.get<CountersComponent>() ?: CountersComponent()
                    c.with(counters.withAdded(CounterType.LOYALTY, 4))
                }

                // Get the -3 ability (second loyalty ability)
                val cardDef = cardRegistry.getCard("Sarkhan, the Dragonspeaker")!!
                val minusThreeAbility = cardDef.script.activatedAbilities[1]

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = sarkhanId,
                        abilityId = minusThreeAbility.id,
                        targets = listOf(ChosenTarget.Permanent(grizzlyId))
                    )
                )

                withClue("Activation should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                // Alpine Grizzly (4/2) takes 4 damage and should die
                withClue("Alpine Grizzly should be dead") {
                    game.isOnBattlefield("Alpine Grizzly") shouldBe false
                }

                // Sarkhan should have 1 loyalty left (4 - 3 = 1)
                val counters = game.state.getEntity(sarkhanId)?.get<CountersComponent>()
                withClue("Sarkhan should have 1 loyalty counter") {
                    counters?.getCount(CounterType.LOYALTY) shouldBe 1
                }
            }

            test("once-per-turn restriction prevents activating a second loyalty ability") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Sarkhan, the Dragonspeaker")
                    .withCardOnBattlefield(2, "Alpine Grizzly") // 4/2
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val sarkhanId = game.findPermanent("Sarkhan, the Dragonspeaker")!!
                val grizzlyId = game.findPermanent("Alpine Grizzly")!!

                // Add loyalty counters
                game.state = game.state.updateEntity(sarkhanId) { c ->
                    val counters = c.get<CountersComponent>() ?: CountersComponent()
                    c.with(counters.withAdded(CounterType.LOYALTY, 4))
                }

                val cardDef = cardRegistry.getCard("Sarkhan, the Dragonspeaker")!!
                val plusOneAbility = cardDef.script.activatedAbilities[0]
                val minusThreeAbility = cardDef.script.activatedAbilities[1]

                // Activate +1 ability first
                val result1 = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = sarkhanId,
                        abilityId = plusOneAbility.id
                    )
                )

                withClue("First activation should succeed: ${result1.error}") {
                    result1.error shouldBe null
                }
                game.resolveStack()

                // Try to activate -3 ability — should fail
                val result2 = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = sarkhanId,
                        abilityId = minusThreeAbility.id,
                        targets = listOf(ChosenTarget.Permanent(grizzlyId))
                    )
                )

                withClue("Second loyalty ability activation should be rejected") {
                    result2.error shouldNotBe null
                }
            }

            test("planeswalker with 0 loyalty is put into graveyard by SBA") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Sarkhan, the Dragonspeaker")
                    .withCardOnBattlefield(2, "Alpine Grizzly") // 4/2
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val sarkhanId = game.findPermanent("Sarkhan, the Dragonspeaker")!!
                val grizzlyId = game.findPermanent("Alpine Grizzly")!!

                // Set loyalty to exactly 3 so -3 ability brings it to 0
                game.state = game.state.updateEntity(sarkhanId) { c ->
                    val counters = c.get<CountersComponent>() ?: CountersComponent()
                    c.with(counters.withAdded(CounterType.LOYALTY, 3))
                }

                val cardDef = cardRegistry.getCard("Sarkhan, the Dragonspeaker")!!
                val minusThreeAbility = cardDef.script.activatedAbilities[1]

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = sarkhanId,
                        abilityId = minusThreeAbility.id,
                        targets = listOf(ChosenTarget.Permanent(grizzlyId))
                    )
                )
                game.resolveStack()

                // Sarkhan should be dead (0 loyalty → SBA)
                withClue("Sarkhan should be in graveyard after reaching 0 loyalty") {
                    game.isOnBattlefield("Sarkhan, the Dragonspeaker") shouldBe false
                    game.isInGraveyard(1, "Sarkhan, the Dragonspeaker") shouldBe true
                }
            }

            test("cannot activate -3 when loyalty is less than 3") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Sarkhan, the Dragonspeaker")
                    .withCardOnBattlefield(2, "Alpine Grizzly")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val sarkhanId = game.findPermanent("Sarkhan, the Dragonspeaker")!!
                val grizzlyId = game.findPermanent("Alpine Grizzly")!!

                // Set loyalty to only 2
                game.state = game.state.updateEntity(sarkhanId) { c ->
                    val counters = c.get<CountersComponent>() ?: CountersComponent()
                    c.with(counters.withAdded(CounterType.LOYALTY, 2))
                }

                val cardDef = cardRegistry.getCard("Sarkhan, the Dragonspeaker")!!
                val minusThreeAbility = cardDef.script.activatedAbilities[1]

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = sarkhanId,
                        abilityId = minusThreeAbility.id,
                        targets = listOf(ChosenTarget.Permanent(grizzlyId))
                    )
                )

                withClue("Activation should fail due to insufficient loyalty") {
                    result.error shouldNotBe null
                }
            }
        }
    }
}
