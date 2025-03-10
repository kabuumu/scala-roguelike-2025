package game

case class GameState(playerEntityId: String, entities: Set[Entity]) {
  private val framesPerSecond = 8

  val playerEntity: Entity = entities.find(_.id == playerEntityId).get

  def update(playerAction: Option[Action]): GameState = {
    val newPlayerEntity = playerAction match {
      case Some(Action.Move(direction)) =>
        val newPlayerEntity = playerEntity.move(direction)
        if (
          entities.exists(
            entity =>
              entity.xPosition == newPlayerEntity.xPosition
                &&
                entity.yPosition == newPlayerEntity.yPosition
                &&
                entity.entityType == EntityType.Wall
          )
        ) {
          playerEntity
        } else newPlayerEntity
      case None => playerEntity
    }

    //update gamestate with new player entity
    if (newPlayerEntity != playerEntity) {
      copy(entities = entities - playerEntity + newPlayerEntity)
    } else this
  }
}