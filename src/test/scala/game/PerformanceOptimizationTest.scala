package game

import org.scalatest.funsuite.AnyFunSuite
import game.entity._
import game.system._
import game.quest._

class PerformanceOptimizationTest extends AnyFunSuite {

  test("SightMemory does not re-allocate Set when no new points are visible") {
    val initialPoints = Set(Point(0, 0), Point(1, 0), Point(0, 1), Point(1, 1))
    val memory = SightMemory(initialPoints)

    val player = Entity("player", Movement(Point(0, 0)), memory)
    val state = GameState("player", Vector(player), worldMap = map.WorldMap(Map.empty, Seq.empty, None, Seq.empty, Set.empty, Set.empty, map.MapBounds(0, 10, 0, 10)))

    val updatedMemory = memory.update(state, player)

    assert(updatedMemory eq memory, "SightMemory must return identical instance when all visible points are already seen")
  }

  test("Active entities filtering reduces iteration scope in GameState") {
    val player = Entity("player", Movement(Point(0, 0)), Active())
    val activeMonster = Entity("m1", Movement(Point(1, 1)), Active(), EntityTypeComponent(EntityType.Enemy))
    val inactiveMonster = Entity("m2", Movement(Point(100, 100)), EntityTypeComponent(EntityType.Enemy))

    val state = GameState("player", Vector(player, activeMonster, inactiveMonster), worldMap = map.WorldMap(Map.empty, Seq.empty, None, Seq.empty, Set.empty, Set.empty, map.MapBounds(0, 100, 0, 100)))

    assert(state.activeEntities.length == 2, s"activeEntities must only contain active entities (found ${state.activeEntities.length})")
    assert(state.activeEntities.contains(player))
    assert(state.activeEntities.contains(activeMonster))
    assert(!state.activeEntities.contains(inactiveMonster))
  }

  test("Game tick execution time is fast with 500+ entities across explored map") {
    val seed = 100L
    val state = StartingState.startAdventure(seed)

    // Simulate extensive exploration by populating sightMemory with 10,000 points
    val exploredPoints = (for {
      x <- -50 to 50
      y <- -50 to 50
    } yield Point(x, y)).toSet

    val playerWithMemory = state.playerEntity.addComponent(SightMemory(exploredPoints))
    val stateWithExploredMap = state.updateEntity(state.playerEntityId, playerWithMemory)

    // Warmup tick
    val warmedState = stateWithExploredMap.updateWithSystems(Nil)

    // Measure tick duration over 50 ticks
    val startTime = System.nanoTime()
    var currentState = warmedState
    for (_ <- 1 to 50) {
      currentState = currentState.updateWithSystems(Nil)
    }
    val elapsedMs = (System.nanoTime() - startTime) / 1000000.0
    val avgTickMs = elapsedMs / 50.0

    println(s"Performance Benchmark: 50 ticks executed in ${elapsedMs} ms (avg ${avgTickMs} ms / tick)")
    assert(avgTickMs < 50.0, s"Average tick duration must be reasonable under 50ms (was ${avgTickMs} ms)")
  }
}
