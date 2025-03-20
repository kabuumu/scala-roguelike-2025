package game

case class GameState(playerEntityId: String, entities: Set[Entity]) {
  private val framesPerSecond = 8

  val playerEntity: Entity = entities.find(_.id == playerEntityId).get

  def update(playerAction: Option[Action]): GameState = {
    playerAction match {
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
          this
        } else update(
          playerEntity,
          newPlayerEntity
            .copy(sightMemory = newPlayerEntity.sightMemory ++ getLineOfSight(newPlayerEntity))
        )
      case Some(game.Action.Attack(cursorX, cursorY)) =>
        getEntity(cursorX, cursorY) match {
          case Some(enemy) if enemy.entityType == EntityType.Enemy =>
            enemy.copy(health = enemy.health - 1) match {
              case newEnemy if newEnemy.health <= 0 => remove(enemy)
              case newEnemy => update(enemy, newEnemy)
            }
          case _ => this
        }
      case None => this
    }
  }

  def update(entity: Entity, newEntity: Entity): GameState = {
    copy(entities = entities - entity + newEntity)
  }

  def getEntity(x: Int, y: Int): Option[Entity] = {
    entities.find(entity => entity.xPosition == x && entity.yPosition == y)
  }

  def remove(entity: Entity): GameState = {
    copy(entities = entities - entity)
  }

  def getLineOfSight(entity: Entity): Set[Entity] = {
    val sightRange = 5
    val entitiesInSight = scala.collection.mutable.Set[Entity]()
    val blockingPositions = entities.collect {
      case e if e.lineOfSightBlocking || e.entityType == EntityType.Wall => (e.xPosition, e.yPosition)
    }.toSet

    def isBlocking(x: Int, y: Int): Boolean = {
      blockingPositions.contains((x, y))
    }

    def bresenhamLine(x0: Int, y0: Int, x1: Int, y1: Int): Seq[(Int, Int)] = {
      val dx = Math.abs(x1 - x0)
      val dy = Math.abs(y1 - y0)
      val sx = if (x0 < x1) 1 else -1
      val sy = if (y0 < y1) 1 else -1
      var err = dx - dy

      var x = x0
      var y = y0
      val line = scala.collection.mutable.Buffer[(Int, Int)]()

      while (x != x1 || y != y1) {
        line.append((x, y))
        val e2 = 2 * err
        if (e2 > -dy) {
          err -= dy
          x += sx
        }
        if (e2 < dx) {
          err += dx
          y += sy
        }
      }
      line.append((x1, y1))
      line.toSeq
    }

    entitiesInSight.add(entity)

    for (dx <- -sightRange to sightRange; dy <- -sightRange to sightRange) {
      val x = entity.xPosition + dx
      val y = entity.yPosition + dy
      val distance = Math.sqrt(dx * dx + dy * dy)
      if (distance <= sightRange) {
        val line = bresenhamLine(entity.xPosition, entity.yPosition, x, y)
        var blocked = false
        for ((lx, ly) <- line if !blocked) {
          if (isBlocking(lx, ly)) {
            entities.find(e => e.xPosition == lx && e.yPosition == ly).foreach(entitiesInSight.add)
            blocked = true
          } else if ((lx, ly) == (x, y)) {
            entities.find(e => e.xPosition == lx && e.yPosition == ly).foreach(entitiesInSight.add)
          }
        }
      }
    }

    entitiesInSight.toSet
  }
}