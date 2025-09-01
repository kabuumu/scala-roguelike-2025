package game.system

import game.GameState
import game.entity.*
import game.entity.EntityType.entityType
import game.entity.Health.hasFullHealth
import game.entity.Movement.position
import game.entity.Inventory.inventoryItems
import game.entity.UsableItem.isUsableItem
import game.entity.Ammo.isAmmoType
import game.event.*
import game.system.event.GameSystemEvent
import game.system.event.GameSystemEvent.SpawnEntityEvent
import ui.InputAction
import data.Sprites

/**
 * New unified item use system that handles all usable items through the UsableItem component.
 * Replaces both LegacyItemUseSystem and ComponentItemUseSystem with a data-driven approach.
 */
object ItemUseSystem extends GameSystem {
  override def update(gameState: GameState, events: Seq[GameSystemEvent.GameSystemEvent]): (GameState, Seq[GameSystemEvent.GameSystemEvent]) = {
    val updatedGameState = events.foldLeft(gameState) {
      case (currentState, GameSystemEvent.InputEvent(entityId, InputAction.UseComponentItem(itemEntityId))) =>
        handleSelfTargetedItem(currentState, entityId, itemEntityId)
      
      case (currentState, GameSystemEvent.InputEvent(entityId, InputAction.UseComponentItemAtPoint(itemEntityId, targetPoint))) =>
        handlePointTargetedItem(currentState, entityId, itemEntityId, targetPoint)
        
      case (currentState, GameSystemEvent.InputEvent(entityId, InputAction.UseComponentItemOnEntity(itemEntityId, targetEntityId))) =>
        handleEntityTargetedItem(currentState, entityId, itemEntityId, targetEntityId)
        
      case (currentState, _) =>
        currentState
    }

    (updatedGameState, Nil)
  }

  /**
   * Handle items that target the user themselves (Targeting.Self)
   * Examples: healing potions
   */
  private def handleSelfTargetedItem(gameState: GameState, userId: String, itemEntityId: String): GameState = {
    (gameState.getEntity(userId), gameState.getEntity(itemEntityId)) match {
      case (Some(user), Some(itemEntity)) =>
        itemEntity.get[UsableItem] match {
          case Some(usableItem) if usableItem.targeting == Targeting.Self =>
            applyItemEffects(gameState, user, itemEntity, usableItem, None, None)
          case _ =>
            gameState // Item not usable or wrong targeting type
        }
      case _ => gameState
    }
  }

  /**
   * Handle items that target a specific point/tile (Targeting.TileInRange)
   * Examples: fireball scrolls
   */
  private def handlePointTargetedItem(gameState: GameState, userId: String, itemEntityId: String, targetPoint: game.Point): GameState = {
    (gameState.getEntity(userId), gameState.getEntity(itemEntityId)) match {
      case (Some(user), Some(itemEntity)) =>
        itemEntity.get[UsableItem] match {
          case Some(usableItem) if usableItem.targeting.isInstanceOf[Targeting.TileInRange] =>
            applyItemEffects(gameState, user, itemEntity, usableItem, Some(targetPoint), None)
          case _ =>
            gameState // Item not usable or wrong targeting type
        }
      case _ => gameState
    }
  }

  /**
   * Handle items that target another entity (Targeting.EnemyActor)
   * Examples: bows shooting at enemies
   */
  private def handleEntityTargetedItem(gameState: GameState, userId: String, itemEntityId: String, targetEntityId: String): GameState = {
    (gameState.getEntity(userId), gameState.getEntity(itemEntityId), gameState.getEntity(targetEntityId)) match {
      case (Some(user), Some(itemEntity), Some(target)) =>
        itemEntity.get[UsableItem] match {
          case Some(usableItem) if usableItem.targeting == Targeting.EnemyActor =>
            applyItemEffects(gameState, user, itemEntity, usableItem, None, Some(target))
          case _ =>
            gameState // Item not usable or wrong targeting type
        }
      case _ => gameState
    }
  }

  /**
   * Apply the effects of a usable item. Now that UsableItem uses Events directly,
   * we can apply them with proper parameterization for the current context.
   */
  private def applyItemEffects(
    gameState: GameState, 
    user: Entity, 
    itemEntity: Entity, 
    usableItem: UsableItem,
    targetPoint: Option[game.Point] = None,
    targetEntity: Option[Entity] = None
  ): GameState = {
    
    // Check ammo requirements if this item needs ammo
    if (usableItem.ammo.isDefined) {
      val requiredAmmoType = usableItem.ammo.get
      val availableAmmo = user.inventoryItems(gameState).find(_.isAmmoType(requiredAmmoType))
      if (availableAmmo.isEmpty) {
        // No ammo available, cannot use item
        return gameState
      }
    }

    // Parameterize the effects for the current usage context
    val effectEvents = usableItem.effects.map { effect =>
      parameterizeEffectForContext(effect, user, targetPoint, targetEntity)
    }

    // Add item consumption events if needed
    val consumptionEvents = if (usableItem.consumeOnUse) {
      Seq(RemoveItemEntityEvent(user.id, itemEntity.id))
    } else {
      Nil
    }

    // Add ammo consumption events if needed
    val ammoConsumptionEvents = usableItem.ammo match {
      case Some(ammoType) =>
        user.inventoryItems(gameState).find(_.isAmmoType(ammoType)) match {
          case Some(ammoEntity) => Seq(RemoveItemEntityEvent(user.id, ammoEntity.id))
          case None => Nil
        }
      case None => Nil
    }

    // Always reset initiative after using an item (turn-based behavior)
    val initiativeEvents = Seq(ResetInitiativeEvent(user.id))

    // Create usage message
    val messageEvents = Seq(MessageEvent(s"${System.nanoTime()}: ${user.entityType} used ${itemEntity.id}"))

    // Apply all events to the game state
    val allEvents = effectEvents ++ consumptionEvents ++ ammoConsumptionEvents ++ initiativeEvents ++ messageEvents
    gameState.handleEvents(allEvents)
  }

  /**
   * Parameterize an Event template for the current usage context.
   * This allows Events to be reused across different systems with different parameters.
   */
  private def parameterizeEffectForContext(
    effect: Event,
    user: Entity,
    targetPoint: Option[game.Point],
    targetEntity: Option[Entity]
  ): Event = {
    effect match {
      case HealEvent(_, amount) =>
        if (user.hasFullHealth) {
          MessageEvent(s"${System.nanoTime()}: ${user.entityType} is already at full health")
        } else {
          HealEvent(user.id, amount)
        }

      case CreateProjectileEvent(_, _, _, collisionDamage, onDeathExplosion) =>
        val finalTargetPoint = targetEntity.map(_.position).orElse(targetPoint).getOrElse(user.position)
        CreateProjectileEvent(user.id, finalTargetPoint, targetEntity, collisionDamage, onDeathExplosion)
        
      case other =>
        // For other event types, return as-is (they may not need parameterization)
        other
    }
  }
}