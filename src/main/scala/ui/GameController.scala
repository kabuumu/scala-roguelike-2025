package ui

import game.Input.*
import game.Item.*
import game.Item.ItemEffect.{NonTargeted, PointTargeted, EntityTargeted}
import game.action.*
import game.entity.*
import game.entity.Inventory.*
import game.{Item, *}
import ui.GameController.*
import ui.UIState.UIState

object GameController {
  val framesPerSecond = 12
  val allowedActionsPerSecond = 6
  val ticksPerSecond: Long = 1000000000l
  val frameTime: Long = ticksPerSecond / allowedActionsPerSecond
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
    val delta = currentTime - lastUpdateTime
    
    //To ensure updates only happen at a certain rate
    if (delta >= ticksPerSecond / framesPerSecond) {
      (optInput match {
        //To ensure inputs only happen at a certain rate
        case Some(input) if delta >= ticksPerSecond / allowedActionsPerSecond =>
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
            (UIState.ListSelect(
              list = enemies,
              effect = target => {
                (UIState.Move, Some(AttackAction(target[Movement].position, optWeapon)))
              }
            ), None)
          } else {
            (UIState.Move, None)
          }
        case Input.UseItem if gameState.playerEntity.groupedUsableItems.keys.nonEmpty =>
          val items = gameState.playerEntity.groupedUsableItems.keys.toSeq
          (UIState.ListSelect[UsableItem](
            list = items,
            effect = _.itemEffect match {
              case EntityTargeted(effect) =>
                val enemies = enemiesWithinRange(5) //TODO - default range for now
                if (enemies.nonEmpty) {
                  (UIState.ListSelect(
                    list = enemies,
                    effect = target => {
                      (UIState.Move, Some(UseItemAction(effect(target))))
                    }
                  ), None)
                } else {
                  (UIState.Move, None)
                }
              case PointTargeted(effect) =>
                (UIState.ScrollSelect(
                  gameState.playerEntity[Movement].position,
                  target => (
                    UIState.Move,
                    Some(UseItemAction(effect(target))))
                ), None)
              case NonTargeted(effect) =>
                (UIState.Move, Some(UseItemAction(effect)))
            }
          ), None)
        case Input.Wait => (UIState.Move, Some(WaitAction))
        case _ => (uiState, None)
      }
    case listSelect: UIState.ListSelect[_] =>
      input match {
        case Input.Move(direction) => (listSelect.iterate, None)
        case Input.UseItem | Input.Attack(_) =>
          listSelect.action
        case Input.Cancel => (UIState.Move, None)
        case _ => (uiState, None)
      }
    case scrollSelect: UIState.ScrollSelect =>
      input match {
        case Input.Move(direction) =>
          val newCursor = scrollSelect.cursor + direction
          if(newCursor.isWithinRangeOf(gameState.playerEntity[Movement].position, 8)
            && gameState.getVisiblePointsFor(gameState.playerEntity).contains(newCursor)) {
            (scrollSelect.copy(cursor = newCursor), None)
          } else {
            (scrollSelect, None)
          }
        case Input.UseItem | Input.Attack(_) =>
          scrollSelect.action
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
  }.sortBy(enemyEntity => enemyEntity[Movement].position.getChebyshevDistance(gameState.playerEntity[Movement].position))
}
