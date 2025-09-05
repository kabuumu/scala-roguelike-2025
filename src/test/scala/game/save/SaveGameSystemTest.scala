package game.save

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterEach
import game.*
import game.entity.*
import game.entity.Movement.position // Add position extension import
import map.Dungeon
import ui.{GameController, UIState}
import scala.util.{Success, Failure}

class SaveGameSystemTest extends AnyFunSpec with Matchers with BeforeAndAfterEach {

  override def beforeEach(): Unit = {
    // Clear any existing save game before each test
    SaveGameSystem.deleteSaveGame()
  }

  override def afterEach(): Unit = {
    // Clean up save game after each test
    SaveGameSystem.deleteSaveGame()
  }

  describe("SaveGameSystem") {
    it("should report no save game exists initially") {
      SaveGameSystem.hasSaveGame() shouldBe false
    }

    it("should save and load a simple game state") {
      val testPlayer = Entity(
        id = "test-player",
        Movement(Point(5, 5)),
        new Health(80, 100),
        Initiative(10, 5),
        Experience(50, false),
        EntityTypeComponent(EntityType.Player)
      )
      
      val testGameState = GameState(
        playerEntityId = "test-player",
        entities = Seq(testPlayer),
        messages = Seq("Test message"),
        dungeon = Dungeon(testMode = true)
      )

      // Save the game state
      val saveResult = SaveGameSystem.saveGame(testGameState)
      saveResult shouldBe a[Success[?]]
      
      // Check that save game exists
      SaveGameSystem.hasSaveGame() shouldBe true

      // Load the game state
      val loadResult = SaveGameSystem.loadGame()
      loadResult shouldBe a[Success[?]]
      
      val loadedGameState = loadResult.get
      loadedGameState.playerEntityId shouldBe "test-player"
      loadedGameState.entities.length shouldBe 1
      loadedGameState.entities.head.id shouldBe "test-player"
      loadedGameState.messages should contain("Test message")
    }

    it("should handle loading when no save exists") {
      SaveGameSystem.hasSaveGame() shouldBe false
      
      val loadResult = SaveGameSystem.loadGame()
      loadResult shouldBe a[Failure[?]]
    }

    it("should allow deleting save game") {
      val testGameState = GameState(
        playerEntityId = "test-player",
        entities = Seq(Entity("test-player", Movement(Point(0, 0)))),
        dungeon = Dungeon(testMode = true)
      )

      SaveGameSystem.saveGame(testGameState)
      SaveGameSystem.hasSaveGame() shouldBe true
      
      SaveGameSystem.deleteSaveGame()
      SaveGameSystem.hasSaveGame() shouldBe false
    }
  }

  describe("MainMenu integration with save system") {
    it("should show Continue Game option when save exists") {
      val testGameState = GameState(
        playerEntityId = "test-player",
        entities = Seq(Entity("test-player", Movement(Point(0, 0)))),
        dungeon = Dungeon(testMode = true)
      )

      SaveGameSystem.saveGame(testGameState)
      
      val mainMenu = UIState.MainMenu()
      mainMenu.options should contain("Continue Game")
      mainMenu.isOptionEnabled(1) shouldBe true
      mainMenu.canConfirmCurrentSelection shouldBe true // New Game is always enabled
    }

    it("should show disabled Continue Game option when no save exists") {
      SaveGameSystem.hasSaveGame() shouldBe false
      
      val mainMenu = UIState.MainMenu()
      mainMenu.options should contain("Continue Game (No Save)")
      mainMenu.isOptionEnabled(1) shouldBe false
      
      // Select Continue Game option
      val menuOnContinue = mainMenu.selectNext
      menuOnContinue.canConfirmCurrentSelection shouldBe false
    }
  }

  describe("GameController save/load integration") {
    it("should autosave before player actions") {
      val testPlayer = Entity(
        id = "test-player",
        Movement(Point(5, 5)),
        Health(100),
        Initiative(10, 10),
        EntityTypeComponent(EntityType.Player)
      )
      
      val testGameState = GameState(
        playerEntityId = "test-player",
        entities = Seq(testPlayer),
        dungeon = Dungeon(testMode = true)
      )

      val gameController = GameController(UIState.Move, testGameState)
      
      // Initially no save
      SaveGameSystem.hasSaveGame() shouldBe false
      
      // Perform a player action (move)
      val updatedController = gameController.update(Some(Input.Move(Direction.Up)), System.nanoTime())
      
      // Should have autosaved
      SaveGameSystem.hasSaveGame() shouldBe true
    }

    it("should load game when Continue Game is selected") {
      // First create and save a game state
      val testPlayer = Entity(
        id = "saved-player",
        Movement(Point(10, 10)),
        new Health(75, 100),
        Initiative(5, 0),
        Experience(100, false),
        EntityTypeComponent(EntityType.Player)
      )
      
      val savedGameState = GameState(
        playerEntityId = "saved-player", 
        entities = Seq(testPlayer),
        messages = Seq("Saved game message"),
        dungeon = Dungeon(testMode = true)
      )
      
      SaveGameSystem.saveGame(savedGameState)
      
      // Create a GameController in MainMenu
      val dummyGameState = GameState(
        playerEntityId = "dummy",
        entities = Seq(Entity("dummy", Movement(Point(0, 0)))),
        dungeon = Dungeon(testMode = true)
      )
      
      val gameController = GameController(UIState.MainMenu(), dummyGameState)
      
      // Navigate to Continue Game and confirm
      val menuOnContinue = gameController.uiState.asInstanceOf[UIState.MainMenu].selectNext
      val controllerOnContinue = gameController.copy(uiState = menuOnContinue)
      
      // Simulate confirming Continue Game
      val updatedController = controllerOnContinue.update(Some(Input.Confirm), System.nanoTime())
      
      // Should have loaded the saved game
      updatedController.uiState shouldBe UIState.Move
      updatedController.gameState.playerEntityId shouldBe "saved-player"
      updatedController.gameState.entities.head.position.shouldBe(Point(10, 10))
      updatedController.gameState.messages should contain("Saved game message")
    }
  }
}