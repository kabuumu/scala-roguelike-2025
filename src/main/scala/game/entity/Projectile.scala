package game.entity

import game.entity.*
import game.entity.EntityType.*
import game.entity.Health.*
import game.{GameState, Point}

case class Projectile(precisePosition: (Double, Double), xVelocity: Double, yVelocity: Double, targetType: EntityType, damage: Int) extends Component {
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
        (entity[Movement].position == updatedProjectile.position || entity[Movement].position == position)
          && entity.entityType == targetType
          && entity.isAlive
      ) match {
        case Some(collision) =>
          currentGameState.copy(
            projectiles = currentGameState.projectiles.filterNot(_.precisePosition == precisePosition)
          ).updateEntity(
            collision.id,
            collision.update[Health](_ - damage)
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

  def apply(start: Point, end: Point, targetType: EntityType, damage: Int): Projectile = {
    val dx = end.x - start.x
    val dy = end.y - start.y
    val magnitude = Math.sqrt(dx * dx + dy * dy)
    val xVelocity = (dx / magnitude) * projectileSpeed
    val yVelocity = (dy / magnitude) * projectileSpeed
    new Projectile(
      (start.x.toDouble + xVelocity, start.y.toDouble + yVelocity),
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
          projectile.update(gameState)
        case None =>
          gameState
      }
    }
  }
}