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

    // Test deterministic map generation with seeds
    test("generateDungeon should produce identical dungeons when given the same seed") {
      val seed = 12345L
      val dungeonSize = 10
      val lockedDoors = 1
      val itemCount = 2

      val dungeon1 = MapGenerator.generateDungeon(dungeonSize, lockedDoors, itemCount, seed)
      val dungeon2 = MapGenerator.generateDungeon(dungeonSize, lockedDoors, itemCount, seed)

      // Verify the dungeons have the same structure
      dungeon1.roomGrid shouldBe dungeon2.roomGrid
      dungeon1.roomConnections shouldBe dungeon2.roomConnections
      dungeon1.items shouldBe dungeon2.items
      dungeon1.lockedDoors shouldBe dungeon2.lockedDoors
      dungeon1.startPoint shouldBe dungeon2.startPoint
      dungeon1.endpoint shouldBe dungeon2.endpoint
      dungeon1.blockedRooms shouldBe dungeon2.blockedRooms

      // Verify tile generation is also deterministic
      dungeon1.tiles shouldBe dungeon2.tiles
      dungeon1.noise shouldBe dungeon2.noise
    }

    test("generateDungeon should produce different dungeons when given different seeds") {
      val seed1 = 12345L
      val seed2 = 54321L
      val dungeonSize = 15
      val lockedDoors = 1
      val itemCount = 1

      val dungeon1 = MapGenerator.generateDungeon(dungeonSize, lockedDoors, itemCount, seed1)
      val dungeon2 = MapGenerator.generateDungeon(dungeonSize, lockedDoors, itemCount, seed2)

      // While they should have the same basic requirements
      dungeon1.roomGrid.size shouldBe dungeonSize
      dungeon2.roomGrid.size shouldBe dungeonSize
      dungeon1.lockedDoors.size shouldBe lockedDoors
      dungeon2.lockedDoors.size shouldBe lockedDoors
      dungeon1.nonKeyItems.size shouldBe itemCount
      dungeon2.nonKeyItems.size shouldBe itemCount

      // They should have different noise maps (very unlikely to be identical with different seeds)
      dungeon1.noise should not equal dungeon2.noise
    }

    test("generateDungeon should be deterministic across multiple generations with the same seed") {
      val seed = 98765L
      val dungeonSize = 8
      val lockedDoors = 0
      val itemCount = 3

      val dungeons = (1 to 5).map(_ => MapGenerator.generateDungeon(dungeonSize, lockedDoors, itemCount, seed))

      // All dungeons should be identical
      dungeons.foreach { dungeon =>
        dungeon.roomGrid shouldBe dungeons.head.roomGrid
        dungeon.roomConnections shouldBe dungeons.head.roomConnections
        dungeon.items shouldBe dungeons.head.items
        dungeon.tiles shouldBe dungeons.head.tiles
        dungeon.noise shouldBe dungeons.head.noise
      }
    }

    test("noise generation should be deterministic with the same seed") {
      val seed = 11111L
      val minX = 0
      val maxX = 50
      val minY = 0
      val maxY = 50

      val noise1 = NoiseGenerator.getNoise(minX, maxX, minY, maxY, seed)
      val noise2 = NoiseGenerator.getNoise(minX, maxX, minY, maxY, seed)

      noise1 shouldBe noise2
    }

    test("noise generation should be different with different seeds") {
      val seed1 = 11111L
      val seed2 = 22222L
      val minX = 0
      val maxX = 30
      val minY = 0
      val maxY = 30

      val noise1 = NoiseGenerator.getNoise(minX, maxX, minY, maxY, seed1)
      val noise2 = NoiseGenerator.getNoise(minX, maxX, minY, maxY, seed2)

      noise1 should not equal noise2
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
