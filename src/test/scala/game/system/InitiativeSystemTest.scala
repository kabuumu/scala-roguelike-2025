package game.system

import data.Sprites
import game.entity.*
import game.entity.EntityType.*
import game.entity.Health.*
import game.entity.Initiative.*
import game.entity.Movement.*
import game.system.event.GameSystemEvent.*
import game.{GameState, Point}
import map.Dungeon
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.matchers.should.Matchers

class InitiativeSystemTest extends AnyFunSuiteLike with Matchers {
  val playerId = "testPlayerId"
  val enemyId = "testEnemyId"
  val testDungeon: Dungeon = Dungeon(testMode = true)

  val basePlayerEntity: Entity = Entity(
    id = playerId,
    Movement(position = Point(5, 5)),
    EntityTypeComponent(EntityType.Player),
    Health(100),
    Initiative(maxInitiative = 10, currentInitiative = 5),
    Drawable(Sprites.playerSprite),
    Hitbox()
  )

  val baseEnemyEntity: Entity = Entity(
    id = enemyId,
    Movement(position = Point(3, 3)),
    EntityTypeComponent(EntityType.Enemy),
    Health(50),
    Initiative(maxInitiative = 8, currentInitiative = 3),
    Drawable(Sprites.enemySprite),
    Hitbox()
  )

  test("InitiativeSystem should decrease initiative for all entities when player is not ready") {
    val gameState = GameState(
      playerEntityId = playerId,
      entities = Seq(basePlayerEntity, baseEnemyEntity),
      messages = Nil,
      dungeon = testDungeon
    )

    val (updatedState, events) = InitiativeSystem.update(gameState, Seq.empty)

    // All entities should have decreased initiative
    val updatedPlayer = updatedState.playerEntity
    val updatedEnemy = updatedState.getEntity(enemyId).get

    updatedPlayer.get[Initiative].map(_.currentInitiative) shouldBe Some(4)
    updatedEnemy.get[Initiative].map(_.currentInitiative) shouldBe Some(2)
    events shouldBe empty
  }

  test("InitiativeSystem should not decrease initiative when player is ready") {
    val readyPlayer = basePlayerEntity.update[Initiative](_.copy(currentInitiative = 0)) // Player is ready
    val gameState = GameState(
      playerEntityId = playerId,
      entities = Seq(readyPlayer, baseEnemyEntity),
      messages = Nil,
      dungeon = testDungeon
    )

    val (updatedState, events) = InitiativeSystem.update(gameState, Seq.empty)

    // No entities should have their initiative decreased since player is ready
    val updatedPlayer = updatedState.playerEntity
    val updatedEnemy = updatedState.getEntity(enemyId).get

    updatedPlayer.get[Initiative].map(_.currentInitiative) shouldBe Some(0)
    updatedEnemy.get[Initiative].map(_.currentInitiative) shouldBe Some(3)
    events shouldBe empty
  }

  test("InitiativeSystem should handle ResetInitiativeEvent") {
    val gameState = GameState(
      playerEntityId = playerId,
      entities = Seq(basePlayerEntity, baseEnemyEntity),
      messages = Nil,
      dungeon = testDungeon
    )

    val resetEvent = ResetInitiativeEvent(playerId)
    val (updatedState, events) = InitiativeSystem.update(gameState, Seq(resetEvent))

    // Player should have ready initiative (0) but both entities actually get decreased since reset sets to max, not 0
    val updatedPlayer = updatedState.playerEntity
    val updatedEnemy = updatedState.getEntity(enemyId).get

    updatedPlayer.get[Initiative].map(_.currentInitiative) shouldBe Some(9) // Reset to 10, then decreased to 9
    updatedEnemy.get[Initiative].map(_.currentInitiative) shouldBe Some(2) // Still decreases because player not ready after reset
    events shouldBe empty
  }

  test("InitiativeSystem should handle ResetInitiativeEvent for multiple entities") {
    val gameState = GameState(
      playerEntityId = playerId,
      entities = Seq(basePlayerEntity, baseEnemyEntity),
      messages = Nil,
      dungeon = testDungeon
    )

    val resetEvents = Seq(
      ResetInitiativeEvent(playerId),
      ResetInitiativeEvent(enemyId)
    )
    val (updatedState, events) = InitiativeSystem.update(gameState, resetEvents)

    // Both entities should have reset initiative then decreased
    val updatedPlayer = updatedState.playerEntity
    val updatedEnemy = updatedState.getEntity(enemyId).get

    updatedPlayer.get[Initiative].map(_.currentInitiative) shouldBe Some(9) // Reset to 10, then decreased
    updatedEnemy.get[Initiative].map(_.currentInitiative) shouldBe Some(7) // Reset to 8, then decreased
    events shouldBe empty
  }

  test("InitiativeSystem should handle ResetInitiativeEvent for non-existent entity gracefully") {
    val gameState = GameState(
      playerEntityId = playerId,
      entities = Seq(basePlayerEntity, baseEnemyEntity),
      messages = Nil,
      dungeon = testDungeon
    )

    val resetEvent = ResetInitiativeEvent("nonexistent")
    val (updatedState, events) = InitiativeSystem.update(gameState, Seq(resetEvent))

    // Only normal initiative decrease should happen
    val updatedPlayer = updatedState.playerEntity
    val updatedEnemy = updatedState.getEntity(enemyId).get

    updatedPlayer.get[Initiative].map(_.currentInitiative) shouldBe Some(4)
    updatedEnemy.get[Initiative].map(_.currentInitiative) shouldBe Some(2)
    events shouldBe empty
  }

  test("InitiativeSystem should ignore other event types") {
    val gameState = GameState(
      playerEntityId = playerId,
      entities = Seq(basePlayerEntity, baseEnemyEntity),
      messages = Nil,
      dungeon = testDungeon
    )

    val otherEvent = DamageEvent(playerId, enemyId, 10)
    val (updatedState, events) = InitiativeSystem.update(gameState, Seq(otherEvent))

    // Should perform normal initiative decrease
    val updatedPlayer = updatedState.playerEntity
    val updatedEnemy = updatedState.getEntity(enemyId).get

    updatedPlayer.get[Initiative].map(_.currentInitiative) shouldBe Some(4)
    updatedEnemy.get[Initiative].map(_.currentInitiative) shouldBe Some(2)
    events shouldBe empty
  }

  test("InitiativeSystem should handle entities at zero initiative correctly") {
    val zeroInitiativePlayer = basePlayerEntity.update[Initiative](_.copy(currentInitiative = 0))
    val zeroInitiativeEnemy = baseEnemyEntity.update[Initiative](_.copy(currentInitiative = 0))
    
    val gameState = GameState(
      playerEntityId = playerId,
      entities = Seq(zeroInitiativePlayer, zeroInitiativeEnemy),
      messages = Nil,
      dungeon = testDungeon
    )

    val (updatedState, events) = InitiativeSystem.update(gameState, Seq.empty)

    // Initiative should not go below 0
    val updatedPlayer = updatedState.playerEntity
    val updatedEnemy = updatedState.getEntity(enemyId).get

    updatedPlayer.get[Initiative].map(_.currentInitiative) shouldBe Some(0)
    updatedEnemy.get[Initiative].map(_.currentInitiative) shouldBe Some(0)
    events shouldBe empty
  }

  test("InitiativeSystem should process reset events before initiative decrease") {
    val readyPlayer = basePlayerEntity.update[Initiative](_.copy(currentInitiative = 0)) // Player is ready
    val gameState = GameState(
      playerEntityId = playerId,
      entities = Seq(readyPlayer, baseEnemyEntity),
      messages = Nil,
      dungeon = testDungeon
    )

    val resetEvent = ResetInitiativeEvent(playerId)
    val (updatedState, events) = InitiativeSystem.update(gameState, Seq(resetEvent))

    // Player gets reset to 10, then since player is not ready, all entities decrease initiative
    val updatedPlayer = updatedState.playerEntity
    val updatedEnemy = updatedState.getEntity(enemyId).get

    updatedPlayer.get[Initiative].map(_.currentInitiative) shouldBe Some(9) // Reset to 10, then decreased
    updatedEnemy.get[Initiative].map(_.currentInitiative) shouldBe Some(2) // Decreased because player not ready after reset
    events shouldBe empty
  }
}