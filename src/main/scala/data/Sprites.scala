package data

import game.{EntityType, Sprite}
import game.EntityType._

object Sprites {
  private val playerSprite = Sprite(16 * 25, 0, 1)
  private val wallSprite = Sprite(16 * 10, 16 * 17, 1)
  private val enemySprite = Sprite(16 * 26, 0, 1)
  private val floorSprite = Sprite(16 * 2, 16 * 0, 0)

  val sprites: Map[EntityType, Sprite] = Map(
    Player -> playerSprite,
    Wall -> wallSprite,
    Enemy -> enemySprite,
    Floor -> floorSprite
  )

  val cursorSprite: Sprite = Sprite(16 * 29, 16 * 14, 2)
}
