# Scala Roguelike 2025

A 2D roguelike game built with Scala 3.6.4, ScalaJS, and the Indigo game engine. The game features turn-based combat, dungeon exploration, inventory management, equipment system, and line-of-sight mechanics. Compiles to both web (JavaScript) and desktop (Electron) versions.

Always reference these instructions first and fallback to search or bash commands only when you encounter unexpected information that does not match the info here.

## Working Effectively

### Prerequisites and Setup
- **Java 17+** is required (use `java -version` to check)
- **SBT 1.10.2** must be installed manually:
  ```bash
  curl -fLo sbt-1.10.2.zip https://github.com/sbt/sbt/releases/download/v1.10.2/sbt-1.10.2.zip
  unzip sbt-1.10.2.zip
  export PATH=$PATH:$(pwd)/sbt/bin
  ```

### Build Commands (NEVER CANCEL - Wait for completion)
- **Compile**: `sbt compile` -- takes 30 seconds. NEVER CANCEL. Set timeout to 60+ seconds.
- **Run Tests**: `sbt test` -- takes 30 seconds, runs 57 tests. NEVER CANCEL. Set timeout to 60+ seconds.
- **Build Web Version**: `sbt build` -- takes 40 seconds total. NEVER CANCEL. Set timeout to 90+ seconds.
- **Coverage Analysis**: `python3 scripts/analyze_coverage.py` -- analyzes test coverage, takes 5 seconds.
- **Coverage Report (Limited)**: `sbt testCoverage` -- limited due to ScalaJS compatibility issues.
- **Build and Run (Web Only)**: `sbt runGame` -- builds and attempts to run via Electron. The Electron portion will fail in sandboxed environments, but the web build works perfectly.

### Manual Validation Steps
After making changes, ALWAYS validate by running this complete scenario:
1. `sbt compile` -- ensure compilation succeeds (some warnings are normal)
2. `sbt test` -- ensure all 57 tests pass
3. `python3 scripts/analyze_coverage.py` -- check coverage hasn't decreased significantly
4. `sbt build` -- creates web version in `target/indigoBuild/`
5. Serve and test the web game:
   ```bash
   cd target/indigoBuild
   python3 -m http.server 8080
   ```
6. Navigate to http://localhost:8080 in browser
7. Test basic gameplay: arrow keys for movement, check UI elements (health bar, equipment panel, inventory)
8. Verify game mechanics: player movement, enemy interaction, dungeon visibility

## Project Structure

### Key Directories
- `src/main/scala/` -- Main game source code
  - `indigoengine/Game.scala` -- Main game entry point and rendering
  - `game/` -- Core game logic (entities, systems, state)
  - `ui/` -- User interface and input handling
  - `map/` -- Dungeon generation and navigation
  - `util/` -- Utility functions (pathfinding, line of sight)
- `src/test/scala/` -- Test suites (57 tests total)
- `assets/` -- Game assets (sprites, fonts)
- `scripts/` -- Coverage analysis and build tools
- `target/indigoBuild/` -- Web build output
- `build.sbt` -- Build configuration with custom commands
- `COVERAGE.md` -- Detailed coverage documentation and metrics

### Important Files to Check After Changes
- **Game Logic Changes**: Always test `GameControllerTest` and run a manual game session
- **Entity System Changes**: Check `EntityTest` and `EquipmentSystemTest`
- **UI Changes**: Test in browser - UI elements are in `indigoengine/view/Elements.scala`
- **Build Changes**: Validate both `sbt compile` and `sbt build` work

## Common Development Tasks

### Running the Game
- **Web Version (Recommended)**: Use `sbt build` then serve from `target/indigoBuild/`
- **Desktop Version**: `sbt runGame` will fail in sandboxed environments but works in local development

### Testing
- **All Tests**: `sbt test` (30 seconds, 57 tests)
- **Specific Test**: `sbt "testOnly ui.GameControllerTest"`
- **Test Categories**: EntityTest, PathfinderTest, GameControllerTest, EquipmentSystemTest, MapGeneratorTest

### Code Coverage
- **Coverage Analysis**: `python3 scripts/analyze_coverage.py` -- comprehensive coverage report (5 seconds)
- **Coverage Baseline**: Project maintains **49.0% estimated coverage** across 2,928 lines of source code
- **Coverage Report (Limited)**: `sbt testCoverage` -- has ScalaJS compatibility issues, use Python script instead
- **Coverage Documentation**: See `COVERAGE.md` for detailed metrics and improvement areas
- **High Coverage Areas**: game/system (~100%), ui (~100%) - core game mechanics are well-tested  
- **Low Coverage Areas**: util (0%), map (9%) - focus areas for improvement
- **Coverage Goal**: Maintain 49%+ baseline, target critical paths and new features

### Common Build Issues
- **Missing SBT**: Install SBT 1.10.2 manually as shown above
- **Java Version**: Requires Java 17+, check with `java -version`
- **Compilation Warnings**: Pattern matching and type inference warnings are normal and non-blocking
- **Electron Failures**: Expected in sandboxed environments; web version always works
- **Coverage Tool Limitations**: Standard coverage tools (scoverage, JaCoCo) have ScalaJS compatibility issues; use custom Python script

## Build Timing and Timeouts
- **Compile**: ~30 seconds → Set 60+ second timeout
- **Test**: ~30 seconds → Set 60+ second timeout  
- **Build**: ~40 seconds → Set 90+ second timeout
- **CRITICAL**: NEVER CANCEL builds or tests early. Scala compilation can appear to hang but is processing.

## Game Architecture

### Core Systems
- **Entity Component System**: Entities have components (Health, Movement, Inventory, etc.)
- **Game Systems**: MovementSystem, CombatSystem, InventorySystem, EquipmentSystem
- **Turn-Based Logic**: Initiative system controls turn order
- **UI States**: Move, UseItem, ScrollSelect, ListSelect for different interaction modes

### Key Game Features to Test
- **Movement**: Arrow keys move player, camera follows
- **Combat**: Player and enemies take turns based on initiative
- **Inventory**: Items can be picked up and used
- **Equipment**: Helmets and armor provide damage reduction
- **Line of Sight**: Fog of war and explored area memory
- **Dungeon**: Procedurally generated with rooms, corridors, locked doors

## CI/CD Information
- **GitHub Actions**: `.github/workflows/scala.yml` runs tests and coverage analysis
- **Deployment**: `.github/workflows/deploy.yml` builds and deploys to GitHub Pages
- **Build Requirements**: Java 11+ in CI, SBT installation handled automatically
- **Coverage Integration**: CI runs custom coverage analysis script and validates baseline

## Troubleshooting
- **Game Won't Load**: Check browser console for asset loading errors
- **Black Screen**: Assets may not be loading; rebuild with `sbt build`
- **Input Not Working**: Ensure game canvas has focus in browser
- **Performance Issues**: Game targets 60fps; check browser dev tools for WebGL issues

## Development Guidelines
- **Always Build and Test**: Compile and test changes before committing
- **Coverage Validation**: Run `python3 scripts/analyze_coverage.py` before PRs to ensure coverage doesn't decrease
- **Manual Validation Required**: The web game MUST be tested manually after changes
- **No Linting Tools**: Project does not use scalafmt or scalafix
- **Test Coverage**: 57 comprehensive tests covering 49% of codebase with focus on core game mechanics
- **Expected Warnings**: Some pattern matching and type warnings are normal
- **Coverage Focus**: Prioritize testing critical paths and new features; existing high-coverage areas (systems, UI) set the standard

Remember: This is a real-time rendered game with complex state management. Always validate changes by playing the actual game to ensure mechanics work correctly.

## Code Coverage Tools and Validation

### Coverage Analysis Commands
The project uses a custom Python script for coverage analysis due to ScalaJS compatibility limitations:

```bash
# Primary coverage analysis (recommended)
python3 scripts/analyze_coverage.py

# Limited coverage via SBT (has ScalaJS issues)
sbt testCoverage
```

### Current Coverage Metrics
- **Overall Coverage**: 49.0% across 2,928 lines of source code
- **Test-to-Source Ratio**: 0.21 (628 test lines / 2,928 source lines)
- **Well-Covered Areas**: game/system (~100%), ui (~100%)
- **Low-Coverage Areas**: util (0%), map (9%), game/entity (40%)

### Coverage Validation Workflow
When making changes, especially to low-coverage areas:

1. **Before Changes**: Run `python3 scripts/analyze_coverage.py` to establish baseline
2. **After Changes**: Re-run coverage analysis to ensure no significant decrease
3. **Add Tests**: For new features in low-coverage areas (util, map, entity packages)
4. **Focus on Critical Paths**: Prioritize testing game-breaking functionality

### ScalaJS Coverage Limitations
- **Standard Tools Don't Work**: JaCoCo, standard scoverage have JVM/bytecode dependency
- **Custom Solution**: Python script analyzes source files and test coverage heuristically  
- **CI Integration**: GitHub Actions runs coverage analysis and validates 49%+ baseline
- **Known Issue**: `sbt testCoverage` may fail during ScalaJS linking phase

### Coverage Expectations for Contributors
- **Maintain Baseline**: Don't decrease overall coverage below 49%
- **New Features**: Include appropriate tests, especially for core game logic
- **High-Risk Areas**: Any changes to combat, movement, or entity systems must have tests
- **Documentation**: Refer to `COVERAGE.md` for detailed area-specific metrics

### Coverage Interpretation
The custom analyzer provides estimates based on:
- Lines of code analysis per package
- Test file coverage mapping
- Heuristic ratios (3:1 test-to-source coverage assumption)
- Manual validation against known well-tested areas

## Validation

### Measured Build Times and Commands
All build commands have been validated and measured on this system:

- **Compile**: `sbt compile` takes ~33 seconds (includes downloading dependencies on first run)
- **Tests**: `sbt test` takes ~31 seconds and runs 57 tests (not 56 as previously documented)
- **Build**: `sbt build` takes ~16 seconds (faster than documented estimate)
- **Specific Tests**: `sbt "testOnly ui.GameControllerTest"` takes ~11 seconds

### Proven Working Commands
These exact commands have been tested and work correctly:

```bash
# Setup (run once)
curl -fLo sbt-1.10.2.zip https://github.com/sbt/sbt/releases/download/v1.10.2/sbt-1.10.2.zip
unzip sbt-1.10.2.zip
export PATH=$PATH:$(pwd)/sbt/bin

# Build and test sequence (always run this for validation)
sbt compile
sbt test
python3 scripts/analyze_coverage.py
sbt build

# Serve the web game for manual testing
cd target/indigoBuild
python3 -m http.server 8080
# Open http://localhost:8080 in browser
# Test arrow keys for movement, verify UI elements load
```

### Web Game Validation Checklist
When testing the web game at http://localhost:8080, verify:
- [ ] Game loads without errors (check browser console)
- [ ] Indigo engine initializes (look for "Game initialisation succeeded" log)
- [ ] Dungeon generates (watch for "Generating Dungeon" and "Completed dungeon" logs)
- [ ] Player character visible (white figure in center)
- [ ] Health bar shows "100/100" in top left
- [ ] Equipment panel visible on right side (shows "Helmet" and "Armor" slots)
- [ ] Enemies visible (white skull-like figures)
- [ ] Blue floor tiles/rooms visible
- [ ] Arrow keys respond to move player
- [ ] Camera follows player movement

### Desktop Version Limitations
`sbt runGame` will fail in sandboxed environments with this error:
```
The SUID sandbox helper binary was found, but is not configured correctly
```
This is expected and normal. The web version always works and is the recommended development target.

## Common Tasks Reference

### File Structure at Repository Root
```
.
├── .git/
├── .github/
│   ├── copilot-instructions.md    # This file
│   └── workflows/
│       ├── deploy.yml             # GitHub Pages deployment
│       └── scala.yml              # CI tests and coverage
├── .gitignore
├── COVERAGE.md                    # Detailed coverage metrics and goals
├── README.md                      # Project documentation
├── assets/                        # Game sprites and fonts
│   ├── fonts/
│   └── sprites/
├── build.sbt                      # SBT build configuration
├── project/                       # SBT plugins
├── scripts/                       # Build and analysis tools
│   └── analyze_coverage.py        # Custom coverage analysis script
├── src/
│   ├── main/scala/                # Game source code
│   │   ├── data/                  # Data definitions
│   │   ├── game/                  # Core game logic
│   │   ├── indigoengine/          # Game engine integration
│   │   ├── map/                   # Dungeon generation
│   │   ├── ui/                    # User interface
│   │   └── util/                  # Utilities
│   └── test/scala/                # Test suites
│       ├── game/                  # Game logic tests
│       ├── map/                   # Map tests
│       └── ui/                    # UI tests
└── target/                        # Build output
    ├── indigoBuild/               # Web game files
    └── scala-3.6.4/              # Compiled Scala
```

### Expected Build Warnings
The following warnings are normal and should not be treated as errors:
- Pattern matching exhaustivity warnings
- Line indentation warnings in AttackSystem.scala
- Type test warnings for runtime checks
- Deprecation warnings

### Performance and System Requirements
- **Target FPS**: 60fps in web browsers
- **WebGL**: Requires WebGL 2.0 (falls back to WebGL 1.0)
- **Memory**: Game loads 3 assets and generates dungeons at runtime
- **Browser Compatibility**: Modern browsers with WebGL support