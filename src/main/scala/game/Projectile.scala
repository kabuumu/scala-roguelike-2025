package game

import game.entity._

case class Projectile(precisePosition: (Double, Double), xVelocity: Double, yVelocity: Double, targetType: EntityType) {
  def update(currentGameState: GameState): GameState = {
    val (currentX, currentY) = precisePosition
    val newX = currentX + xVelocity
    val newY = currentY + yVelocity

    val updatedProjectile = copy(precisePosition = (newX, newY))
    //TODO - find better way to return all collisions - collision class?
    if (currentGameState.dungeon.walls.contains(updatedProjectile.position) || currentGameState.dungeon.walls.contains(position)) {
      currentGameState.copy(
        projectiles = currentGameState.projectiles.filterNot(_.precisePosition == precisePosition)
      )
    } else {
      currentGameState.entities.find(entity =>
        (entity[Movement].position == updatedProjectile.position || entity[Movement].position == position) && entity[EntityTypeComponent].entityType == targetType
      ) match {
        case Some(collision) =>
          currentGameState.copy(
            projectiles = currentGameState.projectiles.filterNot(_.precisePosition == precisePosition)
          ).updateEntity(
            collision.id,
            collision.update[Health](_ - 1)
          )
        case None =>
          currentGameState.copy(
            projectiles = currentGameState.projectiles.filterNot(_.precisePosition == precisePosition) :+ updatedProjectile
          )
      }
    }
  }

  val position: Point = {
    val (x, y) = precisePosition
    Point(x.round.toInt, y.round.toInt)
  }
}

object Projectile {
  val projectileSpeed: Double = 1

  def apply(start: Point, end: Point, targetType: EntityType): Projectile = {
    val dx = end.x - start.x
    val dy = end.y - start.y
    val magnitude = Math.sqrt(dx * dx + dy * dy)
    val xVelocity = (dx / magnitude) * projectileSpeed
    val yVelocity = (dy / magnitude) * projectileSpeed
    new Projectile(
      (start.x.toDouble + xVelocity, start.y.toDouble + yVelocity),
      xVelocity,
      yVelocity,
      targetType
    )
  }
}