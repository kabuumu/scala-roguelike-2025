# Town-to-Dungeon Road Generation & Forest Clearing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ensure clear, walkable roads connect all settlements and dungeons across the world map without any impassable forest (`TileType.Tree`) blocking paths or chunk reloads.

**Architecture:** Update `ChunkManager.scala` to merge `worldMap.paths` and `worldMap.bridges` into newly generated chunk overlays and forbid tree generation on road points; update `WorldMutator.scala` (`PathGenerationMutator`) to generate direct interconnected paths between all settlements and dungeons.

**Tech Stack:** Scala 3.6.4, Scala.js, Indigo Engine, ScalaTest.

---

### Task 1: Prevent ChunkManager from Overwriting Paths with Trees

**Files:**
- Modify: `src/main/scala/map/ChunkManager.scala:129-147,434-447`
- Test: `src/test/scala/map/ForestRoadClearingTest.scala`

- [ ] **Step 1: Write failing unit test for ChunkManager road preservation**

Create `src/test/scala/map/ForestRoadClearingTest.scala`:
```scala
package map

import org.scalatest.funsuite.AnyFunSuite
import game.Point

class ForestRoadClearingTest extends AnyFunSuite {

  test("ChunkManager preserves paths and bridges in forest chunks without spawning trees on roads") {
    val seed = 42L
    val bounds = MapBounds(-10, 10, -10, 10)
    
    // Create road points inside forest
    val pathPoints = (0 to 10).map(y => Point(0, y)).toSet
    val bridgePoints = Set(Point(0, 5))

    val baseWorldMap = WorldMap(
      tiles = Map.empty,
      dungeons = Seq.empty,
      villages = Seq.empty,
      paths = pathPoints,
      bridges = bridgePoints,
      bounds = bounds,
      seed = seed
    )

    val worldConfig = WorldConfig(bounds = bounds, seed = seed)
    val worldMapWithChunks = ChunkManager.updateChunks(
      Point(0, 0),
      baseWorldMap,
      worldConfig,
      seed
    )

    // Verify no path or bridge point is rendered as a Tree or Wall
    pathPoints.foreach { pt =>
      val tile = worldMapWithChunks.getTile(pt)
      assert(tile.nonEmpty, s"Tile at $pt should be present")
      assert(tile.get != TileType.Tree, s"Path point $pt must not be rendered as a Tree")
      assert(tile.get != TileType.Wall && tile.get != TileType.Rock, s"Path point $pt must be walkable")
    }

    // Verify bridge point is Bridge
    assert(worldMapWithChunks.getTile(Point(0, 5)).contains(TileType.Bridge), "Bridge point must be Bridge")
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `sbt "testOnly map.ForestRoadClearingTest"`
Expected: FAIL with "Path point 0:0 must not be rendered as a Tree" or similar.

- [ ] **Step 3: Update ChunkManager.scala to preserve paths and bridges**

Modify `src/main/scala/map/ChunkManager.scala`:
In `freshChunksOverlay`:
```scala
    val pathTiles = worldMap.paths.map(_ -> TileType.Dirt).toMap ++
      worldMap.bridges.map(_ -> TileType.Bridge).toMap

    val structureTiles = worldMap.dungeons.flatMap(_.tiles).toMap ++
      worldMap.villages.flatMap(_.tiles).toMap ++
      pathTiles
```
In `generateTilesForOverworldType`:
```scala
        case OverworldTileType.Forest =>
          if (worldMap.bridges.contains(point)) {
            TileType.Bridge
          } else if (worldMap.paths.contains(point)) {
            TileType.Dirt
          } else {
            val baseTile =
              if (standardTile == TileType.Water) TileType.Grass1
              else standardTile

            val rnd = new scala.util.Random(seed ^ (x * 39119L) ^ (y * 65327L))
            baseTile match {
              case TileType.Grass1 | TileType.Grass2 | TileType.Grass3 =>
                if (rnd.nextDouble() < 0.5) TileType.Tree else baseTile
              case TileType.Dirt =>
                TileType.Dirt
              case _ =>
                TileType.Tree
            }
          }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `sbt "testOnly map.ForestRoadClearingTest"`
Expected: PASS

- [ ] **Step 5: Commit changes**

```bash
git add src/main/scala/map/ChunkManager.scala src/test/scala/map/ForestRoadClearingTest.scala
git commit -m "fix: preserve road and bridge tiles in ChunkManager and clear forest trees on paths"
```

---

### Task 2: Interconnect All Towns and Dungeons in WorldMutator

**Files:**
- Modify: `src/main/scala/map/WorldMutator.scala:463-549`
- Test: `src/test/scala/map/TownDungeonRoadTest.scala`

- [ ] **Step 1: Write unit test for town-to-dungeon connectivity**

Create `src/test/scala/map/TownDungeonRoadTest.scala`:
```scala
package map

import org.scalatest.funsuite.AnyFunSuite
import game.{Point, StartingState}
import game.entity.Movement.position

class TownDungeonRoadTest extends AnyFunSuite {

  test("Towns and dungeons have direct walkable road paths connecting them") {
    val seed = 100L
    val state = StartingState.startAdventure(seed)
    val worldMap = state.worldMap

    assert(worldMap.dungeons.nonEmpty, "Adventure mode must have dungeons")

    val dungeonApproach = Dungeon.getApproachTile(worldMap.dungeons.head.startPoint, worldMap.dungeons.head.entranceSide)
    val playerPos = state.playerEntity.position

    // Verify player spawn village to dungeon approach path consists of non-blocking walkable tiles
    val pathExists = isWalkablePath(playerPos, dungeonApproach, worldMap)
    assert(pathExists, s"Walkable road path must exist between player spawn $playerPos and dungeon approach $dungeonApproach")
  }

  private def isWalkablePath(start: Point, target: Point, worldMap: WorldMap): Boolean = {
    import scala.collection.mutable

    val minX = math.min(start.x, target.x) - 100
    val maxX = math.max(start.x, target.x) + 100
    val minY = math.min(start.y, target.y) - 100
    val maxY = math.max(start.y, target.y) + 100

    def isWalkable(p: Point): Boolean = {
      worldMap.getTile(p) match {
        case Some(TileType.Wall) | Some(TileType.Rock) | Some(TileType.Tree) => false
        case Some(TileType.Water) => worldMap.bridges.contains(p)
        case _ => true
      }
    }

    val queue = mutable.Queue[Point](start)
    val visited = mutable.Set[Point](start)

    while (queue.nonEmpty && visited.size < 50000) {
      val curr = queue.dequeue()
      if (curr == target) return true

      val neighbors = Seq(
        Point(curr.x + 1, curr.y),
        Point(curr.x - 1, curr.y),
        Point(curr.x, curr.y + 1),
        Point(curr.x, curr.y - 1)
      )

      neighbors.foreach { n =>
        if (n.x >= minX && n.x <= maxX && n.y >= minY && n.y <= maxY) {
          if (!visited.contains(n) && isWalkable(n)) {
            visited += n
            queue.enqueue(n)
          }
        }
      }
    }

    false
  }
}
```

- [ ] **Step 2: Run test to verify it passes**

Run: `sbt "testOnly map.TownDungeonRoadTest"`
Expected: PASS

- [ ] **Step 3: Update PathGenerationMutator in WorldMutator.scala to connect towns to dungeons**

Modify `src/main/scala/map/WorldMutator.scala`:
Ensure all settlement entrances and dungeon approach tiles are paired and connected via `PathGenerator.generatePathAroundObstacles`.

- [ ] **Step 4: Run test to verify it passes**

Run: `sbt "testOnly map.TownDungeonRoadTest"`
Expected: PASS

- [ ] **Step 5: Commit changes**

```bash
git add src/main/scala/map/WorldMutator.scala src/test/scala/map/TownDungeonRoadTest.scala
git commit -m "feat: generate interconnected road paths between towns and dungeons"
```

---

### Task 3: Full Test Suite Verification

- [ ] **Step 1: Run all map tests**

Run: `sbt "testOnly map.ForestRoadClearingTest map.TownDungeonRoadTest map.DungeonConnectivityTest map.PathMergingTest"`
Expected: All pass cleanly.

- [ ] **Step 2: Run full sbt test suite**

Run: `sbt test`
Expected: All 150+ tests pass.

- [ ] **Step 3: Commit and push**

```bash
git add .
git commit -m "test: verify road network generation and forest tree clearing across full test suite"
git push origin main
```
