package game.combat

import org.scalatest.funsuite.AnyFunSuite
import game.StartingState
import data.Enemies
import game.entity.Health.*

class CombatBalanceTest extends AnyFunSuite {

  test("Level 1 player combat balance report and survival metrics") {
    val seed = 100L
    val state = StartingState.startAdventure(seed)
    val player = state.playerEntity

    val dummyState = state

    val slimelet = Enemies.slimelet("test-slimelet", game.Point(0, 0))
    val rat = Enemies.rat("test-rat", game.Point(0, 0))
    val snake = Enemies.snake("test-snake", game.Point(0, 0), "spit-1")
    val slime = Enemies.slime("test-slime", game.Point(0, 0))

    val slimeletMetrics = CombatBalanceSimulator.simulate1v1(player, slimelet, dummyState)
    val ratMetrics = CombatBalanceSimulator.simulate1v1(player, rat, dummyState)
    val snakeMetrics = CombatBalanceSimulator.simulate1v1(player, snake, dummyState)
    val slimeMetrics = CombatBalanceSimulator.simulate1v1(player, slime, dummyState)

    println("=== Level 1 Combat Balance Report ===")
    println(s"Player starting HP: ${player.currentHealth}")
    println(s"Vs Slimelet : Damage taken: ${slimeletMetrics.playerDamageTaken} HP | Kills before dying: ${slimeletMetrics.maxSequentialKills}")
    println(s"Vs Rat      : Damage taken: ${ratMetrics.playerDamageTaken} HP | Kills before dying: ${ratMetrics.maxSequentialKills}")
    println(s"Vs Snake    : Damage taken: ${snakeMetrics.playerDamageTaken} HP | Kills before dying: ${snakeMetrics.maxSequentialKills}")
    println(s"Vs Slime    : Damage taken: ${slimeMetrics.playerDamageTaken} HP | Kills before dying: ${slimeMetrics.maxSequentialKills}")

    // Assertions for Level 1 player balance
    assert(slimeletMetrics.survives1v1, "Level 1 player must survive 1v1 against Slimelet")
    assert(ratMetrics.survives1v1, "Level 1 player must survive 1v1 against Rat")
    assert(snakeMetrics.survives1v1, "Level 1 player must survive 1v1 against Snake")
    assert(slimeMetrics.survives1v1, "Level 1 player must survive 1v1 against Slime")

    // A single rat must NOT deal massive damage (> 25 HP) to a level 1 player
    assert(ratMetrics.playerDamageTaken <= 15, s"Rat damage to level 1 player must be <= 15 HP (was ${ratMetrics.playerDamageTaken} HP)")

    // A level 1 player should be able to kill at least 4 rats in sequence without dying
    assert(ratMetrics.maxSequentialKills >= 4, s"Level 1 player should defeat at least 4 rats in sequence (was ${ratMetrics.maxSequentialKills})")
  }
}
