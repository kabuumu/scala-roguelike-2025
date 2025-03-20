package ui

import data.Sprites
import game.EntityType.Wall
import game.{Entity, EntityType, GameState}
import map.{MapGenerator, TileType}
import scalafx.Includes.*
import scalafx.animation.AnimationTimer
import scalafx.application.JFXApp3
import scalafx.application.JFXApp3.PrimaryStage
import scalafx.scene.Scene
import scalafx.scene.canvas.Canvas
import scalafx.scene.image.Image
import scalafx.scene.input.KeyCode
import ui.UIState.Attack

import scala.language.postfixOps


object App extends JFXApp3 {
  val scale = 1
  val spriteScale = 16
  val framesPerSecond = 8

  override def start(): Unit = {
    val spriteSheet = Image("file:src/resources/sprites/sprites.png")

    val canvas = new Canvas(spriteScale * scale * 16, spriteScale * scale * 10)

    var keyCodes: Set[KeyCode] = Set.empty

    val walls = MapGenerator.generateRoomTree().tiles.map {
      case (game.Point(x, y), tileType) =>
        val entityType: EntityType = tileType match {
          case TileType.Floor => EntityType.Floor
          case TileType.Wall => EntityType.Wall
        }

        Entity(xPosition = x, yPosition = y, entityType = entityType, health = 0, lineOfSightBlocking = entityType == Wall)
    }

    val player = Entity(xPosition = 5, yPosition = 5, entityType = EntityType.Player, health = 2)

    val enemy = Entity(xPosition = 9, yPosition = 9, entityType = EntityType.Enemy, health = 2)

    val startingGameState = GameState(player.id, Set(player) ++ walls + enemy)
    var controller = GameController(UIState.Move, startingGameState).init()

    stage = new PrimaryStage {
      title = "scala-roguelike"
      scene = new Scene {
        content = canvas
        //set background to black
        fill = "black"

        onKeyPressed = {
          key => keyCodes += key.code
        }

        onKeyReleased = {
          key => keyCodes -= key.code
        }
      }
    }

    AnimationTimer { (currentTime: Long) =>

      keyCodes.headOption.foreach { keyCode =>
        controller = controller.update(keyCode, currentTime)
      }
      updateCanvas(controller, canvas, spriteSheet)
    }
  }.start()


  private def updateCanvas(state: GameController, canvas: Canvas, spriteSheet: Image): Unit = {
    canvas.graphicsContext2D.clearRect(0, 0, canvas.width.value, canvas.height.value)
    canvas.graphicsContext2D.setImageSmoothing(false)

    //Update to draw entities relative to the player
    val player = state.gameState.playerEntity
    val playerX = player.xPosition
    val playerY = player.yPosition

    val (xOffset, yOffset) = state.uiState match {
      case UIState.Move => (playerX - 7, playerY - 4)
      case UIState.Attack(cursorX, cursorY) => (cursorX - 7, cursorY - 4)
    }

    state.gameState.entities.toSeq
      .filter {
        entity =>
          player.sightMemory.contains(entity) || entity == player
      }.sortBy {
        entity =>
          Sprites.sprites(entity.entityType).layer
      }.foreach {
        entity =>
          val visible = state.gameState.getLineOfSight(player).contains(entity)
          drawEntity(entity, canvas, spriteSheet, xOffset, yOffset, visible)
      }

    drawUiElements(state.uiState, canvas, spriteSheet, xOffset, yOffset)
  }

  private def drawEntity(entity: Entity, canvas: Canvas, spriteSheet: Image, xOffset: Int, yOffset: Int, visible: Boolean): Unit = {
    val x = (entity.xPosition - xOffset) * spriteScale * scale
    val y = (entity.yPosition - yOffset) * spriteScale * scale
    val entitySprite = Sprites.sprites(entity.entityType)

    if (!visible) {
      canvas.graphicsContext2D.setGlobalAlpha(0.5)
    } else {
      canvas.graphicsContext2D.setGlobalAlpha(1)
    }

    canvas.graphicsContext2D.drawImage(spriteSheet, entitySprite.x, entitySprite.y, spriteScale, spriteScale, x, y, spriteScale * scale, spriteScale * scale)
  }

  private def drawUiElements(uiState: UIState, canvas: Canvas, spriteSheet: Image, xOffset: Int, yOffset: Int): Unit = {
    uiState match {
      case Attack(cursorX, cursorY) =>
        val cursorSprite = Sprites.cursorSprite
        val offsetCursorX = (cursorX - xOffset) * spriteScale * scale
        val offsetCursorY = (cursorY - yOffset) * spriteScale * scale
        canvas.graphicsContext2D.drawImage(
          spriteSheet,
          cursorSprite.x,
          cursorSprite.y,
          spriteScale,
          spriteScale,
          offsetCursorX,
          offsetCursorY,
          spriteScale * scale,
          spriteScale * scale
        )
      case _ =>
    }
  }
}