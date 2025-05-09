package game

import data.Sprites
import game.Item.{Item, Key, Potion, Scroll, Weapon}
import game.entity.*
import game.entity.EntityType.entityType
import game.entity.Health.*
import game.entity.Hitbox.*
import game.entity.Initiative.*
import game.entity.UpdateAction.{CollisionCheckAction, ProjectileUpdateAction}

//TODO - Add separate initiative costs for different actions
trait Action {
  def apply(entity: Entity, gameState: GameState): GameState
}

case class MoveAction(direction: Direction) extends Action {
  def apply(movingEntity: Entity, gameState: GameState): GameState = {
    val movedEntity = movingEntity.update[Movement](_.move(direction)).resetInitiative()

    val optItem: Option[(Entity, Item)] = gameState.entities.map {
      entity => entity -> entity.entityType
    }.collectFirst {
      case (itemEntity, EntityType.Key(keyColour)) if movedEntity.collidesWith(itemEntity) =>
        itemEntity -> Item.Key(keyColour)
      case (itemEntity, EntityType.ItemEntity(item)) if movedEntity.collidesWith(itemEntity) =>
        itemEntity -> item
    }

    if (movedEntity.collidesWith(gameState.movementBlockingPoints)) {
      gameState.entities.find(collidingEntity => movedEntity.collidesWith(collidingEntity)) match {
        case Some(lockedDoorEntity @ Entity[EntityTypeComponent](EntityTypeComponent(EntityType.LockedDoor(keyColour)))) if movedEntity[Inventory].contains(Key(keyColour)) =>

          gameState
            .remove(lockedDoorEntity)
            .updateEntity(
              movingEntity.id,
              movedEntity
                .update[Inventory](_ - Key(keyColour))
                .update[SightMemory](_.update(gameState.remove(lockedDoorEntity), movingEntity))
            )
            .addMessage(s"${System.nanoTime()}: ${movingEntity[EntityTypeComponent]} opened the door")

        case Some(blockingEntity) =>
          gameState
            .addMessage(s"${System.nanoTime()}: ${movingEntity[EntityTypeComponent]} cannot move to ${blockingEntity[Movement].position} because it is blocked by ${blockingEntity[EntityTypeComponent]}")
        case None =>
          gameState
            .addMessage(s"${System.nanoTime()}: ${movingEntity[EntityTypeComponent]} cannot move to ${movedEntity[Movement].position} because it is blocked by a wall")
      }
    } else optItem match {
      case Some((itemEntity, item)) if movingEntity.entityType == EntityType.Player =>
        gameState
          .updateEntity(
            movingEntity.id,
            movedEntity
              .update[Inventory](_ + item)
              .update[SightMemory](_.update(gameState, movingEntity))
          )
          .remove(itemEntity)
          .addMessage(s"${System.nanoTime()}: ${movingEntity[EntityTypeComponent]} picked up a $item")

      case _ =>
        gameState
          .updateEntity(movingEntity.id, movedEntity
            .update[SightMemory](_.update(gameState, movingEntity))
          )
    }
  }
}

case class AttackAction(targetPosition: Point, optWeapon: Option[Weapon]) extends Action {
  def apply(attackingEntity: Entity, gameState: GameState): GameState = {
    optWeapon match {
      case Some(Weapon(damage, Item.Ranged(_))) =>
        val targetType = if(attackingEntity.entityType == EntityType.Player) EntityType.Enemy else EntityType.Player
        val startingPosition = attackingEntity[Movement].position
        val projectileEntity =
          Entity(
            id = s"Projectile-${System.nanoTime()}",
            Movement(position = startingPosition),
            Projectile(startingPosition, targetPosition, targetType, damage),
            UpdateController(ProjectileUpdateAction, CollisionCheckAction),
            EntityTypeComponent(EntityType.Projectile),
            Drawable(Sprites.projectileSprite),
            Collision(damage = damage, explodes = false, persistent = false, targetType),
            Hitbox()
          )

        gameState
          .add(projectileEntity)
          .updateEntity(
            attackingEntity.id,
            attackingEntity.resetInitiative()
          )
      case _ =>
        val damage = optWeapon match {
          case Some(weapon) => weapon.damage
          case None => 1
        }
        gameState.getActor(targetPosition) match {
          case Some(target) =>
            gameState
              .updateEntity(
                target.id, target.update[Health](_ - damage)
              )
              .updateEntity(
                attackingEntity.id,
                attackingEntity.resetInitiative()
              )
          case _ =>
            throw new Exception(s"No target found at $targetPosition")
        }
    }
  }
}

case object WaitAction extends Action {
  def apply(entity: Entity, gameState: GameState): GameState = {
    gameState.updateEntity(entity.id, entity.decreaseInitiative())
  }
}

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