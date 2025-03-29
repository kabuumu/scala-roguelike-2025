package ui

import game._
import scalafx.scene.input.KeyCode
import scalafx.App.{allowedActionsPerSecond, framesPerSecond}


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
    val ticksPerSecond = 1000000000
    val delta = currentTime - lastUpdateTime

    //To ensure updates only happen at a certain rate
    if (delta > ticksPerSecond / framesPerSecond) {
      (keyCode match {
        //To ensure inputs only happen at a certain rate
        case Some(keycode) if delta > ticksPerSecond / allowedActionsPerSecond =>
          val (newUiState, optAction) = handleInput(keycode)
          val newGameState = gameState.update(optAction)

          (newUiState, newGameState)
        case _ => (uiState, gameState)
      }) match {
        case (newUiState, newGameState) =>
          val newUpdateTime = if (newGameState != gameState || newUiState != uiState) {
            currentTime
          } else lastUpdateTime

          GameController(newUiState, newGameState.update(), newUpdateTime)
      }
    } else this
  }

  private def handleInput(keyCode: KeyCode): (UIState, Option[Action]) = (uiState, keyCode) match {
      case (UIState.Move, KeyCode.W) => (UIState.Move, Some(MoveAction(Direction.Up)))
      case (UIState.Move, KeyCode.A) => (UIState.Move, Some(MoveAction(Direction.Left)))
      case (UIState.Move, KeyCode.S) => (UIState.Move, Some(MoveAction(Direction.Down)))
      case (UIState.Move, KeyCode.D) => (UIState.Move, Some(MoveAction(Direction.Right)))
      case (UIState.Move, KeyCode.Space) => (UIState.Attack(gameState.playerEntity.xPosition, gameState.playerEntity.yPosition), None)
      case (UIState.Attack(cursorX, cursorY), KeyCode.W) => (UIState.Attack(cursorX, cursorY - 1), None)
      case (UIState.Attack(cursorX, cursorY), KeyCode.A) => (UIState.Attack(cursorX - 1, cursorY), None)
      case (UIState.Attack(cursorX, cursorY), KeyCode.S) => (UIState.Attack(cursorX, cursorY + 1), None)
      case (UIState.Attack(cursorX, cursorY), KeyCode.D) => (UIState.Attack(cursorX + 1, cursorY), None)
      case (UIState.Attack(cursorX, cursorY), KeyCode.Space) => (UIState.Move, Some(AttackAction(cursorX, cursorY)))
      case (UIState.Attack(cursorX, cursorY), KeyCode.Escape) => (UIState.Move, None)
      case _ => (uiState, None)
    }

  private val enemiesWithinRange: Seq[Entity] = gameState.entities.filter { enemyEntity =>
    enemyEntity.entityType == EntityType.Enemy
      &&
      gameState.playerEntity.position.isWithinRangeOf(enemyEntity.position, 1)
  }
}
