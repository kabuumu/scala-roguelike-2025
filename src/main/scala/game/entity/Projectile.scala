package game.entity

import game.entity.Health.*
import game.{GameState, Point}

case class Projectile(precisePosition: (Double, Double), xVelocity: Double, yVelocity: Double, targetType: EntityType, damage: Int) extends Component {
  def update(entity: Entity, currentGameState: GameState): GameState = {
    val (currentX, currentY) = precisePosition
    val newX = currentX + xVelocity
    val newY = currentY + yVelocity

    val updatedProjectile = copy(precisePosition = (newX, newY))

    currentGameState.updateEntity(
      entity.id,
      _.update[Projectile](_ => updatedProjectile)
        .update[Movement](_.copy(position = updatedProjectile.position))
    )
  }

  val position: Point = {
    val (x, y) = precisePosition
    Point(x.round.toInt, y.round.toInt)
  }
}

object Projectile {
  val projectileSpeed: Double = 1

  def apply(start: Point, end: Point, targetType: EntityType, damage: Int): Projectile = {
    val dx = end.x - start.x
    val dy = end.y - start.y
    val magnitude = Math.sqrt(dx * dx + dy * dy)
    val xVelocity = (dx / magnitude) * projectileSpeed
    val yVelocity = (dy / magnitude) * projectileSpeed
    new Projectile(
      (start.x.toDouble, start.y.toDouble),
      xVelocity,
      yVelocity,
      targetType,
      damage
    )
  }

  extension (entity: Entity) {
    def projectileUpdate(gameState: GameState): GameState = {
      entity.get[Projectile] match {
        case Some(projectile) =>
          projectile.update(entity, gameState)
        case None =>
          gameState
      }
    }
  }
}