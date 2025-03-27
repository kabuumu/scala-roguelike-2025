package data

import game.{EntityType, Sprite}
import game.EntityType._

object Sprites {
  private val playerSprite = Sprite(16 * 25, 0, 2)
  private val wallSprite = Sprite(16 * 10, 16 * 17, 2)
  private val enemySprite = Sprite(16 * 26, 0, 2)
  private val floorSprite = Sprite(16 * 2, 16 * 0, 0)
  private val ratSprite = Sprite(16 * 31, 16 * 8, 2)
  private val deadSprite = Sprite (0, 16 * 15, 1)

  val sprites: Map[EntityType, Sprite] = Map(
    Player -> playerSprite,
    Wall -> wallSprite,
    Enemy -> ratSprite,
    Floor -> floorSprite
  )

  val cursorSprite: Sprite = Sprite(16 * 29, 16 * 14, 2)
}
