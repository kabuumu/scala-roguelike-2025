package map

import org.scalatest.funsuite.AnyFunSuite

class DungeonFreezeReproductionTest extends AnyFunSuite {

  test(
    "DungeonGenerator should fail gracefully or throw quickly when bounds are too small for explicit size"
  ) {
    // Small bounds: 2x2 rooms (Area = 4)
    val bounds = MapBounds(0, 1, 0, 1)

    // Requesting 10 rooms (impossible in 4 room space)
    val config = DungeonConfig(
      bounds = bounds,
      explicitSize = Some(10)
    )

    // This should ideally throw an IllegalStateException quickly, not freeze
    // If it freezes/takes too long, this test will time out or fail manually
    val t0 = System.nanoTime()
    try {
      DungeonGenerator.generateDungeon(config)
      fail("Should have thrown IllegalStateException")
    } catch {
      case e: IllegalStateException =>
        val t1 = System.nanoTime()
        val durationMs = (t1 - t0) / 1e6
        println(s"Failed gracefully in ${durationMs}ms")
      // Pass
    }
  }

  test("Reproduce specific scenario from GlobalFeaturePlanner") {
    // Scenario: RegionSize = 48 tiles.
    // DungeonWidth randomly chosen as e.g. 15 tiles.
    // Bounds converted to Rooms: 15/10 = 1 room width?
    // Let's test 1x1 room bounds with size 5

    val smallBounds =
      MapBounds(0, 0, 0, 0) // 1x1 room (start at 0, end at 0 inclusive)
    val config = DungeonConfig(
      bounds = smallBounds,
      explicitSize = Some(5)
    )

    try {
      DungeonGenerator.generateDungeon(config)
      fail("Should have thrown IllegalStateException for 1x1 bounds")
    } catch {
      case e: IllegalStateException =>
      // Expected
    }
  }
}
