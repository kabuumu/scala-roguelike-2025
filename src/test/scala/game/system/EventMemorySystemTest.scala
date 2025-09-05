package game.system

import game.{GameState, Point}
import game.entity.*
import game.entity.EventMemory.*
import game.system.event.GameSystemEvent
import org.scalatest.funsuite.AnyFunSuite
import map.Dungeon

class EventMemorySystemTest extends AnyFunSuite {

  private def createTestGameState(): GameState = {
    val player = Entity(
      id = "player",
      Movement(Point(5, 5)),
      EntityTypeComponent(EntityType.Player),
      Health(100),
      Equipment(),
      EventMemory()
    )
    
    val enemy = Entity(
      id = "enemy",
      Movement(Point(6, 6)),
      EntityTypeComponent(EntityType.Enemy),
      Health(20),
      Equipment(),
      EventMemory()
    )
    
    val item = Entity(
      id = "potion",
      EntityTypeComponent(EntityType.Player), // Use Player as fallback type
      NameComponent("Healing Potion", "Restores health")
    )
    
    val dungeon = Dungeon(testMode = true)
    
    GameState(
      playerEntityId = "player",
      entities = Vector(player, enemy, item),
      dungeon = dungeon
    )
  }

  test("EventMemorySystem should record damage taken events") {
    val gameState = createTestGameState()
    val damageEvent = GameSystemEvent.DamageEvent("player", "enemy", 15)
    
    val (updatedState, _) = EventMemorySystem.update(gameState, Seq(damageEvent))
    
    val playerEvents = updatedState.getEntity("player").get.getMemoryEvents
    val damageTakenEvents = playerEvents.collect { case e: MemoryEvent.DamageTaken => e }
    
    assert(damageTakenEvents.length == 1)
    val damageTaken = damageTakenEvents.head
    assert(damageTaken.damage == 15) // No equipment, so full damage
    assert(damageTaken.baseDamage == 15)
    assert(damageTaken.modifier == 0) // No damage reduction
    assert(damageTaken.source == "enemy")
  }

  test("EventMemorySystem should record damage dealt events") {
    val gameState = createTestGameState()
    val damageEvent = GameSystemEvent.DamageEvent("enemy", "player", 12)
    
    val (updatedState, _) = EventMemorySystem.update(gameState, Seq(damageEvent))
    
    val playerEvents = updatedState.getEntity("player").get.getMemoryEvents
    val damageDealtEvents = playerEvents.collect { case e: MemoryEvent.DamageDealt => e }
    
    assert(damageDealtEvents.length == 1)
    val damageDealt = damageDealtEvents.head
    assert(damageDealt.damage == 12)
    assert(damageDealt.baseDamage == 12)
    assert(damageDealt.modifier == 0)
    assert(damageDealt.target == "enemy")
  }

  test("EventMemorySystem should record movement events") {
    val gameState = createTestGameState()
    val moveEvent = GameSystemEvent.InputEvent("player", ui.InputAction.Move(game.Direction.Up))
    
    val (updatedState, _) = EventMemorySystem.update(gameState, Seq(moveEvent))
    
    val playerEvents = updatedState.getEntity("player").get.getMemoryEvents
    val moveEvents = playerEvents.collect { case e: MemoryEvent.MovementStep => e }
    
    assert(moveEvents.length == 1)
    val movement = moveEvents.head
    assert(movement.direction == game.Direction.Up)
    assert(movement.fromPosition == Point(5, 5))
    assert(movement.toPosition == Point(5, 4))
  }

  test("EventMemorySystem should record item usage events") {
    val gameState = createTestGameState()
    val itemEvent = GameSystemEvent.RemoveItemEntityEvent("player", "potion")
    
    val (updatedState, _) = EventMemorySystem.update(gameState, Seq(itemEvent))
    
    val playerEvents = updatedState.getEntity("player").get.getMemoryEvents
    val itemEvents = playerEvents.collect { case e: MemoryEvent.ItemUsed => e }
    
    assert(itemEvents.length == 1)
    val itemUsed = itemEvents.head
    assert(itemUsed.itemType == "Player") // Fallback entity type
    assert(itemUsed.target.isEmpty)
  }

  test("EventMemorySystem should handle equipment damage reduction") {
    val playerWithArmor = Entity(
      id = "player",
      Movement(Point(5, 5)),
      EntityTypeComponent(EntityType.Player),
      Health(100),
      Equipment(armor = Some(Equippable(EquipmentSlot.Armor, 5, "Iron Armor"))),
      EventMemory()
    )
    
    val enemy = Entity(
      id = "enemy",
      Movement(Point(6, 6)),
      EntityTypeComponent(EntityType.Enemy),
      Health(20),
      Equipment(),
      EventMemory()
    )
    
    val dungeon = Dungeon(testMode = true)
    
    val gameState = GameState(
      playerEntityId = "player",
      entities = Vector(playerWithArmor, enemy),
      dungeon = dungeon
    )
    
    val damageEvent = GameSystemEvent.DamageEvent("player", "enemy", 10)
    
    val (updatedState, _) = EventMemorySystem.update(gameState, Seq(damageEvent))
    
    val playerEvents = updatedState.getEntity("player").get.getMemoryEvents
    val damageTakenEvents = playerEvents.collect { case e: MemoryEvent.DamageTaken => e }
    
    assert(damageTakenEvents.length == 1)
    val damageTaken = damageTakenEvents.head
    assert(damageTaken.damage == 5) // 10 - 5 armor reduction
    assert(damageTaken.baseDamage == 10)
    assert(damageTaken.modifier == 5) // Armor provided 5 damage reduction
    assert(damageTaken.source == "enemy")
  }

  test("EventMemorySystem should ensure minimum 1 damage") {
    val playerWithHeavyArmor = Entity(
      id = "player",
      Movement(Point(5, 5)),
      EntityTypeComponent(EntityType.Player),
      Health(100),
      Equipment(armor = Some(Equippable(EquipmentSlot.Armor, 20, "Heavy Armor"))),
      EventMemory()
    )
    
    val enemy = Entity(
      id = "enemy",
      Movement(Point(6, 6)),
      EntityTypeComponent(EntityType.Enemy),
      Health(20),
      Equipment(),
      EventMemory()
    )
    
    val dungeon = Dungeon(testMode = true)
    
    val gameState = GameState(
      playerEntityId = "player",
      entities = Vector(playerWithHeavyArmor, enemy),
      dungeon = dungeon
    )
    
    val damageEvent = GameSystemEvent.DamageEvent("player", "enemy", 5)
    
    val (updatedState, _) = EventMemorySystem.update(gameState, Seq(damageEvent))
    
    val playerEvents = updatedState.getEntity("player").get.getMemoryEvents
    val damageTakenEvents = playerEvents.collect { case e: MemoryEvent.DamageTaken => e }
    
    assert(damageTakenEvents.length == 1)
    val damageTaken = damageTakenEvents.head
    assert(damageTaken.damage == 1) // Minimum damage is 1
    assert(damageTaken.baseDamage == 5)
    assert(damageTaken.modifier == 20) // Armor reduction
    assert(damageTaken.source == "enemy")
  }

  test("EventMemorySystem should ignore non-relevant events") {
    val gameState = createTestGameState()
    val irrelevantEvent = GameSystemEvent.MessageEvent("Some message")
    
    val (updatedState, _) = EventMemorySystem.update(gameState, Seq(irrelevantEvent))
    
    val playerEvents = updatedState.getEntity("player").get.getMemoryEvents
    val enemyEvents = updatedState.getEntity("enemy").get.getMemoryEvents
    
    assert(playerEvents.isEmpty)
    assert(enemyEvents.isEmpty)
  }

  test("EventMemorySystem should handle non-existent entities gracefully") {
    val gameState = createTestGameState()
    val damageEvent = GameSystemEvent.DamageEvent("non-existent", "player", 10)
    val moveEvent = GameSystemEvent.InputEvent("non-existent", ui.InputAction.Move(game.Direction.Up))
    val itemEvent = GameSystemEvent.RemoveItemEntityEvent("non-existent", "potion")
    
    val (updatedState, _) = EventMemorySystem.update(gameState, Seq(damageEvent, moveEvent, itemEvent))
    
    // Should not crash, and existing entities should be unchanged
    assert(updatedState.entities.length == gameState.entities.length)
    val playerEvents = updatedState.getEntity("player").get.getMemoryEvents
    assert(playerEvents.isEmpty)
  }

  test("EventMemorySystem should process multiple events in one update") {
    val gameState = createTestGameState()
    val events = Seq(
      GameSystemEvent.DamageEvent("player", "enemy", 8),
      GameSystemEvent.InputEvent("player", ui.InputAction.Move(game.Direction.Right)),
      GameSystemEvent.RemoveItemEntityEvent("player", "potion")
    )
    
    val (updatedState, _) = EventMemorySystem.update(gameState, events)
    
    val playerEvents = updatedState.getEntity("player").get.getMemoryEvents
    
    assert(playerEvents.length == 3)
    assert(playerEvents.exists(_.isInstanceOf[MemoryEvent.DamageTaken]))
    assert(playerEvents.exists(_.isInstanceOf[MemoryEvent.MovementStep]))
    assert(playerEvents.exists(_.isInstanceOf[MemoryEvent.ItemUsed]))
  }
}