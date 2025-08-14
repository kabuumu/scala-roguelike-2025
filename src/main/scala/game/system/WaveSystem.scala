package game.system

import game.entity.{Drawable, Entity, Hitbox, Wave}
import game.system.event.GameSystemEvent
import game.{GameState, Point}

object WaveSystem extends GameSystem {
  override def update(gameState: GameState, events: Seq[GameSystemEvent.GameSystemEvent]): (GameState, Seq[GameSystemEvent.GameSystemEvent]) =
    val updatedGamestate = gameState.entities.filter(_.has[Wave])
      .foldLeft(gameState) {
      case (currentState, entity) =>
        entity.get[Wave] match {
          case Some(wave) if wave.range > 0 =>
            currentState.updateEntity(entity.id, expandWave)
          case Some(wave) =>
            // If the wave has no range left, we can remove it
            currentState.remove(entity.id)
          case None =>
            currentState
        }
      }

    (updatedGamestate, Nil)

  def expandWave(entity: Entity): Entity = {
    entity
      .update[Hitbox] {
        hitbox => hitbox.copy(points = expandPoints(hitbox.points))
      }
      .update[Drawable] {
        drawable =>
          val waveSprite = drawable.sprites.head._2 //Assuming that waves will only have a single sprite - won't work otherwise
          assert(drawable.sprites.forall(_._2 == waveSprite))

          val points = drawable.sprites.map(_._1)
          Drawable(expandPoints(points).map(_ -> waveSprite))
      }
      .update[Wave] {
        wave => wave.copy(range = wave.range - 1)
      }
  }
  
  def expandPoints(points: Set[Point]): Set[Point] = {
    val minX = points.map(_.x).min
    val maxX = points.map(_.x).max
    val minY = points.map(_.y).min
    val maxY = points.map(_.y).max

    (for {
      x <- minX - 1 to maxX + 1
      y <- minY - 1 to maxY + 1
    } yield Point(x, y)).toSet
  }
}
