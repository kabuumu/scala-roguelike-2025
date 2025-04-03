package scalafx

import data.Sprites
import game.EntityType.Wall
import game.*
import map.{Dungeon, MapGenerator, TileType}
import scalafx.Includes.*
import scalafx.Resources.*
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

import scala.language.postfixOps

object App extends JFXApp3 {
  val scale = 2
  val spriteScale = 16
  val framesPerSecond = 16
  val allowedActionsPerSecond = 8
  val canvasX: Int = 40
  val canvasY: Int = 25
  val debugOmniscience: Boolean = false

  override def start(): Unit = {
    val canvas = new Canvas(spriteScale * scale * canvasX, spriteScale * scale * canvasY)
    val messageArea = new TextArea {
      editable = false
      prefWidth = spriteScale * scale * canvasX
      prefHeight = spriteScale * scale * 4
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

    val dungeon = MapGenerator.generateDungeon(10, 182)

    val mapTiles = dungeon.tiles.map {
      case (game.Point(x, y), tileType) =>
        val entityType: EntityType = tileType match {
          case TileType.Floor => EntityType.Floor
          case TileType.Wall => EntityType.Wall
        }

        Entity(xPosition = x, yPosition = y, entityType = entityType, health = Health(0), lineOfSightBlocking = entityType == Wall)
    }

    val enemies = dungeon.roomGrid.tail.map {
      case game.Point(x, y) =>
        Entity(xPosition = x * Dungeon.roomSize + 5, yPosition = y * Dungeon.roomSize + 5, entityType = EntityType.Enemy, health = Health(2))
    }

    val player = dungeon.roomGrid.head match {
      case startingRoom =>
        Entity(id = "Player ID", xPosition = startingRoom.x * Dungeon.roomSize + 5, yPosition = startingRoom.y * Dungeon.roomSize + 5, entityType = EntityType.Player, health = Health(10))
    }

    val startingGameState = GameState(player.id, Set(player) ++ mapTiles ++ enemies)
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
      if (controller.gameState.playerEntity.health.current <= 0) {
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
      case UIState.Move => (playerX - (canvasX / 2), playerY - (canvasY / 2))
      case UIState.Attack(cursorX, cursorY) => (cursorX - (canvasX / 2), cursorY - (canvasY / 2))
      case UIState.AttackList(enemies, position) => (enemies(position).xPosition - (canvasX / 2), enemies(position).yPosition - (canvasY / 2))
    }

    val playerVisibleEntities = state.gameState.getVisibleEntitiesFor(player)

    state.gameState.entities.toSeq
      .filter {
        entity =>
          player.sightMemory.exists(visiblePoint => entity.xPosition == visiblePoint.x && entity.yPosition == visiblePoint.y) || debugOmniscience
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
    drawPlayerHearts(canvas, player)
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

    canvas.graphicsContext2D.drawImage(
      spriteSheet,
      entitySprite.x * spriteScale,
      entitySprite.y * spriteScale,
      spriteScale,
      spriteScale,
      x,
      y,
      spriteScale * scale,
      spriteScale * scale
    )

    canvas.graphicsContext2D.restore() // Restore the saved state
  }

  private def drawUiElements(uiState: UIState, canvas: Canvas, spriteSheet: Image, xOffset: Int, yOffset: Int): Unit = {
    def drawCursor(x: Int, y: Int): Unit = {
      val cursorSprite = Sprites.cursorSprite

      canvas.graphicsContext2D.setGlobalAlpha(1)

      canvas.graphicsContext2D.drawImage(
        spriteSheet,
        cursorSprite.x * spriteScale,
        cursorSprite.y * spriteScale,
        spriteScale,
        spriteScale,
        x,
        y,
        spriteScale * scale,
        spriteScale * scale
      )
    }

    uiState match {
      case Attack(cursorX, cursorY) =>
        val offsetCursorX = (cursorX - xOffset) * spriteScale * scale
        val offsetCursorY = (cursorY - yOffset) * spriteScale * scale

        drawCursor(offsetCursorX, offsetCursorY)
      case UIState.AttackList(enemies, position) =>
        val offsetCursorX = (enemies(position).xPosition - xOffset) * spriteScale * scale
        val offsetCursorY = (enemies(position).yPosition - yOffset) * spriteScale * scale

        drawCursor(offsetCursorX, offsetCursorY)
      case _ =>
    }
  }

  def drawPlayerHearts(canvas: Canvas, player: Entity): Unit = {
    val heartWidth = spriteScale * scale
    val heartHeight = spriteScale * scale

    val maxHearts = player.health.max / 2
    val fullHearts = player.health.current / 2
    val hasHalfHeart = player.health.current % 2 != 0

    for (i <- 0 until maxHearts) {
      val heartX = i * heartWidth
      val heartY = 0

      if (i < fullHearts) {
        // Draw full heart
        canvas.graphicsContext2D.drawImage(
          spriteSheet,
          Sprites.fullHeartSprite.x * spriteScale,
          Sprites.fullHeartSprite.y * spriteScale,
          spriteScale,
          spriteScale,
          heartX,
          heartY,
          heartWidth,
          heartHeight
        )
      } else if (i == fullHearts && hasHalfHeart) {
        // Draw half heart
        canvas.graphicsContext2D.drawImage(
          spriteSheet,
          Sprites.halfHeartSprite.x * spriteScale,
          Sprites.halfHeartSprite.y * spriteScale,
          spriteScale,
          spriteScale,
          heartX,
          heartY,
          heartWidth,
          heartHeight
        )
      } else {
        // Draw empty heart
        canvas.graphicsContext2D.drawImage(
          spriteSheet,
          Sprites.emptyHeartSprite.x * spriteScale,
          Sprites.emptyHeartSprite.y * spriteScale,
          spriteScale,
          spriteScale,
          heartX,
          heartY,
          heartWidth,
          heartHeight
        )
      }
    }
  }
}