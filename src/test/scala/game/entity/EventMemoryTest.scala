package game.entity

import game.entity.EventMemory.*
import org.scalatest.funsuite.AnyFunSuite

class EventMemoryTest extends AnyFunSuite {

  test("EventMemory should start with empty events") {
    val eventMemory = EventMemory()
    assert(eventMemory.events.isEmpty)
    assert(eventMemory.getEvents.isEmpty)
  }

  test("EventMemory should add events") {
    val eventMemory = EventMemory()
    val timestamp = System.nanoTime()
    val event = MemoryEvent.MovementStep(timestamp, game.Direction.Up, game.Point(0, 0), game.Point(0, 1))
    
    val updatedMemory = eventMemory.addEvent(event)
    
    assert(updatedMemory.events.length == 1)
    assert(updatedMemory.events.head == event)
  }

  test("EventMemory should add multiple events") {
    val eventMemory = EventMemory()
    val timestamp = System.nanoTime()
    val event1 = MemoryEvent.MovementStep(timestamp, game.Direction.Up, game.Point(0, 0), game.Point(0, 1))
    val event2 = MemoryEvent.ItemUsed(timestamp + 1000, "Potion", None)
    
    val updatedMemory = eventMemory
      .addEvent(event1)
      .addEvent(event2)
    
    assert(updatedMemory.events.length == 2)
    assert(updatedMemory.events.contains(event1))
    assert(updatedMemory.events.contains(event2))
  }

  test("EventMemory should filter events by type") {
    val eventMemory = EventMemory()
    val timestamp = System.nanoTime()
    val moveEvent = MemoryEvent.MovementStep(timestamp, game.Direction.Up, game.Point(0, 0), game.Point(0, 1))
    val itemEvent = MemoryEvent.ItemUsed(timestamp + 1000, "Potion", None)
    val damageEvent = MemoryEvent.DamageTaken(timestamp + 2000, 10, 15, 5, "Enemy-1")
    
    val updatedMemory = eventMemory
      .addEvent(moveEvent)
      .addEvent(itemEvent)
      .addEvent(damageEvent)
    
    val moveEvents = updatedMemory.getEventsByType[MemoryEvent.MovementStep]
    val itemEvents = updatedMemory.getEventsByType[MemoryEvent.ItemUsed]
    val damageEvents = updatedMemory.getEventsByType[MemoryEvent.DamageTaken]
    
    assert(moveEvents.length == 1)
    assert(itemEvents.length == 1)
    assert(damageEvents.length == 1)
    assert(moveEvents.head == moveEvent)
    assert(itemEvents.head == itemEvent)
    assert(damageEvents.head == damageEvent)
  }

  test("Entity extension methods should work correctly") {
    val entity = Entity()
      .addComponent(EventMemory())
    
    val timestamp = System.nanoTime()
    val event = MemoryEvent.MovementStep(timestamp, game.Direction.Up, game.Point(0, 0), game.Point(0, 1))
    
    val updatedEntity = entity.addMemoryEvent(event)
    
    assert(updatedEntity.getMemoryEvents.length == 1)
    assert(updatedEntity.getMemoryEvents.head == event)
  }

  test("Entity extension should count steps correctly") {
    val entity = Entity()
      .addComponent(EventMemory())
    
    val timestamp = System.nanoTime()
    val step1 = MemoryEvent.MovementStep(timestamp, game.Direction.Up, game.Point(0, 0), game.Point(0, 1))
    val step2 = MemoryEvent.MovementStep(timestamp + 1000, game.Direction.Right, game.Point(0, 1), game.Point(1, 1))
    val itemEvent = MemoryEvent.ItemUsed(timestamp + 2000, "Potion", None)
    
    val updatedEntity = entity
      .addMemoryEvent(step1)
      .addMemoryEvent(step2)
      .addMemoryEvent(itemEvent)
    
    assert(updatedEntity.getStepCount == 2)
  }

  test("Entity extension should work with entity without EventMemory component") {
    val entity = Entity() // No EventMemory component
    
    assert(entity.getMemoryEvents.isEmpty)
    assert(entity.getStepCount == 0)
  }

  test("EventMemory should handle all event types") {
    val eventMemory = EventMemory()
    val timestamp = System.nanoTime()
    
    val moveEvent = MemoryEvent.MovementStep(timestamp, game.Direction.Up, game.Point(0, 0), game.Point(0, 1))
    val itemEvent = MemoryEvent.ItemUsed(timestamp + 1000, "Potion", Some("Player"))
    val damageTakenEvent = MemoryEvent.DamageTaken(timestamp + 2000, 10, 15, 5, "Enemy-1")
    val damageDealtEvent = MemoryEvent.DamageDealt(timestamp + 3000, 8, 8, 0, "Enemy-1")
    val enemyDefeatedEvent = MemoryEvent.EnemyDefeated(timestamp + 4000, "Rat", "combat")
    
    val updatedMemory = eventMemory
      .addEvent(moveEvent)
      .addEvent(itemEvent)
      .addEvent(damageTakenEvent)
      .addEvent(damageDealtEvent)
      .addEvent(enemyDefeatedEvent)
    
    assert(updatedMemory.events.length == 5)
    assert(updatedMemory.getEventsByType[MemoryEvent.MovementStep].length == 1)
    assert(updatedMemory.getEventsByType[MemoryEvent.ItemUsed].length == 1)
    assert(updatedMemory.getEventsByType[MemoryEvent.DamageTaken].length == 1)
    assert(updatedMemory.getEventsByType[MemoryEvent.DamageDealt].length == 1)
    assert(updatedMemory.getEventsByType[MemoryEvent.EnemyDefeated].length == 1)
  }
}