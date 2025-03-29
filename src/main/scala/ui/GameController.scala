package ui

import game.*
import scalafx.scene.input.KeyCode
import scalafx.App.{allowedActionsPerSecond, framesPerSecond}
import ui.UIState.UIState


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
      case (UIState.Move, KeyCode.Space) => 
        if (enemiesWithinRange.isEmpty) (UIState.Move, None)
        else (UIState.AttackList(enemiesWithinRange.toSeq, 0), None)
      case (attack: UIState.AttackList, KeyCode.W) => (attack.iterate, None)
      case (attack: UIState.AttackList, KeyCode.A) => (attack.iterate, None)
      case (attack: UIState.AttackList, KeyCode.S) => (attack.iterate, None)
      case (attack: UIState.AttackList, KeyCode.D) => (attack.iterate, None)
      case (UIState.AttackList(enemies, position), KeyCode.Space) =>
        val Point(targetX, targetY) = enemies(position).position
        (UIState.Move, Some(AttackAction(targetX, targetY)))
      case (_: UIState.AttackList, KeyCode.Escape) => (UIState.Move, None)
      case _ => (uiState, None)
    }

  private val enemiesWithinRange: Set[Entity] = gameState.entities.filter { enemyEntity =>
    enemyEntity.entityType == EntityType.Enemy
      &&
      gameState.playerEntity.position.isWithinRangeOf(enemyEntity.position, 1)
  }
}
