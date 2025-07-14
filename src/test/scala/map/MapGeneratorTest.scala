package map

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class MapGeneratorTest extends AnyFunSuite with Matchers {
    for {
      roomSize <- Seq(10, 20)
      lockedDoors <- 0 to 2
      itemCount <- 0 to 2
    } test(s"generateMap creates a simple dungeon for roomSize=$roomSize, lockedDoors=$lockedDoors, itemCount=$itemCount") {
      val dungeon = MapGenerator.generateDungeon(roomSize, lockedDoors, itemCount)

      dungeon.roomGrid.size shouldBe roomSize
      dungeon.lockedDoors.size shouldBe lockedDoors
      dungeon.nonKeyItems.size shouldBe itemCount
    }

//  val testNumber = 20
//  test(s"generating $testNumber dungeons to test average time") {
//
//    val averageTime = (1 to testNumber).map { _ =>
//      val startTime = System.currentTimeMillis()
//      val dungeon = MapGenerator.generateDungeon(dungeonSize = 30, lockedDoorCount = 4, itemCount = 10)
//      val timeTaken = System.currentTimeMillis() - startTime
//
//      timeTaken
//    }.sum / testNumber
//
//    println(s"Average time to generate a dungeon: $averageTime ms")
//  }
}
