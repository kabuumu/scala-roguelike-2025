package game.save

import scala.scalajs.js
import org.scalajs.dom
import scala.util.{Try, Success, Failure}
import game.GameState

/**
 * Browser localStorage-based save game system for web deployment.
 * Provides persistent game state storage between browser sessions.
 */
object SaveGameSystem {
  
  private val SAVE_KEY = "scala-roguelike-save-game"
  
  /**
   * Saves the current game state to browser localStorage
   */
  def saveGame(gameState: GameState): Try[Unit] = {
    Try {
      val jsonString = SaveGameSerializer.serialize(gameState)
      dom.window.localStorage.setItem(SAVE_KEY, jsonString)
    }
  }
  
  /**
   * Loads a saved game state from browser localStorage
   */
  def loadGame(): Try[GameState] = {
    Try {
      val jsonString = dom.window.localStorage.getItem(SAVE_KEY)
      if (jsonString == null) {
        throw new RuntimeException("No save game found")
      }
      SaveGameSerializer.deserialize(jsonString).get
    }
  }
  
  /**
   * Checks if a save game exists in browser localStorage
   */
  def hasSaveGame(): Boolean = {
    Try {
      val jsonString = dom.window.localStorage.getItem(SAVE_KEY)
      jsonString != null && jsonString.nonEmpty
    }.getOrElse(false)
  }
  
  /**
   * Deletes the saved game from browser localStorage
   */
  def deleteSaveGame(): Try[Unit] = {
    Try {
      dom.window.localStorage.removeItem(SAVE_KEY)
    }
  }
  
  /**
   * Auto-saves the game state before a player action.
   * This should be called by the GameController before processing player input.
   */
  def autoSave(gameState: GameState): Unit = {
    saveGame(gameState) match {
      case Success(_) => 
        // Auto-save successful, continue silently
      case Failure(exception) =>
        // Log error but don't interrupt gameplay
        println(s"Auto-save failed: ${exception.getMessage}")
    }
  }
}