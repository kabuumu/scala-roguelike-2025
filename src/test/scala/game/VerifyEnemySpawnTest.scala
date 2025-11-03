package game

import game.entity.{EntityTypeComponent, EntityType, Movement}
import org.scalatest.funsuite.AnyFunSuite

class VerifyEnemySpawnTest extends AnyFunSuite {
  test("Enemies spawn on walkable tiles only") {
    val state = StartingState.startingGameState
    val worldMap = state.worldMap
    
    // Get all enemy entities
    val enemies = state.entities.filter(entity => 
      entity.get[EntityTypeComponent].exists(_.entityType == EntityType.Enemy)
    )
    
    println(s"\n=== Enemy Spawn Verification ===")
    println(s"Total enemies: ${enemies.size}")
    
    // Check each enemy's position
    val nonWalkableEnemies = enemies.filter { enemy =>
      enemy.get[Movement].exists { movement =>
        val pos = movement.position
        // Check if position is on a non-walkable tile (wall, rock, or water)
        worldMap.walls.contains(pos) || worldMap.rocks.contains(pos) || worldMap.water.contains(pos)
      }
    }
    
    if (nonWalkableEnemies.nonEmpty) {
      println(s"\nERROR: Found ${nonWalkableEnemies.size} enemies on non-walkable tiles:")
      nonWalkableEnemies.foreach { enemy =>
        val pos = enemy.get[Movement].map(_.position).getOrElse(Point(0, 0))
        val tileType = worldMap.tiles.get(pos)
        println(s"  Enemy ${enemy.id} at $pos on tile type: $tileType")
        
        // Determine what makes it non-walkable
        val issues = Seq(
          if (worldMap.walls.contains(pos)) Some("wall") else None,
          if (worldMap.rocks.contains(pos)) Some("rock") else None,
          if (worldMap.water.contains(pos)) Some("water") else None
        ).flatten
        println(s"    Non-walkable because of: ${issues.mkString(", ")}")
      }
    } else {
      println(s"✓ All ${enemies.size} enemies are on walkable tiles")
    }
    
    assert(
      nonWalkableEnemies.isEmpty,
      s"All enemies should spawn on walkable tiles, but ${nonWalkableEnemies.size} enemies are on non-walkable tiles"
    )
  }
  
  test("Enemies have valid positions within dungeon") {
    val state = StartingState.startingGameState
    val worldMap = state.worldMap
    
    val enemies = state.entities.filter(entity => 
      entity.get[EntityTypeComponent].exists(_.entityType == EntityType.Enemy)
    )
    
    println(s"\n=== Enemy Position Validation ===")
    
    // Check that all enemy positions exist in the tile map
    val invalidPositions = enemies.filter { enemy =>
      enemy.get[Movement].exists { movement =>
        !worldMap.tiles.contains(movement.position)
      }
    }
    
    if (invalidPositions.nonEmpty) {
      println(s"ERROR: Found ${invalidPositions.size} enemies at invalid positions:")
      invalidPositions.foreach { enemy =>
        val pos = enemy.get[Movement].map(_.position).getOrElse(Point(0, 0))
        println(s"  Enemy ${enemy.id} at $pos (not in worldMap.tiles)")
      }
    } else {
      println(s"✓ All ${enemies.size} enemies have valid positions in the world map")
    }
    
    assert(
      invalidPositions.isEmpty,
      s"All enemies should have valid positions, but ${invalidPositions.size} enemies have invalid positions"
    )
  }
}
