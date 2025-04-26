package game

case class Projectile(precisePosition: (Double, Double), xVelocity: Double, yVelocity: Double) {
  def update(currentGameState: GameState): GameState = {
    val (currentX, currentY) = precisePosition
    val newX = currentX + xVelocity
    val newY = currentY + yVelocity

    val updatedProjectile = Projectile((newX, newY), xVelocity, yVelocity)

    currentGameState.entities.find(entity =>
      entity.position == updatedProjectile.position || entity.position == position
    ) match {
      case Some(collision) =>
        currentGameState.copy(
          projectiles = currentGameState.projectiles.filterNot(_.precisePosition == precisePosition)
        ).updateEntity(
          collision.id,
          collision.takeDamage(1)
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
  val projectileSpeed: Double = 1

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