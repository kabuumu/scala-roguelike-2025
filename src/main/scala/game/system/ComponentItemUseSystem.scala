package game.system

import game.GameState
import game.entity.*
import game.entity.EntityType.entityType
import game.entity.Health.hasFullHealth
import game.entity.Movement.position
import game.entity.Inventory.inventoryItems
import game.event.*
import game.system.event.GameSystemEvent
import game.system.event.GameSystemEvent.SpawnEntityEvent
import ui.InputAction
import data.Sprites

import scala.util.Random

object ComponentItemUseSystem extends GameSystem {
  override def update(gameState: GameState, events: Seq[GameSystemEvent.GameSystemEvent]): (GameState, Seq[GameSystemEvent.GameSystemEvent]) =
    val updatedGameState = events.foldLeft(gameState) {
      case (currentState, GameSystemEvent.InputEvent(entityId, InputAction.UseComponentItem(itemEntityId))) =>
        handleNonTargetedItem(currentState, entityId, itemEntityId)
      
      case (currentState, GameSystemEvent.InputEvent(entityId, InputAction.UseComponentItemAtPoint(itemEntityId, targetPoint))) =>
        handlePointTargetedItem(currentState, entityId, itemEntityId, targetPoint)
        
      case (currentState, GameSystemEvent.InputEvent(entityId, InputAction.UseComponentItemOnEntity(itemEntityId, targetEntityId))) =>
        handleEntityTargetedItem(currentState, entityId, itemEntityId, targetEntityId)
        
      case (currentState, _) =>
        currentState
    }

    (updatedGameState, Nil)

  private def handleNonTargetedItem(gameState: GameState, userId: String, itemEntityId: String): GameState = {
    (gameState.getEntity(userId), gameState.getEntity(itemEntityId)) match {
      case (Some(user), Some(itemEntity)) =>
        if (itemEntity.has[PotionItem]) {
          val potionComponent = itemEntity.get[PotionItem].get
          val events = if (user.hasFullHealth) {
            Seq(MessageEvent(s"${System.nanoTime()}: ${user.entityType} is already at full health"))
          } else {
            Seq(
              HealEvent(user.id, potionComponent.healingValue),
              RemoveItemEntityEvent(user.id, itemEntityId),
              ResetInitiativeEvent(user.id),
              MessageEvent(s"${System.nanoTime()}: ${user.entityType} used a potion to heal ${potionComponent.healingValue} health")
            )
          }
          gameState.handleEvents(events)
        } else {
          gameState
        }
      case _ => gameState
    }
  }

  private def handlePointTargetedItem(gameState: GameState, userId: String, itemEntityId: String, targetPoint: game.Point): GameState = {
    (gameState.getEntity(userId), gameState.getEntity(itemEntityId)) match {
      case (Some(user), Some(itemEntity)) =>
        if (itemEntity.has[ScrollItem]) {
          val scrollDamage = 30
          val targetType = if (user.entityType == EntityType.Player) EntityType.Enemy else EntityType.Player
          val startingPosition = user.position
          
          def explosionEntity(parentEntity: Entity) = Entity(
            s"explosion ${Random.nextInt()}",
            Hitbox(Set(game.Point(0, 0))),
            Collision(damage = scrollDamage, persistent = true, targetType, user.id),
            Movement(position = parentEntity.position),
            Drawable(Sprites.projectileSprite),
            Wave(2),
            EntityTypeComponent(EntityType.Projectile)
          )

          val fireballEntity = Entity(
            id = s"Projectile-${System.nanoTime()}",
            Movement(position = startingPosition),
            game.entity.Projectile(startingPosition, targetPoint, targetType, 0),
            EntityTypeComponent(EntityType.Projectile),
            Drawable(Sprites.projectileSprite),
            Collision(damage = scrollDamage, persistent = false, targetType, user.id),
            Hitbox(),
            DeathEvents(
              deathDetails =>
                Seq(SpawnEntityEvent(explosionEntity(deathDetails.victim)))
            )
          )

          val events = Seq(
            AddEntityEvent(fireballEntity),
            RemoveItemEntityEvent(user.id, itemEntityId),
            ResetInitiativeEvent(user.id),
            MessageEvent(s"${System.nanoTime()}: ${user.entityType} threw a fireball at ${targetPoint}")
          )
          gameState.handleEvents(events)
        } else {
          gameState
        }
      case _ => gameState
    }
  }

  private def handleEntityTargetedItem(gameState: GameState, userId: String, itemEntityId: String, targetEntityId: String): GameState = {
    (gameState.getEntity(userId), gameState.getEntity(itemEntityId), gameState.getEntity(targetEntityId)) match {
      case (Some(user), Some(itemEntity), Some(target)) =>
        if (itemEntity.has[BowItem] && user.inventoryItems(gameState).exists(_.has[ArrowItem])) {
          val bowDamage = 8
          val targetType = if (user.entityType == EntityType.Player) EntityType.Enemy else EntityType.Player
          val startingPosition = user.position
          
          val projectileEntity = Entity(
            id = s"Projectile-${System.nanoTime()}",
            Movement(position = startingPosition),
            game.entity.Projectile(startingPosition, target.position, targetType, bowDamage),
            EntityTypeComponent(EntityType.Projectile),
            Drawable(Sprites.projectileSprite),
            Collision(damage = bowDamage, persistent = false, targetType, user.id),
            Hitbox()
          )

          // Find an arrow to consume
          val arrowEntity = user.inventoryItems(gameState).find(_.has[ArrowItem])
          val events = Seq(
            AddEntityEvent(projectileEntity),
            ResetInitiativeEvent(user.id),
            MessageEvent(s"${System.nanoTime()}: ${user.entityType} used a Bow to attack ${target.id}")
          ) ++ arrowEntity.map(arrow => RemoveItemEntityEvent(user.id, arrow.id)).toSeq
          
          gameState.handleEvents(events)
        } else {
          gameState
        }
      case _ => gameState
    }
  }
}