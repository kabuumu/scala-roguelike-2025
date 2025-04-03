package data

import game.{EntityType, Sprite}
import game.EntityType._

object Sprites {
  val floorLayer = 0
  val backgroundLayer = 1
  val entityLayer = 2
  val uiLayer = 3

  val playerSprite: Sprite = Sprite(25, 0, entityLayer)
  val wallSprite: Sprite = Sprite(10, 17, entityLayer)
  val enemySprite: Sprite = Sprite(26, 0, entityLayer)
  val floorSprite: Sprite = Sprite(2, 0, floorLayer)
  val ratSprite: Sprite = Sprite(31, 8, entityLayer)
  val deadSprite: Sprite = Sprite (0, 15, backgroundLayer)
  val fullHeartSprite: Sprite = Sprite(16, 16, uiLayer)
  val halfHeartSprite: Sprite = Sprite(16, 16, uiLayer)
  val emptyHeartSprite: Sprite = Sprite(16, 16, uiLayer)

  val sprites: Map[EntityType, Sprite] = Map(
    Player -> playerSprite,
    Wall -> wallSprite,
    Enemy -> ratSprite,
    Floor -> floorSprite
  )

  val cursorSprite: Sprite = Sprite(16 * 29, 16 * 14, uiLayer)
}
