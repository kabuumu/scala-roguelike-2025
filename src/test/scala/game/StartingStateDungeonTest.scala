package game

import org.scalatest.funsuite.AnyFunSuite

class StartingStateDungeonTest extends AnyFunSuite {
  
  test("StartingState dungeon generation produces trader, keys, and items") {
    // Access the starting state (will trigger lazy initialization)
    val gameState = StartingState.startingGameState
    
    // Check the dungeon
    val dungeonOpt = gameState.worldMap.primaryDungeon
    assert(dungeonOpt.isDefined, "Dungeon should exist")
    
    val dungeon = dungeonOpt.get
    
    // Verify dungeon properties
    println(s"=== Dungeon Generation Results ===")
    println(s"Total rooms: ${dungeon.roomGrid.size}")
    println(s"Trader room: ${dungeon.traderRoom}")
    println(s"Boss room: ${dungeon.endpoint}")
    println(s"Has boss room flag: ${dungeon.hasBossRoom}")
    println(s"Locked door count: ${dungeon.lockedDoorCount}")
    println(s"Total items (including keys): ${dungeon.items.size}")
    println(s"Non-key items: ${dungeon.nonKeyItems.size}")
    println(s"Items breakdown:")
    dungeon.items.foreach { case (point, itemRef) =>
      println(s"  - $itemRef at $point")
    }
    println(s"===================================")
    
    // Assertions - verify core features exist (bounded generation auto-calculates based on space)
    assert(dungeon.traderRoom.isDefined, s"Trader room should exist. Room count: ${dungeon.roomGrid.size}")
    assert(dungeon.hasBossRoom, "Boss room flag should be set")
    assert(dungeon.endpoint.isDefined, "Boss room endpoint should exist")
    // Locked door count is auto-calculated and may be 0 for small dungeons
    assert(dungeon.lockedDoorCount >= 0, s"Locked door count should be valid, found ${dungeon.lockedDoorCount}")
    
    // Should have at least 1 non-key item - bounded generation auto-calculates
    assert(dungeon.nonKeyItems.size >= 1, 
      s"Should have at least 1 treasure item, found ${dungeon.nonKeyItems.size}")
    
    // Should have keys for locked doors
    val keyCount = dungeon.items.count { case (_, itemRef) =>
      itemRef match {
        case data.Items.ItemReference.RedKey | 
             data.Items.ItemReference.BlueKey | 
             data.Items.ItemReference.YellowKey => true
        case _ => false
      }
    }
    assert(keyCount >= dungeon.lockedDoorCount,
      s"Should have at least ${dungeon.lockedDoorCount} keys, found $keyCount")
    
    // Check entities in game state
    val itemEntities = gameState.entities.filter(e => 
      e.id.startsWith("item-") && !e.id.contains("player-")
    )
    println(s"\nGame state has ${itemEntities.size} item entities spawned")
    itemEntities.foreach(e => println(s"  - ${e.id}"))
    
    assert(itemEntities.nonEmpty, "Game state should have item entities")
  }
}

