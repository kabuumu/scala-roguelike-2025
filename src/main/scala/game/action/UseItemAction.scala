package game.action

import data.Sprites
import game.Item.*
import game.entity.*
import game.entity.EntityType.entityType
import game.entity.Health.*
import game.entity.Initiative.*
import game.entity.UpdateAction.{CollisionCheckAction, ProjectileUpdateAction}
import game.{Item, *}

case class UseItemAction(item: Item, target: Entity) extends Action {
  def apply(entity: Entity, gameState: GameState): GameState =
    if (entity.exists[Inventory](_.contains(item)))
      item match
        case Potion =>
          if (target.hasFullHealth)
            gameState
              .addMessage(s"${System.nanoTime()}: ${target[EntityTypeComponent]} is already at full health")
          else
            gameState.updateEntity(
              target.id,
              _.heal(Item.potionValue)
            ).updateEntity(
              entity.id,
              _.update[Inventory](_ - item)
                .resetInitiative()
            ).addMessage(
              s"${System.nanoTime()}: ${entity[EntityTypeComponent]} used a $item to heal ${Item.potionValue} health"
            )
        case Scroll =>
          val scrollDamage = 3

          val targetType = if (entity.entityType == EntityType.Player) EntityType.Enemy else EntityType.Player
          val startingPosition = entity[Movement].position
          val fireballEntity =
            Entity(
              id = s"Projectile-${System.nanoTime()}",
              Movement(position = startingPosition),
              Projectile(startingPosition, target[Movement].position, targetType, scrollDamage),
              UpdateController(ProjectileUpdateAction, CollisionCheckAction),
              EntityTypeComponent(EntityType.Projectile),
              Drawable(Sprites.projectileSprite),
              Collision(damage = scrollDamage, explodes = true, persistent = false, targetType),
              Hitbox()
            )


          gameState
            .add(fireballEntity)
            .updateEntity(
              entity.id,
              _.update[Inventory](_ - item)
                .resetInitiative(),
            ).addMessage(
              s"${System.nanoTime()}: ${entity[EntityTypeComponent]} used a $item to attack ${target[EntityTypeComponent]}"
            )
    else
      gameState
        .addMessage(s"${System.nanoTime()}: ${entity[EntityTypeComponent]} does not have a $item")
}
