package scalafx

import data.Sprites
import game.EntityType.Wall
import game.{Entity, EntityType, GameState, Point}
import map.{MapGenerator, TileType}
import scalafx.Includes.*
import scalafx.animation.AnimationTimer
import scalafx.application.JFXApp3
import scalafx.application.JFXApp3.PrimaryStage
import scalafx.scene.Scene
import scalafx.scene.canvas.Canvas
import scalafx.scene.control.ScrollPane.ScrollBarPolicy
import scalafx.scene.control.{ScrollPane, TextArea}
import scalafx.scene.effect.ColorAdjust
import scalafx.scene.image.Image
import scalafx.scene.input.KeyCode
import scalafx.scene.layout.VBox
import scalafx.scene.text.Font
import ui.UIState.{Attack, UIState}
import ui.{GameController, UIState}
import Resources._

import scala.language.postfixOps

object App extends JFXApp3 {
  val scale = 3
  val spriteScale = 16
  val framesPerSecond = 16
  val allowedActionsPerSecond = 8

  override def start(): Unit = {
    val canvas = new Canvas(spriteScale * scale * 16, spriteScale * scale * 14)
    val messageArea = new TextArea {
      editable = false
      prefHeight = 64 * scale
      font = pixelFont
      style = "-fx-control-inner-background: black; -fx-text-fill: white;"
      focusTraversable = false
    }

    val scrollPane = new ScrollPane {
      content = messageArea
      vbarPolicy = ScrollBarPolicy.Never
      hbarPolicy = ScrollBarPolicy.Never
    }

    var keyCodes: Set[KeyCode] = Set.empty

    val walls = MapGenerator.generateRoomTree().tiles.map {
      case (game.Point(x, y), tileType) =>
        val entityType: EntityType = tileType match {
          case TileType.Floor => EntityType.Floor
          case TileType.Wall => EntityType.Wall
        }

        Entity(xPosition = x, yPosition = y, entityType = entityType, health = 0, lineOfSightBlocking = entityType == Wall)
    }

    val player = Entity(id = "Player ID", xPosition = 4, yPosition = 4, entityType = EntityType.Player, health = 10)

    val enemy = Entity(xPosition = 12, yPosition = 12, entityType = EntityType.Enemy, health = 2)
    val enemy2 = Entity(xPosition = 14, yPosition = 12, entityType = EntityType.Enemy, health = 2)

    val startingGameState = GameState(player.id, Set(player) ++ walls + enemy + enemy2)
    var controller = GameController(UIState.Move, startingGameState).init()

    val vbox = new VBox {
      children = Seq(canvas, scrollPane)
    }

    stage = new PrimaryStage {
      title = "scala-roguelike"
      scene = new Scene {
        content = vbox
        //set background to black
        fill = "black"

        onKeyPressed = { key =>
          keyCodes += key.code
        }

        onKeyReleased = { key =>
          keyCodes -= key.code
        }
      }
    }

    vbox.requestFocus()

    AnimationTimer { (currentTime: Long) =>
      if (controller.gameState.playerEntity.health <= 0) {
        println("Game Over")
        System.exit(0)
      }

      controller = controller.update(keyCodes.headOption, currentTime)

      updateCanvas(controller, canvas, spriteSheet)
      updateMessageArea(controller, messageArea)
    }.start()
  }

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
      case UIState.AttackList(enemies, position) => (enemies(position).xPosition - 7, enemies(position).yPosition - 4)
    }

    val playerVisibleEntities = state.gameState.getVisibleEntitiesFor(player)

    state.gameState.entities.toSeq
      .filter {
        entity =>
          player.sightMemory.exists(visiblePoint => entity.xPosition == visiblePoint.x && entity.yPosition == visiblePoint.y)
      }.sortBy {
        entity =>
          Sprites.sprites(entity.entityType).layer
      }.foreach {
        entity =>
          val visible = playerVisibleEntities.contains(entity)

          //Do not draw dynamic entities that are not visible
          if (entity.entityType.isStatic || visible) {
            drawEntity(entity, canvas, spriteSheet, xOffset, yOffset, visible)
          }
      }

    drawUiElements(state.uiState, canvas, spriteSheet, xOffset, yOffset)
  }

  private def updateMessageArea(state: GameController, messageArea: TextArea): Unit = {
    val messages = state.gameState.messages.take(4).mkString("\n")

    messageArea.text = messages
  }

  private def drawEntity(entity: Entity, canvas: Canvas, spriteSheet: Image, xOffset: Int, yOffset: Int, visible: Boolean): Unit = {
    val x = (entity.xPosition - xOffset) * spriteScale * scale
    val y = (entity.yPosition - yOffset) * spriteScale * scale
    val entitySprite = if (entity.isDead) Sprites.deadSprite else Sprites.sprites(entity.entityType)

    canvas.graphicsContext2D.save() // Save the current state

    if (!visible) {
      canvas.graphicsContext2D.setGlobalAlpha(0.5)
    } else {
      canvas.graphicsContext2D.setGlobalAlpha(1)
    }

    // Apply ColorAdjust effect if the entity is dead
    if (entity.isDead) {
      val colorAdjust = new ColorAdjust {
        hue = -0.5 // Adjust the hue (range: -1.0 to 1.0)
        saturation = -0.5 // Adjust the saturation (range: -1.0 to 1.0)
        brightness = -0.5 // Adjust the brightness (range: -1.0 to 1.0)
        contrast = 0.5 // Adjust the contrast (range: -1.0 to 1.0)
      }
      canvas.graphicsContext2D.setEffect(colorAdjust)
    } else {
      canvas.graphicsContext2D.setEffect(null)
    }

    canvas.graphicsContext2D.drawImage(spriteSheet, entitySprite.x, entitySprite.y, spriteScale, spriteScale, x, y, spriteScale * scale, spriteScale * scale)

    canvas.graphicsContext2D.restore() // Restore the saved state
  }

  private def drawUiElements(uiState: UIState, canvas: Canvas, spriteSheet: Image, xOffset: Int, yOffset: Int): Unit = {
    uiState match {
      case Attack(cursorX, cursorY) =>
        val cursorSprite = Sprites.cursorSprite
        val offsetCursorX = (cursorX - xOffset) * spriteScale * scale
        val offsetCursorY = (cursorY - yOffset) * spriteScale * scale
        canvas.graphicsContext2D.setGlobalAlpha(1)

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
      case UIState.AttackList(enemies, position) =>
        val cursorSprite = Sprites.cursorSprite
        val offsetCursorX = (enemies(position).xPosition - xOffset) * spriteScale * scale
        val offsetCursorY = (enemies(position).yPosition - yOffset) * spriteScale * scale
        canvas.graphicsContext2D.setGlobalAlpha(1)

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