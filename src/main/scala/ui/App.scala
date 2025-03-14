package ui

import data.Sprites
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
  val scale = 3
  val spriteScale = 16
  val framesPerSecond = 8

  override def start(): Unit = {
    val spriteSheet = Image("file:src/resources/sprites/sprites.png")

    val canvas = new Canvas(spriteScale * scale * 16, spriteScale * scale * 10)

    var keyCodes: Set[KeyCode] = Set.empty

    val walls = MapGenerator.generate(20, 15).tiles.collect {
      case (game.Point(x, y), TileType.Wall) => Entity(xPosition = x, yPosition = y, entityType = EntityType.Wall)
    }

    val player = Entity(xPosition = 5, yPosition = 5, entityType = EntityType.Player)

    val enemy = Entity(xPosition = 9, yPosition = 9, entityType = EntityType.Enemy)

    val startingGameState = GameState(player.id, Set(player) ++ walls + enemy)
    var state = CoreState(UIState.Move, startingGameState)


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
        state = state.update(keyCode, currentTime)
      }
      updateCanvas(state, canvas, spriteSheet)
    }
  }.start()


  private def updateCanvas(state: CoreState, canvas: Canvas, spriteSheet: Image): Unit = {
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

    state.gameState.entities.foreach { entity =>
      drawEntity(entity, canvas, spriteSheet, xOffset, yOffset)
    }

    drawUiElements(state.uiState, canvas, spriteSheet, xOffset, yOffset)
  }

  private def drawEntity(entity: Entity, canvas: Canvas, spriteSheet: Image, xOffset: Int, yOffset: Int): Unit = {
    val x = (entity.xPosition - xOffset) * spriteScale * scale
    val y = (entity.yPosition - yOffset) * spriteScale * scale
    val entitySprite = Sprites.sprites(entity.entityType)
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