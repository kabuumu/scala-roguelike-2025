package game.system

import game.GameState
import game.entity.*
import game.entity.EntityType.entityType
import game.entity.Health.hasFullHealth
import game.entity.Movement.position
import game.entity.Inventory.inventoryItems
import game.system.event.GameSystemEvent
import game.system.event.GameSystemEvent.*
import ui.InputAction

/**
 * New unified item use system that handles all usable items through the UsableItem component.
 * Replaces both LegacyItemUseSystem and ComponentItemUseSystem with a data-driven approach.
 * Now uses type-safe functions without placeholder strings and with proper targeting context.
 */
object ItemUseSystem extends GameSystem {
  override def update(gameState: GameState, events: Seq[GameSystemEvent.GameSystemEvent]): (GameState, Seq[GameSystemEvent.GameSystemEvent]) = {
    val newEvents = events.flatMap {
      case GameSystemEvent.InputEvent(entityId, InputAction.UseComponentItem(itemEntityId)) =>
        handleSelfTargetedItem(gameState, entityId, itemEntityId)
      
      case GameSystemEvent.InputEvent(entityId, InputAction.UseComponentItemAtPoint(itemEntityId, targetPoint)) =>
        handlePointTargetedItem(gameState, entityId, itemEntityId, targetPoint)
        
      case GameSystemEvent.InputEvent(entityId, InputAction.UseComponentItemOnEntity(itemEntityId, targetEntityId)) =>
        handleEntityTargetedItem(gameState, entityId, itemEntityId, targetEntityId)
        
      case _ =>
        Nil
    }

    (gameState, newEvents)
  }

  /**
   * Handle items that target the user themselves (Self-targeting)
   * Examples: healing potions
   */
  private def handleSelfTargetedItem(gameState: GameState, userId: String, itemEntityId: String): Seq[GameSystemEvent.GameSystemEvent] = {
    (gameState.getEntity(userId), gameState.getEntity(itemEntityId)) match {
      case (Some(user), Some(itemEntity)) =>
        UsableItem.getUsableItem(itemEntity) match {
          case Some(selfItem: SelfTargetingItem) =>
            generateSelfTargetingEvents(gameState, user, itemEntity, selfItem)
          case _ =>
            Nil
        }
      case _ =>
        Nil
    }
  }

  /**
   * Handle items that target a specific point (Tile-targeting)
   * Examples: fireball scrolls
   */
  private def handlePointTargetedItem(gameState: GameState, userId: String, itemEntityId: String, targetPoint: game.Point): Seq[GameSystemEvent.GameSystemEvent] = {
    (gameState.getEntity(userId), gameState.getEntity(itemEntityId)) match {
      case (Some(user), Some(itemEntity)) =>
        UsableItem.getUsableItem(itemEntity) match {
          case Some(tileItem: TileTargetingItem) =>
            generateTileTargetingEvents(gameState, user, itemEntity, tileItem, targetPoint)
          case _ =>
            Nil
        }
      case _ =>
        Nil
    }
  }

  /**
   * Handle items that target another entity (Entity-targeting)
   * Examples: bows targeting enemies
   */
  private def handleEntityTargetedItem(gameState: GameState, userId: String, itemEntityId: String, targetEntityId: String): Seq[GameSystemEvent.GameSystemEvent] = {
    (gameState.getEntity(userId), gameState.getEntity(itemEntityId), gameState.getEntity(targetEntityId)) match {
      case (Some(user), Some(itemEntity), Some(targetEntity)) =>
        UsableItem.getUsableItem(itemEntity) match {
          case Some(entityItem: EntityTargetingItem) =>
            generateEntityTargetingEvents(gameState, user, itemEntity, entityItem, targetEntity)
          case _ =>
            Nil
        }
      case _ =>
        Nil
    }
  }

  /**
   * Generate events for self-targeting items.
   * No target parameter needed - cleaner than the old EntityTarget(user) approach.
   */
  private def generateSelfTargetingEvents(
    gameState: GameState,
    user: Entity,
    itemEntity: Entity,
    usableItem: SelfTargetingItem
  ): Seq[GameSystemEvent.GameSystemEvent] = {

    // Check ammo requirements if this item needs ammo
    if (!hasRequiredAmmo(gameState, user, usableItem)) {
      return Nil
    }

    // Handle special case for healing when already at full health
    val effectEvents = if (user.hasFullHealth) {
      // Check if this is a healing item by examining the effects
      val sampleEvents = usableItem.effects(user)
      if (sampleEvents.exists(_.isInstanceOf[HealEvent])) {
        Seq(GameSystemEvent.MessageEvent(s"${System.nanoTime()}: ${user.entityType} is already at full health"))
      } else {
        sampleEvents
      }
    } else {
      usableItem.effects(user)
    }

    // Generate common events (consumption, ammo, initiative, message)
    effectEvents ++ generateCommonEvents(gameState, user, itemEntity, usableItem)
  }

  /**
   * Generate events for entity-targeting items.
   * Takes user and target entity directly - much cleaner.
   */
  private def generateEntityTargetingEvents(
    gameState: GameState,
    user: Entity,
    itemEntity: Entity,
    usableItem: EntityTargetingItem,
    targetEntity: Entity
  ): Seq[GameSystemEvent.GameSystemEvent] = {

    // Check ammo requirements if this item needs ammo
    if (!hasRequiredAmmo(gameState, user, usableItem)) {
      return Nil
    }

    val effectEvents = usableItem.effects(user, targetEntity)

    // Generate common events (consumption, ammo, initiative, message)
    effectEvents ++ generateCommonEvents(gameState, user, itemEntity, usableItem)
  }

  /**
   * Generate events for tile-targeting items.
   * Takes user and target point directly - much cleaner.
   */
  private def generateTileTargetingEvents(
    gameState: GameState,
    user: Entity,
    itemEntity: Entity,
    usableItem: TileTargetingItem,
    targetPoint: game.Point
  ): Seq[GameSystemEvent.GameSystemEvent] = {

    // Check ammo requirements if this item needs ammo
    if (!hasRequiredAmmo(gameState, user, usableItem)) {
      return Nil
    }

    val effectEvents = usableItem.effects(user, targetPoint)

    // Generate common events (consumption, ammo, initiative, message)
    effectEvents ++ generateCommonEvents(gameState, user, itemEntity, usableItem)
  }

  /**
   * Check if the user has the required ammo for this item
   */
  private def hasRequiredAmmo(gameState: GameState, user: Entity, usableItem: UsableItem): Boolean = {
    usableItem.ammo match {
      case Some(ammoType) =>
        user.inventoryItems(gameState).exists(_.exists[Ammo](_.ammoType == ammoType))
      case None =>
        true // No ammo required
    }
  }

  /**
   * Generate common events that apply to all item usage:
   * - Item consumption (if consumeOnUse = true)
   * - Ammo consumption (if ammo is specified)
   * - Initiative reset (turn-based behavior)
   * - Usage message
   */
  private def generateCommonEvents(
    gameState: GameState,
    user: Entity,
    itemEntity: Entity,
    usableItem: UsableItem
  ): Seq[GameSystemEvent.GameSystemEvent] = {

    // Add item consumption events if needed
    val consumptionEvents = if (usableItem.consumeOnUse) {
      Seq(GameSystemEvent.RemoveItemEntityEvent(user.id, itemEntity.id))
    } else {
      Nil
    }

    // Add ammo consumption events if needed
    val ammoConsumptionEvents = usableItem.ammo match {
      case Some(ammoType) =>
        user.inventoryItems(gameState).find(_.exists[Ammo](_.ammoType == ammoType)) match {
          case Some(ammo) =>
            Seq(GameSystemEvent.RemoveItemEntityEvent(user.id, ammo.id))
          case None =>
            Nil
        }
      case None => Nil
    }

    // Always reset initiative after using an item (turn-based behavior)
    val initiativeEvents = Seq(GameSystemEvent.ResetInitiativeEvent(user.id))

    // Create usage message
    val messageEvents = Seq(GameSystemEvent.MessageEvent(s"${System.nanoTime()}: ${user.entityType} used ${itemEntity.id}"))

    consumptionEvents ++ ammoConsumptionEvents ++ initiativeEvents ++ messageEvents
  }
}