package game.system

import data.Sprites
import game.entity.*
import game.entity.EntityType.*
import game.entity.Health.*
import game.entity.Movement.*
import game.system.event.GameSystemEvent.*
import game.{GameState, Point}
import map.Dungeon
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.matchers.should.Matchers

class HealingSystemTest extends AnyFunSuiteLike with Matchers {
  val playerId = "testPlayerId"
  val enemyId = "testEnemyId"
  val testDungeon: Dungeon = Dungeon(testMode = true)

  val playerEntity: Entity = Entity(
    id = playerId,
    Movement(position = Point(5, 5)),
    EntityTypeComponent(EntityType.Player),
    Health(baseCurrent = 50, baseMax = 100), // Half health
    Drawable(Sprites.playerSprite),
    Hitbox()
  )

  val enemyEntity: Entity = Entity(
    id = enemyId,
    Movement(position = Point(3, 3)),
    EntityTypeComponent(EntityType.Enemy),
    Health(baseCurrent = 25, baseMax = 50), // Half health
    Drawable(Sprites.enemySprite),
    Hitbox()
  )

  test("HealingSystem should heal entity when heal event is processed") {
    val gameState = GameState(
      playerEntityId = playerId,
      entities = Seq(playerEntity, enemyEntity),
      messages = Nil,
      dungeon = testDungeon
    )

    val healEvent = HealEvent(playerId, 30)
    val (updatedState, events) = HealingSystem.update(gameState, Seq(healEvent))

    // Player should be healed
    val healedPlayer = updatedState.playerEntity
    healedPlayer.currentHealth shouldBe 80 // 50 + 30
    events shouldBe empty
  }

  test("HealingSystem should cap healing at max health") {
    val gameState = GameState(
      playerEntityId = playerId,
      entities = Seq(playerEntity, enemyEntity),
      messages = Nil,
      dungeon = testDungeon
    )

    val healEvent = HealEvent(playerId, 100) // Heal more than missing health
    val (updatedState, events) = HealingSystem.update(gameState, Seq(healEvent))

    // Player should be at max health
    val healedPlayer = updatedState.playerEntity
    healedPlayer.currentHealth shouldBe 100 // Capped at max
    events shouldBe empty
  }

  test("HealingSystem should generate message when already at full health") {
    val fullHealthPlayer = playerEntity.update[Health](_.copy(baseCurrent = 100))
    val gameState = GameState(
      playerEntityId = playerId,
      entities = Seq(fullHealthPlayer, enemyEntity),
      messages = Nil,
      dungeon = testDungeon
    )

    val healEvent = HealEvent(playerId, 30)
    val (updatedState, events) = HealingSystem.update(gameState, Seq(healEvent))

    // Player health should be unchanged
    val unchangedPlayer = updatedState.playerEntity
    unchangedPlayer.currentHealth shouldBe 100
    
    // Should generate a message event
    events.length shouldBe 1
    events.head shouldBe a[MessageEvent]
    val messageEvent = events.head.asInstanceOf[MessageEvent]
    messageEvent.message should include("full health")
  }

  test("HealingSystem should heal enemy entities correctly") {
    val gameState = GameState(
      playerEntityId = playerId,
      entities = Seq(playerEntity, enemyEntity),
      messages = Nil,
      dungeon = testDungeon
    )

    val healEvent = HealEvent(enemyId, 15)
    val (updatedState, events) = HealingSystem.update(gameState, Seq(healEvent))

    // Enemy should be healed
    val healedEnemy = updatedState.getEntity(enemyId).get
    healedEnemy.currentHealth shouldBe 40 // 25 + 15
    events shouldBe empty
  }

  test("HealingSystem should handle non-existent entity gracefully") {
    val gameState = GameState(
      playerEntityId = playerId,
      entities = Seq(playerEntity, enemyEntity),
      messages = Nil,
      dungeon = testDungeon
    )

    val healEvent = HealEvent("nonexistent", 30)
    val (updatedState, events) = HealingSystem.update(gameState, Seq(healEvent))

    // Game state should be unchanged
    updatedState shouldBe gameState
    events shouldBe empty
  }

  test("HealingSystem should ignore non-heal events") {
    val gameState = GameState(
      playerEntityId = playerId,
      entities = Seq(playerEntity, enemyEntity),
      messages = Nil,
      dungeon = testDungeon
    )

    val nonHealEvent = DamageEvent(playerId, enemyId, 10)
    val (updatedState, events) = HealingSystem.update(gameState, Seq(nonHealEvent))

    // Game state should be unchanged
    updatedState shouldBe gameState
    events shouldBe empty
  }

  test("HealingSystem should handle multiple heal events") {
    val gameState = GameState(
      playerEntityId = playerId,
      entities = Seq(playerEntity, enemyEntity),
      messages = Nil,
      dungeon = testDungeon
    )

    val healEvents = Seq(
      HealEvent(playerId, 20),
      HealEvent(enemyId, 10)
    )
    val (updatedState, events) = HealingSystem.update(gameState, healEvents)

    // Both entities should be healed
    val healedPlayer = updatedState.playerEntity
    val healedEnemy = updatedState.getEntity(enemyId).get
    
    healedPlayer.currentHealth shouldBe 70 // 50 + 20
    healedEnemy.currentHealth shouldBe 35 // 25 + 10
    events shouldBe empty
  }

  test("HealingSystem should handle zero healing amount") {
    val gameState = GameState(
      playerEntityId = playerId,
      entities = Seq(playerEntity, enemyEntity),
      messages = Nil,
      dungeon = testDungeon
    )

    val healEvent = HealEvent(playerId, 0)
    val (updatedState, events) = HealingSystem.update(gameState, Seq(healEvent))

    // Player health should be unchanged
    val unchangedPlayer = updatedState.playerEntity
    unchangedPlayer.currentHealth shouldBe 50
    events shouldBe empty
  }
}