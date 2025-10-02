package data

import game.entity.KeyColour.*
import game.entity.EntityType
import game.entity.EntityType.*
import game.Sprite

object Sprites {
  val floorLayer = 0
  val backgroundLayer = 1
  val entityLayer = 2
  val uiLayer = 3

  val playerSprite: Sprite = Sprite(25, 0, entityLayer)
  val enemySprite: Sprite = Sprite(26, 0, entityLayer)
  
  val wallSprite: Sprite = Sprite(10, 17, entityLayer)
  val floorSprite: Sprite = Sprite(2, 0, floorLayer)
  val maybeFloorSprite: Sprite = Sprite(1, 0, floorLayer)
  val waterSprite: Sprite = Sprite(8, 5, floorLayer)
  val bridgeSprite: Sprite = Sprite(16, 5, floorLayer)
  val rockSprite: Sprite = Sprite(5, 2, floorLayer)
  
  val ratSprite: Sprite = Sprite(31, 8, entityLayer)
  val snakeSprite: Sprite = Sprite(28, 8, entityLayer)
  val slimeSprite: Sprite = Sprite(29, 8, entityLayer)
  val slimeletSprite: Sprite = Sprite(30, 8, entityLayer)
  val deadSprite: Sprite = Sprite(0, 15, backgroundLayer)
  
  val bossSpriteTL: Sprite = Sprite(25, 2, entityLayer)
  val bossSpriteTR: Sprite = Sprite(26, 2, entityLayer)
  val bossSpriteBL: Sprite = Sprite(25, 3, entityLayer)
  val bossSpriteBR: Sprite = Sprite(26, 3, entityLayer)
  
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

  // Equipment sprites
  val leatherHelmetSprite: Sprite = Sprite(33, 0, uiLayer)
  val ironHelmetSprite: Sprite = Sprite(36, 0, uiLayer)
  val chainmailArmorSprite: Sprite = Sprite(32, 1, uiLayer)
  val plateArmorSprite: Sprite = Sprite(33, 1, uiLayer)
  
  // New equipment sprites - using default sprite for items without specific sprites
  val leatherBootsSprite: Sprite = Sprite(39, 1, uiLayer) // using defaultItemSprite
  val ironBootsSprite: Sprite = Sprite(39, 0, uiLayer) // using defaultItemSprite  
  val leatherGlovesSprite: Sprite = Sprite(41, 1, uiLayer) // using defaultItemSprite
  val ironGlovesSprite: Sprite = Sprite(41, 0, uiLayer) // using defaultItemSprite
  val basicSwordSprite: Sprite = Sprite(32, 6, uiLayer) // using defaultItemSprite
  val ironSwordSprite: Sprite = Sprite(32, 7, uiLayer) // using defaultItemSprite
  
  val defaultItemSprite: Sprite = Sprite(37, 13, uiLayer)
  
  val stairsSprite: Sprite = Sprite(5, 17, entityLayer)

  val errorSprite: Sprite = Sprite(35, 21, uiLayer)
}
