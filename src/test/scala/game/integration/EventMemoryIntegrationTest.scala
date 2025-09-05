package game.integration

import game.{GameState, Point}
import game.entity.*
import game.entity.EventMemory.*
import game.system.event.GameSystemEvent
import org.scalatest.funsuite.AnyFunSuite
import map.Dungeon
import ui.InputAction

class EventMemoryIntegrationTest extends AnyFunSuite {

  test("EventMemory system integration test - complete gameplay scenario") {
    // Create a test game state with player and enemy
    val player = Entity(
      id = "player",
      Movement(Point(5, 5)),
      EntityTypeComponent(EntityType.Player),
      Health(100),
      Equipment(),
      EventMemory(),
      Initiative(10)
    )
    
    val enemy = Entity(
      id = "enemy",
      Movement(Point(6, 6)),
      EntityTypeComponent(EntityType.Enemy),
      Health(20),
      Equipment(),
      EventMemory(),
      Initiative(10)
    )
    
    val potion = Entity(
      id = "potion",
      EntityTypeComponent(EntityType.Player), // Use Player as fallback
      NameComponent("Healing Potion", "Restores health")
    )
    
    val dungeon = Dungeon(testMode = true)
    
    val initialGameState = GameState(
      playerEntityId = "player",
      entities = Vector(player, enemy, potion),
      dungeon = dungeon
    )
    
    // Simulate a complete turn sequence with multiple events
    val events = Seq(
      // Player moves
      GameSystemEvent.InputEvent("player", InputAction.Move(game.Direction.Up)),
      // Player takes damage from enemy
      GameSystemEvent.DamageEvent("player", "enemy", 15),
      // Player deals damage to enemy
      GameSystemEvent.DamageEvent("enemy", "player", 12),
      // Player uses a potion
      GameSystemEvent.RemoveItemEntityEvent("player", "potion")
    )
    
    // Process events through the game system
    val finalGameState = initialGameState.updateWithSystems(events)
    
    // Verify player's event memory
    val playerEntity = finalGameState.getEntity("player").get
    val playerEvents = playerEntity.getMemoryEvents
    
    // Should have recorded all player actions
    assert(playerEvents.length == 4)
    
    // Check movement event
    val moveEvents = playerEntity.getMemoryEventsByType[MemoryEvent.MovementStep]
    assert(moveEvents.length == 1)
    assert(moveEvents.head.direction == game.Direction.Up)
    assert(moveEvents.head.fromPosition == Point(5, 5))
    assert(moveEvents.head.toPosition == Point(5, 4))
    
    // Check damage taken event
    val damageTakenEvents = playerEntity.getMemoryEventsByType[MemoryEvent.DamageTaken]
    assert(damageTakenEvents.length == 1)
    assert(damageTakenEvents.head.damage == 15)
    assert(damageTakenEvents.head.source == "enemy")
    
    // Check damage dealt event
    val damageDealtEvents = playerEntity.getMemoryEventsByType[MemoryEvent.DamageDealt]
    assert(damageDealtEvents.length == 1)
    assert(damageDealtEvents.head.damage == 12)
    assert(damageDealtEvents.head.target == "enemy")
    
    // Check item used event
    val itemUsedEvents = playerEntity.getMemoryEventsByType[MemoryEvent.ItemUsed]
    assert(itemUsedEvents.length == 1)
    assert(itemUsedEvents.head.itemType == "Player")
    
    // Verify enemy's event memory
    val enemyEntity = finalGameState.getEntity("enemy").get
    val enemyEvents = enemyEntity.getMemoryEvents
    
    // Enemy should have recorded damage taken and damage dealt
    assert(enemyEvents.length == 2)
    
    val enemyDamageTaken = enemyEntity.getMemoryEventsByType[MemoryEvent.DamageTaken]
    assert(enemyDamageTaken.length == 1)
    assert(enemyDamageTaken.head.damage == 12)
    assert(enemyDamageTaken.head.source == "player")
    
    val enemyDamageDealt = enemyEntity.getMemoryEventsByType[MemoryEvent.DamageDealt]
    assert(enemyDamageDealt.length == 1)
    assert(enemyDamageDealt.head.damage == 15)
    assert(enemyDamageDealt.head.target == "player")
    
    // Verify step count
    assert(playerEntity.getStepCount == 1)
    assert(enemyEntity.getStepCount == 0) // Enemy didn't move
  }

  test("EventMemory extensibility test - adding new event types") {
    // This test demonstrates how easy it is to extend the system
    // with new event types without breaking existing functionality
    
    val eventMemory = EventMemory()
    val timestamp = System.nanoTime()
    
    // Add all current event types
    val events = Seq(
      MemoryEvent.MovementStep(timestamp, game.Direction.Up, Point(0, 0), Point(0, 1)),
      MemoryEvent.ItemUsed(timestamp + 1000, "Potion", Some("Player")),
      MemoryEvent.DamageTaken(timestamp + 2000, 10, 15, 5, "Enemy"),
      MemoryEvent.DamageDealt(timestamp + 3000, 8, 8, 0, "Enemy"),
      MemoryEvent.EnemyDefeated(timestamp + 4000, "Rat", "combat")
    )
    
    val updatedMemory = events.foldLeft(eventMemory)(_.addEvent(_))
    
    // Verify all event types are stored and retrievable
    assert(updatedMemory.getEventsByType[MemoryEvent.MovementStep].length == 1)
    assert(updatedMemory.getEventsByType[MemoryEvent.ItemUsed].length == 1)
    assert(updatedMemory.getEventsByType[MemoryEvent.DamageTaken].length == 1)
    assert(updatedMemory.getEventsByType[MemoryEvent.DamageDealt].length == 1)
    assert(updatedMemory.getEventsByType[MemoryEvent.EnemyDefeated].length == 1)
    
    // Total events should match
    assert(updatedMemory.getEvents.length == 5)
    
    // Future developers can easily add new event types by:
    // 1. Adding a new case class to MemoryEvent sealed trait
    // 2. Adding processing logic to EventMemorySystem
    // 3. No changes needed to EventMemory component itself
  }
}