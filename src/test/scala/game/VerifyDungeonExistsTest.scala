package game

import org.scalatest.funsuite.AnyFunSuite

class VerifyDungeonExistsTest extends AnyFunSuite {
  test("Verify dungeon exists in StartingState") {
    val state = StartingState.startingGameState
    val worldMap = state.worldMap
    
    println(s"\n=== WorldMap Structure ===")
    println(s"Total tiles: ${worldMap.tiles.size}")
    println(s"Dungeons count: ${worldMap.dungeons.size}")
    println(s"Primary dungeon: ${worldMap.primaryDungeon.map(_ => "EXISTS").getOrElse("NONE")}")
    
    // This should NOT be empty
    assert(worldMap.dungeons.nonEmpty, "WorldMap should have at least one dungeon")
    assert(worldMap.primaryDungeon.isDefined, "Primary dungeon should be defined")
    
    val dungeon = worldMap.primaryDungeon.get
    println(s"\nDungeon details:")
    println(s"  Rooms: ${dungeon.roomGrid.size}")
    println(s"  Start point: ${dungeon.startPoint}")
    println(s"  Has trader: ${dungeon.traderRoom.isDefined}")
    println(s"  Has boss: ${dungeon.hasBossRoom}")
    
    assert(dungeon.roomGrid.nonEmpty, "Dungeon should have rooms")
  }
}
