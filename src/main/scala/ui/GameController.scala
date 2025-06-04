package ui

import game.Input.*
import game.Item.*
import game.Item.ChargeType.{Ammo, SingleUse}
import game.Item.ItemEffect.{EntityTargeted, NonTargeted, PointTargeted}
import game.action.*
import game.entity.*
import game.entity.EntityType.*
import game.entity.Experience.*
import game.entity.Health.*
import game.entity.Initiative.*
import game.entity.Inventory.*
import game.entity.Movement.*
import game.perk.IncreaseMaxHealthPerk
import game.{Item, *}
import ui.GameController.*
import ui.UIState.UIState

object GameController {
  val framesPerSecond = 18
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
        case Some(input) if delta >= ticksPerSecond / allowedActionsPerSecond && gameState.playerEntity.isReady =>
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
        case Input.Move(direction) =>
          (UIState.Move, Some(MoveAction(direction)))
        case Input.Attack(attackType) =>
          val optWeapon = attackType match {
            case Input.PrimaryAttack => gameState.playerEntity.get[Inventory].get.primaryWeapon
            case Input.SecondaryAttack => gameState.playerEntity.get[Inventory].get.secondaryWeapon
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
                (UIState.Move, Some(AttackAction(target.position, optWeapon)))
              }
            ), None)
          } else {
            (UIState.Move, None)
          }
        case Input.UseItem if gameState.playerEntity.groupedUsableItems.keys.nonEmpty =>
          val items = gameState.playerEntity.groupedUsableItems.keys.toSeq
          (UIState.ListSelect[UsableItem](
            list = items,
            effect = item => {
              val canUseItem = item.chargeType match {
                case SingleUse => true
                case Ammo(ammoItem) if gameState.playerEntity.items.contains(ammoItem) => true
                case _ => false
              }

              if (canUseItem) {
                item.itemEffect match {
                  case EntityTargeted(effect) =>
                    val enemies = enemiesWithinRange(10) //TODO - default range for now
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
                      gameState.playerEntity.position,
                      target => (
                        UIState.Move,
                        Some(UseItemAction(effect(target))))
                    ), None)
                  case NonTargeted(effect) =>
                    (UIState.Move, Some(UseItemAction(effect)))
                }
              } else {
                (UIState.Move, None)
              }
            }
          ), None)
        case Input.LevelUp if gameState.playerEntity.canLevelUp =>
          val levelUpState = UIState.ListSelect(
            list = gameState.playerEntity.getPossiblePerks,
            effect = selectedPerk => {
              (UIState.Move, Some(LevelUpAction(selectedPerk)))
            }
          )
          //Give player choice of level up perks
          (levelUpState, None)
        case Input.Wait => (UIState.Move, Some(WaitAction))
        case _ => (uiState, None)
      }
    case listSelect: UIState.ListSelect[_] =>
      input match {
        case Input.Move(direction) => (listSelect.iterate, None)
        case Input.UseItem | Input.Attack(_) | Input.Confirm =>
          listSelect.action
        case Input.Cancel => (UIState.Move, None)
        case _ => (uiState, None)
      }
    case scrollSelect: UIState.ScrollSelect =>
      input match {
        case Input.Move(direction) =>
          val newCursor = scrollSelect.cursor + direction
          if (newCursor.isWithinRangeOf(gameState.playerEntity.position, 8)
            && gameState.getVisiblePointsFor(gameState.playerEntity).contains(newCursor)) {
            (scrollSelect.copy(cursor = newCursor), None)
          } else {
            (scrollSelect, None)
          }
        case Input.UseItem | Input.Attack(_) | Input.Confirm =>
          scrollSelect.action
        case Input.Cancel => (UIState.Move, None)
        case _ => (uiState, None)
      }

  }

  def enemiesWithinRange(range: Int): Seq[Entity] = gameState.entities.filter { enemyEntity =>
    enemyEntity.entityType == EntityType.Enemy
      &&
      gameState.playerEntity.position.isWithinRangeOf(enemyEntity.position, range)
      &&
      gameState.getVisiblePointsFor(gameState.playerEntity).contains(enemyEntity.position)
      &&
      enemyEntity.isAlive
  }.sortBy(enemyEntity => enemyEntity.position.getChebyshevDistance(gameState.playerEntity.position))
}
