package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Heart-Piercer Bow.
 *
 * Heart-Piercer Bow: {2} Artifact — Equipment
 * "Whenever equipped creature attacks, Heart-Piercer Bow deals 1 damage to
 * target creature defending player controls."
 * Equip {1}
 */
class HeartPiercerBowScenarioTest : ScenarioTestBase() {

    init {
        context("Heart-Piercer Bow equip ability") {

            test("equipping attaches the bow to a creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Heart-Piercer Bow")
                    .withCardOnBattlefield(1, "Devoted Hero") // 1/2
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bowId = game.findPermanent("Heart-Piercer Bow")!!
                val heroId = game.findPermanent("Devoted Hero")!!

                val cardDef = cardRegistry.getCard("Heart-Piercer Bow")!!
                val equipAbility = cardDef.script.activatedAbilities.first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = bowId,
                        abilityId = equipAbility.id,
                        targets = listOf(ChosenTarget.Permanent(heroId))
                    )
                )

                withClue("Equip activation should succeed: ${result.error}") {
                    result.error shouldBe null
                }

                game.resolveStack()

                val bowEntity = game.state.getEntity(bowId)!!
                val attachedTo = bowEntity.get<AttachedToComponent>()
                withClue("Bow should be attached to Devoted Hero") {
                    attachedTo shouldNotBe null
                    attachedTo!!.targetId shouldBe heroId
                }

                val heroEntity = game.state.getEntity(heroId)!!
                val attachments = heroEntity.get<AttachmentsComponent>()
                withClue("Devoted Hero should have bow in attachments") {
                    attachments shouldNotBe null
                    attachments!!.attachedIds shouldBe listOf(bowId)
                }
            }

            test("re-equipping detaches from old creature and attaches to new one") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Heart-Piercer Bow")
                    .withCardOnBattlefield(1, "Devoted Hero")
                    .withCardOnBattlefield(1, "Jeskai Student")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bowId = game.findPermanent("Heart-Piercer Bow")!!
                val heroId = game.findPermanent("Devoted Hero")!!
                val studentId = game.findPermanent("Jeskai Student")!!

                val cardDef = cardRegistry.getCard("Heart-Piercer Bow")!!
                val equipAbility = cardDef.script.activatedAbilities.first()

                // First equip to Devoted Hero
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = bowId,
                        abilityId = equipAbility.id,
                        targets = listOf(ChosenTarget.Permanent(heroId))
                    )
                )
                game.resolveStack()

                // Re-equip to Jeskai Student
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = bowId,
                        abilityId = equipAbility.id,
                        targets = listOf(ChosenTarget.Permanent(studentId))
                    )
                )

                withClue("Re-equip should succeed: ${result.error}") {
                    result.error shouldBe null
                }

                game.resolveStack()

                val bowEntity = game.state.getEntity(bowId)!!
                val attachedTo = bowEntity.get<AttachedToComponent>()
                withClue("Bow should be attached to Jeskai Student") {
                    attachedTo shouldNotBe null
                    attachedTo!!.targetId shouldBe studentId
                }

                val heroEntity = game.state.getEntity(heroId)!!
                withClue("Devoted Hero should no longer have attachments") {
                    heroEntity.get<AttachmentsComponent>() shouldBe null
                }

                val studentEntity = game.state.getEntity(studentId)!!
                val studentAttachments = studentEntity.get<AttachmentsComponent>()
                withClue("Jeskai Student should have bow in attachments") {
                    studentAttachments shouldNotBe null
                    studentAttachments!!.attachedIds shouldBe listOf(bowId)
                }
            }
        }

        context("Heart-Piercer Bow attack trigger") {

            test("deals 1 damage to target opponent creature when equipped creature attacks") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Heart-Piercer Bow")
                    .withCardOnBattlefield(1, "Devoted Hero", summoningSickness = false)
                    .withCardOnBattlefield(2, "Alpine Grizzly") // 4/2 opponent creature
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bowId = game.findPermanent("Heart-Piercer Bow")!!
                val heroId = game.findPermanent("Devoted Hero")!!
                val grizzlyId = game.findPermanent("Alpine Grizzly")!!

                // Equip the bow to Devoted Hero
                val cardDef = cardRegistry.getCard("Heart-Piercer Bow")!!
                val equipAbility = cardDef.script.activatedAbilities.first()

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = bowId,
                        abilityId = equipAbility.id,
                        targets = listOf(ChosenTarget.Permanent(heroId))
                    )
                )
                game.resolveStack()

                // Move to combat and declare Devoted Hero as attacker
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Devoted Hero" to 2))

                // The trigger fires — pending decision for target selection
                withClue("Should have pending decision for trigger target") {
                    game.state.pendingDecision shouldNotBe null
                }

                // Select Alpine Grizzly as the target
                game.selectTargets(listOf(grizzlyId))

                // Resolve the triggered ability
                game.resolveStack()

                // Alpine Grizzly should have taken 1 damage from the bow
                val grizzlyEntity = game.state.getEntity(grizzlyId)!!
                val damageComponent = grizzlyEntity.get<DamageComponent>()
                withClue("Alpine Grizzly should have 1 damage marked") {
                    damageComponent shouldNotBe null
                    damageComponent!!.amount shouldBe 1
                }
            }

            test("kills a 1-toughness creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Heart-Piercer Bow")
                    .withCardOnBattlefield(1, "Glory Seeker", summoningSickness = false) // 2/2
                    .withCardOnBattlefield(2, "Raging Goblin") // 1/1 opponent creature
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bowId = game.findPermanent("Heart-Piercer Bow")!!
                val seekerId = game.findPermanent("Glory Seeker")!!
                val goblinId = game.findPermanent("Raging Goblin")!!

                val cardDef = cardRegistry.getCard("Heart-Piercer Bow")!!
                val equipAbility = cardDef.script.activatedAbilities.first()

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = bowId,
                        abilityId = equipAbility.id,
                        targets = listOf(ChosenTarget.Permanent(seekerId))
                    )
                )
                game.resolveStack()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Glory Seeker" to 2))

                // Select Raging Goblin as target for the trigger
                game.selectTargets(listOf(goblinId))
                game.resolveStack()

                // Raging Goblin should be dead (1 damage to a 1/1)
                withClue("Raging Goblin should be destroyed by 1 damage") {
                    game.isOnBattlefield("Raging Goblin") shouldBe false
                }
            }
        }
    }
}
