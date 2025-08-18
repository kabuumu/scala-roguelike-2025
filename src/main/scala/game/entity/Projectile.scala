package game.entity

import game.entity.Health.*
import game.event.Event
import game.{GameState, Point}

case class Projectile(precisePosition: (Double, Double), xVelocity: Double, yVelocity: Double, targetType: EntityType, damage: Int, targetPoint: Point) extends Component {
  val position: Point = {
    val (x, y) = precisePosition
    Point(x.round.toInt, y.round.toInt)
  }
  
  def updatePosition(): Projectile = {
    val (currentX, currentY) = precisePosition
    val newX = currentX + xVelocity
    val newY = currentY + yVelocity

    copy(precisePosition = (newX, newY))
  }
  
  def isAtTarget: Boolean = position == targetPoint
}

object Projectile {
  private val projectileSpeed: Double = 1

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
      damage,
      end
    )
  }
}