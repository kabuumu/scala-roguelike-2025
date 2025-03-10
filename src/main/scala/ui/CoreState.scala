package ui

import game.{Action, Direction, GameState}
import scalafx.scene.input.KeyCode
import ui.App.framesPerSecond


case class CoreState(uiState: UIState, gameState: GameState, lastUpdateTime: Long = 0) {
  def update(keyCode: KeyCode, currentTime: Long): CoreState = {
    if (currentTime - lastUpdateTime > 1000000000 / framesPerSecond) {
      val (newUIState, optAction) = uiState match
        case UIState.Move =>
          keyCode match
            case KeyCode.W => (UIState.Move, Some(Action.Move(Direction.Up)))
            case KeyCode.A => (UIState.Move, Some(Action.Move(Direction.Left)))
            case KeyCode.S => (UIState.Move, Some(Action.Move(Direction.Down)))
            case KeyCode.D => (UIState.Move, Some(Action.Move(Direction.Right)))
            case KeyCode.Space => (UIState.Attack(gameState.playerEntity.xPosition, gameState.playerEntity.yPosition), None)
            case _ => (UIState.Move, None)
        case UIState.Attack(cursorX, cursorY) =>
          keyCode match
            case KeyCode.W => (UIState.Attack(cursorX, cursorY - 1), None)
            case KeyCode.A => (UIState.Attack(cursorX - 1, cursorY), None)
            case KeyCode.S => (UIState.Attack(cursorX, cursorY + 1), None)
            case KeyCode.D => (UIState.Attack(cursorX + 1, cursorY), None)
            case KeyCode.Space => (UIState.Move, Some(Action.Attack(cursorX, cursorY)))
            case KeyCode.Escape => (UIState.Move, None)
            case _ => (UIState.Attack(cursorX, cursorY), None)

      val newGameState = gameState.update(optAction)

      if (newGameState != gameState || newUIState != uiState) {
        CoreState(newUIState, newGameState, currentTime)
      } else this
    }
    else {
      this //TODO - update to perform non-player actions
    }
  }
}
