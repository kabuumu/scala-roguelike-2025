package game

object TestDungeonExists extends App {
  println("Testing dungeon generation in StartingState...")
  val state = StartingState.startingGameState
  val worldMap = state.worldMap
  
  println(s"World map has ${worldMap.tiles.size} total tiles")
  println(s"World map has ${worldMap.dungeons.size} dungeons")
  
  if (worldMap.dungeons.isEmpty) {
    println("ERROR: No dungeons found in world map!")
  } else {
    worldMap.dungeons.zipWithIndex.foreach { case (dungeon, idx) =>
      println(s"\nDungeon $idx:")
      println(s"  - Rooms: ${dungeon.roomGrid.size}")
      println(s"  - Start point: ${dungeon.startPoint}")
      println(s"  - Has trader: ${dungeon.traderRoom.isDefined}")
      println(s"  - Has boss: ${dungeon.hasBossRoom}")
      println(s"  - Locked doors: ${dungeon.lockedDoorCount}")
      println(s"  - Items: ${dungeon.nonKeyItems.size}")
    }
  }
  
  // Check primaryDungeon
  println(s"\nPrimary dungeon: ${worldMap.primaryDungeon.map(_ => "EXISTS").getOrElse("NOT SET")}")
  
  if (worldMap.primaryDungeon.isEmpty) {
    println("WARNING: primaryDungeon is not set even though dungeons exist!")
  }
}
