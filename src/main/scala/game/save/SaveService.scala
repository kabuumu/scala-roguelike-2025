package game.save

import scala.util.Try
import game.GameState

/**
 * Abstraction for save game operations to decouple GameController from specific implementations
 */
trait SaveService {
  def saveGame(gameState: GameState): Try[Unit]
  def loadGame(): Try[GameState]
  def hasSaveGame(): Boolean
  def deleteSaveGame(): Try[Unit]
  def autoSave(gameState: GameState): Unit
}

/**
 * Browser-based implementation using localStorage
 */
object BrowserSaveService extends SaveService {
  def saveGame(gameState: GameState): Try[Unit] = SaveGameSystem.saveGame(gameState)
  def loadGame(): Try[GameState] = SaveGameSystem.loadGame()
  def hasSaveGame(): Boolean = SaveGameSystem.hasSaveGame()
  def deleteSaveGame(): Try[Unit] = SaveGameSystem.deleteSaveGame()
  def autoSave(gameState: GameState): Unit = SaveGameSystem.autoSave(gameState)
}