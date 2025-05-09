package ui

import game.*
import game.Input.*
import game.Item.{Potion, Scroll}
import game.entity.*
import game.entity.Inventory.*
import ui.GameController.*
import ui.UIState.UIState

object GameController {
  val framesPerSecond = 12
  val allowedActionsPerSecond = 6
}

case class GameController(uiState: UIState, gameState: GameState, lastUpdateTime: Long = 0) {
  def init(): GameController = {
    copy(gameState =
      gameState.updateEntity(
        gameState.playerEntity.id,
        gameState.playerEntity.update[SightMemory](_.update(gameState, entity = gameState.playerEntity))
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
          val updatedGameState = newGameState.update()

          val newUpdateTime = if (
            updatedGameState.drawableChanges != gameState.drawableChanges
              || newUiState != uiState
              || updatedGameState.messages != gameState.messages
          ) {
            currentTime
          } else lastUpdateTime

          GameController(newUiState, updatedGameState, newUpdateTime)
      }
    } else this
  }

  private def handleInput(input: Input): (UIState, Option[Action]) = uiState match {
    case UIState.Move =>
      input match {
        case Input.Move(direction) => (UIState.Move, Some(MoveAction(direction)))
        case Input.Attack(attackType) =>
          val optWeapon = attackType match {
            case Input.PrimaryAttack => gameState.playerEntity[Inventory].primaryWeapon
            case Input.SecondaryAttack => gameState.playerEntity[Inventory].secondaryWeapon
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
        case Input.UseItem if gameState.playerEntity.groupedUsableItems.keys.nonEmpty =>
          val items = gameState.playerEntity.groupedUsableItems.keys.toSeq
          (UIState.SelectItem(items), None)
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
    case selectItem: UIState.SelectItem =>
      input match {
        case Input.Move(direction) => (selectItem.iterate, None)
        case Input.UseItem if selectItem.selectedItem == Potion =>
          (UIState.Move, Some(UseItemAction(selectItem.selectedItem, gameState.playerEntity)))
        case Input.UseItem if selectItem.selectedItem == Scroll =>
          (UIState.ScrollSelect(gameState.playerEntity[Movement].position), None)
        case Input.Cancel => (UIState.Move, None)
        case _ => (uiState, None)
      }
    case scrollSelect: UIState.ScrollSelect =>
      input match {
        case Input.Move(direction) =>
          val newCursor = scrollSelect.cursor + direction
          (UIState.ScrollSelect(newCursor), None)
        case Input.UseItem =>
          val targetPosition = scrollSelect.cursor
          (UIState.Move, Some(UseItemAction(Scroll, gameState.getActor(targetPosition).get)))
        case Input.Cancel => (UIState.Move, None)
        case _ => (uiState, None)
      }

  }

  def enemiesWithinRange(range: Int): Seq[Entity] = gameState.entities.filter { enemyEntity =>
    enemyEntity[EntityTypeComponent].entityType == EntityType.Enemy
      &&
      gameState.playerEntity[Movement].position.isWithinRangeOf(enemyEntity[Movement].position, range)
      &&
      gameState.getVisiblePointsFor(gameState.playerEntity).contains(enemyEntity[Movement].position)
      &&
      enemyEntity[Health].isAlive
  }
}
