package game.save

import scala.collection.mutable
import scala.util.Try
import game.GameState

/**
 * Test-friendly version of SaveGameSystem that uses in-memory storage
 * instead of browser localStorage for unit testing.
 */
object TestSaveGameSystem {
  private val storage = mutable.Map[String, String]()
  private val SAVE_KEY = "scala-roguelike-save-game"
  
  /**
   * Saves the current game state to in-memory storage
   */
  def saveGame(gameState: GameState): Try[Unit] = {
    Try {
      val jsonString = SaveGameSerializer.serialize(gameState)
      storage(SAVE_KEY) = jsonString
    }
  }
  
  /**
   * Loads a saved game state from in-memory storage
   */
  def loadGame(): Try[GameState] = {
    Try {
      val jsonString = storage.get(SAVE_KEY).getOrElse {
        throw new RuntimeException("No save game found")
      }
      SaveGameSerializer.deserialize(jsonString).get
    }
  }
  
  /**
   * Checks if a save game exists in in-memory storage
   */
  def hasSaveGame(): Boolean = {
    storage.contains(SAVE_KEY) && storage(SAVE_KEY).nonEmpty
  }
  
  /**
   * Deletes the saved game from in-memory storage
   */
  def deleteSaveGame(): Try[Unit] = {
    Try {
      storage.remove(SAVE_KEY)
    }
  }
  
  /**
   * Auto-saves the game state before a player action.
   */
  def autoSave(gameState: GameState): Unit = {
    saveGame(gameState) match {
      case scala.util.Success(_) => 
        // Auto-save successful, continue silently
      case scala.util.Failure(exception) =>
        // Log error but don't interrupt gameplay
        println(s"Auto-save failed: ${exception.getMessage}")
    }
  }
  
  /**
   * Clear all storage - useful for testing
   */
  def clearStorage(): Unit = {
    storage.clear()
  }
}