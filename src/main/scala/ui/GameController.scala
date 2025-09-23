package ui

import game.Input.*
import game.entity.SightMemory
import game.entity.Initiative.isReady
import game.system.event.GameSystemEvent.InputEvent
import game.*
import ui.UIState.UIState

object GameController {
  val framesPerSecond = 18
  val allowedActionsPerSecond = 6
  val ticksPerSecond: Long = 1000000000l
  val frameTime: Long = ticksPerSecond / allowedActionsPerSecond
}

case class GameController(uiState: UIState, gameState: GameState, lastUpdateTime: Long = 0) {
  import GameController.*
  
  def init(): GameController = {
    copy(gameState =
      gameState.updateEntity(
        gameState.playerEntity.id,
        gameState.playerEntity.update[SightMemory](_.update(gameState, entity = gameState.playerEntity))
      )
    )
  }

  def update(optInput: Option[Input], currentTime: Long): GameController = {
    val delta = currentTime - lastUpdateTime
    
    //To ensure updates only happen at a certain rate
    if (delta >= ticksPerSecond / framesPerSecond) {
      val (newUiState, optAction) = optInput match {
        case Some(input) if delta >= ticksPerSecond / allowedActionsPerSecond =>
          uiState match {
            case _: UIState.MainMenu | UIState.GameOver => InputHandler.handleInput(input, uiState, gameState)  // MainMenu and GameOver don't need ready check
            case _ if gameState.playerEntity.isReady => InputHandler.handleInput(input, uiState, gameState)  // Other states need ready check
            case _ => (uiState, None)
          }
        case _ =>
          (uiState, None)
      }
      
      // Check for special state transitions that require immediate return
      GameStateTransitions.handleSpecialStateTransitions(uiState, newUiState, optAction, gameState, currentTime)
        .getOrElse {
          // Continue with normal flow
          GameStateTransitions.performAutosave(uiState, gameState, optAction)
          
          val newGameState = gameState.updateWithSystems(optAction.map(
            action => InputEvent(gameState.playerEntity.id, action)
          ).toSeq)

          // Check for player death transition
          GameStateTransitions.handlePlayerDeathTransition(uiState, newGameState, currentTime)
            .getOrElse {
              // Normal game state update
              val newUpdateTime = if (
                newGameState.drawableChanges != gameState.drawableChanges
                  || newUiState != uiState
                  || newGameState.messages != gameState.messages
              ) {
                currentTime
              } else lastUpdateTime

              GameController(newUiState, newGameState, newUpdateTime)
            }
        }
    } else this
  }
}
