# Scala Roguelike 2025

A 2D roguelike game built with Scala 3.6.4, ScalaJS, and the Indigo game engine. Features turn-based combat, dungeon exploration, inventory management, equipment system, and line-of-sight mechanics. Compiles to both web (JavaScript) and desktop (Electron) versions.

## 🎮 What is this?

Scala Roguelike 2025 is a classic roguelike dungeon crawler featuring:

- **Turn-Based Combat**: Strategic initiative-based combat system
- **Procedural Dungeons**: Randomly generated dungeons with rooms, corridors, and locked doors
- **Inventory Management**: Collect and use items like potions, scrolls, and arrows
- **Equipment System**: Helmets and armor provide damage reduction
- **Line of Sight**: Realistic fog of war and exploration memory
- **Entity Component System**: Modern game architecture with flexible component-based entities
- **Web & Desktop**: Runs in browsers via WebGL and as desktop app via Electron

### Game Features

- **Movement**: Use arrow keys to navigate dungeons
- **Combat**: Automatic turn-based combat when encountering enemies
- **Items**: 
  - Potions for healing
  - Scrolls for ranged fireball attacks
  - Bow and arrows for ranged combat
  - Keys to unlock colored doors
- **Equipment**:
  - Helmets (Leather, Iron) for head protection
  - Armor (Chainmail, Plate) for body protection
- **Progression**: Experience system with level-ups

## 🚀 Quick Start

### Prerequisites

- **Java 17+** (check with `java -version`)
- **SBT 1.10.2** (will be installed automatically below)

### Setup

1. **Clone the repository**:
   ```bash
   git clone https://github.com/kabuumu/scala-roguelike-2025.git
   cd scala-roguelike-2025
   ```

2. **Install SBT** (if not already installed):
   ```bash
   curl -fLo sbt-1.10.2.zip https://github.com/sbt/sbt/releases/download/v1.10.2/sbt-1.10.2.zip
   unzip sbt-1.10.2.zip
   export PATH=$PATH:$(pwd)/sbt/bin
   ```

3. **Build and run the game**:
   ```bash
   # Build web version
   sbt build
   
   # Serve the game locally
   cd target/indigoBuild
   python3 -m http.server 8080
   ```

4. **Play the game**:
   - Open http://localhost:8080 in your browser
   - Use arrow keys to move
   - Click on the game canvas to ensure input focus

### Alternative: Desktop Version

```bash
# Build and attempt to run desktop version (requires Electron)
sbt runGame
```

*Note: Desktop version may not work in all environments; web version is recommended.*

## 🛠 Build & Test

### Prerequisites

- **Java 17+** (check with `java -version`)
- **SBT 1.10.2** (install if needed):
  ```bash
  curl -fLo sbt-1.10.2.zip https://github.com/sbt/sbt/releases/download/v1.10.2/sbt-1.10.2.zip
  unzip sbt-1.10.2.zip
  export PATH=$PATH:$(pwd)/sbt/bin
  ```

### Core Commands

```bash
# Build and compilation
sbt compile                    # Compile Scala code (~30 seconds)
sbt build                      # Build web version (~40 seconds)
sbt runGame                    # Build and run (web + attempt desktop)

# Testing
sbt test                       # Run all tests (~30 seconds, 128 tests)
sbt "testOnly ui.GameControllerTest"  # Run specific test suite

# Coverage analysis
python3 scripts/analyze_coverage.py  # Custom coverage analysis (recommended)
sbt testCoverage              # Standard scoverage (limited ScalaJS support)
```

### Test Coverage

The project maintains **54.1% estimated code coverage** across **128 tests** in **14 test suites**:

- **MovementSystemTest** (7 tests): Player movement and collision detection
- **GameControllerStoryTest** (6 tests): Game logic and player actions  
- **EntityTest** (10 tests): Entity component system functionality
- **InitiativeSystemTest** (2 tests): Turn-based initiative system
- **EquipmentSystemTest** (19 tests): Equipment and inventory mechanics
- **MapGeneratorTest** (23 tests): Dungeon generation with various configurations
- **And 8 additional test suites** covering pathfinding, combat, and UI systems

#### Coverage Analysis

Due to ScalaJS compilation limitations with standard coverage tools (scoverage, JaCoCo), this project uses a custom Python analysis script:

```bash
# Recommended: Custom analysis script (works reliably with ScalaJS)
python3 scripts/analyze_coverage.py

# Limited: Standard scoverage (may fail during ScalaJS linking)
sbt testCoverage
```

**Known Limitations:**
- Standard scoverage instruments JVM bytecode, not JavaScript output
- ScalaJS linking can fail with instrumentation enabled
- Custom script provides reliable estimates based on test file analysis

For detailed coverage metrics and improvement goals, see [`COVERAGE.md`](COVERAGE.md).

### Build Configuration

The `build.sbt` configures:
- **Scala 3.6.4** with ScalaJS plugin for web compilation
- **Indigo 0.22.0** game engine for rendering and input
- **ScalaTest 3.2.19** for comprehensive testing framework
- **Custom asset pipeline** for embedding fonts and processing sprites

### Project Structure

```
scala-roguelike-2025/
├── src/
│   ├── main/scala/
│   │   ├── indigoengine/     # Game engine integration
│   │   │   ├── Game.scala    # Main game entry point
│   │   │   └── view/         # UI rendering
│   │   ├── game/             # Core game logic
│   │   │   ├── entity/       # Entity component system
│   │   │   ├── system/       # Game systems (movement, combat, etc.)
│   │   │   └── event/        # Game events
│   │   ├── ui/               # User interface and input
│   │   ├── map/              # Dungeon generation
│   │   └── util/             # Utilities (pathfinding, line of sight)
│   └── test/scala/           # Test suites (60 tests)
├── assets/                   # Game assets
│   ├── sprites/              # Sprite sheets
│   └── fonts/                # Pixel fonts
├── scripts/                  # Build and analysis scripts
│   └── analyze_coverage.py   # Custom coverage analyzer
├── target/indigoBuild/       # Web build output
└── build.sbt                 # Build configuration
```

### Asset Pipeline

The build system automatically:

- Embeds pixel fonts from TTF files
- Generates sprite configurations
- Processes asset directories
- Creates optimized JavaScript bundles

## 🧪 Development

For complete build and testing instructions, see the [Build & Test](#-build--test) section above.

### Development Workflow

1. **Make changes** to Scala source files
2. **Test compilation**: `sbt compile`
3. **Run tests**: `sbt test`
4. **Check coverage**: `python3 scripts/analyze_coverage.py`
5. **Build web version**: `sbt build`
6. **Test in browser**: Serve from `target/indigoBuild/` with `python3 -m http.server 8080`

### Code Quality

The project maintains high code quality with:

- Comprehensive test suite (128 tests across 14 suites)
- Entity Component System architecture
- Functional programming principles
- Type-safe Scala 3 features

*Note: Some compilation warnings for pattern matching are expected and non-blocking.*

## 🚢 Deployment

### CI/CD Pipeline

The project uses GitHub Actions for automated deployment:

1. **Continuous Integration** (`.github/workflows/scala.yml`):
   - Runs on every push to main branch
   - Tests with Java 11+ and SBT
   - Validates all tests pass

2. **Pull Request Preview** (`.github/workflows/pr-preview.yml`):
   - Builds playable preview for every pull request
   - Deploys to GitHub Pages with PR-specific URL pattern
   - Automatically comments with direct preview link

3. **Production Deployment** (`.github/workflows/deploy.yml`):
   - Deploys main game to https://kabuumu.github.io/scala-roguelike-2025/
   - Runs on merges to main branch
   - Preserves PR preview deployments

4. **Preview Cleanup** (`.github/workflows/pr-cleanup.yml`):
   - Automatically removes PR previews when PRs are closed
   - Keeps GitHub Pages clean and organized

### Manual Deployment

To deploy manually:

```bash
# Build optimized version
sbt build

# Deploy contents of target/indigoBuild/ to your web server
```

## 🎯 Game Controls

- **Arrow Keys**: Move player character
- **Q**: Equip nearby equipment
- **Auto**: Items are automatically picked up when walked over
- **Auto**: Combat happens automatically when encountering enemies

## 🏗 Architecture

### Entity Component System

The game uses a modern ECS architecture:

- **Entities**: Game objects with unique IDs
- **Components**: Data containers (Health, Movement, Inventory, etc.)
- **Systems**: Logic processors (MovementSystem, CombatSystem, etc.)

### Core Systems

- **MovementSystem**: Handles player and enemy movement
- **CombatSystem**: Turn-based combat with initiative
- **InventorySystem**: Item pickup and management
- **EquipmentSystem**: Equipment slots and damage reduction
- **EnemyAISystem**: Basic AI for enemy behavior
- **LineOfSight**: Realistic visibility and fog of war

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Follow the [Development Workflow](#development-workflow) to test changes
5. Submit a pull request

### Pull Request Previews

When you submit a pull request:

1. **Automatic Build**: GitHub Actions will automatically build your changes
2. **Live Preview**: Your changes are deployed to a unique URL: `https://kabuumu.github.io/scala-roguelike-2025/pr-{number}/`
3. **Direct Access**: The bot will comment on your PR with a direct link to play the game immediately
4. **Auto-updates**: Preview is updated automatically when you push new commits
5. **Auto-cleanup**: Preview is removed when the PR is closed

**Example**: PR #123 → https://kabuumu.github.io/scala-roguelike-2025/pr-123/

This enables instant testing of changes without any downloads or local setup required.

### Coverage Requirements

- Maintain minimum **54% overall coverage** (current baseline)
- New features should include appropriate tests
- Critical systems (game/system, ui) should maintain high coverage
- Use `python3 scripts/analyze_coverage.py` to verify coverage levels

## 📜 License

This project is available under the [MIT License](LICENSE).

## 🔗 Links

- **Play Online**: [GitHub Pages](https://kabuumu.github.io/scala-roguelike-2025/) (when deployed)
- **Repository**: https://github.com/kabuumu/scala-roguelike-2025
- **Indigo Engine**: https://indigoengine.io/
- **Scala**: https://scala-lang.org/

---

Built with ❤️ using Scala 3.6.4 and Indigo Game Engine