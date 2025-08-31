# Game Story DSL

The Game Story DSL provides an immutable, natural-language testing framework for GameController integration tests. It's built directly on the ECS component system for maintainable, component-centric testing.

## Overview

The DSL offers a fluent API that reads like a narrated play-through:

```scala
Given
  .thePlayerAt(4, 4)
  .withItems(potion)
  .beginStory()
  .thePlayer.hasHealth(5)
  .thePlayer.opensItems()
  .thePlayer.confirmsSelection()
  .thePlayer.hasHealth(10)
```

## Core Design Principles

### Immutable and Referentially Transparent
Each action returns a new GameStory instance, allowing safe branching and composition:

```scala
val story = Given.thePlayerAt(4, 4).beginStory()
val afterMoving = story.thePlayer.moves(Direction.Up)
val afterHealing = story.thePlayer.opensItems().thePlayer.confirmsSelection()
// Both branches are independent
```

### Component-Centric
Tests reuse the ECS APIs (`Entity.get[T]`, `Entity.update[T]`) to avoid proliferation of bespoke test methods:

```scala
// Generic component assertions
.thePlayer.component[Health].satisfies(_.baseCurrent == 5)
.enemy("goblin").component[Movement].is(Movement(Point(3, 3)))

// Generic component modifications in setup
Given.thePlayerAt(4, 4)
  .modifyPlayer[Initiative](_.copy(maxInitiative = 10))
  .setEntityComponent("enemy1", Health(50))
```

### Natural Language Structure
Tests follow Given/When/Then patterns but with a unified fluent interface:

- **Given**: World setup using component-based builders
- **When/Actions**: Player movements, item usage, cursor control
- **Then/Assertions**: Component checks, entity state validation

## Key Classes

### GameStory
The immutable harness that advances the game state deterministically:

```scala
final case class GameStory(controller: GameController, tick: Int)
```

- `step(input)`: Advances one frame with input
- `timePasses(ticks)`: Advances multiple frames without input
- Provides both action methods (thePlayer.moves) and assertion methods (thePlayer.hasHealth)

### GivenWorld
Component-based world builder:

```scala
Given.thePlayerAt(4, 4)
  .modifyPlayer[Health](h => h.copy(baseCurrent = 5))
  .withItems(potion, scroll)
  .withEntities(enemies*)
  .beginStory()
```

## Action Methods

### Player Actions
- `thePlayer.moves(direction, steps)`: Move player
- `thePlayer.opensItems()`: Enter item usage menu
- `thePlayer.confirmsSelection()`: Confirm current selection

### Cursor Actions
- `cursor.moves(direction, times)`: Move targeting cursor
- `cursor.confirm()`: Confirm target selection

### Time Control
- `timePasses(ticks)`: Advance game time for system updates

## Assertion Methods

### Player Assertions
- `thePlayer.isAt(x, y)`: Assert player position
- `thePlayer.hasHealth(hp)`: Assert player health
- `thePlayer.component[C]`: Generic component assertions

### Entity Assertions
- `enemy(id).hasHealth(hp)`: Assert enemy health
- `entity(id).component[C]`: Generic component assertions
- `entityMissing(id)`: Assert entity no longer exists

### Game State Assertions
- `projectilesAre(count)`: Assert projectile count
- `uiIsScrollTargetAt(x, y)`: Assert UI targeting state

## Example Tests

### Basic Movement
```scala
test("Player moves when given a move action") {
  Given
    .thePlayerAt(4, 4)
    .beginStory()
    .thePlayer.moves(Direction.Up)
    .thePlayer.isAt(4, 3)
}
```

### Item Usage with Targeting
```scala
test("Using a fireball scroll creates a projectile after targeting") {
  val scroll = items.scroll("test-scroll-1")
  
  Given
    .thePlayerAt(4, 4)
    .withItems(scroll)
    .beginStory()
    .thePlayer.opensItems()
    .thePlayer.confirmsSelection() // select the scroll
    .cursor.moves(Direction.Up, 3) // move target cursor up
    .uiIsScrollTargetAt(4, 1)
    .projectilesAre(0)
    .cursor.confirm() // confirm target
    .projectilesAre(1)
}
```

### Component-Based Assertions
```scala
test("Initiative decreases over time") {
  Given
    .thePlayerAt(4, 4)
    .modifyPlayer[Initiative](_.copy(maxInitiative = 1, currentInitiative = 1))
    .beginStory()
    .timePasses(1)
    .thePlayer.component[Initiative].is(Initiative(1, 0))
}
```

## Benefits

1. **Maintainable**: Component-centric approach means new components automatically work with existing test infrastructure
2. **Readable**: Natural language flow makes tests serve as living documentation
3. **Flexible**: Immutable design allows branching test scenarios and composition
4. **Type-Safe**: Generic component methods provide compile-time safety
5. **Extensible**: New actions or assertions can be added without breaking existing tests

## Migration from Existing Tests

The DSL can coexist with existing test suites. Migrate incrementally:

1. Create new tests using the DSL for new features
2. Replace complex setup code with Given builders
3. Replace assertion chains with fluent component checks
4. Keep both test suites running during transition