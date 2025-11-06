# Pathfinding Diagonal Path Fix

## Issues Identified and Fixed

### 1. Diagonal Paths (Original Issue) ✅

**Problem:** Paths were being generated with diagonal movement instead of straight orthogonal lines.

**Root Cause:** The fallback `findPathLine` function used Bresenham's line algorithm, which can move diagonally by incrementing both X and Y coordinates in the same step.

**Solution:** Replaced Bresenham with an L-shaped orthogonal pathfinding algorithm that:
- Moves horizontally first (only changing X coordinate)
- Then moves vertically (only changing Y coordinate)  
- **Never creates diagonal steps** - only straight orthogonal lines

**File Changed:** `src/main/scala/map/PathGenerator.scala` - `findPathLine` method

### 2. A* Pathfinding Failures (Discovered Issue) ✅

**Problem:** A* pathfinding was failing and falling back to direct line generation, shown by these console logs:
```
A* pathfinding failed: No path found from 0:0 to -25:-55 (checked 38414 tiles)
Falling back to direct line from 0:0 to -25:-55
```

**Root Cause:** 
- Destinations (dungeon/village entrances) were placed in areas completely surrounded by obstacles
- The spawn point (0,0) had no cleared entrance area, making it easily blocked by obstacles
- A* would exhaust almost the entire map (38,414 out of 44,521 tiles) trying to find a path

**Solution:** Added spawn area clearance in `WorldMutator`:
- Now clears a 5x5 area around the spawn point (matching what was done for destinations)
- Removes obstacles from both spawn AND destination areas before pathfinding
- This prevents paths from being impossible to generate

**File Changed:** `src/main/scala/map/WorldMutator.scala` - `PathGenerationMutator` class

### 3. Dungeon Approach Tiles (Prevents Wall Cutting) ✅

**Problem:** Paths were being routed directly to dungeon entrance tiles, which could cause them to cut through dungeon walls.

**Root Cause:** The pathfinding system didn't account for the entrance orientation. It would route paths directly to the entrance tile, which is inside the dungeon structure. Depending on the path angle, this could cause paths to cut diagonally through dungeon walls.

**Solution:** Implemented a two-phase path generation approach:
1. **Main paths** route to "approach tiles" (tiles in front of dungeon entrances, outside the dungeon)
2. **Connecting paths** create short 2-tile links from approach tiles to actual dungeon entrances

This ensures:
- Main paths stay outside dungeon boundaries
- No paths can cut through dungeon walls
- Clear entrance from approach tile to dungeon
- Villages and shops retain direct path routing (unchanged)

**Files Changed:**
- `src/main/scala/map/Dungeon.scala` - Added `entranceSide` field and `getApproachTile` helper
- `src/main/scala/map/DungeonGenerator.scala` - Pass entranceSide through generation
- `src/main/scala/map/WorldMutator.scala` - Updated `PathGenerationMutator` to use approach tiles

### 4. Diagnostic Logging (Added for Debugging) ✅

**Added:** Comprehensive diagnostic logging to identify why pathfinding fails:
- `"A* pathfinding failed: Start point (x,y) is an obstacle"`
- `"A* pathfinding failed: Target point (x,y) is an obstacle"`
- `"A* pathfinding failed: Start point (x,y) is outside bounds"`
- `"A* pathfinding failed: Target point (x,y) is outside bounds"`
- `"A* pathfinding failed: No path found from (x1,y1) to (x2,y2) (checked N tiles)"`
- `"Falling back to direct line from (x1,y1) to (x2,y2)"`

**File Changed:** `src/main/scala/map/PathGenerator.scala` - `findPathAroundObstacles` and `generatePathAroundObstacles`

## Verification Steps

### 1. Build and Run the Game
```bash
sbt build
cd target/indigoBuild
python3 -m http.server 8080
```

Navigate to http://localhost:8080 in your browser.

### 2. Check Console Output

**Before Fix:**
```
A* pathfinding failed: No path found from 0:0 to -25:-55 (checked 38414 tiles)
Falling back to direct line from 0:0 to -25:-55
```
Multiple fallbacks to direct line, each checking 38,414 tiles (almost entire map).

**After Fix (Expected):**
- Fewer or no "Falling back to direct line" messages
- If fallbacks occur, they should be for legitimate reasons (not blocked spawn)
- Any fallback paths will now be orthogonal (L-shaped) instead of diagonal

### 3. Visual Verification in Game

When the game loads:
- **Paths (brown/tan tiles)** connecting spawn to dungeons/villages should be visible
- **Paths should be orthogonal** - straight horizontal and vertical lines, forming L or rectangular shapes
- **No diagonal paths** - paths should never go diagonally from one tile to the next
- **Bridges (wooden planks)** where paths cross rivers should also be orthogonal

## Technical Details

### Orthogonal Path Algorithm

The new `findPathLine` implementation:

```scala
// Move horizontally first
var x = start.x
while (x != target.x) {
  points += Point(x, start.y)
  x += xStep
}

// Then move vertically
var y = start.y
while (y != target.y) {
  points += Point(target.x, y)
  y += yStep
}
```

This creates an L-shaped path that only moves in one direction at a time.

### Spawn Area Clearance

The new spawn area clearance:

```scala
// Create an entrance area around the spawn point (5x5 area)
val spawnArea = (for {
  dx <- -2 to 2
  dy <- -2 to 2
} yield Point(startPoint.x + dx, startPoint.y + dy)).toSet

// Remove both spawn and destination areas from obstacles
val obstaclesForThisPath = baseObstacles -- spawnArea -- destinationArea
```

This ensures the spawn point has a 5x5 clear area, matching the destination clearance.

## Expected Behavior

### A* Pathfinding Success
When A* succeeds, paths will:
- Navigate around obstacles (dungeons, villages, rocks)
- Prefer straight lines (due to direction change penalty in A* cost function)
- Be orthogonal (4-directional movement)

### Fallback to Direct Line
When A* fails (legitimate reasons):
- Paths will be **L-shaped** (horizontal then vertical)
- Paths will be **orthogonal** (no diagonals)
- Console will show diagnostic message explaining why A* failed

## Files Modified

1. **src/main/scala/map/PathGenerator.scala**
   - Fixed `findPathLine` to use orthogonal L-shaped pathfinding
   - Added diagnostic logging to `findPathAroundObstacles`
   - Added diagnostic logging to `generatePathAroundObstacles`

2. **src/main/scala/map/WorldMutator.scala**
   - Added spawn area clearance in `PathGenerationMutator`
   - Now clears obstacles around both spawn and destinations
   - Updated to use dungeon approach tiles instead of direct entrance routing
   - Added two-phase path generation (main paths + connecting paths)

3. **src/main/scala/map/Dungeon.scala**
   - Added `entranceSide` field to store entrance direction
   - Added `getApproachTile` helper function to calculate tile in front of entrance

4. **src/main/scala/map/DungeonGenerator.scala**
   - Updated to pass `entranceSide` from config to generated dungeon

5. **src/test/scala/map/PathfindingDiagnosticTest.scala** (NEW)
   - Test file to verify diagnostic logging works
   - Tests various pathfinding failure scenarios

## Build Status

✅ Compilation successful
✅ No errors introduced
⚠️  One pre-existing warning (deprecated wildcard in Game.scala - unrelated)

## Next Steps

1. **Test in Browser:** Load the game and visually verify paths are orthogonal
2. **Check Console:** Verify diagnostic messages show A* is succeeding more often
3. **Manual Gameplay:** Walk along paths to ensure they connect properly to dungeons/villages
4. **Monitor Performance:** Ensure A* isn't taking too long (should be fast with spawn area cleared)

## Rollback Instructions (if needed)

If issues arise, the changes can be reverted by:
```bash
git checkout HEAD -- src/main/scala/map/PathGenerator.scala
git checkout HEAD -- src/main/scala/map/WorldMutator.scala
sbt compile
```
# Pathfinding Diagonal Path Fix

## Issues Identified and Fixed

### 1. Diagonal Paths (Original Issue) ✅

**Problem:** Paths were being generated with diagonal movement instead of straight orthogonal lines.

**Root Cause:** The fallback `findPathLine` function used Bresenham's line algorithm, which can move diagonally by incrementing both X and Y coordinates in the same step.

**Solution:** Replaced Bresenham with an L-shaped orthogonal pathfinding algorithm that:
- Moves horizontally first (only changing X coordinate)
- Then moves vertically (only changing Y coordinate)  
- **Never creates diagonal steps** - only straight orthogonal lines

**File Changed:** `src/main/scala/map/PathGenerator.scala` - `findPathLine` method

### 2. A* Pathfinding Failures (Discovered Issue) ✅

**Problem:** A* pathfinding was failing and falling back to direct line generation, shown by these console logs:
```
A* pathfinding failed: No path found from 0:0 to -25:-55 (checked 38414 tiles)
Falling back to direct line from 0:0 to -25:-55
```

**Root Cause:** 
- Destinations (dungeon/village entrances) were placed in areas completely surrounded by obstacles
- The spawn point (0,0) had no cleared entrance area, making it easily blocked by obstacles
- A* would exhaust almost the entire map (38,414 out of 44,521 tiles) trying to find a path

**Solution:** Added spawn area clearance in `WorldMutator`:
- Now clears a 5x5 area around the spawn point (matching what was done for destinations)
- Removes obstacles from both spawn AND destination areas before pathfinding
- This prevents paths from being impossible to generate

**File Changed:** `src/main/scala/map/WorldMutator.scala` - `PathGenerationMutator` class

### 3. Dungeon Approach Tiles (Prevents Wall Cutting) ✅

**Problem:** Paths were being routed directly to dungeon entrance tiles, which could cause them to cut through dungeon walls.

**Root Cause:** The pathfinding system didn't account for the entrance orientation. It would route paths directly to the entrance tile, which is inside the dungeon structure. Depending on the path angle, this could cause paths to cut diagonally through dungeon walls.

**Solution:** Implemented a two-phase path generation approach:
1. **Main paths** route to "approach tiles" (tiles in front of dungeon entrances, outside the dungeon)
2. **Connecting paths** create short 2-tile links from approach tiles to actual dungeon entrances

This ensures:
- Main paths stay outside dungeon boundaries
- No paths can cut through dungeon walls
- Clear entrance from approach tile to dungeon
- Villages and shops retain direct path routing (unchanged)

**Files Changed:**
- `src/main/scala/map/Dungeon.scala` - Added `entranceSide` field and `getApproachTile` helper
- `src/main/scala/map/DungeonGenerator.scala` - Pass entranceSide through generation
- `src/main/scala/map/WorldMutator.scala` - Updated `PathGenerationMutator` to use approach tiles

### 4. Diagnostic Logging (Added for Debugging) ✅

**Added:** Comprehensive diagnostic logging to identify why pathfinding fails:
- `"A* pathfinding failed: Start point (x,y) is an obstacle"`
- `"A* pathfinding failed: Target point (x,y) is an obstacle"`
- `"A* pathfinding failed: Start point (x,y) is outside bounds"`
- `"A* pathfinding failed: Target point (x,y) is outside bounds"`
- `"A* pathfinding failed: No path found from (x1,y1) to (x2,y2) (checked N tiles)"`
- `"Falling back to direct line from (x1,y1) to (x2,y2)"`

**File Changed:** `src/main/scala/map/PathGenerator.scala` - `findPathAroundObstacles` and `generatePathAroundObstacles`

## Verification Steps

### 1. Build and Run the Game
```bash
sbt build
cd target/indigoBuild
python3 -m http.server 8080
```

Navigate to http://localhost:8080 in your browser.

### 2. Check Console Output

**Before Fix:**
```
A* pathfinding failed: No path found from 0:0 to -25:-55 (checked 38414 tiles)
Falling back to direct line from 0:0 to -25:-55
```
Multiple fallbacks to direct line, each checking 38,414 tiles (almost entire map).

**After Fix (Expected):**
- Fewer or no "Falling back to direct line" messages
- If fallbacks occur, they should be for legitimate reasons (not blocked spawn)
- Any fallback paths will now be orthogonal (L-shaped) instead of diagonal

### 3. Visual Verification in Game

When the game loads:
- **Paths (brown/tan tiles)** connecting spawn to dungeons/villages should be visible
- **Paths should be orthogonal** - straight horizontal and vertical lines, forming L or rectangular shapes
- **No diagonal paths** - paths should never go diagonally from one tile to the next
- **Bridges (wooden planks)** where paths cross rivers should also be orthogonal

## Technical Details

### Orthogonal Path Algorithm

The new `findPathLine` implementation:

```scala
// Move horizontally first
var x = start.x
while (x != target.x) {
  points += Point(x, start.y)
  x += xStep
}

// Then move vertically
var y = start.y
while (y != target.y) {
  points += Point(target.x, y)
  y += yStep
}
```

This creates an L-shaped path that only moves in one direction at a time.

### Dungeon Approach Tile Calculation

The new approach tile system:

```scala
// Calculate the tile in front of the dungeon entrance
def getApproachTile(entranceTile: Point, entranceSide: Direction): Point = {
  entranceSide match {
    case Direction.Up => Point(entranceTile.x, entranceTile.y - 1)    // One tile above
    case Direction.Down => Point(entranceTile.x, entranceTile.y + 1)  // One tile below
    case Direction.Left => Point(entranceTile.x - 1, entranceTile.y)  // One tile left
    case Direction.Right => Point(entranceTile.x + 1, entranceTile.y) // One tile right
  }
}

// Two-phase path generation
val mainPathTiles = destinations.flatMap { approachTile =>
  // Route main paths to approach tiles (outside dungeons)
  PathGenerator.generatePathAroundObstacles(...)
}

val dungeonConnectingPaths = worldMap.dungeons.flatMap { dungeon =>
  val entranceTile = Dungeon.roomToTile(dungeon.startPoint)
  val approachTile = Dungeon.getApproachTile(entranceTile, dungeon.entranceSide)
  // Create short 2-tile path: approach tile + entrance tile
  Set(approachTile, entranceTile)
}
```

This ensures dungeon entrances are accessible without paths cutting through walls.

### Spawn Area Clearance

The new spawn area clearance:

```scala
// Create an entrance area around the spawn point (5x5 area)
val spawnArea = (for {
  dx <- -2 to 2
  dy <- -2 to 2
} yield Point(startPoint.x + dx, startPoint.y + dy)).toSet

// Remove both spawn and destination areas from obstacles
val obstaclesForThisPath = baseObstacles -- spawnArea -- destinationArea
```

This ensures the spawn point has a 5x5 clear area, matching the destination clearance.

## Expected Behavior

### A* Pathfinding Success
When A* succeeds, paths will:
- Navigate around obstacles (dungeons, villages, rocks)
- Prefer straight lines (due to direction change penalty in A* cost function)
- Be orthogonal (4-directional movement)

### Fallback to Direct Line
When A* fails (legitimate reasons):
- Paths will be **L-shaped** (horizontal then vertical)
- Paths will be **orthogonal** (no diagonals)
- Console will show diagnostic message explaining why A* failed

## Files Modified

1. **src/main/scala/map/PathGenerator.scala**
   - Fixed `findPathLine` to use orthogonal L-shaped pathfinding
   - Added diagnostic logging to `findPathAroundObstacles`
   - Added diagnostic logging to `generatePathAroundObstacles`

2. **src/main/scala/map/WorldMutator.scala**
   - Added spawn area clearance in `PathGenerationMutator`
   - Now clears obstacles around both spawn and destinations

3. **src/test/scala/map/PathfindingDiagnosticTest.scala** (NEW)
   - Test file to verify diagnostic logging works
   - Tests various pathfinding failure scenarios

## Build Status

✅ Compilation successful
✅ No errors introduced
⚠️  One pre-existing warning (deprecated wildcard in Game.scala - unrelated)

## Next Steps

1. **Test in Browser:** Load the game and visually verify paths are orthogonal
2. **Check Console:** Verify diagnostic messages show A* is succeeding more often
3. **Manual Gameplay:** Walk along paths to ensure they connect properly to dungeons/villages
4. **Monitor Performance:** Ensure A* isn't taking too long (should be fast with spawn area cleared)

## Rollback Instructions (if needed)

If issues arise, the changes can be reverted by:
```bash
git checkout HEAD -- src/main/scala/map/PathGenerator.scala
git checkout HEAD -- src/main/scala/map/WorldMutator.scala
sbt compile
```
# Pathfinding Diagonal Path Fix

## Issues Identified and Fixed

### 1. Diagonal Paths (Original Issue) ✅

**Problem:** Paths were being generated with diagonal movement instead of straight orthogonal lines.

**Root Cause:** The fallback `findPathLine` function used Bresenham's line algorithm, which can move diagonally by incrementing both X and Y coordinates in the same step.

**Solution:** Replaced Bresenham with an L-shaped orthogonal pathfinding algorithm that:
- Moves horizontally first (only changing X coordinate)
- Then moves vertically (only changing Y coordinate)  
- **Never creates diagonal steps** - only straight orthogonal lines

**File Changed:** `src/main/scala/map/PathGenerator.scala` - `findPathLine` method

### 2. A* Pathfinding Failures (Discovered Issue) ✅

**Problem:** A* pathfinding was failing and falling back to direct line generation, shown by these console logs:
```
A* pathfinding failed: No path found from 0:0 to -25:-55 (checked 38414 tiles)
Falling back to direct line from 0:0 to -25:-55
```

**Root Cause:** 
- Destinations (dungeon/village entrances) were placed in areas completely surrounded by obstacles
- The spawn point (0,0) had no cleared entrance area, making it easily blocked by obstacles
- A* would exhaust almost the entire map (38,414 out of 44,521 tiles) trying to find a path

**Solution:** Added spawn area clearance in `WorldMutator`:
- Now clears a 5x5 area around the spawn point (matching what was done for destinations)
- Removes obstacles from both spawn AND destination areas before pathfinding
- This prevents paths from being impossible to generate

**File Changed:** `src/main/scala/map/WorldMutator.scala` - `PathGenerationMutator` class

### 3. Dungeon Approach Tiles (Prevents Wall Cutting) ✅

**Problem:** Paths were being routed directly to dungeon entrance tiles, which could cause them to cut through dungeon walls.

**Root Cause:** The pathfinding system didn't account for the entrance orientation. It would route paths directly to the entrance tile, which is inside the dungeon structure. Depending on the path angle, this could cause paths to cut diagonally through dungeon walls.

**Solution:** Implemented a two-phase path generation approach:
1. **Main paths** route to "approach tiles" (tiles in front of dungeon entrances, outside the dungeon)
2. **Connecting paths** create short 2-tile links from approach tiles to actual dungeon entrances

This ensures:
- Main paths stay outside dungeon boundaries
- No paths can cut through dungeon walls
- Clear entrance from approach tile to dungeon
- Villages and shops retain direct path routing (unchanged)

**Files Changed:**
- `src/main/scala/map/Dungeon.scala` - Added `entranceSide` field and `getApproachTile` helper
- `src/main/scala/map/DungeonGenerator.scala` - Pass entranceSide through generation
- `src/main/scala/map/WorldMutator.scala` - Updated `PathGenerationMutator` to use approach tiles

### 4. Diagnostic Logging (Added for Debugging) ✅

**Added:** Comprehensive diagnostic logging to identify why pathfinding fails:
- `"A* pathfinding failed: Start point (x,y) is an obstacle"`
- `"A* pathfinding failed: Target point (x,y) is an obstacle"`
- `"A* pathfinding failed: Start point (x,y) is outside bounds"`
- `"A* pathfinding failed: Target point (x,y) is outside bounds"`
- `"A* pathfinding failed: No path found from (x1,y1) to (x2,y2) (checked N tiles)"`
- `"Falling back to direct line from (x1,y1) to (x2,y2)"`

**File Changed:** `src/main/scala/map/PathGenerator.scala` - `findPathAroundObstacles` and `generatePathAroundObstacles`

## Verification Steps

### 1. Build and Run the Game
```bash
sbt build
cd target/indigoBuild
python3 -m http.server 8080
```

Navigate to http://localhost:8080 in your browser.

### 2. Check Console Output

**Before Fix:**
```
A* pathfinding failed: No path found from 0:0 to -25:-55 (checked 38414 tiles)
Falling back to direct line from 0:0 to -25:-55
```
Multiple fallbacks to direct line, each checking 38,414 tiles (almost entire map).

**After Fix (Expected):**
- Fewer or no "Falling back to direct line" messages
- If fallbacks occur, they should be for legitimate reasons (not blocked spawn)
- Any fallback paths will now be orthogonal (L-shaped) instead of diagonal

### 3. Visual Verification in Game

When the game loads:
- **Paths (brown/tan tiles)** connecting spawn to dungeons/villages should be visible
- **Paths should be orthogonal** - straight horizontal and vertical lines, forming L or rectangular shapes
- **No diagonal paths** - paths should never go diagonally from one tile to the next
- **Dungeon entrances should have clear approaches** - paths should lead to a tile in front of the dungeon entrance, not cut through walls
- **Short connecting paths** from approach tiles into dungeon entrances should be visible
- **Bridges (wooden planks)** where paths cross rivers should also be orthogonal

## Technical Details

### Orthogonal Path Algorithm

The new `findPathLine` implementation:

```scala
// Move horizontally first
var x = start.x
while (x != target.x) {
  points += Point(x, start.y)
  x += xStep
}

// Then move vertically
var y = start.y
while (y != target.y) {
  points += Point(target.x, y)
  y += yStep
}
```

This creates an L-shaped path that only moves in one direction at a time.

### Spawn Area Clearance

The new spawn area clearance:

```scala
// Create an entrance area around the spawn point (5x5 area)
val spawnArea = (for {
  dx <- -2 to 2
  dy <- -2 to 2
} yield Point(startPoint.x + dx, startPoint.y + dy)).toSet

// Remove both spawn and destination areas from obstacles
val obstaclesForThisPath = baseObstacles -- spawnArea -- destinationArea
```

This ensures the spawn point has a 5x5 clear area, matching the destination clearance.

## Expected Behavior

### A* Pathfinding Success
When A* succeeds, paths will:
- Navigate around obstacles (dungeons, villages, rocks)
- Prefer straight lines (due to direction change penalty in A* cost function)
- Be orthogonal (4-directional movement)

### Fallback to Direct Line
When A* fails (legitimate reasons):
- Paths will be **L-shaped** (horizontal then vertical)
- Paths will be **orthogonal** (no diagonals)
- Console will show diagnostic message explaining why A* failed

## Files Modified

1. **src/main/scala/map/PathGenerator.scala**
   - Fixed `findPathLine` to use orthogonal L-shaped pathfinding
   - Added diagnostic logging to `findPathAroundObstacles`
   - Added diagnostic logging to `generatePathAroundObstacles`

2. **src/main/scala/map/WorldMutator.scala**
   - Added spawn area clearance in `PathGenerationMutator`
   - Now clears obstacles around both spawn and destinations

3. **src/test/scala/map/PathfindingDiagnosticTest.scala** (NEW)
   - Test file to verify diagnostic logging works
   - Tests various pathfinding failure scenarios

## Build Status

✅ Compilation successful
✅ No errors introduced
⚠️  One pre-existing warning (deprecated wildcard in Game.scala - unrelated)

## Next Steps

1. **Test in Browser:** Load the game and visually verify paths are orthogonal
2. **Check Console:** Verify diagnostic messages show A* is succeeding more often
3. **Manual Gameplay:** Walk along paths to ensure they connect properly to dungeons/villages
4. **Monitor Performance:** Ensure A* isn't taking too long (should be fast with spawn area cleared)

## Rollback Instructions (if needed)

If issues arise, the changes can be reverted by:
```bash
git checkout HEAD -- src/main/scala/map/PathGenerator.scala
git checkout HEAD -- src/main/scala/map/WorldMutator.scala
sbt compile
```

