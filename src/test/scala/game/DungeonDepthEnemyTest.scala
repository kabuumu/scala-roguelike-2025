package game

import game.entity.EntityType
import game.entity.Health.currentHealth
import map.{Dungeon, MapGenerator}
import org.scalatest.funsuite.AnyFunSuite
import data.Enemies

class DungeonDepthEnemyTest extends AnyFunSuite {
  
  test("Dungeon depth calculation works correctly") {
    val dungeon = MapGenerator.generateDungeon(dungeonSize = 5, lockedDoorCount = 0, itemCount = 0)
    val depths = dungeon.roomDepths
    
    // Start point should have depth 0
    assert(depths(dungeon.startPoint) == 0)
    
    // All rooms should have a depth assigned
    assert(dungeon.roomGrid.forall(room => depths.contains(room)))
    
    // Depths should be reasonable (non-negative and not too large)
    assert(depths.values.forall(depth => depth >= 0 && depth < dungeon.roomGrid.size))
    
    println(s"Room depths: ${depths.toSeq.sortBy(_._2)}")
  }
  
  test("Enemy generation follows depth progression") {
    val startingState = StartingState
    val enemies = startingState.enemies
    val dungeon = startingState.dungeon
    
    // Should have enemies generated
    assert(enemies.nonEmpty)
    
    // All enemies should be in non-starting rooms
    val enemyPositions = enemies.map(_.get[game.entity.Movement].map(_.position)).flatten
    val startCenter = game.Point(
      dungeon.startPoint.x * map.Dungeon.roomSize + map.Dungeon.roomSize / 2,
      dungeon.startPoint.y * map.Dungeon.roomSize + map.Dungeon.roomSize / 2
    )
    
    assert(!enemyPositions.contains(startCenter), "No enemies should be in starting room")
    
    // Verify enemy types exist
    val enemyTypes = enemies.map(_.id).map(_.split("-")(0)).toSet
    assert(enemyTypes.contains("Slimelet"), "Should have slimelets for early depths")
    
    println(s"Generated ${enemies.size} enemies of types: ${enemyTypes.mkString(", ")}")
  }
  
  test("Enemy difficulty system has correct values") {
    import Enemies.EnemyDifficulty._
    import Enemies.EnemyReference._
    
    assert(difficultyFor(Slimelet) == 1)
    assert(difficultyFor(Slime) == 2)
    assert(difficultyFor(Rat) == 3)
    assert(difficultyFor(Snake) == 4)
    
    // Verify progression
    assert(difficultyFor(Slimelet) < difficultyFor(Slime))
    assert(difficultyFor(Slime) < difficultyFor(Rat))
    assert(difficultyFor(Rat) < difficultyFor(Snake))
  }
  
  test("Depth-based enemy groups follow progression rules") {
    import StartingState.EnemyGeneration._
    
    // Test specific depth examples from requirements
    val depth1 = enemiesForDepth(1)
    assert(depth1.enemies.length == 1)
    assert(depth1.enemies.head == Enemies.EnemyReference.Slimelet)
    
    val depth2 = enemiesForDepth(2)
    assert(depth2.enemies.length == 2)
    assert(depth2.enemies.forall(_ == Enemies.EnemyReference.Slimelet))
    
    val depth3 = enemiesForDepth(3)
    assert(depth3.enemies.length == 1)
    assert(depth3.enemies.head == Enemies.EnemyReference.Slime)
    
    val depth4 = enemiesForDepth(4)
    assert(depth4.enemies.length == 2)
    assert(depth4.enemies.contains(Enemies.EnemyReference.Slime))
    assert(depth4.enemies.contains(Enemies.EnemyReference.Slimelet))
    
    val depth5 = enemiesForDepth(5)
    assert(depth5.enemies.length == 1)
    assert(depth5.enemies.head == Enemies.EnemyReference.Rat)
    
    val depth6 = enemiesForDepth(6)
    assert(depth6.enemies.length == 1)
    assert(depth6.enemies.head == Enemies.EnemyReference.Snake)
    
    println("Depth progression test passed")
  }
  
  test("Slimelet can be created as standalone enemy") {
    val position = game.Point(50, 50)
    val slimelet = Enemies.slimelet("test-slimelet", position)
    
    // Verify it's an enemy
    assert(slimelet.get[game.entity.EntityTypeComponent].exists(_.entityType == EntityType.Enemy))
    
    // Verify position
    assert(slimelet.get[game.entity.Movement].exists(_.position == position))
    
    // Verify it has appropriate stats (lower than slime)
    val health = slimelet.currentHealth
    val initiative = slimelet.get[game.entity.Initiative].map(_.maxInitiative).getOrElse(0)
    
    assert(health == 10, s"Slimelet health should be 10, got $health")
    assert(initiative == 8, s"Slimelet initiative should be 8, got $initiative")
    
    // Should have no weapon (uses default 1 damage)
    assert(slimelet.get[game.entity.Equipment].forall(_.weapon.isEmpty))
    
    println("Slimelet creation test passed")
  }
}