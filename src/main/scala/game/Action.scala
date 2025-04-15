package game

import game.Item.{Item, Key}

//TODO - Add separate initiative costs for different actions
trait Action {
  def apply(entity: Entity, gameState: GameState): GameState
}

case class MoveAction(direction: Direction) extends Action {
  def apply(movingEntity: Entity, gameState: GameState): GameState = {
    val movedEntity = movingEntity.move(direction)

    val optItem: Option[(Entity, Item)] = gameState.entities.map {
      entity => entity -> entity.entityType
    }.collectFirst {
      case (entity, EntityType.Key(keyColour)) if movedEntity.position == entity.position =>
        entity -> Item.Key(keyColour)
      case (entity, EntityType.ItemEntity(Item.Potion)) if movedEntity.position == entity.position =>
        entity -> Item.Potion
    }

    if (gameState.movementBlockingPoints.contains(movedEntity.position)) {
      gameState.entities.find(_.position == movedEntity.position) match {
        case Some(lockedDoorEntity@Entity(_, _, _, EntityType.LockedDoor(keyColour), _, _, _, _, _, _)) if movedEntity.inventory.contains(Key(keyColour)) =>
          val newInventory = movedEntity.inventory.patch(movedEntity.inventory.indexOf(Key(keyColour)), Nil, 1)

          gameState
            .remove(lockedDoorEntity)
            .updateEntity(
              movingEntity.id,
              movedEntity
                .copy(inventory = newInventory)
                .updateSightMemory(gameState.remove(lockedDoorEntity)) //TODO - move updating sight memory to a central point - should be done after every action
            )
            .addMessage(s"${System.nanoTime()}: ${movingEntity.name} opened the door")

        case Some(blockingEntity) =>
          gameState
            .addMessage(s"${System.nanoTime()}: ${movingEntity.name} cannot move to ${blockingEntity.position} because it is blocked by ${blockingEntity.entityType}")
        case None =>
          gameState
            .addMessage(s"${System.nanoTime()}: ${movingEntity.name} cannot move to ${movedEntity.position} because it is blocked by a wall")
      }
    } else optItem match {
      case Some((entity, item)) =>
        gameState
          .updateEntity(
            movingEntity.id,
            movedEntity.copy(inventory = movedEntity.inventory :+ item)
              .updateSightMemory(gameState))
          .remove(entity)
          .addMessage(s"${System.nanoTime()}: ${movingEntity.name} picked up a $item")

      case None =>
        gameState.updateEntity(
          movingEntity.id,
          movedEntity.updateSightMemory(gameState)
        )
    }
  }
}

case class AttackAction(cursorX: Int, cursorY: Int) extends Action {
  def apply(attackingEntity: Entity, gameState: GameState): GameState = {
    gameState.getActor(cursorX, cursorY) match {
      case Some(target) =>
        val newEnemy = target.copy(health = target.health - 1)
        if (newEnemy.health.current <= 0) {
          gameState
            .updateEntity(target.id, newEnemy.copy(isDead = true))
            .updateEntity(
              attackingEntity.id,
              attackingEntity.copy(initiative = attackingEntity.INITIATIVE_MAX)
            )
            .addMessage(s"${System.nanoTime()}: ${attackingEntity.name} killed ${target.name}")
        } else {
          gameState
            .updateEntity(target.id, newEnemy)
            .updateEntity(
              attackingEntity.id,
              attackingEntity.copy(initiative = attackingEntity.INITIATIVE_MAX)
            )
            .addMessage(s"${System.nanoTime()}: ${attackingEntity.name} attacked ${target.name}")
        }
      case _ =>
        throw new Exception(s"No target found at $cursorX, $cursorY")
    }
  }
}

case object WaitAction extends Action {
  def apply(entity: Entity, gameState: GameState): GameState = {
    gameState.updateEntity(entity.id, entity.copy(initiative = entity.INITIATIVE_MAX))
  }
}

//TODO make it for more items, not just potions
case class UseItemAction(item: Item) extends Action {
  def apply(entity: Entity, gameState: GameState): GameState = {

    if (entity.health.isFull) {
      gameState
        .addMessage(s"${System.nanoTime()}: ${entity.name} is already at full health")
    } else if (entity.inventory.isEmpty) {
      gameState
        .addMessage(s"${System.nanoTime()}: ${entity.name} has no items to use")
    } else if (!entity.inventory.headOption.contains(item)) {
      gameState
        .addMessage(s"${System.nanoTime()}: ${entity.name} does not have a $item")
    } else {
      val newEntity = entity.copy(
        inventory = entity.inventory.drop(1)
      ).copy(
        health = entity.health + Item.potionValue
      )
      gameState
        .updateEntity(entity.id, newEntity)
        .addMessage(s"${System.nanoTime()}: ${entity.name} used a $item")
    }
  }
}