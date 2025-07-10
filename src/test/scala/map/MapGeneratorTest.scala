package map

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class MapGeneratorTest extends AnyFunSuite with Matchers {
  for {
    roomSize <- Seq(10)
    lockedDoors <- Seq(0, 1)
    itemCount <- Seq(0, 1)
  } test(s"generateMap creates a simple dungeon for roomSize=$roomSize, lockedDoors=$lockedDoors, itemCount=$itemCount") {
    val dungeon = MapGenerator.generateDungeon(roomSize, lockedDoors, itemCount)
    
    dungeon.roomGrid.size shouldBe roomSize
    dungeon.lockedDoors.size shouldBe lockedDoors
    dungeon.nonKeyItems.size shouldBe itemCount
  }
}
