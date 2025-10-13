package ui

import game.Input.*
import game.entity.*
import game.entity.{UsableItem, Targeting, Ammo}
import game.entity.EntityType.*
import game.entity.Experience.*
import game.entity.Initiative.*
import game.entity.Inventory.*
import game.entity.Movement.*
import game.entity.NameComponent.name
import game.entity.Equippable.isEquippable
import game.*
import ui.ActionTargets.*
import ui.UIState.UIState

/**
 * Handles input processing for different UI states.
 * This contains the main input handling logic that was previously in GameController.
 */
object InputHandler {
  
  def handleInput(
    input: Input, 
    uiState: UIState, 
    gameState: GameState
  ): (UIState, Option[InputAction]) = uiState match {
    case mainMenu: UIState.MainMenu =>
      input match {
        case Input.Move(Direction.Up) => (mainMenu.selectPrevious, None)
        case Input.Move(Direction.Down) => (mainMenu.selectNext, None)
        case Input.UseItem | Input.Attack(_) | Input.Confirm | Input.Action =>
          if (mainMenu.canConfirmCurrentSelection) {
            mainMenu.getSelectedOption match {
              case "New Game" => (UIState.Move, None)  // Just transition to Move, update() will handle creating new game
              case "Continue Game" => (UIState.Move, Some(InputAction.LoadGame))  // Load saved game
              case "Debug Menu" => (UIState.DebugMenu(), None)  // Enter debug menu
              case _ => (mainMenu, None)
            }
          } else {
            (mainMenu, None) // Can't confirm disabled options
          }
        case _ => (mainMenu, None)
      }
    case debugMenu: UIState.DebugMenu =>
      input match {
        case Input.Move(Direction.Left) => (debugMenu.previousSprite, None)
        case Input.Move(Direction.Right) => (debugMenu.nextSprite, None)
        case Input.Cancel => (UIState.MainMenu(), None)
        case _ => (debugMenu, None)
      }
    case UIState.Move =>
      input match {
        case Input.Move(direction) =>
          (UIState.Move, Some(InputAction.Move(direction)))
        case Input.UseItem =>
          val usableItems = gameState.playerEntity.usableItems(gameState).distinctBy(_.get[NameComponent])
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
                        (UIState.Move, 
                          Some(InputAction.UseItem(
                            itemId = itemEntity.id, 
                            itemType = usableItem, 
                            useContext = UseContext(gameState.playerEntity.id, None)
                          ))
                        )
                      case Targeting.TileInRange(_) =>
                        // Tile-targeted items (scrolls)
                        (UIState.ScrollSelect(
                          cursor = gameState.playerEntity.position,
                          effect = targetPoint => (UIState.Move, Some(InputAction.UseItem(itemEntity.id, usableItem, UseContext(gameState.playerEntity.id, Some(targetPoint)))))
                        ), None)
                      case Targeting.EnemyActor(range) =>
                        // Entity-targeted items (bows)
                        val enemies = GameTargeting.enemiesWithinRange(gameState, range)
                        val hasRequiredAmmo = usableItem.chargeType match {
                          case ChargeType.Ammo(ammoType) => gameState.playerEntity.inventoryItems(gameState).exists(_.exists[Ammo](_.ammoType == ammoType))
                          case _ => true
                        }
                        if (enemies.nonEmpty && hasRequiredAmmo) {
                          (UIState.ListSelect(
                            list = enemies,
                            effect = target => (
                              UIState.Move,
                              Some(InputAction.UseItem(
                                itemId = itemEntity.id,
                                itemType = usableItem,
                                useContext = UseContext(gameState.playerEntity.id, Some(target))
                              ))
                          )), 
                            None)
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
        case Input.Action =>
          val targets = GameTargeting.nearbyActionTargets(gameState)
          if (targets.nonEmpty) {
            (UIState.ListSelect(
              list = targets,
              effect = target => {
                target match {
                  case ActionTargets.AttackTarget(entity) =>
                    (UIState.Move, Some(InputAction.Attack(entity)))
                  case ActionTargets.EquipTarget(entity) =>
                    // Instead of InputAction.Equip, we need to target specific equipment
                    (UIState.Move, Some(InputAction.EquipSpecific(entity)))
                  case ActionTargets.DescendStairsTarget(_) =>
                    (UIState.Move, Some(InputAction.DescendStairs))
                }
              }
            ), None)
          } else {
            (UIState.Move, None)
          }
        case Input.Wait => (UIState.Move, Some(InputAction.Wait))
        case _ => (uiState, None)
      }
    case listSelect: UIState.ListSelect[_] =>
      input match {
        case Input.Move(Direction.Down | Direction.Left) => (listSelect.iterateDown, None)
        case Input.Move(Direction.Up | Direction.Right) => (listSelect.iterate, None)
        case Input.UseItem | Input.Action =>
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
        case Input.UseItem | Input.Action =>
          scrollSelect.action
        case Input.Cancel => (UIState.Move, None)
        case _ => (uiState, None)
      }
    case _: UIState.GameOver =>
      input match {
        case Input.UseItem | Input.Confirm | Input.Action | Input.Attack(_) =>
          // Return to main menu on action key press
          (UIState.MainMenu(), None)
        case _ => (uiState, None)
      }
  }
}