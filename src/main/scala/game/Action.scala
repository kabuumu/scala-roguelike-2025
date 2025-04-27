package game

import game.Item.{Item, Key, Weapon}

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
        case Some(lockedDoorEntity@Entity(_, _, EntityType.LockedDoor(keyColour), _, _, _, _, _, _)) if movedEntity.inventory.contains(Key(keyColour)) =>
          val newInventory = movedEntity.inventory - Key(keyColour)

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
            movedEntity.copy(inventory = movedEntity.inventory + item)
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

case class AttackAction(targetPosition: Point, optWeapon: Option[Weapon]) extends Action {
  def apply(attackingEntity: Entity, gameState: GameState): GameState = {
    val damage = optWeapon match {
      case Some(weapon) => weapon.damage
      case None => 1
    }

    optWeapon match {
      case Some(_, Item.Ranged(_)) =>
        val targetType = if(attackingEntity.entityType == EntityType.Player) EntityType.Enemy else EntityType.Player
        val projectile = Projectile(attackingEntity.position, targetPosition, targetType)

        gameState
          .copy(projectiles = gameState.projectiles :+ projectile)
          .updateEntity(
            attackingEntity.id,
            attackingEntity.resetInitiative()
          )
      case _ =>
        gameState.getActor(targetPosition) match {
          case Some(target) =>
            gameState
              .updateEntity(
                target.id, target.takeDamage(damage)
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
    gameState.updateEntity(entity.id, entity.copy(initiative = entity.INITIATIVE_MAX))
  }
}

//TODO make it for more items, not just potions
case class UseItemAction(item: Item) extends Action {
  def apply(entity: Entity, gameState: GameState): GameState = {

    if (entity.health.isFull) {
      gameState
        .addMessage(s"${System.nanoTime()}: ${entity.name} is already at full health")
    } else if (!entity.inventory.contains(item)) {
      gameState
        .addMessage(s"${System.nanoTime()}: ${entity.name} does not have a $item")
    } else {
      val newEntity = entity.copy(
        inventory = entity.inventory - item
      ).copy(
        health = entity.health + Item.potionValue
      )
      gameState
        .updateEntity(entity.id, newEntity)
        .addMessage(s"${System.nanoTime()}: ${entity.name} used a $item")
    }
  }
}