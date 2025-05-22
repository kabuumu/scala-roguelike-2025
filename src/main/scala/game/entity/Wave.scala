package game.entity

import game.{GameState, Point}
import game.entity.Drawable.*
import game.entity.Hitbox.*

case class Wave(range: Int) extends Component {
  //If range is > 0, expand hitbox and drawable outwards in all directions and decrease range by one
  def update(entity: Entity): Entity = {
    if (range > 0) {
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
          wave => wave.copy(range = range - 1)
        }
    } else entity
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

object Wave {
  extension (entity: Entity) {
    def waveUpdate(gameState: GameState): GameState = {
      entity.get[Wave] match {
        case Some(wave) if wave.range > 0 =>
          gameState.updateEntity(entity.id, wave.update)
        case Some(wave) if wave.range <= 0 =>
          gameState.remove(entity.id)
        case _ =>
          gameState
      }
    }
  }
}
