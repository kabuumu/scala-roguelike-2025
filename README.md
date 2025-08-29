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

## 🛠 Build System

### SBT Commands

The project uses SBT (Scala Build Tool) with custom commands:

- **`sbt compile`** - Compile the Scala code (~30 seconds)
- **`sbt test`** - Run all tests (~30 seconds, 57 tests)
- **`sbt build`** - Build web version (~40 seconds)
- **`sbt runGame`** - Build and run (web + attempt desktop)
- **`sbt testCoverage`** - Run tests with coverage instrumentation

### Build Configuration

The `build.sbt` file configures:

- **Scala 3.6.4** with ScalaJS plugin
- **Indigo 0.22.0** game engine
- **Custom asset pipeline** for sprites and fonts
- **ScalaTest 3.2.19** for testing
- **sbt-scoverage 2.2.2** for code coverage

### Code Coverage

The project maintains **49.0% estimated code coverage** with comprehensive testing across core systems:

#### Coverage by Area:
- **game/system**: ~100% (773 lines, 299 test lines) - Combat, movement, equipment systems
- **ui**: ~100% (200 lines, 203 test lines) - User interface and input handling
- **game/entity**: ~40% (521 lines, 70 test lines) - Entity component system
- **map**: ~9% (478 lines, 15 test lines) - Dungeon generation
- **util**: ~0% (91 lines, 0 test lines) - Pathfinding and utilities
- **indigoengine**: ~0% (445 lines, 0 test lines) - Engine integration

#### Coverage Commands:
```bash
# Run custom coverage analysis (works with ScalaJS)
python3 scripts/analyze_coverage.py

# Standard scoverage (limited ScalaJS support)
sbt testCoverage
```

*Note: Standard scoverage has limitations with ScalaJS compilation. The custom analysis script provides reliable coverage estimates for this project.*

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
│   └── test/scala/           # Test suites (57 tests)
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

### Running Tests

```bash
# Run all tests (57 tests in 5 suites)
sbt test

# Run specific test suite
sbt "testOnly ui.GameControllerTest"

# Run coverage analysis
python3 scripts/analyze_coverage.py

# Attempt standard coverage (limited ScalaJS support)
sbt testCoverage
```

### Test Coverage

The project maintains **49.0% overall code coverage** with comprehensive test suites:

- **EntityTest**: Component system tests (10 tests)
- **GameControllerTest**: Game logic and player actions (6 tests)  
- **PathfinderTest**: A* pathfinding algorithm (4 tests)
- **EquipmentSystemTest**: Equipment and inventory mechanics (19 tests)
- **MapGeneratorTest**: Dungeon generation with various configurations (18 tests)

**Coverage Breakdown:**
- Core game systems (game/system): ~100% coverage
- User interface (ui): ~100% coverage  
- Entity component system (game/entity): ~40% coverage
- Map generation (map): ~9% coverage
- Utilities and engine integration: Limited coverage

### Code Quality

The project maintains high code quality with:

- Comprehensive test suite (57 tests)
- Entity Component System architecture
- Functional programming principles
- Type-safe Scala 3 features

*Note: Some compilation warnings for pattern matching are expected and non-blocking.*

## 🚢 Deployment

### CI/CD Pipeline

The project uses GitHub Actions for:

1. **Continuous Integration** (`.github/workflows/scala.yml`):
   - Runs on every push and pull request
   - Tests with Java 11+ and SBT
   - Validates all tests pass

2. **Continuous Deployment** (`.github/workflows/deploy.yml`):
   - Deploys to GitHub Pages on main branch pushes
   - Builds optimized JavaScript bundle
   - Serves the game at GitHub Pages URL

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
4. Run tests: `sbt test`
5. Check coverage: `python3 scripts/analyze_coverage.py`
6. Build and test web version: `sbt build`
7. Submit a pull request

### Development Workflow

1. **Make changes** to Scala source files
2. **Test compilation**: `sbt compile`
3. **Run tests**: `sbt test`
4. **Check coverage**: `python3 scripts/analyze_coverage.py`
5. **Build web version**: `sbt build`
6. **Test in browser**: Serve from `target/indigoBuild/`

### Coverage Requirements

- Maintain minimum **49% overall coverage** (current baseline)
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