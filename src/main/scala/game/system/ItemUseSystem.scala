package game.system

import game.GameState
import game.entity.*
import game.entity.EntityType.entityType
import game.entity.Health.hasFullHealth
import game.entity.Movement.position
import game.entity.Inventory.inventoryItems
import game.entity.UsableItem.isUsableItem
import game.system.event.GameSystemEvent
import game.system.event.GameSystemEvent.*
import ui.InputAction

/**
 * New unified item use system that handles all usable items through the UsableItem component.
 * Replaces both LegacyItemUseSystem and ComponentItemUseSystem with a data-driven approach.
 * Now uses GameSystemEvent architecture for better integration with all game systems.
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
        itemEntity.get[UsableItem] match {
          case Some(usableItem) if usableItem.targeting == Targeting.Self =>
            generateItemUseEvents(gameState, user, itemEntity, usableItem, None, None)
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
        itemEntity.get[UsableItem] match {
          case Some(usableItem) if usableItem.targeting.isInstanceOf[Targeting.TileInRange] =>
            generateItemUseEvents(gameState, user, itemEntity, usableItem, Some(targetPoint), None)
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
        itemEntity.get[UsableItem] match {
          case Some(usableItem) if usableItem.targeting == Targeting.EnemyActor =>
            generateItemUseEvents(gameState, user, itemEntity, usableItem, None, Some(targetEntity))
          case _ =>
            Nil
        }
      case _ =>
        Nil
    }
  }

  /**
   * Generate all events needed for using an item.
   * This replaces the deprecated Event system with GameSystemEvent.
   */
  private def generateItemUseEvents(
    gameState: GameState,
    user: Entity,
    itemEntity: Entity,
    usableItem: UsableItem,
    targetPoint: Option[game.Point] = None,
    targetEntity: Option[Entity] = None
  ): Seq[GameSystemEvent.GameSystemEvent] = {

    // Check ammo requirements if this item needs ammo
    val hasAmmo = usableItem.ammo match {
      case Some(ammoType) =>
        user.inventoryItems(gameState).exists(_.exists[Ammo](_.ammoType == ammoType))
      case None =>
        true // No ammo required
    }

    // Parameterize the effects for the current usage context
    val effectEvents = usableItem.effects.map { effect =>
      parameterizeEffectForContext(effect, user, targetPoint, targetEntity)
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
    if(hasAmmo) {
      effectEvents ++ consumptionEvents ++ ammoConsumptionEvents ++ initiativeEvents ++ messageEvents
    } else Nil
  }

  /**
   * Parameterize a GameSystemEvent template for the current usage context.
   * This allows GameSystemEvents to be reused across different systems with different parameters.
   */
  private def parameterizeEffectForContext(
    effect: GameSystemEvent.GameSystemEvent,
    user: Entity,
    targetPoint: Option[game.Point],
    targetEntity: Option[Entity]
  ): GameSystemEvent.GameSystemEvent = {
    effect match {
      case GameSystemEvent.HealEvent(_, amount) =>
        if (user.hasFullHealth) {
          GameSystemEvent.MessageEvent(s"${System.nanoTime()}: ${user.entityType} is already at full health")
        } else {
          GameSystemEvent.HealEvent(user.id, amount)
        }

      case GameSystemEvent.CreateProjectileEvent(_, _, _, collisionDamage, onDeathExplosion) =>
        val finalTargetPoint = targetEntity.map(_.position).orElse(targetPoint).getOrElse(user.position)
        val finalTargetEntityId = targetEntity.map(_.id)
        GameSystemEvent.CreateProjectileEvent(user.id, finalTargetPoint, finalTargetEntityId, collisionDamage, onDeathExplosion)
        
      case other =>
        // For other event types, return as-is (they may not need parameterization)
        other
    }
  }
}