# Game Performance Optimizations Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Eliminate O(N) entity scans, redundant system executions, un-cached FOV raycasting, and rendering allocations across tick and frame updates.

**Architecture:** Maintain O(1) entity and spatial index lookups on `GameState`, deduplicate `GameState.phases` system invocations, cache FOV visible point sets, optimize `Pathfinder` AI steps, and streamline render-loop allocations in `Game.scala`.

**Tech Stack:** Scala 3, ScalaTest, Indigo Game Engine, sbt.

---

### Task 1: Deduplicate Game System Phases in `GameState`

**Files:**
- Modify: `src/main/scala/game/GameState.scala:41-98`

- [ ] **Step 1: Inspect duplicate system entries in `phases`**

Inspect lines 41-98 in [`GameState.scala`](file:///Users/robhawkes/Documents/personal/scala-roguelike-2025/src/main/scala/game/GameState.scala#L41-L98) to confirm duplicate entries for `VelocitySystem`, `DescendStairsSystem`, and `GrowthSystem`.

- [ ] **Step 2: Remove duplicate system entries**

Update `phases` in `GameState.scala` to keep exactly one reference per system:

```scala
  private val phases: Seq[Seq[GameSystem]] = Seq(
    // Phase 1: Input processing and early systems
    Seq(
      CullingSystem,
      DeathHandlerSystem,
      DebugSystem,
      StairsSpawnSystem,
      ExperienceSystem,
      GrowthSystem,
      EnemyAISystem,
      FarmerAISystem,
      MovementSystem,
      VelocitySystem,
      WaveSystem,
      ItemUseSystem,
      HealingSystem,
      EquipInputSystem,
      DescendStairsSystem,
      TradeSystem,
      ConversationSystem
    ),
    // Phase 2: Creation and spawning
    Seq(
      MessageSystem,
      SpawnEntitySystem,
      SpawnProjectileSystem,
      WorldGenerationSystem,
      WildAnimalSpawnSystem,
      VillageSystem,
      CaravanSystem,
      WaitSystem,
      OpenDoorSystem
    ),
    // Phase 3: Combat and collision processing
    Seq(
      CollisionCheckSystem,
      AttackSystem,
      RangeCheckSystem,
      CollisionHandlerSystem,
      DamageSystem,
      InventorySystem
    ),
    // Phase 4: Equipment and progression
    Seq(
      EquipmentSystem,
      InitiativeSystem,
      QuestSystem,
      LevelUpSystem,
      SightMemorySystem,
      EventMemorySystem
    )
  )
```

- [ ] **Step 3: Run test suite to verify behavior**

Run: `sbt test`
Expected: PASS (All tests pass)

- [ ] **Step 4: Commit phase deduplication**

```bash
git add src/main/scala/game/GameState.scala
git commit -m "perf: deduplicate game system phase execution in GameState"
```

---

### Task 2: Fast O(1) Entity Lookups & Updates in `GameState`

**Files:**
- Modify: `src/main/scala/game/GameState.scala:28-36`, `143-160`

- [ ] **Step 1: Implement pre-indexed entity maps in `GameState`**

Update `GameState` to maintain `entityIndex: Map[String, Entity]` and `entityIndexMap: Map[String, Int]` computed directly from `entities`:

```scala
  private lazy val entityIndex: Map[String, Entity] =
    entities.map(e => e.id -> e).toMap

  private lazy val entityIndexMap: Map[String, Int] =
    entities.iterator.zipWithIndex.map { case (e, idx) => e.id -> idx }.toMap

  val playerEntity: Entity = entityIndex(playerEntityId)

  def getEntity(entityId: String): Option[Entity] =
    entityIndex.get(entityId)
```

- [ ] **Step 2: Optimize `updateEntity` methods**

Use `entityIndexMap` to replace entity updating with direct indexed replacement:

```scala
  def updateEntity(entityId: String, newEntity: Entity): GameState = {
    entityIndexMap.get(entityId) match {
      case Some(index) => copy(entities = entities.updated(index, newEntity))
      case None        => this
    }
  }

  def updateEntity(entityId: String, update: Entity => Entity): GameState = {
    entityIndexMap.get(entityId) match {
      case Some(index) => copy(entities = entities.updated(index, update(entities(index))))
      case None        => this
    }
  }
```

- [ ] **Step 3: Verify with test suite**

Run: `sbt test`
Expected: PASS

- [ ] **Step 4: Commit O(1) entity lookup optimization**

```bash
git add src/main/scala/game/GameState.scala
git commit -m "perf: optimize entity lookup and updateEntity to O(1) map indexing"
```

---

### Task 3: O(1) Spatial Lookups for Actors & Dynamic Blockers

**Files:**
- Modify: `src/main/scala/game/GameState.scala:160-198`, `237-291`

- [ ] **Step 1: Add cached spatial indices to `GameState`**

Add pre-calculated spatial lookups to `GameState`:

```scala
  lazy val actorSpatialIndex: Map[Point, Entity] = {
    val builder = Map.newBuilder[Point, Entity]
    entities.foreach { entity =>
      if (entity.exists[Movement] && entity.exists[EntityTypeComponent](c => c.entityType == EntityType.Enemy || c.entityType == EntityType.Player)) {
        entity.position.foreach(pos => builder += (pos -> entity))
      }
    }
    builder.result()
  }

  lazy val lockedDoorPositions: Set[Point] = {
    entities.collect {
      case e if e.entityType.isInstanceOf[LockedDoor] =>
        e.get[Movement].map(_.position)
    }.flatten.toSet
  }
```

- [ ] **Step 2: Update `getActor`, `getVisiblePointsFor`, and `isBlocked`**

Update methods to consume spatial maps:

```scala
  def getActor(point: Point): Option[Entity] = actorSpatialIndex.get(point)

  def getVisiblePointsFor(entity: Entity): Set[Point] = {
    val isBlocked = (p: Point) =>
      worldMap.staticLineOfSightBlockingPoints.contains(p) || lockedDoorPositions.contains(p)

    val builder = Set.newBuilder[Point]
    for {
      entityPosition <- entity.hitbox
      lineOfSight = LineOfSight.getVisiblePoints(entityPosition, isBlocked, 10)
    } builder ++= lineOfSight

    builder.result()
  }
```

- [ ] **Step 3: Verify with test suite**

Run: `sbt test`
Expected: PASS

- [ ] **Step 4: Commit spatial indexing updates**

```bash
git add src/main/scala/game/GameState.scala
git commit -m "perf: add spatial actor indexing and pre-calculated blocker sets in GameState"
```

---

### Task 4: Line of Sight Optimizations & FOV Caching

**Files:**
- Modify: `src/main/scala/util/LineOfSight.scala:8-34`
- Modify: `src/main/scala/game/GameState.scala:180-198`

- [ ] **Step 1: Optimize `LineOfSight.getVisiblePoints` loop**

Refactor `LineOfSight.getVisiblePoints` to use optimized range checking and set building:

```scala
  def getVisiblePoints(
      start: Point,
      isBlocked: Point => Boolean,
      sightRange: Int
  ): Set[Point] = {
    val result = Set.newBuilder[Point]
    val r2 = sightRange * sightRange

    var dx = -sightRange
    while (dx <= sightRange) {
      var dy = -sightRange
      while (dy <= sightRange) {
        if (dx * dx + dy * dy <= r2) {
          val end = Point(start.x + dx, start.y + dy)
          if (isVisible(start, end, isBlocked)) {
            result += end
          }
        }
        dy += 1
      }
      dx += 1
    }

    result.result()
  }
```

- [ ] **Step 2: Add FOV memoization per entity position**

Cache entity visible points per `(entityId, position)` in `GameState` or `SightMemorySystem`:

```scala
  private static var fovCache: Map[(String, Point, Int), Set[Point]] = Map.empty
```

- [ ] **Step 3: Run test suite to verify line-of-sight correctness**

Run: `sbt test`
Expected: PASS

- [ ] **Step 4: Commit line-of-sight optimizations**

```bash
git add src/main/scala/util/LineOfSight.scala src/main/scala/game/GameState.scala
git commit -m "perf: optimize LineOfSight raycasting loop and add FOV position caching"
```

---

### Task 5: Pathfinder and AI Step Scoping Optimizations

**Files:**
- Modify: `src/main/scala/util/Pathfinder.scala:103-165`

- [ ] **Step 1: Refactor `Pathfinder.getNextStepWithSize` for fast entity retrieval**

Avoid redundant entity scanning by using `gameState.getEntity` or position index:

```scala
  def getNextStepWithSize(
      startPosition: Point,
      targetPosition: Point,
      gameState: GameState,
      entitySize: Point
  ): Option[Direction] = {
    val movingEntity = gameState.getActor(startPosition)
    val targetEntity = gameState.getActor(targetPosition)

    val movingEntityTiles = movingEntity.map(_.hitbox).getOrElse(Set(startPosition))
    val targetTiles = targetEntity.map(_.hitbox).getOrElse(Set(targetPosition))

    val originalBlockers = gameState.movementBlockingPoints
    val ignoredPoints = movingEntityTiles ++ targetTiles

    val path = Pathfinder.findPathWithSize(
      startPosition,
      targetPosition,
      originalBlockers,
      entitySize,
      ignoredPoints
    )

    path.drop(1).headOption.map(nextStep => Direction.fromPoints(startPosition, nextStep))
  }
```

- [ ] **Step 2: Run test suite**

Run: `sbt test`
Expected: PASS

- [ ] **Step 3: Commit Pathfinder optimizations**

```bash
git add src/main/scala/util/Pathfinder.scala
git commit -m "perf: optimize Pathfinder entity lookups using spatial index"
```

---

### Task 6: CullingSystem Performance Optimization

**Files:**
- Modify: `src/main/scala/game/system/CullingSystem.scala:14-55`

- [ ] **Step 1: Fast-path `CullingSystem` when entities have not changed state**

Update `CullingSystem.update` to avoid component recreation when active state already matches target:

```scala
  override def update(
      gameState: GameState,
      events: Seq[GameSystemEvent]
  ): (GameState, Seq[GameSystemEvent]) = {
    val player = gameState.playerEntity
    val playerPos = player.position

    var modified = false
    val updatedEntities = gameState.entities.map { entity =>
      if (entity.id == player.id) {
        if (!entity.has[Active]) { modified = true; entity.addComponent(Active()) } else entity
      } else if (entity.has[Movement]) {
        val dist = entity.position.map(_.getChebyshevDistance(playerPos)).getOrElse(0)
        if (dist <= ActivationRadius) {
          if (!entity.has[Active]) { modified = true; entity.addComponent(Active()) } else entity
        } else {
          if (entity.has[Active]) { modified = true; entity.removeComponent[Active] } else entity
        }
      } else {
        if (!entity.has[Active]) { modified = true; entity.addComponent(Active()) } else entity
      }
    }

    val finalState = if (modified) gameState.copy(entities = updatedEntities) else gameState
    (finalState, Seq.empty)
  }
```

- [ ] **Step 2: Verify test suite**

Run: `sbt test`
Expected: PASS

- [ ] **Step 3: Commit CullingSystem optimization**

```bash
git add src/main/scala/game/system/CullingSystem.scala
git commit -m "perf: skip GameState updates in CullingSystem when entity active states are unchanged"
```

---

### Task 7: Render Loop Allocation Cleanups in `Game.scala`

**Files:**
- Modify: `src/main/scala/indigoengine/Game.scala:280-340`

- [ ] **Step 1: Optimize dual-grid vertex neighbor checking**

Replace `List[Point]` allocations in `terrainSprites` rendering loop with direct boolean checks:

```scala
      val p1 = game.Point(x - 1, y - 1)
      val p2 = game.Point(x, y - 1)
      val p3 = game.Point(x - 1, y)
      val p4 = game.Point(x, y)

      val anyVisible = visiblePoints.contains(p1) || visiblePoints.contains(p2) || visiblePoints.contains(p3) || visiblePoints.contains(p4)
      val anySeen = sightMemory.contains(p1) || sightMemory.contains(p2) || sightMemory.contains(p3) || sightMemory.contains(p4)
```

- [ ] **Step 2: Verify test suite**

Run: `sbt test`
Expected: PASS

- [ ] **Step 3: Commit render-loop cleanups**

```bash
git add src/main/scala/indigoengine/Game.scala
git commit -m "perf: eliminate vertex list allocations in dual grid terrain renderer"
```

---

### Task 8: Full Suite Verification & Final Checkpoint

- [ ] **Step 1: Run complete test suite**

Run: `sbt test`
Expected: PASS (`All tests passed`, 161+ succeeded, 0 failed)

- [ ] **Step 2: Push changes to main branch**

```bash
git push origin main
```
