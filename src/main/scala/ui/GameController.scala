package ui

import game.Input.*
import game.entity.*
import game.entity.{UsableItem, Targeting, Ammo} // New item system imports
import game.entity.EntityType.*
import game.entity.Experience.*
import game.entity.Health.*
import game.entity.Hitbox.isWithinRangeOfHitbox
import game.entity.Initiative.*
import game.entity.Inventory.*
import game.entity.Movement.*
import game.entity.NameComponent.name
import game.entity.Equippable.isEquippable
import game.system.event.GameSystemEvent.InputEvent
import game.*
import game.save.SaveGameSystem // Add save system import
import ui.GameController.*
import ui.UIState.UIState

object GameController {
  val framesPerSecond = 18
  val allowedActionsPerSecond = 6
  val ticksPerSecond: Long = 1000000000l
  val frameTime: Long = ticksPerSecond / allowedActionsPerSecond
  
  // Unified action target for the new input system
  sealed trait ActionTarget {
    def entity: Entity
    def description: String
  }
  
  case class AttackTarget(entity: Entity) extends ActionTarget {
    def description: String = {
      val healthText = if (entity.has[Health]) {
        s" (${entity.currentHealth}/${entity.maxHealth} HP)"
      } else ""
      s"Attack ${entity.name.getOrElse("Enemy")}$healthText"
    }
  }
  
  case class EquipTarget(entity: Entity) extends ActionTarget {
    def description: String = {
      entity.get[Equippable] match {
        case Some(equippable) =>
          if (equippable.slot == EquipmentSlot.Weapon) {
            s"Equip ${equippable.itemName} (Damage bonus +${equippable.damageBonus})"
          } else {
            s"Equip ${equippable.itemName} (Damage reduction +${equippable.damageReduction})"
          }
        case None => s"Equip ${entity.name.getOrElse("Item")}"
      }
    }
  }
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
        case Some(input) if delta >= ticksPerSecond / allowedActionsPerSecond =>
          uiState match {
            case _: UIState.MainMenu => handleInput(input)  // MainMenu doesn't need ready check
            case _ if gameState.playerEntity.isReady => handleInput(input)  // Other states need ready check
            case _ => (uiState, None)
          }
        case _ =>
          (uiState, None)
      }
      
      // Handle special case for MainMenu transitions
      (newUiState, optAction) match {
        case (newState, None) if newState != uiState =>
          // This is likely a MainMenu -> NewGame transition
          uiState match {
            case _: UIState.MainMenu =>
              newState match {
                case UIState.Move =>
                  // Starting a new game, return a fresh GameController
                  return GameController(UIState.Move, StartingState.startingGameState, currentTime).init()
                case _ => // Continue with normal flow
              }
            case _ => // Continue with normal flow
          }
        case (UIState.Move, Some(InputAction.LoadGame)) =>
          // Handle loading saved game
          SaveGameSystem.loadGame() match {
            case scala.util.Success(savedGameState) =>
              return GameController(UIState.Move, savedGameState, currentTime).init()
            case scala.util.Failure(exception) =>
              // Load failed, stay in current state with error message
              val errorGameState = gameState.addMessage(s"Failed to load save game: ${exception.getMessage}")
              return GameController(uiState, errorGameState, currentTime)
          }
        case _ => // Continue with normal flow
      }
      
      // Autosave before processing player actions (except for MainMenu and special actions)
      uiState match {
        case _: UIState.MainMenu => // Don't autosave in main menu
        case _ =>
          optAction match {
            case Some(InputAction.LoadGame) => // Don't autosave when loading
            case Some(_) => SaveGameSystem.autoSave(gameState) // Autosave before any other player action
            case None => // No action, no autosave needed
          }
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
                        val enemies = enemiesWithinRange(range)
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
          val targets = nearbyActionTargets()
          if (targets.nonEmpty) {
            (UIState.ListSelect(
              list = targets,
              effect = target => {
                target match {
                  case GameController.AttackTarget(entity) =>
                    (UIState.Move, Some(InputAction.Attack(entity)))
                  case GameController.EquipTarget(entity) =>
                    // Instead of InputAction.Equip, we need to target specific equipment
                    (UIState.Move, Some(InputAction.EquipSpecific(entity)))
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

  }

  def enemiesWithinRange(range: Int): Seq[Entity] = gameState.entities.filter { enemyEntity =>
    enemyEntity.entityType == EntityType.Enemy
      &&
      // Use hitbox-aware range checking for multi-tile entities
      enemyEntity.isWithinRangeOfHitbox(gameState.playerEntity, range)
      &&
      gameState.getVisiblePointsFor(gameState.playerEntity).contains(enemyEntity.position)
      &&
      enemyEntity.isAlive
  }.sortBy(enemyEntity => enemyEntity.position.getChebyshevDistance(gameState.playerEntity.position))
  
  def nearbyActionTargets(): Seq[GameController.ActionTarget] = {
    import GameController.*
    val playerPosition = gameState.playerEntity.position
    val adjacentPositions = Direction.values.map(dir => playerPosition + Direction.asPoint(dir)).toSet
    
    // Get nearby enemies for attacking (range 1)
    val attackTargets = enemiesWithinRange(1).map(AttackTarget.apply)
    
    // Get nearby equippable items
    val equipTargets = gameState.entities
      .filter(e => adjacentPositions.contains(e.position))
      .filter(_.isEquippable)
      .map(EquipTarget.apply)
    
    attackTargets ++ equipTargets
  }
}
