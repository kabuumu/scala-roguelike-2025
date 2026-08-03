# Dungeon Accessibility, Path Merging & Entrance Rendering Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans or superpowers:subagent-driven-development to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ensure all dungeons are 100% accessible via clear paths connecting settlements and dungeons (with paths merging to eliminate duplication), and ensure dungeon entrances are correctly generated on the map and rendered without blocking wall bugs.

**Architecture:** 
1. Fix wall boundary math in `Dungeon.scala` so `Right` and `Down` entrance doors lie on the wall perimeter (offset `roomSize`), preventing wall tiles from blocking doors.
2. Implement 3x3 clearance zones around entrance door and approach tiles to guarantee impassable terrain (rocks/trees/walls) never traps dungeon entrances.
3. Update `PathGenerator` A* pathfinding to offer cost discounts (0.15 vs 1.0) for existing path/road tiles, driving paths to naturally merge into shared trunks.
4. Ensure all dungeons (including initial quest dungeon and regional dungeons) are connected to settlements during world generation.

**Tech Stack:** Scala 3.6.4, Indigo Engine, SBT test

---

### Task 1: Fix Dungeon Entrance Door Coordinates & Wall Alignment

**Files:**
- Create: `src/test/scala/map/DungeonEntranceTest.scala`
- Modify: `src/main/scala/map/Dungeon.scala:90-105,455-467,283`

- [ ] **Step 1: Write the failing unit test for entrance door placement**

Create `src/test/scala/map/DungeonEntranceTest.scala`:
```scala
package map

import org.scalatest.funsuite.AnyFunSuite
import game.{Direction, Point}

class DungeonEntranceTest extends AnyFunSuite {

  test("getEntranceDoor places door on wall perimeter for all 4 directions") {
    val startRoom = Point(0, 0)
    val roomSize = Dungeon.roomSize // 10

    val upDoor = Dungeon.getEntranceDoor(startRoom, Direction.Up)
    val downDoor = Dungeon.getEntranceDoor(startRoom, Direction.Down)
    val leftDoor = Dungeon.getEntranceDoor(startRoom, Direction.Left)
    val rightDoor = Dungeon.getEntranceDoor(startRoom, Direction.Right)

    assert(upDoor == Point(5, 0), s"Up door should be at (5,0), got $upDoor")
    assert(downDoor == Point(5, 10), s"Down door should be at (5,10), got $downDoor")
    assert(leftDoor == Point(0, 5), s"Left door should be at (0,5), got $leftDoor")
    assert(rightDoor == Point(10, 5), s"Right door should be at (10,5), got $rightDoor")
  }

  test("dungeon tiles mark entrance door as walkable non-wall tile for Right/Down entrances") {
    val configRight = DungeonConfig(
      bounds = MapBounds(0, 3, 0, 3),
      seed = 12345L,
      entranceSide = Direction.Right
    )
    val dungeonRight = DungeonGenerator.generateDungeon(configRight)
    val rightDoor = Dungeon.getEntranceDoor(dungeonRight.startPoint, Direction.Right)

    assert(!dungeonRight.walls.contains(rightDoor), "Right entrance door must NOT be a wall")
    assert(dungeonRight.tiles.get(rightDoor).contains(TileType.Floor) || dungeonRight.tiles.get(rightDoor).contains(TileType.Dirt) || dungeonRight.tiles.get(rightDoor).contains(TileType.Bridge), "Entrance door tile must be walkable floor/dirt/bridge")

    val configDown = DungeonConfig(
      bounds = MapBounds(0, 3, 0, 3),
      seed = 12345L,
      entranceSide = Direction.Down
    )
    val dungeonDown = DungeonGenerator.generateDungeon(configDown)
    val downDoor = Dungeon.getEntranceDoor(dungeonDown.startPoint, Direction.Down)

    assert(!dungeonDown.walls.contains(downDoor), "Down entrance door must NOT be a wall")
    assert(dungeonDown.tiles.get(downDoor).contains(TileType.Floor) || dungeonDown.tiles.get(downDoor).contains(TileType.Dirt) || dungeonDown.tiles.get(downDoor).contains(TileType.Bridge), "Entrance door tile must be walkable floor/dirt/bridge")
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `sbt "testOnly map.DungeonEntranceTest"`
Expected: FAIL due to `downDoor` returning `Point(5, 9)` instead of `Point(5, 10)`, and `rightDoor` returning `Point(9, 5)` instead of `Point(10, 5)`.

- [ ] **Step 3: Fix `getEntranceDoor`, `doorPoints`, and entrance door tile in `Dungeon.scala`**

In `src/main/scala/map/Dungeon.scala`:
1. Update `doorPoints` (lines 90-105):
```scala
  val doorPoints: Set[Point] = roomConnections.map {
    case RoomConnection(originRoom, direction, _, _) =>
      val originRoomX = originRoom.x * Dungeon.roomSize
      val originRoomY = originRoom.y * Dungeon.roomSize

      direction match {
        case Direction.Up =>
          Point(originRoomX + Dungeon.roomSize / 2, originRoomY)
        case Direction.Down =>
          Point(
            originRoomX + Dungeon.roomSize / 2,
            originRoomY + Dungeon.roomSize
          )
        case Direction.Left =>
          Point(originRoomX, originRoomY + Dungeon.roomSize / 2)
        case Direction.Right =>
          Point(
            originRoomX + Dungeon.roomSize,
            originRoomY + Dungeon.roomSize / 2
          )
      }
  }
```

2. Update `getEntranceDoor` (lines 455-467):
```scala
  def getEntranceDoor(startRoom: Point, entranceSide: Direction): Point = {
    val roomX = startRoom.x * roomSize
    val roomY = startRoom.y * roomSize

    entranceSide match {
      case Direction.Up    => Point(roomX + roomSize / 2, roomY)
      case Direction.Down  => Point(roomX + roomSize / 2, roomY + roomSize)
      case Direction.Left  => Point(roomX, roomY + roomSize / 2)
      case Direction.Right => Point(roomX + roomSize, roomY + roomSize / 2)
    }
  }
```

3. Update line 283:
```scala
    regularTiles + (entranceDoorPoint -> TileType.Floor)
```

- [ ] **Step 4: Run test to verify it passes**

Run: `sbt "testOnly map.DungeonEntranceTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/map/Dungeon.scala src/test/scala/map/DungeonEntranceTest.scala
git commit -m "fix: correct dungeon entrance door alignment and wall boundary calculation"
```

---

### Task 2: Implement Entrance Clearance Zones & Approach Safety

**Files:**
- Modify: `src/main/scala/map/Dungeon.scala`
- Modify: `src/main/scala/map/GlobalFeaturePlanner.scala:264-270`
- Modify: `src/test/scala/map/DungeonEntranceTest.scala`

- [ ] **Step 1: Write tests for 3x3 entrance clearance zone**

Add to `src/test/scala/map/DungeonEntranceTest.scala`:
```scala
  test("dungeon entrance area (3x3 around approach tile) contains no impassable walls/rocks/trees") {
    val config = DungeonConfig(
      bounds = MapBounds(0, 4, 0, 4),
      seed = 99999L,
      entranceSide = Direction.Left
    )
    val dungeon = DungeonGenerator.generateDungeon(config)
    val entranceDoor = Dungeon.getEntranceDoor(dungeon.startPoint, config.entranceSide)
    val approachTile = Dungeon.getApproachTile(dungeon.startPoint, config.entranceSide)

    val clearanceArea = for {
      dx <- -1 to 1
      dy <- -1 to 1
    } yield Point(approachTile.x + dx, approachTile.y + dy)

    clearanceArea.foreach { pt =>
      assert(!dungeon.walls.contains(pt), s"Clearance point $pt must not be a wall")
      assert(!dungeon.rocks.contains(pt), s"Clearance point $pt must not be a rock")
    }
  }
```

- [ ] **Step 2: Run test to verify status**

Run: `sbt "testOnly map.DungeonEntranceTest"`

- [ ] **Step 3: Ensure clearance zone around entrance door and approach tile in `Dungeon.scala` and `GlobalFeaturePlanner.scala`**

In `src/main/scala/map/Dungeon.scala`, update `tiles` generation (around lines 280-284) to explicitly override the 3x3 area around `entranceDoorPoint` and `approachTile` to be walkable:
```scala
    val approachTile = Dungeon.getApproachTile(startPoint, entranceSide)
    val entranceArea = (for {
      dx <- -1 to 1
      dy <- -1 to 1
    } yield Point(approachTile.x + dx, approachTile.y + dy)).toSet + entranceDoorPoint

    val clearanceTiles = entranceArea.map { pt =>
      val tile = if (pt == entranceDoorPoint) TileType.Floor else TileType.Dirt
      pt -> tile
    }.toMap

    regularTiles ++ clearanceTiles
```

- [ ] **Step 4: Run test to verify it passes**

Run: `sbt "testOnly map.DungeonEntranceTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/map/Dungeon.scala src/test/scala/map/DungeonEntranceTest.scala
git commit -m "feat: enforce 3x3 walkable clearance zone around dungeon entrance doors"
```

---

### Task 3: Path Merging Pathfinder with Existing Path Cost Discount

**Files:**
- Create: `src/test/scala/map/PathMergingTest.scala`
- Modify: `src/main/scala/map/PathGenerator.scala`

- [ ] **Step 1: Write failing unit test for path merging and A* obstacle tolerance**

Create `src/test/scala/map/PathMergingTest.scala`:
```scala
package map

import org.scalatest.funsuite.AnyFunSuite
import game.Point

class PathMergingTest extends AnyFunSuite {

  test("pathfinder reuses existing path tiles to minimize path duplication") {
    val bounds = MapBounds(-10, 10, -10, 10)
    val obstacles = Set.empty[Point]

    // Existing trunk path from (0,0) to (0,10)
    val existingTrunk = (0 to 10).map(y => Point(0, y)).toSet

    // Path from (5,0) to (0,10). Without discount, straight line (5,0)->(0,10) creates 10 new tiles.
    // With discount, path should move west to (0,0) or (0,2) and follow trunk to (0,10), reusing trunk tiles!
    val path = PathGenerator.generatePathAroundObstacles(
      startPoint = Point(5, 0),
      targetPoint = Point(0, 10),
      obstacles = obstacles,
      width = 0,
      bounds = bounds,
      existingPaths = existingTrunk
    )

    // Check that path intersects and reuses trunk tiles
    val reusedTiles = path.intersect(existingTrunk)
    assert(reusedTiles.nonEmpty, "New path should merge into and reuse existing trunk path")
  }

  test("pathfinder succeeds even when start/target borders obstacles") {
    val bounds = MapBounds(-10, 10, -10, 10)
    val start = Point(0, 0)
    val target = Point(5, 5)
    // Obstacles touch start and target
    val obstacles = Set(Point(-1, 0), Point(1, 0), Point(0, -1), Point(6, 5), Point(5, 6))

    val path = PathGenerator.generatePathAroundObstacles(
      startPoint = start,
      targetPoint = target,
      obstacles = obstacles,
      width = 0,
      bounds = bounds
    )

    assert(path.nonEmpty, "Pathfinder should succeed when start/target touch obstacles")
    assert(path.contains(start) && path.contains(target), "Path must connect start to target")
  }
}
```

- [ ] **Step 2: Run test to verify failure**

Run: `sbt "testOnly map.PathMergingTest"`
Expected: FAIL (no `existingPaths` argument on `generatePathAroundObstacles`).

- [ ] **Step 3: Implement existing path cost discount & obstacle tolerance in `PathGenerator.scala`**

In `src/main/scala/map/PathGenerator.scala`:
1. Update `generatePathAroundObstacles` signature to accept `existingPaths: Set[Point] = Set.empty`:
```scala
  def generatePathAroundObstacles(
    startPoint: Point,
    targetPoint: Point,
    obstacles: Set[Point],
    width: Int,
    bounds: MapBounds,
    existingPaths: Set[Point] = Set.empty
  ): Set[Point] = {
    // Dynamically exclude start and target from obstacles so pathfinder never fails at endpoints
    val safeObstacles = obstacles - startPoint - targetPoint
    val mainPath = findPathAroundObstacles(startPoint, targetPoint, safeObstacles, bounds, existingPaths)
    
    val finalPath = if (mainPath.isEmpty) {
      findPathLine(startPoint, targetPoint)
    } else {
      mainPath
    }
    
    widenPathAvoidingObstacles(finalPath, width, bounds, safeObstacles)
  }
```

2. Update `findPathAroundObstacles`:
```scala
  private def findPathAroundObstacles(
    start: Point, 
    target: Point, 
    obstacles: Set[Point],
    bounds: MapBounds,
    existingPaths: Set[Point] = Set.empty
  ): Seq[Point] = {
    import scala.collection.mutable

    val safeObstacles = obstacles - start - target

    if (!isWithinBounds(start, bounds) || !isWithinBounds(target, bounds)) {
      return Seq.empty
    }

    case class Node(point: Point, g: Double, h: Double, parent: Option[Node], direction: Option[(Int, Int)]) {
      val f: Double = g + h
    }

    def heuristic(a: Point, b: Point): Double =
      math.abs(a.x - b.x) + math.abs(a.y - b.y)

    def getDirection(from: Point, to: Point): (Int, Int) = {
      val dx = if (to.x > from.x) 1 else if (to.x < from.x) -1 else 0
      val dy = if (to.y > from.y) 1 else if (to.y < from.y) -1 else 0
      (dx, dy)
    }

    def reconstructPath(node: Node): Seq[Point] = {
      @tailrec
      def loop(n: Node, acc: List[Point]): Seq[Point] = n.parent match {
        case Some(parent) => loop(parent, n.point :: acc)
        case None => n.point :: acc
      }
      loop(node, Nil)
    }

    implicit val nodeOrdering: Ordering[Node] = Ordering.by[Node, Double](-_.f)
    val openSet = mutable.PriorityQueue(Node(start, 0.0, heuristic(start, target), None, None))
    val closedSet = mutable.HashSet[Point]()
    val gScores = mutable.HashMap[Point, Double](start -> 0.0)

    while (openSet.nonEmpty) {
      val current = openSet.dequeue()

      if (current.point == target) {
        return reconstructPath(current)
      }

      if (!closedSet.contains(current.point)) {
        closedSet += current.point

        val neighbors = Seq(
          Point(current.point.x + 1, current.point.y),
          Point(current.point.x - 1, current.point.y),
          Point(current.point.x, current.point.y + 1),
          Point(current.point.x, current.point.y - 1)
        ).filter { neighbor =>
          isWithinBounds(neighbor, bounds) && !safeObstacles.contains(neighbor)
        }

        neighbors.foreach { neighbor =>
          val neighborDirection = getDirection(current.point, neighbor)

          // Step cost discount for existing path/road tiles (0.15 vs 1.0)
          var stepCost = if (existingPaths.contains(neighbor)) 0.15 else 1.0

          // Penalty for turning
          current.direction match {
            case Some(prevDir) if prevDir != neighborDirection =>
              stepCost += 0.1
            case _ =>
          }

          val tentativeG = current.g + stepCost

          if (tentativeG < gScores.getOrElse(neighbor, Double.MaxValue)) {
            gScores(neighbor) = tentativeG
            val h = heuristic(neighbor, target)
            openSet.enqueue(Node(neighbor, tentativeG, h, Some(current), Some(neighborDirection)))
          }
        }
      }
    }

    Seq.empty
  }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `sbt "testOnly map.PathMergingTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/map/PathGenerator.scala src/test/scala/map/PathMergingTest.scala
git commit -m "feat: add path-merging cost discounts and obstacle safety to PathGenerator"
```

---

### Task 4: Connect All Dungeons & Settlements in World Generation

**Files:**
- Modify: `src/main/scala/game/StartingState.scala`
- Modify: `src/main/scala/map/WorldMutator.scala`
- Create: `src/test/scala/map/DungeonConnectivityTest.scala`

- [ ] **Step 1: Write integration test verifying continuous connectivity from spawn to all dungeons**

Create `src/test/scala/map/DungeonConnectivityTest.scala`:
```scala
package map

import org.scalatest.funsuite.AnyFunSuite
import game.{Point, StartingState}
import util.LineOfSight

class DungeonConnectivityTest extends AnyFunSuite {

  test("every dungeon in Adventure mode has a clear walkable path from spawn village across 10 seeds") {
    (100L to 110L).foreach { seed =>
      val gameState = StartingState.startAdventure(seed)
      val playerPos = gameState.playerEntity.position
      val worldMap = gameState.worldMap

      assert(worldMap.dungeons.nonEmpty, s"World map for seed $seed must contain dungeons")

      worldMap.dungeons.foreach { dungeon =>
        val entranceDoor = Dungeon.getEntranceDoor(dungeon.startPoint, dungeon.entranceSide)
        val approachTile = Dungeon.getApproachTile(dungeon.startPoint, dungeon.entranceSide)

        // Verify entrance door & approach tile are walkable
        assert(!worldMap.staticMovementBlockingPoints.contains(entranceDoor),
          s"Seed $seed: Entrance door $entranceDoor must be walkable")
        assert(!worldMap.staticMovementBlockingPoints.contains(approachTile),
          s"Seed $seed: Approach tile $approachTile must be walkable")

        // Perform BFS pathfinding from player spawn to dungeon approach tile
        val reachable = isReachable(playerPos, approachTile, worldMap)
        assert(reachable, s"Seed $seed: Dungeon entrance $approachTile must be reachable from player spawn $playerPos")
      }
    }
  }

  private def isReachable(start: Point, target: Point, worldMap: WorldMap): Boolean = {
    import scala.collection.mutable

    val queue = mutable.Queue[Point](start)
    val visited = mutable.Set[Point](start)

    while (queue.nonEmpty) {
      val curr = queue.dequeue()
      if (curr == target) return true

      val neighbors = Seq(
        Point(curr.x + 1, curr.y),
        Point(curr.x - 1, curr.y),
        Point(curr.x, curr.y + 1),
        Point(curr.x, curr.y - 1)
      )

      neighbors.foreach { n =>
        if (!visited.contains(n) && !worldMap.staticMovementBlockingPoints.contains(n)) {
          visited += n
          queue.enqueue(n)
        }
      }
    }

    false
  }
}
```

- [ ] **Step 2: Run test to check connectivity**

Run: `sbt "testOnly map.DungeonConnectivityTest"`

- [ ] **Step 3: Connect starting quest dungeon & update mutators in `StartingState.scala` & `WorldMutator.scala`**

In `src/main/scala/game/StartingState.scala`:
After creating `worldMapWithChunks` in `startAdventure` (or during `baseWorldMap` / mutator pass), generate paths connecting `playerSpawnPoint` to `createAdventureQuestDungeon` entrance using `PathGenerationMutator` / `PathGenerator`:
```scala
    // Generate path to the initial quest dungeon
    val initialDungeon = baseWorldMap.dungeons.head
    val approachTile = Dungeon.getApproachTile(initialDungeon.startPoint, initialDungeon.entranceSide)
    val entranceDoor = Dungeon.getEntranceDoor(initialDungeon.startPoint, initialDungeon.entranceSide)

    val obstacles = baseWorldMap.staticMovementBlockingPoints - playerSpawnPoint - approachTile - entranceDoor
    val questDungeonPath = PathGenerator.generatePathAroundObstacles(
      playerSpawnPoint,
      approachTile,
      obstacles,
      width = 0,
      worldBounds,
      existingPaths = baseWorldMap.paths
    ) + entranceDoor + approachTile

    val questDungeonTiles = questDungeonPath.map { p =>
      val currentTile = baseWorldMap.tiles.getOrElse(p, TileType.Grass1)
      val tile = if (currentTile == TileType.Water) TileType.Bridge else TileType.Dirt
      p -> tile
    }.toMap

    val baseWorldMapWithQuestPath = baseWorldMap.copy(
      tiles = baseWorldMap.tiles ++ questDungeonTiles,
      paths = baseWorldMap.paths ++ questDungeonPath,
      bridges = baseWorldMap.bridges ++ questDungeonPath.filter(p => baseWorldMap.tiles.get(p).contains(TileType.Water))
    )
```

In `src/main/scala/map/WorldMutator.scala`:
In `PathGenerationMutator`, accumulate `existingPaths` as each destination is connected, passing `existingPaths` to `PathGenerator.generatePathAroundObstacles` so paths merge into shared trunks.

- [ ] **Step 4: Run connectivity test to verify all dungeons are 100% connected and accessible**

Run: `sbt "testOnly map.DungeonConnectivityTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/game/StartingState.scala src/main/scala/map/WorldMutator.scala src/test/scala/map/DungeonConnectivityTest.scala
git commit -m "feat: guarantee 100% path connectivity between settlements and dungeons with path merging"
```

---

### Task 5: Complete Verification Suite

- [ ] **Step 1: Run full project test suite**

Run: `sbt test`
Expected: `All tests passed.` (148+ tests pass, 0 failures)

- [ ] **Step 2: Final commit & status check**

Verify clean git working tree: `git status`
