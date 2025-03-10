package data

import game.{EntityType, Sprite}
import game.EntityType._

object Sprites {
  private val playerSprite = Sprite(16 * 25, 0)
  private val wallSprite = Sprite(16 * 10, 16 * 17)
  private val enemySprite = Sprite(16 * 26, 0)

  val sprites: Map[EntityType, Sprite] = Map(
    Player -> playerSprite,
    Wall -> wallSprite,
    Enemy -> enemySprite
  )

  val cursorSprite: Sprite = Sprite(16 * 29, 16 * 14)
}
