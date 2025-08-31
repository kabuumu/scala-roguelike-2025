# Components in DSL

The Game Story DSL is designed around the ECS (Entity-Component-System) architecture. This document explains how components integrate with the testing framework.

## Component-Centric Philosophy

Instead of creating bespoke test methods for every entity attribute, the DSL reuses the existing ECS APIs:

```scala
// Instead of many specific methods:
// .thePlayer.hasMaxHealth(100)
// .thePlayer.hasCurrentHealth(75)
// .thePlayer.hasHealthModifier(5)

// Use generic component access:
.thePlayer.component[Health].satisfies(h => h.baseMax == 100)
.thePlayer.component[Health].satisfies(h => h.baseCurrent == 75)
.thePlayer.hasHealth(80) // convenience for currentHealth extension method
```

## Component Access Patterns

### Getting Components
```scala
// Type-safe component retrieval
.thePlayer.component[Health]        // ComponentExpect[Health]
.enemy("orc").component[Movement]   // ComponentExpect[Movement]
.entity("item1").component[CanPickUp] // ComponentExpect[CanPickUp]
```

### Component Assertions
```scala
// Exact equality
.thePlayer.component[Initiative].is(Initiative(10, 5))

// Predicate-based with custom error messages
.thePlayer.component[Health].satisfies(_.baseCurrent > 0, "player should be alive")

// Existence check
.enemy("ghost").component[Drawable].exists()

// Value extraction for complex assertions
val health = story.thePlayer.component[Health].value
assert(health.baseCurrent + health.baseMax == 200)
```

## World Setup with Components

### Player Modification
```scala
Given.thePlayerAt(4, 4)
  // Modify existing component
  .modifyPlayer[Health](h => h.copy(baseCurrent = 5))
  .modifyPlayer[Initiative](_.copy(maxInitiative = 15, currentInitiative = 0))
  
  // Replace component entirely
  .setPlayer[Experience](Experience(currentExperience = 1000))
```

### Entity Modification
```scala
Given.thePlayerAt(4, 4)
  .withEntities(enemy)
  // Modify by entity ID
  .modifyEntity[Health]("orc1")(_.copy(baseMax = 200))
  .setEntityComponent("orc1", Initiative(20, 10))
```

## Supported Components

The DSL works with any component that extends the `Component` trait. Common components include:

### Core Components
- **Health**: `baseCurrent`, `baseMax`, plus extensions like `currentHealth`, `maxHealth`
- **Movement**: `position` for entity location
- **Initiative**: `maxInitiative`, `currentInitiative`, `isReady`
- **Inventory**: Item storage and weapon slots
- **Experience**: Current experience and level progression

### Behavior Components  
- **EntityTypeComponent**: Player, Enemy, Projectile, etc.
- **Drawable**: Sprite information for rendering
- **Hitbox**: Collision detection
- **SightMemory**: What the entity has seen

### Item Components
- **WeaponItem**: Damage and weapon type (Melee/Ranged)
- **PotionItem**: Healing items
- **ScrollItem**: Spell scrolls
- **CanPickUp**: Items that can be collected

### Advanced Components
- **DeathEvents**: Actions triggered on entity death
- **Equipment**: Worn armor and helmets
- **MarkedForDeath**: Entities scheduled for removal

## Extension Methods

Many components provide extension methods on Entity that can be used in assertions:

```scala
// Health extensions
.thePlayer.hasHealth(75)         // Uses currentHealth extension
entity.heal(10)                  // Uses heal extension  
entity.damage(5, "attacker")     // Uses damage extension

// Movement extensions  
entity.position                  // Uses position extension

// Experience extensions
entity.level                     // Uses level extension
entity.canLevelUp               // Uses canLevelUp extension
```

## Component Creation Helpers

### Items
```scala
val potion = items.potion("health-pot")      // Creates PotionItem + CanPickUp + Hitbox
val scroll = items.scroll("fireball")        // Creates ScrollItem + CanPickUp + Hitbox  
val bow = items.bow("longbow")               // Creates BowItem + CanPickUp + Hitbox
val arrow = items.arrow("arrow1")            // Creates ArrowItem + CanPickUp + Hitbox
val weapon = items.weapon("sword", 10, Melee) // Creates WeaponItem + CanPickUp + Hitbox
```

### Enemies
```scala
val enemies = Given.enemies.basic(
  id = "orc1", 
  x = 5, y = 5, 
  health = 25,
  withWeapons = true,  // Adds primary/secondary weapons
  deathEvents = Some(DeathEvents(...)) // Custom death behavior
)
```

## Type Safety

The component system provides compile-time type safety:

```scala
// ✅ Compiles - Health is a Component
.thePlayer.component[Health].is(Health(10))

// ❌ Compile error - String is not a Component  
.thePlayer.component[String].is("invalid")

// ✅ Compiles - type-safe predicate
.thePlayer.component[Health].satisfies(h => h.baseCurrent > 0)

// ❌ Compile error - wrong component type in predicate
.thePlayer.component[Health].satisfies(m => m.position.x > 0) // Movement method on Health
```

## Custom Components

To add new components to the DSL:

1. **Create the component**:
```scala
case class Stealth(isHidden: Boolean, stealthPoints: Int) extends Component
```

2. **Use immediately in tests**:
```scala
Given.thePlayerAt(4, 4)
  .setPlayer(Stealth(isHidden = true, stealthPoints = 50))
  .beginStory()
  .thePlayer.component[Stealth].satisfies(_.isHidden, "player should be hidden")
```

3. **Add convenience methods** (optional):
```scala
// In GameStory class
def isHidden: GameStory = {
  assert(gs.playerEntity.get[Stealth].exists(_.isHidden), "Expected player to be hidden")
  this
}
```

## Component Testing Best Practices

### 1. Use Generic Methods When Possible
```scala
// Preferred - reusable, type-safe
.thePlayer.component[Health].satisfies(_.baseCurrent == 5)

// Avoid - requires specific method for each component field
.thePlayer.hasBaseCurrent(5)
```

### 2. Provide Meaningful Error Messages
```scala
.thePlayer.component[Health].satisfies(
  h => h.baseCurrent > 0, 
  "player should be alive after healing"
)
```

### 3. Test Component Interactions
```scala
// Test that equipment affects health calculations
.thePlayer.component[Equipment].exists()
.thePlayer.component[Health].satisfies(h => h.baseMax == 100)
.thePlayer.hasHealth(120) // Includes equipment bonuses
```

### 4. Use Component Factories Consistently
```scala
// Good - uses component factory for consistent setup
val enemies = Given.enemies.basic("orc", 5, 5, health = 25)

// Avoid - manual component assembly in tests
val orc = Entity("orc", Health(25), Movement(Point(5,5)), ...)
```

This component-centric approach ensures that:
- Tests remain maintainable as new components are added
- Component behavior is tested consistently across entities
- The ECS architecture is properly exercised in integration tests
- Type safety prevents common testing errors