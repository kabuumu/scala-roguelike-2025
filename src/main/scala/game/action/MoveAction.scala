package game.action

import game.Item.*
import game.entity.*
import game.entity.EntityType.entityType
import game.entity.Hitbox.*
import game.entity.Initiative.*
import game.entity.Inventory.*
import game.entity.Movement.*
import game.event.*
import game.{Item, *}

case class MoveAction(direction: Direction) extends Action {
  def apply(movingEntity: Entity, gameState: GameState): Seq[Event] = {
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
        case Some(lockedDoorEntity@EntityType(EntityType.LockedDoor(keyColour))) if movedEntity.items.contains(Key(keyColour)) => Seq(
          RemoveEntityEvent(lockedDoorEntity.id),
          ResetInitiativeEvent(movingEntity.id),
          RemoveItemEvent(movingEntity.id, Key(keyColour)),
          UpdateSightMemoryEvent(movingEntity.id, gameState.remove(lockedDoorEntity.id)),
        )
        case _ =>
          Nil
      }
    } else optItem match {
      case Some((itemEntity, item)) if movingEntity.entityType == EntityType.Player => Seq(
        MoveEvent(movingEntity.id, direction),
        ResetInitiativeEvent(movingEntity.id),
        AddItemEvent(movingEntity.id, item),
        RemoveEntityEvent(itemEntity.id),
        UpdateSightMemoryEvent(movingEntity.id, gameState),
      )
      case _ => Seq(
        MoveEvent(movingEntity.id, direction),
        ResetInitiativeEvent(movingEntity.id),
        UpdateSightMemoryEvent(movingEntity.id, gameState),
      )
    }
  }
}
