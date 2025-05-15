package game.action

import game.Item.*
import game.entity.*
import game.entity.EntityType.entityType
import game.entity.Hitbox.*
import game.entity.Initiative.*
import game.{Item, *}

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
        case Some(lockedDoorEntity@Entity[EntityTypeComponent] (EntityTypeComponent (EntityType.LockedDoor (keyColour) ) ) ) if movedEntity[Inventory].contains (Key (keyColour) ) =>

      gameState
      .remove (lockedDoorEntity)
      .updateEntity (
      movingEntity.id,
      movedEntity
      .update[Inventory] (_- Key (keyColour) )
      .update[SightMemory] (_.update (gameState.remove (lockedDoorEntity), movingEntity) )
      )
      .addMessage (s"${
      System.nanoTime ()
      }: ${
      movingEntity[EntityTypeComponent]
      } opened the door")

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
