package ui

import game.{GameState, StartingState}
import game.save.SaveGameSystem
import game.entity.Health.isDead
import ui.UIState.UIState

/**
 * Handles special state transitions for the GameController.
 * This includes new game creation, save/load operations, and player death handling.
 */
object GameStateTransitions {
  
  def handleSpecialStateTransitions(
    currentUIState: UIState,
    newUIState: UIState, 
    optAction: Option[InputAction], 
    gameState: GameState,
    currentTime: Long
  ): Option[GameController] = {
    (newUIState, optAction) match {
      case (newState, None) if newState != currentUIState =>
        // This is likely a MainMenu -> NewGame transition
        currentUIState match {
          case _: UIState.MainMenu =>
            newState match {
              case UIState.Move =>
                // Starting a new game
                Some(GameController(UIState.Move, StartingState.startingGameState, currentTime).init())
              case _ => None // Continue with normal flow
            }
          case _ => None // Continue with normal flow
        }
      case (UIState.Move, Some(InputAction.LoadGame)) =>
        // Handle loading saved game
        SaveGameSystem.loadGame() match {
          case scala.util.Success(savedGameState) =>
            Some(GameController(UIState.Move, savedGameState, currentTime).init())
          case scala.util.Failure(exception) =>
            // Load failed, stay in current state with error message
            val errorGameState = gameState.addMessage(s"Failed to load save game: ${exception.getMessage}")
            Some(GameController(currentUIState, errorGameState, currentTime))
        }
      case _ => None // Continue with normal flow
    }
  }

  def handlePlayerDeathTransition(
    currentUIState: UIState,
    newGameState: GameState, 
    currentTime: Long
  ): Option[GameController] = {
    // Check for player death and transition to GameOver state
    currentUIState match {
      case UIState.GameOver(_) | UIState.MainMenu(_) => None // Already in GameOver, no change
      case _ if newGameState.playerEntity.isDead =>
        println(s"Player has died, transitioning to GameOver state. State is ${currentUIState}")
        Some(GameController(UIState.GameOver(newGameState.playerEntity), newGameState, currentTime))
      case _ => None // No death, continue
    }
  }

  def performAutosave(currentUIState: UIState, gameState: GameState, optAction: Option[InputAction]): Unit = {
    // Autosave before processing player actions (except for MainMenu and special actions)
    currentUIState match {
      case _: UIState.MainMenu => // Don't autosave in main menu
      case _ =>
        optAction match {
          case Some(InputAction.LoadGame) => // Don't autosave when loading
          case Some(_) => SaveGameSystem.autoSave(gameState) // Autosave before any other player action
          case None => // No action, no autosave needed
        }
    }
  }
}