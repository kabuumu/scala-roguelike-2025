package ui

import game.*
import game.Input.*
import game.Item.Potion
import ui.GameController.*
import ui.UIState.UIState

object GameController {
  val framesPerSecond = 16
  val allowedActionsPerSecond = 8
}

case class GameController(uiState: UIState, gameState: GameState, lastUpdateTime: Long = 0) {
  def init(): GameController = {
    copy(gameState =
      gameState.updateEntity(
        gameState.playerEntity.id,
        gameState.playerEntity.updateSightMemory(gameState)
      )
    )
  }

  def update(optInput: Option[Input], currentTime: Long): GameController = {
    val ticksPerSecond = 1000000000
    val delta = currentTime - lastUpdateTime

    //To ensure updates only happen at a certain rate
    if (delta > ticksPerSecond / framesPerSecond) {
      (optInput match {
        //To ensure inputs only happen at a certain rate
        case Some(input) if delta > ticksPerSecond / allowedActionsPerSecond =>
          val (newUiState, optAction) = handleInput(input)
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

  private def handleInput(input: Input): (UIState, Option[Action]) = uiState match {
    case UIState.Move =>
      input match {
        case Input.Move(direction) => (UIState.Move, Some(MoveAction(direction)))
        case Input.Attack(attackType) =>
          val optWeapon = attackType match {
            case Input.PrimaryAttack => gameState.playerEntity.inventory.primaryWeapon
            case Input.SecondaryAttack => gameState.playerEntity.inventory.secondaryWeapon
          }

          val range = optWeapon match {
            case Some(weapon) => weapon.range
            case None => 1 //Default to melee range
          }

          val enemies = enemiesWithinRange(range)
          if (enemies.nonEmpty) {
            (UIState.Attack(
              enemies = enemies,
              optWeapon = optWeapon
            ), None)
          } else {
            (UIState.Move, None)
          }
        case Input.UseItem => (UIState.Move, Some(UseItemAction(Potion)))
        case Input.Wait => (UIState.Move, Some(WaitAction))
        case _ => (uiState, None)
      }
    case attack: UIState.Attack =>
      input match {
        case Input.Move(direction) => (attack.iterate, None)
        case Input.Attack(_) =>
          val targetPosition = attack.position
          (UIState.Move, Some(AttackAction(targetPosition, attack.optWeapon)))
        case Input.Cancel => (UIState.Move, None)
        case _ => (uiState, None)
      }
    case _ => (uiState, None)
  }

  def enemiesWithinRange(range: Int): Seq[Entity] = gameState.entities.filter { enemyEntity =>
    enemyEntity.entityType == EntityType.Enemy
      &&
      gameState.playerEntity.position.isWithinRangeOf(enemyEntity.position, range)
      &&
      !enemyEntity.isDead
  }
}
