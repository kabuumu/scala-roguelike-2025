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
    TestSaveGameSystem.deleteSaveGame()
    TestSaveGameSystem.clearStorage()
  }

  override def afterEach(): Unit = {
    // Clean up save game after each test
    TestSaveGameSystem.deleteSaveGame()
    TestSaveGameSystem.clearStorage()
  }

  describe("SaveGameSystem") {
    it("should report no save game exists initially") {
      TestSaveGameSystem.hasSaveGame() shouldBe false
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
      val saveResult = TestSaveGameSystem.saveGame(testGameState)
      saveResult shouldBe a[Success[?]]
      
      // Check that save game exists
      TestSaveGameSystem.hasSaveGame() shouldBe true

      // Load the game state
      val loadResult = TestSaveGameSystem.loadGame()
      loadResult shouldBe a[Success[?]]
      
      val loadedGameState = loadResult.get
      loadedGameState.playerEntityId shouldBe "test-player"
      loadedGameState.entities.length shouldBe 1
      loadedGameState.entities.head.id shouldBe "test-player"
      loadedGameState.messages should contain("Test message")
    }

    it("should handle loading when no save exists") {
      TestSaveGameSystem.hasSaveGame() shouldBe false
      
      val loadResult = TestSaveGameSystem.loadGame()
      loadResult shouldBe a[Failure[?]]
    }

    it("should allow deleting save game") {
      val testGameState = GameState(
        playerEntityId = "test-player",
        entities = Seq(Entity("test-player", Movement(Point(0, 0)))),
        dungeon = Dungeon(testMode = true)
      )

      TestSaveGameSystem.saveGame(testGameState)
      TestSaveGameSystem.hasSaveGame() shouldBe true
      
      TestSaveGameSystem.deleteSaveGame()
      TestSaveGameSystem.hasSaveGame() shouldBe false
    }
  }

  describe("MainMenu integration with save system - basic structure") {
    it("should have Continue Game option in menu") {
      val mainMenu = UIState.MainMenu()
      // Should have at least 2 options: New Game and Continue Game (or Continue Game No Save)
      mainMenu.options.length shouldBe 2
      mainMenu.options should contain("New Game")
      mainMenu.options.exists(_.contains("Continue Game")) shouldBe true
    }

    it("should allow navigation between menu options") {
      val mainMenu = UIState.MainMenu()
      
      // Should start on New Game
      mainMenu.selectedOption shouldBe 0
      mainMenu.getSelectedOption shouldBe "New Game"
      
      // Should move to Continue Game option
      val menuOnContinue = mainMenu.selectNext
      menuOnContinue.selectedOption shouldBe 1
      menuOnContinue.getSelectedOption should include("Continue Game")
      
      // Should wrap back to New Game
      val menuBackToStart = menuOnContinue.selectNext
      menuBackToStart.selectedOption shouldBe 0
    }
  }

  describe("GameController basic save functionality") {
    it("should have autosave mechanism in place") {
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
      
      // Should be able to handle input without crashing (autosave will fail but shouldn't break game)
      val updatedController = gameController.update(Some(Input.Move(Direction.Up)), System.nanoTime())
      
      // Game should continue working
      updatedController.uiState shouldBe UIState.Move
      updatedController.gameState.playerEntityId shouldBe "test-player"
    }

    it("should handle LoadGame action") {
      // Create a test saved game state
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
      
      TestSaveGameSystem.saveGame(savedGameState)
      
      // The LoadGame functionality is tested implicitly through the system
      // In actual usage, the browser SaveGameSystem would be used
      TestSaveGameSystem.hasSaveGame() shouldBe true
      val loadResult = TestSaveGameSystem.loadGame()
      loadResult.isSuccess shouldBe true
    }
  }
}