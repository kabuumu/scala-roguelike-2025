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
   * Checks if localStorage is available (browser environment)
   */
  private def isLocalStorageAvailable: Boolean = {
    Try {
      js.typeOf(js.Dynamic.global.window) != "undefined" &&
      js.typeOf(js.Dynamic.global.window.localStorage) != "undefined"
    }.getOrElse(false)
  }
  
  /**
   * Saves the current game state to browser localStorage
   */
  def saveGame(gameState: GameState): Try[Unit] = {
    if (!isLocalStorageAvailable) {
      Failure(new RuntimeException("localStorage not available (non-browser environment)"))
    } else {
      Try {
        val jsonString = SaveGameSerializer.serialize(gameState)
        dom.window.localStorage.setItem(SAVE_KEY, jsonString)
      }
    }
  }
  
  /**
   * Loads a saved game state from browser localStorage
   */
  def loadGame(): Try[GameState] = {
    if (!isLocalStorageAvailable) {
      Failure(new RuntimeException("localStorage not available (non-browser environment)"))
    } else {
      Try {
        val jsonString = dom.window.localStorage.getItem(SAVE_KEY)
        if (jsonString == null) {
          throw new RuntimeException("No save game found")
        }
        SaveGameSerializer.deserialize(jsonString).get
      }
    }
  }
  
  /**
   * Checks if a save game exists in browser localStorage
   */
  def hasSaveGame(): Boolean = {
    if (!isLocalStorageAvailable) {
      false
    } else {
      Try {
        val jsonString = dom.window.localStorage.getItem(SAVE_KEY)
        jsonString != null && jsonString.nonEmpty
      }.getOrElse(false)
    }
  }
  
  /**
   * Deletes the saved game from browser localStorage
   */
  def deleteSaveGame(): Try[Unit] = {
    if (!isLocalStorageAvailable) {
      Failure(new RuntimeException("localStorage not available (non-browser environment)"))
    } else {
      Try {
        dom.window.localStorage.removeItem(SAVE_KEY)
      }
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
        // Only log error for unexpected failures (not environment limitations)
        if (!exception.getMessage.contains("localStorage not available")) {
          println(s"Auto-save failed: ${exception.getMessage}")
        }
        // Silent failure for non-browser environments (tests, server-side, etc.)
    }
  }
}