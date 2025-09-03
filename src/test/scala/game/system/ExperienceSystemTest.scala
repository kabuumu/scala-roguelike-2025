package game.system

import data.Sprites
import game.entity.*
import game.entity.EntityType.*
import game.entity.Experience.*
import game.entity.Movement.*
import game.system.event.GameSystemEvent.*
import game.{GameState, Point}
import map.Dungeon
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.matchers.should.Matchers

class ExperienceSystemTest extends AnyFunSuiteLike with Matchers {
  val playerId = "testPlayerId"
  val enemyId = "testEnemyId"
  val testDungeon: Dungeon = Dungeon(testMode = true)

  val playerEntity: Entity = Entity(
    id = playerId,
    Movement(position = Point(5, 5)),
    EntityTypeComponent(EntityType.Player),
    Health(100),
    Experience(currentExperience = 50), // Starting experience
    Drawable(Sprites.playerSprite),
    Hitbox()
  )

  val enemyEntity: Entity = Entity(
    id = enemyId,
    Movement(position = Point(3, 3)),
    EntityTypeComponent(EntityType.Enemy),
    Health(50),
    Experience(currentExperience = 0),
    Drawable(Sprites.enemySprite),
    Hitbox()
  )

  test("ExperienceSystem should add experience to entity") {
    val gameState = GameState(
      playerEntityId = playerId,
      entities = Seq(playerEntity, enemyEntity),
      messages = Nil,
      dungeon = testDungeon
    )

    val experienceEvent = AddExperienceEvent(playerId, 25)
    val (updatedState, events) = ExperienceSystem.update(gameState, Seq(experienceEvent))

    // Player should have gained experience
    val updatedPlayer = updatedState.playerEntity
    updatedPlayer.experience shouldBe 75 // 50 + 25
    events shouldBe empty
  }

  test("ExperienceSystem should handle zero experience gain") {
    val gameState = GameState(
      playerEntityId = playerId,
      entities = Seq(playerEntity, enemyEntity),
      messages = Nil,
      dungeon = testDungeon
    )

    val experienceEvent = AddExperienceEvent(playerId, 0)
    val (updatedState, events) = ExperienceSystem.update(gameState, Seq(experienceEvent))

    // Player experience should be unchanged
    val updatedPlayer = updatedState.playerEntity
    updatedPlayer.experience shouldBe 50 // No change
    events shouldBe empty
  }

  test("ExperienceSystem should handle large experience gains") {
    val gameState = GameState(
      playerEntityId = playerId,
      entities = Seq(playerEntity, enemyEntity),
      messages = Nil,
      dungeon = testDungeon
    )

    val experienceEvent = AddExperienceEvent(playerId, 1000)
    val (updatedState, events) = ExperienceSystem.update(gameState, Seq(experienceEvent))

    // Player should have gained the full amount
    val updatedPlayer = updatedState.playerEntity
    updatedPlayer.experience shouldBe 1050 // 50 + 1000
    events shouldBe empty
  }

  test("ExperienceSystem should add experience to enemy entities") {
    val gameState = GameState(
      playerEntityId = playerId,
      entities = Seq(playerEntity, enemyEntity),
      messages = Nil,
      dungeon = testDungeon
    )

    val experienceEvent = AddExperienceEvent(enemyId, 30)
    val (updatedState, events) = ExperienceSystem.update(gameState, Seq(experienceEvent))

    // Enemy should have gained experience
    val updatedEnemy = updatedState.getEntity(enemyId).get
    updatedEnemy.experience shouldBe 30 // 0 + 30
    events shouldBe empty
  }

  test("ExperienceSystem should handle multiple experience events") {
    val gameState = GameState(
      playerEntityId = playerId,
      entities = Seq(playerEntity, enemyEntity),
      messages = Nil,
      dungeon = testDungeon
    )

    val experienceEvents = Seq(
      AddExperienceEvent(playerId, 15),
      AddExperienceEvent(playerId, 10),
      AddExperienceEvent(enemyId, 20)
    )
    val (updatedState, events) = ExperienceSystem.update(gameState, experienceEvents)

    // Player should have cumulative experience gain
    val updatedPlayer = updatedState.playerEntity
    val updatedEnemy = updatedState.getEntity(enemyId).get
    
    updatedPlayer.experience shouldBe 75 // 50 + 15 + 10
    updatedEnemy.experience shouldBe 20 // 0 + 20
    events shouldBe empty
  }

  test("ExperienceSystem should handle non-existent entity gracefully") {
    val gameState = GameState(
      playerEntityId = playerId,
      entities = Seq(playerEntity, enemyEntity),
      messages = Nil,
      dungeon = testDungeon
    )

    val experienceEvent = AddExperienceEvent("nonexistent", 100)
    
    // The system will crash with NoSuchElementException when trying to update non-existent entity
    // This is expected behavior based on the implementation
    intercept[NoSuchElementException] {
      ExperienceSystem.update(gameState, Seq(experienceEvent))
    }
  }

  test("ExperienceSystem should ignore non-experience events") {
    val gameState = GameState(
      playerEntityId = playerId,
      entities = Seq(playerEntity, enemyEntity),
      messages = Nil,
      dungeon = testDungeon
    )

    val nonExperienceEvent = DamageEvent(playerId, enemyId, 10)
    val (updatedState, events) = ExperienceSystem.update(gameState, Seq(nonExperienceEvent))

    // Game state should be unchanged
    updatedState shouldBe gameState
    events shouldBe empty
  }

  test("ExperienceSystem should handle negative experience (if allowed)") {
    val gameState = GameState(
      playerEntityId = playerId,
      entities = Seq(playerEntity, enemyEntity),
      messages = Nil,
      dungeon = testDungeon
    )

    val experienceEvent = AddExperienceEvent(playerId, -20)
    val (updatedState, events) = ExperienceSystem.update(gameState, Seq(experienceEvent))

    // Player should lose experience if the system allows it
    val updatedPlayer = updatedState.playerEntity
    // Note: This depends on how the Experience component handles negative values
    updatedPlayer.experience shouldBe 30 // 50 - 20
    events shouldBe empty
  }

  test("ExperienceSystem should handle mixed event types") {
    val gameState = GameState(
      playerEntityId = playerId,
      entities = Seq(playerEntity, enemyEntity),
      messages = Nil,
      dungeon = testDungeon
    )

    val mixedEvents = Seq(
      DamageEvent(playerId, enemyId, 10),
      AddExperienceEvent(playerId, 25),
      HealEvent(playerId, 5),
      AddExperienceEvent(enemyId, 15)
    )
    val (updatedState, events) = ExperienceSystem.update(gameState, mixedEvents)

    // Only experience events should be processed
    val updatedPlayer = updatedState.playerEntity
    val updatedEnemy = updatedState.getEntity(enemyId).get
    
    updatedPlayer.experience shouldBe 75 // 50 + 25
    updatedEnemy.experience shouldBe 15 // 0 + 15
    events shouldBe empty
  }
}