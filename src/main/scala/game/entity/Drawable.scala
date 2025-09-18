package game.entity

import data.Sprites
import game.entity.Health.*
import game.{Point, Sprite}

case class Drawable(sprites: Set[(Point, Sprite)]) extends Component

object Drawable {
  def apply(sprite: Sprite): Drawable = {
    Drawable(Set((Point(0, 0), sprite)))
  }

  extension (entity: Entity) {
    def sprites: Set[(Point, Sprite)] = {
      for {
        movement <- entity.get[Movement].toSet
        drawable <- entity.get[Drawable].toSet
        (point, sprite) <- drawable.sprites
      } yield (point + movement.position, sprite)
    }
  }
}