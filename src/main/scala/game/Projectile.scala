package game

case class Projectile(precisePosition: (Double, Double), xVelocity: Double, yVelocity: Double) {


  def update(currentGameState: GameState): GameState = {
    val (currentX, currentY) = precisePosition
    val newX = currentX + xVelocity
    val newY = currentY + yVelocity

    val updatedProjectile = Projectile((newX, newY), xVelocity, yVelocity)

    currentGameState.entities.find(_.position == updatedProjectile.position) match {
      case Some(collision) =>
        println(s"Projectile hit ${collision.entityType} at ${collision.position}")

        currentGameState.copy(
          projectiles = currentGameState.projectiles.filterNot(_.precisePosition == precisePosition)
        )
      case None =>
        currentGameState.copy(
          projectiles = currentGameState.projectiles.filterNot(_.precisePosition == precisePosition) :+ updatedProjectile
        )
    }
  }

  val position: Point = {
    val (x, y) = precisePosition
    Point(x.round.toInt, y.round.toInt)
  }
}

object Projectile {
  val projectileSpeed = 1

  def apply(start: Point, end: Point): Projectile = {
    val dx = end.x - start.x
    val dy = end.y - start.y
    val magnitude = Math.sqrt(dx * dx + dy * dy)
    val xVelocity = (dx / magnitude) * projectileSpeed
    val yVelocity = (dy / magnitude) * projectileSpeed
    Projectile(
      (start.x.toDouble + xVelocity, start.y.toDouble + yVelocity),
      xVelocity,
      yVelocity
    )
  }
}