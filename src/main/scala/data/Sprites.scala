package data

import game.Item.KeyColour.*
import game.entity.EntityType
import game.entity.EntityType.*
import game.{Item, Sprite}

object Sprites {
  val floorLayer = 0
  val backgroundLayer = 1
  val entityLayer = 2
  val uiLayer = 3

  val playerSprite: Sprite = Sprite(25, 0, entityLayer)
  val wallSprite: Sprite = Sprite(10, 17, entityLayer)
  val enemySprite: Sprite = Sprite(26, 0, entityLayer)
  val floorSprite: Sprite = Sprite(2, 0, floorLayer)
  val maybeFloorSprite: Sprite = Sprite(1, 0, floorLayer)
  val waterSprite: Sprite = Sprite(8, 5, floorLayer)
  val ratSprite: Sprite = Sprite(31, 8, entityLayer)
  val snakeSprite: Sprite = Sprite(28, 8, entityLayer)
  val deadSprite: Sprite = Sprite(0, 15, backgroundLayer)
  val fullHeartSprite: Sprite = Sprite(42, 10, uiLayer)
  val halfHeartSprite: Sprite = Sprite(41, 10, uiLayer)
  val emptyHeartSprite: Sprite = Sprite(40, 10, uiLayer)
  val cursorSprite: Sprite = Sprite(29, 14, uiLayer)
  val potionSprite: Sprite = Sprite(33, 13, uiLayer)
  val scrollSprite: Sprite = Sprite(33, 15, uiLayer)
  val bowSprite: Sprite = Sprite(37, 6, uiLayer)
  val yellowKeySprite: Sprite = Sprite(32, 11, uiLayer)
  val blueKeySprite: Sprite = Sprite(33, 11, uiLayer)
  val redKeySprite: Sprite = Sprite(34, 11, uiLayer)
  val yellowDoorSprite: Sprite = Sprite(0, 11, entityLayer)
  val blueDoorSprite: Sprite = Sprite(0, 9, entityLayer)
  val redDoorSprite: Sprite = Sprite(0, 10, entityLayer)
  val arrowSprite: Sprite = Sprite(40, 5, entityLayer)
  val projectileSprite: Sprite = Sprite(28, 11, entityLayer)

  val errorSprite: Sprite = Sprite(35, 21, uiLayer)

  val itemSprites: Map[Item.Item, Sprite] = Map(
    game.Item.Potion -> potionSprite,
    game.Item.Key(Yellow) -> yellowKeySprite,
    game.Item.Key(Blue) -> blueKeySprite,
    game.Item.Key(Red) -> redKeySprite,
    game.Item.Scroll -> scrollSprite,
    game.Item.Bow -> bowSprite,
    game.Item.Arrow -> arrowSprite,
  )
}
