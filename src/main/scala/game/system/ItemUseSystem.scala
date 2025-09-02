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
 * Now uses type-safe functions instead of placeholder strings and parameterization.
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
   * Handle items that target the user themselves (Targeting.Self)
   * Examples: healing potions
   */
  private def handleSelfTargetedItem(gameState: GameState, userId: String, itemEntityId: String): Seq[GameSystemEvent.GameSystemEvent] = {
    (gameState.getEntity(userId), gameState.getEntity(itemEntityId)) match {
      case (Some(user), Some(itemEntity)) =>
        UsableItem.getUsableItem(itemEntity) match {
          case Some(usableItem) if isSelfTargeting(usableItem) =>
            // We know this is a self-targeting item, so we can safely cast
            val selfItem = usableItem.asInstanceOf[UsableItem[EntityTarget]]
            generateItemUseEvents(gameState, user, itemEntity, selfItem, EntityTarget(user))
          case _ =>
            Nil
        }
      case _ =>
        Nil
    }
  }

  /**
   * Handle items that target a specific point (Targeting.TileInRange)
   * Examples: fireball scrolls
   */
  private def handlePointTargetedItem(gameState: GameState, userId: String, itemEntityId: String, targetPoint: game.Point): Seq[GameSystemEvent.GameSystemEvent] = {
    (gameState.getEntity(userId), gameState.getEntity(itemEntityId)) match {
      case (Some(user), Some(itemEntity)) =>
        UsableItem.getUsableItem(itemEntity) match {
          case Some(usableItem) if isTileTargeting(usableItem) =>
            // We know this is a tile-targeting item, so we can safely cast
            val tileItem = usableItem.asInstanceOf[UsableItem[TileTarget]]
            generateItemUseEvents(gameState, user, itemEntity, tileItem, TileTarget(targetPoint))
          case _ =>
            Nil
        }
      case _ =>
        Nil
    }
  }

  /**
   * Handle items that target another entity (Targeting.EnemyActor)
   * Examples: bows targeting enemies
   */
  private def handleEntityTargetedItem(gameState: GameState, userId: String, itemEntityId: String, targetEntityId: String): Seq[GameSystemEvent.GameSystemEvent] = {
    (gameState.getEntity(userId), gameState.getEntity(itemEntityId), gameState.getEntity(targetEntityId)) match {
      case (Some(user), Some(itemEntity), Some(targetEntity)) =>
        UsableItem.getUsableItem(itemEntity) match {
          case Some(usableItem) if isEnemyTargeting(usableItem) =>
            // We know this is an enemy-targeting item, so we can safely cast
            val enemyItem = usableItem.asInstanceOf[UsableItem[EntityTarget]]
            generateItemUseEvents(gameState, user, itemEntity, enemyItem, EntityTarget(targetEntity))
          case _ =>
            Nil
        }
      case _ =>
        Nil
    }
  }

  /**
   * Generate all events needed for using an item.
   * Now uses the type-safe function approach instead of parameterization.
   */
  private def generateItemUseEvents[T <: TargetType](
    gameState: GameState,
    user: Entity,
    itemEntity: Entity,
    usableItem: UsableItem[T],
    target: T
  ): Seq[GameSystemEvent.GameSystemEvent] = {

    // Check ammo requirements if this item needs ammo
    val hasAmmo = usableItem.ammo match {
      case Some(ammoType) =>
        user.inventoryItems(gameState).exists(_.exists[Ammo](_.ammoType == ammoType))
      case None =>
        true // No ammo required
    }

    if (!hasAmmo) {
      return Nil
    }

    // Handle special case for healing when already at full health
    val effectEvents = target match {
      case EntityTarget(entity) if entity == user && user.hasFullHealth =>
        // Check if this is a healing item by examining the effects
        val sampleEvents = usableItem.effects(user, target)
        if (sampleEvents.exists(_.isInstanceOf[HealEvent])) {
          Seq(GameSystemEvent.MessageEvent(s"${System.nanoTime()}: ${user.entityType} is already at full health"))
        } else {
          sampleEvents
        }
      case _ =>
        usableItem.effects(user, target)
    }

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

    // Return all events to be processed by the appropriate systems
    effectEvents ++ consumptionEvents ++ ammoConsumptionEvents ++ initiativeEvents ++ messageEvents
  }

  // Helper methods to check targeting types safely
  private def isSelfTargeting(usableItem: UsableItem[?]): Boolean = {
    usableItem.targeting == Targeting.Self
  }

  private def isEnemyTargeting(usableItem: UsableItem[?]): Boolean = {
    usableItem.targeting == Targeting.EnemyActor
  }

  private def isTileTargeting(usableItem: UsableItem[?]): Boolean = {
    usableItem.targeting.isInstanceOf[Targeting.TileInRange]
  }
}