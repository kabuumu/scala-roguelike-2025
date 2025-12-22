package map

import org.scalatest.funsuite.AnyFunSuite

class WorldMapDeterminismTest extends AnyFunSuite {

  test("DungeonGenerator is deterministic with same seed") {
    // Run multiple times to increase chance of catching non-determinism
    for (i <- 1 to 2) {
      val seed = 12345L + i
      // Use reasonable bounds (8x8) to ensure quick execution
      val bounds = MapBounds(0, 8, 0, 8)
      val config = DungeonConfig(bounds, seed)

      val run1 = DungeonGenerator.generateDungeon(config)
      val run2 = DungeonGenerator.generateDungeon(config)

      assert(
        run1.tiles.keySet == run2.tiles.keySet,
        s"Tiles should be identical for seed $seed iteration $i"
      )
      assert(
        run1.walls == run2.walls,
        s"Walls should be identical for seed $seed iteration $i"
      )
      assert(
        run1.startPoint == run2.startPoint,
        s"Start point should be identical for seed $seed iteration $i"
      )
    }
  }

  ignore("World generation is deterministic") {
    for (i <- 1 to 2) {
      val seed = 98765L + i
      // Use reasonable bounds (20x20)
      val bounds = MapBounds(-10, 10, -10, 10)
      val worldConfig = WorldConfig(bounds, seed)

      // Just test functionality that uses random heavily
      val mutator =
        new DungeonPlacementMutator(seed = seed, exclusionRadius = 2)

      val initialWorld = WorldMap(
        tiles = Map.empty,
        dungeons = Seq.empty,
        paths = Set.empty,
        bridges = Set.empty,
        bounds = bounds
      )

      val run1 = mutator.mutateWorld(initialWorld)
      val run2 = mutator.mutateWorld(initialWorld)

      assert(
        run1.dungeons.size == run2.dungeons.size,
        s"Dungeon count differ for seed $seed"
      )

      run1.dungeons.zip(run2.dungeons).foreach { case (d1, d2) =>
        assert(
          d1.tiles.keySet == d2.tiles.keySet,
          s"Dungeon tiles differ for seed $seed"
        )
      }
    }
  }
}
