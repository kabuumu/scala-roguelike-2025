package ui

import game.{Action, Direction, GameState}
import scalafx.scene.input.KeyCode
import ui.App.framesPerSecond


case class GameController(uiState: UIState, gameState: GameState, lastUpdateTime: Long = 0) {
  def init(): GameController = {
    copy(gameState =
      gameState.updateEntity(
        gameState.playerEntity.id,
        gameState.playerEntity.updateSightMemory(gameState)
      )
    )
  }

  def update(keyCode: Option[KeyCode], currentTime: Long): GameController = {
    if (currentTime - lastUpdateTime > 1000000000 / framesPerSecond) {
      val (newUIState, optAction) = uiState match
        case UIState.Move =>
          keyCode match
            case Some(KeyCode.W) => (UIState.Move, Some(Action.Move(Direction.Up)))
            case Some(KeyCode.A) => (UIState.Move, Some(Action.Move(Direction.Left)))
            case Some(KeyCode.S) => (UIState.Move, Some(Action.Move(Direction.Down)))
            case Some(KeyCode.D) => (UIState.Move, Some(Action.Move(Direction.Right)))
            case Some(KeyCode.Space) => (UIState.Attack(gameState.playerEntity.xPosition, gameState.playerEntity.yPosition), None)
            case _ => (UIState.Move, None)
        case UIState.Attack(cursorX, cursorY) =>
          keyCode match
            case Some(KeyCode.W) => (UIState.Attack(cursorX, cursorY - 1), None)
            case Some(KeyCode.A) => (UIState.Attack(cursorX - 1, cursorY), None)
            case Some(KeyCode.S) => (UIState.Attack(cursorX, cursorY + 1), None)
            case Some(KeyCode.D) => (UIState.Attack(cursorX + 1, cursorY), None)
            case Some(KeyCode.Space) => (UIState.Move, Some(Action.Attack(cursorX, cursorY)))
            case Some(KeyCode.Escape) => (UIState.Move, None)
            case _ => (UIState.Attack(cursorX, cursorY), None)

      val newGameState = gameState.update(optAction).update()

      if (newGameState != gameState || newUIState != uiState) {
        GameController(newUIState, newGameState, currentTime)
      } else this
    }
    else {
      this //TODO - update to perform non-player actions
    }
  }
}
