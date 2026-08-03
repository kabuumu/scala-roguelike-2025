# Design Spec: Dungeon Accessibility, Path Merging & Entrance Rendering

## Executive Summary
This design ensures that all dungeons in the game are 100% accessible, connected to settlements via a clean, merged path network that minimizes path duplication, and that dungeon entrances are generated and rendered correctly without being blocked by wall/terrain bugs.

---

## 1. Root Cause Analysis & Problem Statement

### 1.1 Dungeon Entrance Wall Blocking Bug
- **Bug**: In `Dungeon.scala`, `getEntranceDoor` and `doorPoints` used `roomSize - 1` (offset 9) for `Right` and `Down` directions, whereas room walls (`isWall`) are defined at `roomSize` (offset 10).
- **Consequence**: Doors facing `Right` or `Down` were generated at offset 9 (inside the room), while offset 10 remained a solid wall (`TileType.Wall`), completely blocking access into or out of the dungeon.
- **Fix**: Align `getEntranceDoor` and `doorPoints` to use `roomX + roomSize` for `Right` and `roomY + roomSize` for `Down`, matching `lockedDoors` and `roomPaths`.

### 1.2 Path Duplication & A* Fallback Issues
- **Issue A (Path Duplication)**: Paths were generated independently from a single start point to each destination without cost discounts for reusing existing path/road tiles, creating redundant radiating paths.
- **Issue B (A* Failure on Entrance Obstacles)**: `PathGenerator.findPathAroundObstacles` immediately failed if `start` or `target` coincided with or touched obstacle sets (e.g., entrance door/wall perimeter), causing immediate fallback to naive L-shaped straight lines that cut through walls or rivers.
- **Fix**:
  1. Introduce existing-path cost discounts (e.g. 0.1-0.2 for existing `Dirt`/`Bridge`/`Road` vs 1.0 for grass/terrain) into A* pathfinding to encourage paths to naturally merge into shared trunk roads.
  2. Ensure `start` and `target` coordinates (and immediate entrance approach padding) are dynamically excluded from the obstacle set in `PathGenerator`.

### 1.3 Unconnected Starting Quest Dungeon
- **Issue**: `StartingState.startAdventure` generates an initial quest dungeon (`createAdventureQuestDungeon`) near the player spawn, but does not run path generation to link it to the starting village or road network.
- **Fix**: Explicitly connect all dungeons (including `createAdventureQuestDungeon`) to the nearest village/road network upon initial world map creation.

---

## 2. Detailed Technical Design

### 2.1 Dungeon Entrance Geometry & Clearance
- **Door Point Definitions**:
  - `Direction.Up`: `Point(roomX + roomSize / 2, roomY)`
  - `Direction.Down`: `Point(roomX + roomSize / 2, roomY + roomSize)`
  - `Direction.Left`: `Point(roomX, roomY + roomSize / 2)`
  - `Direction.Right`: `Point(roomX + roomSize, roomY + roomSize / 2)`
- **Approach Tile**:
  - 1 tile outside the door in `entranceSide` direction.
- **Clearance Zone**:
  - Clear a 3x3 area around `approachTile` and `entranceDoor`:
    - Convert `Rock`, `Tree`, `Wall` in clearance zone to `Dirt` (outside) or `Floor` (at door).
    - Convert `Water` in clearance zone to `Bridge`.

### 2.2 Merged Path Network Algorithm
- **Network Construction (Minimum Spanning Tree / Shared Trunks)**:
  - Collect all nodes: `Settlement` centers/entrances + `Dungeon` approach tiles.
  - Sort nodes or construct an MST/nearest-neighbor graph.
  - Sequentially run pathfinding to connect nodes.
  - Maintain a mutable `existingPathTiles` set.
- **Pathfinder Cost Model (`PathGenerator`)**:
  - Base terrain step cost: `1.0`
  - Existing path/road/bridge step cost: `0.15` (85% discount!)
  - Direction change penalty: `0.1` (keeps roads smooth)
  - Terrain penalty for Forest/Desert/Water: higher costs, with water requiring `Bridge`.
- **Result**: New paths seek out existing paths and merge with them, minimizing duplication while guaranteeing connectivity.

### 2.3 Entrance Rendering & Tile Representation
- **Tile Types**:
  - Entrance door tile (`entranceDoorPoint`): `TileType.Floor` or `TileType.Dirt` (walkable, non-blocking threshold).
  - Approach path: `TileType.Dirt` (or `Bridge` if crossing water).
- **Map Views**:
  - `WorldMapUI`: Renders entrance door threshold cleanly as part of the dungeon floor outline.
  - `OverworldMapUI`: Renders paths/trails connecting village/settlement markers directly to dungeon entrance points.

---

## 3. Verification & Testing Strategy
1. **Unit Tests**:
   - `DungeonEntranceTest`: Test `getEntranceDoor` and `getApproachTile` for all 4 directions (`Up`, `Down`, `Left`, `Right`) to verify door tiles are placed on wall perimeters and marked as walkable (`isWall(doorPoint)` is false, tile is `Floor`/`Dirt`).
   - `PathMergingTest`: Verify that pathfinding between 3+ nodes reuses existing path tiles and minimizes total newly created path tiles.
   - `DungeonAccessibilityTest`: Verify across 50+ random seeds that every dungeon (including starting quest dungeon and regional dungeons) has a continuous walkable path from the starting village / settlement.
2. **Integration Verification**:
   - Run `sbt test` to ensure all 148+ tests pass with zero regressions.
