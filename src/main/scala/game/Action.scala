package game

import game.Item.KeyColour.*
import game.Item.{Item, Key, Weapon}
import game.entity.*

//TODO - Add separate initiative costs for different actions
trait Action {
  def apply(entity: Entity, gameState: GameState): GameState
}

case class MoveAction(direction: Direction) extends Action {
  def apply(movingEntity: Entity, gameState: GameState): GameState = {
    val movedEntity = movingEntity.update[Movement](_.move(direction)).update[Initiative](_.reset())

    val optItem: Option[(Entity, Item)] = gameState.entities.map {
      entity => entity -> entity[EntityTypeComponent].entityType
    }.collectFirst {
      case (entity, EntityType.Key(keyColour)) if movedEntity[Movement].position == entity[Movement].position =>
        entity -> Item.Key(keyColour)
      case (entity, EntityType.ItemEntity(Item.Potion)) if movedEntity[Movement].position == entity[Movement].position =>
        entity -> Item.Potion
    }

    if (gameState.movementBlockingPoints.contains(movedEntity[Movement].position)) {
      gameState.entities.find(_[Movement].position == movedEntity[Movement].position) match {
        case Some(lockedDoorEntity @ Entity[EntityTypeComponent](EntityTypeComponent(EntityType.LockedDoor(keyColour)))) if movedEntity[Inventory].contains(Key(keyColour)) =>

          gameState
            .remove(lockedDoorEntity)
            .updateEntity(
              movingEntity.id,
              movedEntity
                .update[Inventory](_ - Key(keyColour))
                .update[SightMemory](_.update(gameState.remove(lockedDoorEntity), movingEntity))
            )
            .addMessage(s"${System.nanoTime()}: ${movingEntity.toString} opened the door")

        case Some(blockingEntity) =>
          gameState
            .addMessage(s"${System.nanoTime()}: ${movingEntity.toString} cannot move to ${blockingEntity[Movement].position} because it is blocked by ${blockingEntity[EntityTypeComponent]}")
        case None =>
          gameState
            .addMessage(s"${System.nanoTime()}: ${movingEntity.toString} cannot move to ${movedEntity[Movement].position} because it is blocked by a wall")
      }
    } else optItem match {
      case Some((itemEntity, item)) if movingEntity.exists[EntityTypeComponent](_.entityType == EntityType.Player) =>
        gameState
          .updateEntity(
            movingEntity.id,
            movedEntity
              .update[Inventory](_ + item)
              .update[SightMemory](_.update(gameState, movingEntity))
          )
          .remove(itemEntity)
          .addMessage(s"${System.nanoTime()}: ${movingEntity.toString} picked up a $item")

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
    val damage = optWeapon match {
      case Some(weapon) => weapon.damage
      case None => 1
    }

    optWeapon match {
      case Some(_, Item.Ranged(_)) =>
        val targetType = if(attackingEntity[EntityTypeComponent].entityType == EntityType.Player) EntityType.Enemy else EntityType.Player
        val projectile = Projectile(attackingEntity[Movement].position, targetPosition, targetType)

        gameState
          .copy(projectiles = gameState.projectiles :+ projectile)
          .updateEntity(
            attackingEntity.id,
            attackingEntity.update[Initiative](_.reset())
          )
      case _ =>
        gameState.getActor(targetPosition) match {
          case Some(target) =>
            gameState
              .updateEntity(
                target.id, target.update[Health](_ - damage)
              )
              .updateEntity(
                attackingEntity.id,
                attackingEntity.update[Initiative](_.reset())
              )
          case _ =>
            throw new Exception(s"No target found at $targetPosition")
        }
    }
  }
}

case object WaitAction extends Action {
  def apply(entity: Entity, gameState: GameState): GameState = {
    gameState.updateEntity(entity.id, entity.update[Initiative](_.reset()))
  }
}

//TODO make it for more items, not just potions
case class UseItemAction(item: Item) extends Action {
  def apply(entity: Entity, gameState: GameState): GameState = {

    if (entity[Health].isFull) {
      gameState
        .addMessage(s"${System.nanoTime()}: ${entity.toString} is already at full health")
    } else if (!entity[Inventory].contains(item)) {
      gameState
        .addMessage(s"${System.nanoTime()}: ${entity.toString} does not have a $item")
    } else {
      val newEntity = entity
        .update[Inventory](_ - item)
        .update[Health](_ + Item.potionValue)

      gameState
        .updateEntity(entity.id, newEntity)
        .addMessage(s"${System.nanoTime()}: ${entity.toString} used a $item")
    }
  }
}