package map

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class MapGeneratorTest extends AnyFunSuite with Matchers {
  for {
//    roomSize <- 5 to 10
    lockedDoors <- 0 to 3
    itemCount <- 0 to 3
  } test(s"generateMap creates a simple dungeon for roomSize=10, lockedDoors=$lockedDoors, itemCount=$itemCount") {
    val roomSize = 10
    val dungeon = MapGenerator.generateDungeon(roomSize, lockedDoors, itemCount)
    
    dungeon.roomGrid.size shouldBe roomSize
    dungeon.lockedDoors.size shouldBe lockedDoors
    dungeon.nonKeyItems.size shouldBe itemCount
  }
}
