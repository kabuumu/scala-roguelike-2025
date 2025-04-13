package data

import dungeongenerator.generator.Entity.KeyColour._
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
  val yellowKeySprite: Sprite = Sprite(32, 11, uiLayer)
  val blueKeySprite: Sprite = Sprite(33, 11, uiLayer)
  val redKeySprite: Sprite = Sprite(34, 11, uiLayer)
  val yellowDoorSprite: Sprite = Sprite(0, 11, entityLayer)
  val blueDoorSprite: Sprite = Sprite(0, 9, entityLayer)
  val redDoorSprite: Sprite = Sprite(0, 10, entityLayer)

  val sprites: Map[EntityType, Sprite] = Map(
    Player -> playerSprite,
    Wall -> wallSprite,
    Enemy -> ratSprite,
    Floor -> floorSprite,
    LockedDoor(Yellow) -> yellowDoorSprite,
    LockedDoor(Blue) -> blueDoorSprite,
    LockedDoor(Red) -> redDoorSprite,
    Key(Yellow) -> yellowKeySprite,
    Key(Blue) -> blueKeySprite,
    Key(Red) -> redKeySprite,
    ItemEntity(game.Item.Potion) -> potionSprite,
  )
}
