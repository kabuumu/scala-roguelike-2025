package ui

import game.Input.*
import game.entity.*
import game.entity.{UsableItem, Targeting, Ammo} // New item system imports
import game.entity.EntityType.*
import game.entity.Experience.*
import game.entity.Health.*
import game.entity.Initiative.*
import game.entity.Inventory.*
import game.entity.WeaponItem.weaponItem
import game.entity.Movement.*
import game.system.event.GameSystemEvent.InputEvent
import game.*
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
      val (newUiState, optAction) = optInput match {
        case Some(input) if delta >= ticksPerSecond / allowedActionsPerSecond && gameState.playerEntity.isReady =>
          handleInput(input)
        case _ =>
          (uiState, None)
      }
      
      val newGameState = gameState.updateWithSystems(optAction.map(
        action => InputEvent(gameState.playerEntity.id, action)
      ).toSeq)

      val newUpdateTime = if (
        newGameState.drawableChanges != gameState.drawableChanges
          || newUiState != uiState
          || newGameState.messages != gameState.messages
      ) {
        currentTime
      } else lastUpdateTime

      GameController(newUiState, newGameState, newUpdateTime)
    } else this
  }

  private def handleInput(input: Input): (UIState, Option[InputAction]) = uiState match {
    case UIState.Move =>
      input match {
        case Input.Move(direction) =>
          (UIState.Move, Some(InputAction.Move(direction)))
        case Input.Attack(attackType) =>
          val optWeaponEntity = attackType match {
            case Input.PrimaryAttack => gameState.playerEntity.primaryWeapon(gameState)
            case Input.SecondaryAttack => gameState.playerEntity.secondaryWeapon(gameState)
          }

          val range = optWeaponEntity.flatMap(_.weaponItem.map(_.range)).getOrElse(1) //Default to melee range

          val enemies = enemiesWithinRange(range)
          if (enemies.nonEmpty) {
            (UIState.ListSelect(
              list = enemies,
              effect = target => {
                (UIState.Move, Some(InputAction.Attack(target)))
              }
            ), None)
          } else {
            (UIState.Move, None)
          }
        case Input.UseItem =>
          val usableItems = gameState.playerEntity.usableItems(gameState)
          if (usableItems.nonEmpty) {
            (UIState.ListSelect[Entity](
              list = usableItems,
              effect = itemEntity => {
                // Handle different item types based on UsableItem component
                itemEntity.get[UsableItem] match {
                  case Some(usableItem) =>
                    usableItem.targeting match {
                      case Targeting.Self =>
                        // Self-targeted items (potions)
                        (UIState.Move, Some(InputAction.UseComponentItem(itemEntity.id)))
                      case Targeting.TileInRange(_) =>
                        // Tile-targeted items (scrolls)
                        (UIState.ScrollSelect(
                          cursor = gameState.playerEntity.position,
                          effect = targetPoint => (UIState.Move, Some(InputAction.UseComponentItemAtPoint(itemEntity.id, targetPoint)))
                        ), None)
                      case Targeting.EnemyActor =>
                        // Entity-targeted items (bows)
                        val enemies = enemiesWithinRange(10)
                        val hasRequiredAmmo = usableItem.ammo match {
                          case Some(ammoType) => gameState.playerEntity.inventoryItems(gameState).exists(_.get[Ammo].exists(_.tag == ammoType))
                          case None => true
                        }
                        if (enemies.nonEmpty && hasRequiredAmmo) {
                          (UIState.ListSelect(
                            list = enemies,
                            effect = target => (UIState.Move, Some(InputAction.UseComponentItemOnEntity(itemEntity.id, target.id)))
                          ), None)
                        } else {
                          (UIState.Move, None) // No enemies in range or no required ammo
                        }
                    }
                  case None =>
                    (UIState.Move, None) // Item has no UsableItem component
                }
              }
            ), None)
          } else {
            (UIState.Move, None)
          }
        case Input.LevelUp if gameState.playerEntity.canLevelUp =>
          val levelUpState = UIState.ListSelect(
            list = gameState.playerEntity.getPossiblePerks,
            effect = selectedPerk => {
              (UIState.Move, Some(InputAction.LevelUp(selectedPerk)))
            }
          )
          //Give player choice of level up perks
          (levelUpState, None)
        case Input.Equip => (UIState.Move, Some(InputAction.Equip))
        case Input.Wait => (UIState.Move, Some(InputAction.Wait))
        case _ => (uiState, None)
      }
    case listSelect: UIState.ListSelect[_] =>
      input match {
        case Input.Move(Direction.Up | Direction.Right) => (listSelect.iterateDown, None)
        case Input.Move(Direction.Down | Direction.Left) => (listSelect.iterate, None)
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
