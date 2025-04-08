package data

import game.EntityType.*
import game.{EntityType, Sprite}

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
  val deadSprite: Sprite = Sprite(0, 15, backgroundLayer)
  val fullHeartSprite: Sprite = Sprite(42, 10, uiLayer)
  val halfHeartSprite: Sprite = Sprite(41, 10, uiLayer)
  val emptyHeartSprite: Sprite = Sprite(40, 10, uiLayer)
  val cursorSprite: Sprite = Sprite(29, 14, uiLayer)
  val potionSprite: Sprite = Sprite(33, 13, uiLayer)
  val keySprite: Sprite = Sprite(32, 11, uiLayer)
  val doorSprite: Sprite = Sprite(0, 9, entityLayer)

  val sprites: Map[EntityType, Sprite] = Map(
    Player -> playerSprite,
    Wall -> wallSprite,
    Enemy -> ratSprite,
    Floor -> floorSprite,
    Door -> doorSprite,
    Key -> keySprite
  )
}
