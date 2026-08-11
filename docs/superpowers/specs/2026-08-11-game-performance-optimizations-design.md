# Design Spec: Game Performance Optimizations

**Date**: 2026-08-11  
**Goal**: Optimize tick execution, entity indexing, line-of-sight calculation, AI pathfinding, and map rendering to eliminate frame drops and tick latency when map visibility or entity counts scale.

---

## 1. Executive Summary

As the game world expands with larger visible areas, dense villages, and numerous active entities, tick processing and frame rendering encounter several O(N) linear search and allocation bottlenecks. This design introduces O(1) spatial indexing, FOV result caching, AI pathfinding optimizations, system execution deduplication, and rendering allocation cleanups—improving performance while maintaining 100% functional equality and compatibility with all existing tests.

---

## 2. Identified Performance Bottlenecks

### 2.1 O(N) Entity Updates and Lookups in `GameState`
- `updateEntity(id, ...)` uses `entities.indexWhere(_.id == entityId)`, scanning the entire entity list on every component change.
- `entityIndex` was a `lazy val` discarded on every `GameState.copy`, causing repeated map re-allocations.

### 2.2 O(N) Spatial and Collision Queries
- `getActor(point)`, `isBlocked(points)`, `lineOfSightBlockingPoints`, and `dynamicMovementBlockingPoints` iterate linearly across all entities in `GameState.entities`.
- When multiple AI entities evaluate movement or collision, thousands of linear entity scans occur per tick.

### 2.3 Redundant System Execution in `GameState.phases`
- `VelocitySystem` is invoked twice in Phase 1.
- `DescendStairsSystem` is invoked twice in Phase 1.
- `GrowthSystem` is invoked twice (Phase 1 and Phase 2).

### 2.4 Un-cached FOV (Line of Sight) Calculation
- `getVisiblePointsFor(entity)` scans all entities for `LockedDoor` and executes 441 Bresenham raycasts every tick, even when the entity and nearby doors have not moved.

### 2.5 Pathfinding & AI Entity Scans
- `Pathfinder.getNextStep` scans `gameState.entities` multiple times to find the source entity, target entity, and blockers.
- Full A* path structures (up to 6,000 nodes) are constructed to determine only the immediate next step direction.

### 2.6 Full Entity Mapping in `CullingSystem`
- `CullingSystem` maps across all entities every tick to evaluate Chebyshev distance to the player, recreating entity instances even for stationary or distant entities.

### 2.7 Render-Loop Allocation Hotspots
- Dual-grid terrain rendering in `Game.scala` allocates temporary `List[Point]` instances for every vertex on every frame.
- Material tinting (`.modifyMaterial(_.toImageEffects.withTint(...))`) creates new material wrappers dynamically per tile on every frame.

---

## 3. Targeted Optimizations & Architecture

### 3.1 Spatial & Entity Indexing in `GameState`
- **O(1) Entity Lookups & Updates**:
  - `GameState` will maintain pre-indexed data structures: `entityMap: Map[String, Entity]` and `entityIndexMap: Map[String, Int]`.
  - `updateEntity(id, update)` will use `entityIndexMap` to replace entities in O(1) time without linear search.
- **O(1) Spatial Lookups**:
  - Maintain a spatial index `actorSpatialIndex: Map[Point, Entity]` for actors (Player, Enemy, Animal).
  - `getActor(point)` becomes `actorSpatialIndex.get(point)`.
- **Pre-computed Dynamic Blocker Sets**:
  - Maintain cached sets for `lockedDoorPositions: Set[Point]` and `dynamicActorPositions: Set[Point]`.
  - `isBlocked(points)` and `dynamicMovementBlockingPoints` perform set intersections instead of scanning entity lists.

### 3.2 System Phase Deduplication
- Remove duplicate references to `VelocitySystem`, `DescendStairsSystem`, and `GrowthSystem` in `GameState.phases`.

### 3.3 FOV Caching & Raycast Optimization
- Implement FOV caching in `GameState` or `SightMemorySystem`:
  - Key: `(entityId, entityPosition, lockedDoorStateVersion)`.
  - If entity position and door state haven't changed, return the cached `Set[Point]` of visible tiles.
- In `LineOfSight.getVisiblePoints`:
  - Optimize distance checks and array/set builder allocations.

### 3.4 AI & Pathfinding Efficiency
- Refactor `Pathfinder.getNextStepWithSize`:
  - Use `GameState` spatial indexing to fetch start/target entity hitboxes in O(1) time.
  - Pass pre-computed blocker sets directly.
  - Implement early exit when adjacent step directly reaches target or if path is trivially clear.

### 3.5 CullingSystem Active Set Optimization
- Avoid re-instantiating entities whose active status does not change.
- Fast-path check: if player position hasn't changed since last tick, skip distance re-evaluations.

### 3.6 Rendering & Material Allocation Cleanup
- Refactor vertex neighbor checking in `Game.scala` to use fixed arrays/tuples without allocating `List[Point]`.
- Cache sepia-tinted sprite graphics or materials so image effects are not re-created frame-by-frame.

---

## 4. Verification & Testing Strategy

1. **Unit & Integration Tests**:
   - Run `sbt test` to ensure all 161+ tests pass without regressions.
2. **Deterministic Verification**:
   - Ensure seed determinism (world generation, pathfinding, AI choices) is strictly preserved.
3. **Performance Benchmark**:
   - Verify tick execution times and memory allocations scale cleanly under high entity counts and large visible map regions.
