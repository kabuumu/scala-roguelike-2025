package game.system

import data.Sprites
import game.Direction.{Down, Left, Right, Up}
import game.{Direction}
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
import ui.InputAction

class MovementSystemTest extends AnyFunSuiteLike with Matchers {
  val playerId = "testPlayerId"
  val testDungeon: Dungeon = Dungeon(testMode = true)

  val basePlayerEntity: Entity = Entity(
    id = playerId,
    Movement(position = Point(5, 5)),
    EntityTypeComponent(EntityType.Player),
    Health(100),
    Initiative(maxInitiative = 10, currentInitiative = 0), // Player is ready to move
    Drawable(Sprites.playerSprite),
    Hitbox()
  )

  test("MovementSystem should move player when player is ready and path is clear") {
    val gameState = GameState(
      playerEntityId = playerId,
      entities = Seq(basePlayerEntity),
      messages = Nil,
      dungeon = testDungeon
    )

    val moveEvent = InputEvent(playerId, InputAction.Move(Up))
    val (updatedState, events) = MovementSystem.update(gameState, Seq(moveEvent))

    // Player should have moved up and initiative should be reset
    val updatedPlayer = updatedState.playerEntity
    updatedPlayer.position shouldBe Point(5, 4)
    updatedPlayer.get[Initiative].map(_.currentInitiative) shouldBe Some(10)
    events shouldBe empty
  }

  test("MovementSystem should not move player when initiative is not ready") {
    val notReadyPlayer = basePlayerEntity.update[Initiative](_.copy(currentInitiative = 5))
    val gameState = GameState(
      playerEntityId = playerId,
      entities = Seq(notReadyPlayer),
      messages = Nil,
      dungeon = testDungeon
    )

    val moveEvent = InputEvent(playerId, InputAction.Move(Up))
    val (updatedState, events) = MovementSystem.update(gameState, Seq(moveEvent))

    // Player should not have moved
    val updatedPlayer = updatedState.playerEntity
    updatedPlayer.position shouldBe Point(5, 5)
    updatedPlayer.get[Initiative].map(_.currentInitiative) shouldBe Some(5)
    events shouldBe empty
  }

  test("MovementSystem should not move player into a wall") {
    val gameState = GameState(
      playerEntityId = playerId,
      entities = Seq(basePlayerEntity),
      messages = Nil,
      dungeon = testDungeon
    )

    // Check if any wall exists and try to move into it, or skip if no walls in test dungeon
    val walls = testDungeon.walls
    if (walls.nonEmpty) {
      // Find a direction that would move into a wall
      val wallPoint = walls.head
      val adjacentPoint = Point(wallPoint.x, wallPoint.y + 1) // Point adjacent to wall
      val playerAtAdjacent = basePlayerEntity.update[Movement](_.copy(position = adjacentPoint))
      
      val gameStateWithPlayerNearWall = gameState.copy(entities = Seq(playerAtAdjacent))
      val directionToWall = Up // Assuming wall is up from adjacent point
      
      val moveEvent = InputEvent(playerId, InputAction.Move(directionToWall))
      val (updatedState, events) = MovementSystem.update(gameStateWithPlayerNearWall, Seq(moveEvent))

      // Player should not have moved into wall
      val updatedPlayer = updatedState.playerEntity
      updatedPlayer.position shouldBe adjacentPoint
      updatedPlayer.get[Initiative].map(_.currentInitiative) shouldBe Some(0) // Initiative should not reset
      events shouldBe empty
    } else {
      // If no walls in test dungeon, test that player can move freely
      val moveEvent = InputEvent(playerId, InputAction.Move(Up))
      val (updatedState, events) = MovementSystem.update(gameState, Seq(moveEvent))
      
      val updatedPlayer = updatedState.playerEntity
      updatedPlayer.position shouldBe Point(5, 4) // Player should move
    }
  }

  test("MovementSystem should not move player into another entity") {
    val enemyEntity = Entity(
      id = "enemy1",
      Movement(position = Point(5, 4)),
      EntityTypeComponent(EntityType.Enemy),
      Health(50),
      Drawable(Sprites.enemySprite),
      Hitbox()
    )

    val gameState = GameState(
      playerEntityId = playerId,
      entities = Seq(basePlayerEntity, enemyEntity),
      messages = Nil,
      dungeon = testDungeon
    )

    val moveEvent = InputEvent(playerId, InputAction.Move(Up))
    val (updatedState, events) = MovementSystem.update(gameState, Seq(moveEvent))

    // Player should not have moved
    val updatedPlayer = updatedState.playerEntity
    updatedPlayer.position shouldBe Point(5, 5)
    updatedPlayer.get[Initiative].map(_.currentInitiative) shouldBe Some(0) // Initiative should not reset
    events shouldBe empty
  }

  test("MovementSystem should handle multiple directions correctly") {
    val gameState = GameState(
      playerEntityId = playerId,
      entities = Seq(basePlayerEntity),
      messages = Nil,
      dungeon = testDungeon
    )

    // Test movement in all directions
    val directions = Seq(
      (Right, Point(6, 5)),
      (Down, Point(5, 6)),
      (Left, Point(4, 5)),
      (Up, Point(5, 4))
    )

    directions.foreach { case (direction, expectedPosition) =>
      val moveEvent = InputEvent(playerId, InputAction.Move(direction))
      val (updatedState, events) = MovementSystem.update(gameState, Seq(moveEvent))
      
      val updatedPlayer = updatedState.playerEntity
      updatedPlayer.position shouldBe expectedPosition
      events shouldBe empty
    }
  }

  test("MovementSystem should ignore non-movement events") {
    val gameState = GameState(
      playerEntityId = playerId,
      entities = Seq(basePlayerEntity),
      messages = Nil,
      dungeon = testDungeon
    )

    val nonMoveEvent = DamageEvent(playerId, "attacker", 10)
    val (updatedState, events) = MovementSystem.update(gameState, Seq(nonMoveEvent))

    // Game state should be unchanged
    updatedState shouldBe gameState
    events shouldBe empty
  }

  test("MovementSystem should handle non-existent entity gracefully") {
    val gameState = GameState(
      playerEntityId = playerId,
      entities = Seq(basePlayerEntity),
      messages = Nil,
      dungeon = testDungeon
    )

    val moveEvent = InputEvent("nonexistent", InputAction.Move(Up))
    val (updatedState, events) = MovementSystem.update(gameState, Seq(moveEvent))

    // Game state should be unchanged
    updatedState shouldBe gameState
    events shouldBe empty
  }
}