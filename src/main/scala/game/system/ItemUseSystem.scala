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

import scala.util.Random

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
   * Apply the effects of a usable item. This is the core logic that converts
   * item effects into the existing game event system.
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

    // Convert item effects to game events
    val effectEvents = usableItem.effects.flatMap { effect =>
      convertEffectToEvents(effect, user, targetPoint, targetEntity)
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
   * Convert an ItemEffect into the appropriate game events.
   * This maintains compatibility with the existing event system.
   */
  private def convertEffectToEvents(
    effect: ItemEffect,
    user: Entity,
    targetPoint: Option[game.Point],
    targetEntity: Option[Entity]
  ): Seq[Event] = {
    effect match {
      case ItemEffect.Heal(amount) =>
        if (user.hasFullHealth) {
          Seq(MessageEvent(s"${System.nanoTime()}: ${user.entityType} is already at full health"))
        } else {
          Seq(HealEvent(user.id, amount))
        }

      case ItemEffect.CreateProjectile(collisionDamage, onDeathExplosion) =>
        createProjectileEvents(user, collisionDamage, onDeathExplosion, targetPoint, targetEntity)
    }
  }

  /**
   * Create projectile-related events. This replicates the existing projectile logic
   * from ComponentItemUseSystem but in a data-driven way.
   */
  private def createProjectileEvents(
    user: Entity,
    collisionDamage: Int,
    onDeathExplosion: Option[ExplosionEffect],
    targetPoint: Option[game.Point],
    targetEntity: Option[Entity]
  ): Seq[Event] = {
    val targetType = if (user.entityType == EntityType.Player) EntityType.Enemy else EntityType.Player
    val startingPosition = user.position
    
    // Determine the target position for the projectile
    val finalTargetPoint = targetEntity.map(_.position).orElse(targetPoint).getOrElse(startingPosition)

    // Create the projectile entity
    val projectileEntity = Entity(
      id = s"Projectile-${System.nanoTime()}",
      Movement(position = startingPosition),
      game.entity.Projectile(startingPosition, finalTargetPoint, targetType, collisionDamage),
      EntityTypeComponent(EntityType.Projectile),
      Drawable(Sprites.projectileSprite),
      Collision(damage = collisionDamage, persistent = false, targetType, user.id),
      Hitbox()
    )

    // Add explosion behavior if specified - reusing the OnDeath pattern from the original system
    val finalProjectileEntity = onDeathExplosion match {
      case Some(explosion) =>
        projectileEntity.addComponent(DeathEvents(
          deathDetails => Seq(SpawnEntityEvent(createExplosionEntity(deathDetails.victim, explosion, targetType, user.id)))
        ))
      case None =>
        projectileEntity
    }

    Seq(AddEntityEvent(finalProjectileEntity))
  }
  
  /** 
   * Creates an explosion entity on projectile death - reuses the pattern from the original ComponentItemUseSystem
   */
  private def createExplosionEntity(parentEntity: Entity, explosion: ExplosionEffect, targetType: EntityType, creatorId: String): Entity = {
    Entity(
      s"explosion ${Random.nextInt()}",
      Hitbox(Set(game.Point(0, 0))),
      Collision(damage = explosion.damage, persistent = true, targetType, creatorId),
      Movement(position = parentEntity.position),
      Drawable(Sprites.projectileSprite),
      Wave(explosion.radius),
      EntityTypeComponent(EntityType.Projectile) // Consistent with original system
    )
  }
}