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
import game.entity.Coins.coins
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
              case _ => (mainMenu, None)
            }
          } else {
            (mainMenu, None) // Can't confirm disabled options
          }
        case _ => (mainMenu, None)
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
                  case ActionTargets.TradeTarget(entity) =>
                    (UIState.TradeMenu(entity), Some(InputAction.Trade(entity)))
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
    case tradeMenu: UIState.TradeMenu =>
      input match {
        case Input.Move(Direction.Up) => (tradeMenu.selectPrevious, None)
        case Input.Move(Direction.Down) => (tradeMenu.selectNext, None)
        case Input.UseItem | Input.Action | Input.Confirm =>
          tradeMenu.getSelectedOption match {
            case "Buy" =>
              // Show list of items to buy from trader
              val trader = tradeMenu.trader
              trader.get[game.entity.Trader] match {
                case Some(traderComponent) =>
                  val buyableItems = traderComponent.tradeInventory.keys.toSeq
                  if (buyableItems.nonEmpty) {
                    (UIState.ListSelect[data.Items.ItemReference](
                      list = buyableItems,
                      effect = itemRef => {
                        traderComponent.buyPrice(itemRef) match {
                          case Some(price) if gameState.playerEntity.coins >= price =>
                            (UIState.TradeMenu(trader), Some(InputAction.BuyItem(trader, itemRef)))
                          case _ =>
                            (UIState.TradeMenu(trader), None) // Can't afford
                        }
                      }
                    ), None)
                  } else {
                    (tradeMenu, None)
                  }
                case None => (UIState.Move, None)
              }
            case "Sell" =>
              // Show player's inventory to sell (including equipped items)
              import game.entity.Equipment
              import scala.util.Random
              
              // Get inventory items
              val inventoryItems = gameState.playerEntity.inventoryItems(gameState)
              
              // Get equipped items as entities (create temporary entities for them)
              val equippedItems = gameState.playerEntity.get[Equipment]
                .map(_.getAllEquipped.map { equippable =>
                  // Find the ItemReference for this equipped item
                  val itemRefOpt = data.Items.ItemReference.values.find { ref =>
                    val tempEntity = ref.createEntity("temp")
                    tempEntity.get[game.entity.Equippable].exists(_.itemName == equippable.itemName)
                  }
                  
                  itemRefOpt.map { itemRef =>
                    // Create a temporary entity for display purposes
                    itemRef.createEntity(s"equipped-${equippable.itemName}-${Random.nextString(8)}")
                  }
                }.flatten)
                .getOrElse(Seq.empty)
              
              // Combine both lists
              val allItems = inventoryItems ++ equippedItems
              
              // Filter to only items the trader buys
              val sellableItems = allItems.filter { item =>
                item.get[game.entity.NameComponent].exists { nameComp =>
                  tradeMenu.trader.get[game.entity.Trader].exists { traderComp =>
                    traderComp.tradeInventory.keys.exists { ref =>
                      val refEntity = ref.createEntity("temp")
                      refEntity.get[game.entity.NameComponent].map(_.name) == Some(nameComp.name)
                    }
                  }
                }
              }
              if (sellableItems.nonEmpty) {
                (UIState.ListSelect[Entity](
                  list = sellableItems,
                  effect = itemEntity => {
                    (UIState.TradeMenu(tradeMenu.trader), Some(InputAction.SellItem(tradeMenu.trader, itemEntity)))
                  }
                ), None)
              } else {
                (tradeMenu, None) // No sellable items
              }
            case "Exit" =>
              (UIState.Move, None)
            case _ => (tradeMenu, None)
          }
        case Input.Cancel => (UIState.Move, None)
        case _ => (tradeMenu, None)
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