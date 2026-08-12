package game.combat

import game.GameState
import game.entity._
import game.entity.Health.*
import game.entity.Initiative.*
import game.entity.Equipment.*
import game.system.event.GameSystemEvent
import data.Enemies
import data.Enemies.EnemyReference

case class EncounterMetrics(
    enemyName: String,
    playerDamagePerHit: Int,
    enemyDamagePerHit: Int,
    playerTurnsToKill: Int,
    enemyHitsReceived: Int,
    playerDamageTaken: Int,
    playerEndHp: Int,
    maxSequentialKills: Int,
    survives1v1: Boolean
)

object CombatBalanceSimulator {

  /** Simulates a 1v1 turn-based combat encounter between player and enemy.
    * Returns an EncounterMetrics breakdown of the fight.
    */
  def simulate1v1(
      player: Entity,
      enemy: Entity,
      dummyState: GameState = createDummyState()
  ): EncounterMetrics = {
    var p = player
    var e = enemy

    val pBaseDamage = 1
    val pDamageBreakdown = DamageCalculator.compute(pBaseDamage, p, e, dummyState, GameSystemEvent.DamageSource.Melee)
    val pDamagePerHit = pDamageBreakdown.finalDamage

    val eBaseDamage = 1
    val eDamageBreakdown = DamageCalculator.compute(eBaseDamage, e, p, dummyState, GameSystemEvent.DamageSource.Melee)
    val eDamagePerHit = eDamageBreakdown.finalDamage

    var playerAttacksLanded = 0
    var enemyAttacksLanded = 0
    var playerDamageTaken = 0

    val initialPlayerHp = p.currentHealth

    // Step-by-step initiative simulation
    var step = 0
    val maxSteps = 1000

    while (p.isAlive && e.isAlive && step < maxSteps) {
      step += 1

      // Decrease initiative
      p = p.decreaseInitiative(1)
      e = e.decreaseInitiative(1)

      if (p.isReady && p.isAlive && e.isAlive) {
        playerAttacksLanded += 1
        e = e.damage(pDamagePerHit, p.id)
        p = p.resetInitiative()
      }

      if (e.isReady && e.isAlive && p.isAlive) {
        enemyAttacksLanded += 1
        playerDamageTaken += eDamagePerHit
        p = p.damage(eDamagePerHit, e.id)
        e = e.resetInitiative()
      }
    }

    val maxSequentialKills = if (playerDamageTaken > 0) initialPlayerHp / playerDamageTaken else 999
    val enemyNameStr = enemy.get[NameComponent].map(_.name).getOrElse(enemy.id)

    EncounterMetrics(
      enemyName = enemyNameStr,
      playerDamagePerHit = pDamagePerHit,
      enemyDamagePerHit = eDamagePerHit,
      playerTurnsToKill = playerAttacksLanded,
      enemyHitsReceived = enemyAttacksLanded,
      playerDamageTaken = playerDamageTaken,
      playerEndHp = math.max(0, p.currentHealth),
      maxSequentialKills = maxSequentialKills,
      survives1v1 = p.isAlive
    )
  }

  /** Simulates N consecutive 1v1 battles against identical enemies until player dies or limit is reached.
    */
  def simulateSequence(
      player: Entity,
      enemyFactory: (String, game.Point) => Entity,
      maxBattles: Int = 20
  ): Int = {
    var p = player
    var kills = 0
    val dummyState = createDummyState()

    while (p.isAlive && kills < maxBattles) {
      val enemy = enemyFactory(s"enemy-$kills", game.Point(0, 0))
      val metrics = simulate1v1(p, enemy, dummyState)
      if (metrics.survives1v1) {
        kills += 1
        p = p.update[Health](h => h.copy(baseCurrent = metrics.playerEndHp))
      } else {
        return kills
      }
    }
    kills
  }

  private def createDummyState(): GameState = {
    GameState(
      playerEntityId = "player",
      entities = Vector.empty,
      worldMap = map.WorldMap(Map.empty, Seq.empty, None, Seq.empty, Set.empty, Set.empty, map.MapBounds(0, 10, 0, 10))
    )
  }
}
