package ui

import data.Sprites
import game.Direction.Up
import game.Item.*
import game.entity.*
import game.entity.EntityType.*
import game.entity.Health.*
import game.entity.Inventory.*
import game.entity.UpdateAction.UpdateInitiative
import game.{GameState, Input, Point}
import map.Dungeon
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.matchers.should.Matchers
import ui.GameController.frameTime
import ui.UIState.Move

class GameControllerTest extends AnyFunSuiteLike with Matchers {
  val playerId = "testPlayerId"

  val playerEntity: Entity = Entity(
    id = playerId,
    Movement(position = Point(0, 0)),
    EntityTypeComponent(EntityType.Player),
    Health(10),
    Initiative(0),
    Inventory(Seq(), Some(Weapon(2, Melee)), Some(Weapon(1, Ranged(6)))),
    SightMemory(),
    UpdateController(UpdateInitiative),
    Drawable(Sprites.playerSprite),
    Hitbox()
  )

  test("Player initiative should decrease when not 0") {
    val unreadyPlayerEntity = playerEntity.update[Initiative](_.copy(maxInitiative = 1, currentInitiative = 1))

    val gameState = GameState(playerEntityId = playerId, entities = Seq(unreadyPlayerEntity), messages = Nil, dungeon = Dungeon())
    val gameController = GameController(Move, gameState)

    val updatedGameState = gameController.update(None, Long.MaxValue)
    updatedGameState.gameState.playerEntity.get[Initiative] should contain(Initiative(1, 0))
  }

  test("Player should move when given a move action") {
    val gameState = GameState(playerEntityId = playerId, entities = Seq(playerEntity), messages = Nil, dungeon = Dungeon())
    val gameController = GameController(Move, gameState)

    val updatedGameState = gameController.update(Some(Input.Move(Up)), Long.MaxValue)
    updatedGameState.gameState.playerEntity.get[Movement] should contain(Movement(Point(0, -1)))
  }

  test("Player should heal when using a potion") {
    val woundedPlayer = playerEntity.damage(5).addItem(Potion)

    val gameState = GameState(playerEntityId = playerId, entities = Seq(woundedPlayer), messages = Nil, dungeon = Dungeon())
    val gameController = GameController(Move, gameState)

    gameController.gameState.playerEntity.currentHealth shouldBe 5

    val updatedGameState =
      gameController
        .update(Some(Input.UseItem), frameTime) //To enter the use item state
        .update(Some(Input.UseItem), frameTime * 2) //To select the potion

    updatedGameState.gameState.playerEntity.currentHealth shouldBe 10
  }

  test("Player using a scroll fireball scroll should create a projectile") {
    val playerWithScroll = playerEntity.addItem(Scroll)

    val gameState = GameState(playerEntityId = playerId, entities = Seq(playerWithScroll), messages = Nil, dungeon = Dungeon())
    val gameController = GameController(Move, gameState)

    val beforeSelectingFireball =
      gameController
        .update(Some(Input.UseItem), frameTime) //To enter the use item state
        .update(Some(Input.UseItem), frameTime * 2) //To select the scroll
        .update(Some(Input.Move(Up)), frameTime * 3) //To move the target cursor up
        .update(Some(Input.Move(Up)), frameTime * 4) //To move the target cursor up
        .update(Some(Input.Move(Up)), frameTime * 5) //To move the target cursor up

    beforeSelectingFireball.gameState.entities.count(_.entityType == EntityType.Projectile) shouldBe 0

    val afterSelectingFireball =
      beforeSelectingFireball
        .update(Some(Input.UseItem), frameTime * 6) //To select the target

    afterSelectingFireball.gameState.entities.count(_.entityType == EntityType.Projectile) shouldBe 1
  }
}
