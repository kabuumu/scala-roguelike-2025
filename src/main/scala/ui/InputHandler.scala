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
import game.entity.Conversation.conversation
import game.entity.ConversationAction.*
import ui.UIState.UIState

/** Handles input processing for different UI states. This contains the main
  * input handling logic that was previously in GameController.
  */
object InputHandler {

  def handleInput(
      input: Input,
      uiState: UIState,
      gameState: GameState
  ): (UIState, Option[InputAction]) = uiState match {
    case mainMenu: UIState.MainMenu =>
      input match {
        case Input.Move(Direction.Up)   => (mainMenu.selectPrevious, None)
        case Input.Move(Direction.Down) => (mainMenu.selectNext, None)
        case Input.UseItem | Input.Attack(_) | Input.Confirm | Input.Action =>
          if (mainMenu.canConfirmCurrentSelection) {
            mainMenu.getSelectedOption match {
              case "New Adventure" =>
                (
                  UIState.Move,
                  Some(InputAction.NewAdventure)
                )
              case "New Gauntlet" =>
                (
                  UIState.Move,
                  Some(InputAction.NewGauntlet)
                )
              case "Continue Game" =>
                (UIState.Move, Some(InputAction.LoadGame)) // Load saved game
              case _ => (mainMenu, None)
            }
          } else {
            (mainMenu, None) // Can't confirm disabled options
          }
        case _ => (mainMenu, None)
      }
    case UIState.Move =>
      input match {
        case Input.OpenMap =>
          (UIState.WorldMap, None)
        case Input.Move(direction) =>
          (UIState.Move, Some(InputAction.Move(direction)))
        case Input.Inventory =>
          (UIState.Inventory(), None)
        case Input.CharacterScreen =>
          (UIState.Character, None)

        case Input.LevelUp if gameState.playerEntity.canLevelUp =>
          val levelUpState = UIState.StatusEffectSelect(
            list = gameState.playerEntity.getPossiblePerks,
            effect = selectedPerk => {
              (UIState.Move, Some(InputAction.LevelUp(selectedPerk)))
            }
          )
          // Give player choice of level up perks
          (levelUpState, None)
        case Input.Action =>
          val targets = GameTargeting.nearbyActionTargets(gameState)
          if (targets.nonEmpty) {
            (
              UIState.ActionTargetSelect(
                list = targets,
                effect = target => {
                  target match {
                    case ActionTargets.AttackTarget(entity) =>
                      (UIState.Move, Some(InputAction.Attack(entity)))

                    case ActionTargets.DescendStairsTarget(_) =>
                      (UIState.Move, Some(InputAction.DescendStairs))
                    case ActionTargets.TradeTarget(entity) =>
                      // Redirect to ConversationTarget handling so we use one uniform menu system
                      // We can recursively call the effect or just copy the logic.
                      // Since we are inside the effect lambda, we can't easily recurse.
                      // Let's just create the InteractionState directly here too.

                      // Determine options for Trader (since we know it's a TradeTarget)
                      val options = Seq(
                        ("Buy", BuyAction),
                        ("Sell", SellAction),
                        ("Leave", CloseAction)
                      )

                      // Determine message
                      val message = entity
                        .get[game.entity.Conversation]
                        .map(_.text)
                        .orElse(entity.get[game.entity.Dialogue].map(_.message))
                        .getOrElse("Greetings traveler.")

                      (
                        UIState.InteractionState(entity, message, options),
                        None
                      )
                    case ActionTargets.ConversationTarget(entity) =>
                      // Generate options based on entity type
                      import game.entity.{Trader, Healer, Conversation}
                      import game.entity.Trader.isTrader
                      import game.entity.Healer.isHealer
                      import game.entity.Conversation.hasConversation

                      // Determine options
                      val options: Seq[(String, ConversationAction)] =
                        if (entity.isTrader) {
                          Seq(
                            ("Buy", BuyAction),
                            ("Sell", SellAction),
                            ("Leave", CloseAction)
                          )
                        } else if (entity.isHealer) {
                          entity.get[Healer] match {
                            case Some(healer) =>
                              Seq(
                                (
                                  s"Heal (${healer.cost}g)",
                                  HealAction(healer.healAmount, healer.cost)
                                ),
                                ("Leave", CloseAction)
                              )
                            case None => Seq(("Leave", CloseAction))
                          }
                        } else {
                          // Generic NPC or Fallback
                          // Currently ignoring Conversation component choices to enforce standard "Leave" only
                          // But we could pull them if we wanted custom menus for specific NPCs
                          Seq(("Leave", CloseAction))
                        }

                      // Determine message
                      val message = entity
                        .get[Conversation]
                        .map(_.text)
                        .orElse(entity.get[game.entity.Dialogue].map(_.message))
                        .getOrElse("...")

                      (
                        UIState.InteractionState(entity, message, options),
                        None
                      )
                  }
                }
              ),
              None
            )
          } else {
            (UIState.Move, None)
          }
        case Input.Wait      => (UIState.Move, Some(InputAction.Wait))
        case Input.DebugMenu =>
          // Toggle Debug Menu
          (UIState.DebugMenu(), None)
        case _ => (uiState, None)
      }
    case debugMenu: UIState.DebugMenu =>
      input match {
        case Input.Move(Direction.Up)     => (debugMenu.selectPrevious, None)
        case Input.Move(Direction.Down)   => (debugMenu.selectNext, None)
        case Input.Confirm | Input.Action =>
          debugMenu.getSelectedOption match {
            case "Give Item" =>
              val allItems = data.Items.ItemReference.values.toSeq
              (
                UIState.DebugGiveItemSelect(
                  list = allItems,
                  effect = itemRef =>
                    (
                      uiState, // Return to debug menu
                      Some(InputAction.DebugGiveItem(itemRef))
                    )
                ),
                None
              )
            case "Give Gold" =>
              (
                uiState,
                Some(InputAction.DebugGiveGold(100))
              )
            case "Give Experience" =>
              (
                uiState,
                Some(InputAction.DebugGiveExperience(experienceForLevel(2)))
              )
            case "Restore Health" =>
              (
                uiState,
                Some(InputAction.DebugRestoreHealth)
              )
            case "Give Perk" =>
              val allPerks = game.perk.Perks.allPerks.map(_.perk)
              (
                UIState.DebugGivePerkSelect(
                  list = allPerks,
                  effect = perk =>
                    (
                      uiState, // Return to debug menu
                      Some(InputAction.DebugGivePerk(perk))
                    )
                ),
                None
              )
            case "Reveal Map" =>
              (
                uiState,
                Some(InputAction.DebugRevealMap)
              )
            case _ => (uiState, None)
          }
        case Input.Cancel | Input.DebugMenu =>
          (UIState.Move, None) // Close menu
        case _ => (debugMenu, None)
      }
    case listSelect: UIState.ListSelectState =>
      input match {
        case Input.Move(Direction.Up | Direction.Left) =>
          listSelect match {
            case s: UIState.BuyItemSelect       => (s.iterateDown, None)
            case s: UIState.SellItemSelect      => (s.iterateDown, None)
            case s: UIState.StatusEffectSelect  => (s.iterateDown, None)
            case s: UIState.ActionTargetSelect  => (s.iterateDown, None)
            case s: UIState.EnemyTargetSelect   => (s.iterateDown, None)
            case s: UIState.DebugGiveItemSelect => (s.iterateDown, None)
            case s: UIState.DebugGivePerkSelect => (s.iterateDown, None)
          }
        case Input.Move(Direction.Down | Direction.Right) =>
          listSelect match {
            case s: UIState.BuyItemSelect       => (s.iterate, None)
            case s: UIState.SellItemSelect      => (s.iterate, None)
            case s: UIState.StatusEffectSelect  => (s.iterate, None)
            case s: UIState.ActionTargetSelect  => (s.iterate, None)
            case s: UIState.EnemyTargetSelect   => (s.iterate, None)
            case s: UIState.DebugGiveItemSelect => (s.iterate, None)
            case s: UIState.DebugGivePerkSelect => (s.iterate, None)
          }
        case Input.UseItem | Input.Action =>
          listSelect.action
        case Input.Cancel => (UIState.Move, None)
        case _            => (uiState, None)
      }
    case scrollSelect: UIState.ScrollSelect =>
      input match {
        case Input.Move(direction) =>
          val newCursor = scrollSelect.cursor + direction
          if (
            newCursor.isWithinRangeOf(gameState.playerEntity.position, 8)
            && gameState
              .getVisiblePointsFor(gameState.playerEntity)
              .contains(newCursor)
          ) {
            (scrollSelect.copy(cursor = newCursor), None)
          } else {
            (scrollSelect, None)
          }
        case Input.UseItem | Input.Action =>
          scrollSelect.action
        case Input.Cancel => (UIState.Move, None)
        case _            => (uiState, None)
      }
    case interactionState: UIState.InteractionState =>
      input match {
        case Input.Move(Direction.Up) =>
          (interactionState.selectPrevious, None)
        case Input.Move(Direction.Down) => (interactionState.selectNext, None)
        case Input.UseItem | Input.Action | Input.Confirm =>
          // Get the selected action
          val (_, action) = interactionState.getSelectedOption

          action match {
            case CloseAction =>
              (UIState.Move, None)

            case BuyAction =>
              // Transition to BuyItemSelect
              val trader = interactionState.entity
              trader.get[game.entity.Trader] match {
                case Some(traderComponent) =>
                  val buyableItems = traderComponent.tradeInventory.keys.toSeq
                  if (buyableItems.nonEmpty) {
                    (
                      UIState.BuyItemSelect(
                        list = buyableItems,
                        effect = itemRef => {
                          traderComponent.buyPrice(itemRef) match {
                            case Some(price)
                                if gameState.playerEntity.coins >= price =>
                              (
                                interactionState, // Return to interaction menu after buy
                                Some(InputAction.BuyItem(trader, itemRef))
                              )
                            case _ =>
                              (interactionState, None) // Can't afford
                          }
                        }
                      ),
                      None
                    )
                  } else {
                    (interactionState, None)
                  }
                case None => (UIState.Move, None)
              }

            case SellAction =>
              // Transition to SellItemSelect
              val trader = interactionState.entity

              // Helper logic for sell list (copied/refactored from TradeMenu)
              import game.entity.Equipment
              import scala.util.Random

              val inventoryItems =
                gameState.playerEntity.inventoryItems(gameState)

              // Get equipped items as entities
              val equippedItems = gameState.playerEntity
                .get[Equipment]
                .map(
                  _.getAllEquipped
                    .map { equippedItem =>
                      val equippable = equippedItem.stats
                      val itemRefOpt =
                        data.Items.ItemReference.values.find { ref =>
                          val tempEntity = ref.createEntity("temp")
                          tempEntity
                            .get[game.entity.Equippable]
                            .exists(_.itemName == equippable.itemName)
                        }
                      itemRefOpt.map { itemRef =>
                        // We can actually just look up the entity by ID now!
                        gameState
                          .getEntity(equippedItem.id)
                          .getOrElse(
                            // Fallback if not found (shouldn't happen)
                            itemRef.createEntity(
                              s"equipped-${equippable.itemName}-${scala.util.Random.nextString(8)}"
                            )
                          )
                      }
                    }
                    .flatten
                )
                .getOrElse(Seq.empty)

              val allItems = inventoryItems ++ equippedItems

              val sellableItems = allItems.filter { item =>
                trader.get[game.entity.Trader].exists { traderComp =>
                  traderComp.tradeInventory.keys.exists { ref =>
                    val refEntity = ref.createEntity("temp")
                    val itemName = item
                      .get[game.entity.Equippable]
                      .map(_.itemName)
                      .orElse(item.get[game.entity.NameComponent].map(_.name))
                    val refName = refEntity
                      .get[game.entity.Equippable]
                      .map(_.itemName)
                      .orElse(
                        refEntity.get[game.entity.NameComponent].map(_.name)
                      )
                    itemName.isDefined && itemName == refName
                  }
                }
              }

              if (sellableItems.nonEmpty) {
                (
                  UIState.SellItemSelect(
                    list = sellableItems,
                    effect = itemEntity => {
                      (
                        interactionState, // Return to interaction menu after sell
                        Some(InputAction.SellItem(trader, itemEntity))
                      )
                    }
                  ),
                  None
                )
              } else {
                (interactionState, None) // No sellable items
              }

            case heal: HealAction =>
              (
                interactionState,
                Some(
                  InputAction.ConversationAction(interactionState.entity, heal)
                )
              )

            case TradeAction =>
              // Legacy support or fallback
              (interactionState, None)
          }
        case Input.Cancel => (UIState.Move, None)
        case _            => (uiState, None)
      }

    case _: UIState.GameOver =>
      input match {
        case Input.UseItem | Input.Confirm | Input.Action | Input.Attack(_) =>
          // Return to main menu on action key press
          (UIState.MainMenu(), None)
        case _ => (uiState, None)
      }
    case inventory: UIState.Inventory =>
      input match {
        case Input.Move(direction) =>
          import game.Direction
          (inventory.moveCursor(Direction.asPoint(direction)), None)
        case Input.Confirm | Input.Action =>
          val player = gameState.playerEntity
          val inventoryItems = player.inventoryItems(gameState)
          val equippedItemIds = player
            .get[game.entity.Equipment]
            .map(_.getAllEquipped.map(_.id).toSet)
            .getOrElse(Set.empty)

          // All items are in inventoryItems (since equipping adds them to inventory)
          // We just need to display them.
          // Note: Logic in EquipmentSystem adds to inventory.

          val uniqueItems = inventoryItems.distinctBy { item =>
            item.get[game.entity.NameComponent].map(_.name).getOrElse(item.id)
          }

          val columns = 5
          val index = inventory.gridCursor.y * columns + inventory.gridCursor.x

          if (index >= 0 && index < uniqueItems.length) {
            val selectedItem = uniqueItems(index)

            import game.entity.UsableItem
            import game.entity.Equippable

            val isEquipped = equippedItemIds.contains(selectedItem.id)

            // Check component existence properly
            val canUse = selectedItem.has[UsableItem]
            val canEquip = selectedItem.has[Equippable] && !isEquipped
            val canUnequip = isEquipped
            val canDrop = true

            var options = Seq.empty[String]

            if (canUnequip) options = options :+ "Unequip"
            if (canEquip) options = options :+ "Equip"
            if (canUse) options = options :+ "Use"
            if (canDrop) options = options :+ "Drop"
            options = options :+ "Cancel"

            (UIState.InventoryActionState(selectedItem, options), None)
          } else {
            (inventory, None)
          }

        case Input.Inventory | Input.Cancel | Input.CharacterScreen =>
          (UIState.Move, None) // Close inventory
        case _ => (inventory, None)
      }

    case actionState: UIState.InventoryActionState =>
      input match {
        case Input.Move(Direction.Up)     => (actionState.selectPrevious, None)
        case Input.Move(Direction.Down)   => (actionState.selectNext, None)
        case Input.Confirm | Input.Action =>
          actionState.getSelectedOption match {
            case "Use" =>
              // Reuse UseItem logic
              val item = actionState.selectedItem
              item.get[game.entity.UsableItem] match {
                case Some(usable) =>
                  // We need to replicate the targeting logic from UseItemSelect here
                  // Ideally we refactor that into a helper function
                  // For now, let's just handle Self and Simple target,
                  // and if it needs complex targeting (Enemy/Tile), we could transition to those headers?
                  // Or just trigger the action and let value resolution happen?

                  usable.targeting match {
                    case Targeting.Self =>
                      (
                        UIState.Move,
                        Some(
                          InputAction.UseItem(
                            item.id,
                            usable,
                            UseContext(gameState.playerEntity.id, None)
                          )
                        )
                      )
                    case _ =>
                      // For complex targeting, we might need to go to a targeting state
                      // But we want to close inventory?
                      // "Use - ... drops back into the main game screen using the current use item system"
                      // So we should transition to the targeting states defined in Input.UseItem

                      // We can trigger a fake Input.UseItem transition? No.
                      // We can return the state corresponding to the start of targeting.

                      // Let's copy the logic from Input.UseItem for now or call a shared helper
                      // Since I cannot easily refactor in this tool call without viewing more, I will duplicate logic carefully
                      // actually, we can return (UIState.Move, Some(InputAction.UseItem...)) if it fails?

                      // Let's do the easy thing: Close inventory and return the Action that triggers the generic use system?
                      // InputAction.UseItem(..., target=None) might fail for things needing targets.

                      // Better: Transition to the specific TargetSelect state.
                      usable.targeting match {
                        case Targeting.TileInRange(_) =>
                          (
                            UIState.ScrollSelect(
                              gameState.playerEntity.position,
                              target =>
                                (
                                  UIState.Move,
                                  Some(
                                    InputAction.UseItem(
                                      item.id,
                                      usable,
                                      UseContext(
                                        gameState.playerEntity.id,
                                        Some(target)
                                      )
                                    )
                                  )
                                )
                            ),
                            None
                          )
                        case Targeting.EnemyActor(range) =>
                          val enemies =
                            GameTargeting.enemiesWithinRange(gameState, range)
                          if (enemies.nonEmpty) {
                            (
                              UIState.EnemyTargetSelect(
                                enemies,
                                effect = target =>
                                  (
                                    UIState.Move,
                                    Some(
                                      InputAction.UseItem(
                                        item.id,
                                        usable,
                                        UseContext(
                                          gameState.playerEntity.id,
                                          Some(target)
                                        )
                                      )
                                    )
                                  )
                              ),
                              None
                            )
                          } else {
                            (UIState.Move, None) // Failed to find targets
                          }
                        case _ => (UIState.Move, None)
                      }
                  }
                case None => (UIState.Move, None)
              }
            case "Equip" =>
              (
                UIState.Move,
                Some(InputAction.EquipSpecific(actionState.selectedItem))
              )
            case "Unequip" =>
              val slot = actionState.selectedItem
                .get[game.entity.Equippable]
                .map(_.slot)
                .getOrElse(game.entity.EquipmentSlot.Weapon)
              (UIState.Move, Some(InputAction.UnequipItem(slot)))
            case "Drop" =>
              (
                UIState.Move,
                Some(InputAction.DropItem(actionState.selectedItem.id))
              )
            case "Cancel" =>
              (UIState.Inventory(Point(0, 0)), None) // Go back to grid
            case _ => (actionState, None)
          }
        case Input.Cancel =>
          (UIState.Inventory(Point(0, 0)), None) // Back to grid
        case _ => (actionState, None)
      }

    case UIState.WorldMap =>
      // Any key press returns to normal game
      (UIState.Move, None)

    case UIState.Character =>
      input match {
        case Input.CharacterScreen | Input.Cancel => (UIState.Move, None)
        case _                                    => (uiState, None)
      }
  }
}
