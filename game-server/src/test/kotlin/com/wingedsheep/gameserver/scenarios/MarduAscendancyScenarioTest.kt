package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Mardu Ascendancy.
 *
 * Card reference:
 * - Mardu Ascendancy ({R}{W}{B}): Enchantment
 *   Whenever a nontoken creature you control attacks, create a 1/1 red Goblin
 *   creature token that's tapped and attacking.
 *   Sacrifice Mardu Ascendancy: Creatures you control get +0/+3 until end of turn.
 */
class MarduAscendancyScenarioTest : ScenarioTestBase() {

    init {
        context("Mardu Ascendancy - token creation on attack") {

            test("creates a tapped and attacking Goblin token when a nontoken creature attacks") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Mardu Ascendancy")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Move to combat and declare attackers
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Hill Giant" to 2))

                // Resolve the triggered ability (creates Goblin token)
                game.resolveStack()

                // Verify a Goblin token was created
                val goblins = game.findAllPermanents("Goblin Token")
                goblins.size shouldBe 1

                val goblinId = goblins.first()
                val goblinEntity = game.state.getEntity(goblinId)!!

                // Token should be tapped
                goblinEntity.has<TappedComponent>() shouldBe true

                // Token should be attacking
                goblinEntity.has<AttackingComponent>() shouldBe true

                // Token should be a token
                goblinEntity.has<TokenComponent>() shouldBe true
            }

            test("creates one token per nontoken attacker") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Mardu Ascendancy")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Hill Giant" to 2, "Grizzly Bears" to 2))

                // Resolve both triggered abilities
                game.resolveStack()

                // Two Goblin tokens should be created
                val goblins = game.findAllPermanents("Goblin Token")
                goblins.size shouldBe 2
            }
        }

        context("Mardu Ascendancy - sacrifice ability") {

            test("sacrifice gives creatures +0/+3 until end of turn") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Mardu Ascendancy")
                    .withCardOnBattlefield(1, "Hill Giant") // 3/3
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Activate sacrifice ability
                val ascendancyId = game.findPermanent("Mardu Ascendancy")!!
                val cardDef = cardRegistry.getCard("Mardu Ascendancy")!!
                val ability = cardDef.script.activatedAbilities.first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = ascendancyId,
                        abilityId = ability.id
                    )
                )
                result.error shouldBe null

                // Resolve the ability
                game.resolveStack()

                // Mardu Ascendancy should be in graveyard
                game.isOnBattlefield("Mardu Ascendancy") shouldBe false
                game.isInGraveyard(1, "Mardu Ascendancy") shouldBe true

                // Hill Giant should be 3/6 (3/3 base + 0/+3)
                val hillGiantId = game.findPermanent("Hill Giant")!!
                val projected = game.state.projectedState
                projected.getPower(hillGiantId) shouldBe 3
                projected.getToughness(hillGiantId) shouldBe 6
            }
        }
    }
}
