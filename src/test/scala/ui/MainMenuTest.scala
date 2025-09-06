package ui

import game.{GameState, StartingState}
import game.Input
import game.entity.{Entity, Movement, EntityTypeComponent, Health, Initiative, Inventory, Drawable, Hitbox, DeathEvents}
import game.entity.EntityType
import data.Sprites
import map.Dungeon
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import ui.UIState.MainMenu

class MainMenuTest extends AnyFunSpec with Matchers {

  private def createDummyGameState(): GameState = {
    val dummyDungeon = Dungeon(
      roomGrid = Set(game.Point(0, 0)),
      roomConnections = Set.empty,
      blockedRooms = Set.empty,
      startPoint = game.Point(0, 0),
      endpoint = None,
      items = Set.empty,
      testMode = true,
      seed = 0L
    )

    val dummyPlayer = Entity(
      id = "dummy",
      Movement(position = game.Point(0, 0)),
      EntityTypeComponent(EntityType.Player),
      Health(100),
      Initiative(10),
      Inventory(Nil, None),
      Drawable(Sprites.playerSprite),
      Hitbox(),
      DeathEvents()
    )

    GameState(
      playerEntityId = "dummy",
      entities = Vector(dummyPlayer),
      dungeon = dummyDungeon
    )
  }

  describe("MainMenu") {
    it("should initialize with 'New Game' as the first option") {
      val mainMenu = MainMenu()
      mainMenu.options should contain("New Game")
      mainMenu.selectedOption shouldBe 0
      mainMenu.getSelectedOption shouldBe "New Game"
    }

    it("should cycle through options correctly") {
      val mainMenu = MainMenu()
      val nextMenu = mainMenu.selectNext
      // Now has two options, should move to second option
      nextMenu.selectedOption shouldBe 1
      nextMenu.getSelectedOption should (equal("Continue Game") or equal("Continue Game (No Save)"))
    }

    it("should handle navigation input") {
      val dummyGameState = createDummyGameState()
      val controller = GameController(MainMenu(), dummyGameState)

      // Test up/down navigation (with one option, should stay the same)
      val updatedController = controller.update(Some(Input.Move(game.Direction.Down)), 1000000000L)
      updatedController.uiState shouldBe a[MainMenu]
    }

    it("should transition to game when New Game is selected") {
      val dummyGameState = createDummyGameState()
      val controller = GameController(MainMenu(), dummyGameState)

      // Test selecting New Game
      val updatedController = controller.update(Some(Input.Confirm), 1000000000L)
      
      // Should transition to Move state with a proper game state
      updatedController.uiState shouldBe UIState.Move
      updatedController.gameState.playerEntity.id shouldBe "Player ID"
      updatedController.gameState.entities.length should be > 10 // Should have player + enemies + items
    }
  }
}