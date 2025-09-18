package game.save

import org.scalatest.funsuite.AnyFunSuite
import game.{GameState, StartingState}
import scala.util.{Success, Failure}

class SaveGameSystemTest extends AnyFunSuite {

  test("SaveGameSystem gracefully handles non-browser environment") {
    val gameState = StartingState.startingGameState
    
    // In test environment (Node.js), localStorage should not be available
    assert(!SaveGameSystem.hasSaveGame())
    
    // Save attempt should fail gracefully with descriptive error
    SaveGameSystem.saveGame(gameState) match {
      case Failure(exception) =>
        assert(exception.getMessage.contains("localStorage not available"))
      case Success(_) =>
        fail("Save should fail in non-browser environment")
    }
    
    // Load attempt should fail gracefully
    SaveGameSystem.loadGame() match {
      case Failure(exception) =>
        assert(exception.getMessage.contains("localStorage not available"))
      case Success(_) =>
        fail("Load should fail in non-browser environment")
    }
    
    // Delete attempt should fail gracefully  
    SaveGameSystem.deleteSaveGame() match {
      case Failure(exception) =>
        assert(exception.getMessage.contains("localStorage not available"))
      case Success(_) =>
        fail("Delete should fail in non-browser environment")
    }
  }
  
  test("autoSave handles non-browser environment gracefully") {
    val gameState = StartingState.startingGameState
    
    // autoSave should not throw exceptions in test environment
    // It should fail silently with logged message
    SaveGameSystem.autoSave(gameState)
    // If we reach here without exception, the test passes
    succeed
  }
}