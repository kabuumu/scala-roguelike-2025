package scalafx

import data.Sprites
import dungeongenerator.generator.Entity.KeyColour.*
import game.*
import game.Item.{Key, Potion}
import map.TileType
import scalafx.Includes.*
import scalafx.Resources.*
import scalafx.animation.AnimationTimer
import scalafx.application.JFXApp3
import scalafx.application.JFXApp3.PrimaryStage
import scalafx.scene.Scene
import scalafx.scene.canvas.Canvas
import scalafx.scene.control.ScrollPane.ScrollBarPolicy
import scalafx.scene.control.{ScrollPane, TextArea}
import scalafx.scene.image.Image
import scalafx.scene.input.KeyCode
import scalafx.scene.layout.VBox
import scalafx.scene.text.Font
import ui.UIState.{Attack, UIState}
import ui.{GameController, UIState}

import scala.language.postfixOps

object App extends JFXApp3 {
  val uiScale = 3
  val spriteScale = 16
  val canvasX: Int = 90 / uiScale
  val canvasY: Int = 45 / uiScale
  val debugOmniscience: Boolean = false

  override def start(): Unit = {
    val canvas = new Canvas(spriteScale * uiScale * canvasX, spriteScale * uiScale * canvasY)
    val messageArea = new TextArea {
      editable = false
      prefWidth = spriteScale * uiScale * canvasX
      prefHeight = spriteScale * uiScale * 4
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

    val startingGameState = StartingState.startingGameState
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

    //Draw first frame
    updateCanvas(controller, canvas, spriteSheet)
    updateMessageArea(controller, messageArea)

    AnimationTimer { (currentTime: Long) =>
      if (controller.gameState.playerEntity.health.current <= 0) {
        println("Game Over")
      }
      val newController = controller.update(keyCodes.headOption.map(InputTranslator.translateKeyCode), currentTime)

      if (newController != controller) {
        controller = newController
        updateCanvas(controller, canvas, spriteSheet)
        updateMessageArea(controller, messageArea)
      }

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

    val playerVisiblePoints = state.gameState.playerVisiblePoints
    val visibleEntities = state.gameState.entities.filter {
      entity =>
        (entity.position.getChebyshevDistance(player.position) <= canvasX / 2) &&
          (player.sightMemory.contains(entity.position) || debugOmniscience)
    }


    val visibleTiles = for {
      x <- playerX - (canvasX / 2) to playerX + (canvasX / 2)
      y <- playerY - (canvasY / 2) to playerY + (canvasY / 2)
      tilePosition = Point(x, y)
      if player.sightMemory.contains(tilePosition) || debugOmniscience
      tile <- state.gameState.dungeon.tiles.get(tilePosition)
    } yield tilePosition -> tile

    visibleTiles.foreach {
      case (tilePosition, tileType) =>
        val visible = playerVisiblePoints.contains(tilePosition)

        drawTile(tileType, canvas, spriteSheet, tilePosition.x - xOffset, tilePosition.y - yOffset, visible)
    }


    visibleEntities.sortBy {
      entity =>
        Sprites.sprites(entity.entityType).layer
    }.foreach {
      entity =>
        val visible = playerVisiblePoints.contains(entity.position)

        //Do not draw dynamic entities that are not visible
        if (entity.entityType.isStatic || visible) {
          drawEntity(entity, canvas, spriteSheet, xOffset, yOffset, visible)
        }
    }

    drawUiElements(state.uiState, canvas, spriteSheet, xOffset, yOffset)
    drawPlayerHearts(canvas, player)
    drawInventory(canvas, player)
  }

  private def updateMessageArea(state: GameController, messageArea: TextArea): Unit = {
    val messages = state.gameState.messages.take(4).mkString("\n")

    messageArea.text = messages
  }

  private def drawTile(tileType: TileType, canvas: Canvas, spriteSheet: Image, x: Int, y: Int, visible: Boolean): Unit = {
    val tileSprite = tileType match {
      case TileType.Wall => Sprites.wallSprite
      case TileType.Floor => Sprites.floorSprite
    }

    if (!visible) {
      canvas.graphicsContext2D.setGlobalAlpha(0.5)
    } else {
      canvas.graphicsContext2D.setGlobalAlpha(1)
    }

    canvas.graphicsContext2D.drawImage(
      spriteSheet,
      tileSprite.x * spriteScale,
      tileSprite.y * spriteScale,
      spriteScale,
      spriteScale,
      x * spriteScale * uiScale,
      y * spriteScale * uiScale,
      spriteScale * uiScale,
      spriteScale * uiScale
    )
  }

  private def drawEntity(entity: Entity, canvas: Canvas, spriteSheet: Image, xOffset: Int, yOffset: Int, visible: Boolean): Unit = {
    val x = (entity.xPosition - xOffset) * spriteScale * uiScale
    val y = (entity.yPosition - yOffset) * spriteScale * uiScale
    val entitySprite = if (entity.isDead) Sprites.deadSprite else Sprites.sprites(entity.entityType)

    if (!visible) {
      canvas.graphicsContext2D.setGlobalAlpha(0.5)
    } else {
      canvas.graphicsContext2D.setGlobalAlpha(1)
    }

    canvas.graphicsContext2D.drawImage(
      spriteSheet,
      entitySprite.x * spriteScale,
      entitySprite.y * spriteScale,
      spriteScale,
      spriteScale,
      x,
      y,
      spriteScale * uiScale,
      spriteScale * uiScale
    )
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
        spriteScale * uiScale,
        spriteScale * uiScale
      )
    }

    uiState match {
      case Attack(cursorX, cursorY) =>
        val offsetCursorX = (cursorX - xOffset) * spriteScale * uiScale
        val offsetCursorY = (cursorY - yOffset) * spriteScale * uiScale

        drawCursor(offsetCursorX, offsetCursorY)
      case UIState.AttackList(enemies, position) =>
        val offsetCursorX = (enemies(position).xPosition - xOffset) * spriteScale * uiScale
        val offsetCursorY = (enemies(position).yPosition - yOffset) * spriteScale * uiScale

        drawCursor(offsetCursorX, offsetCursorY)
      case _ =>
    }
  }

  def drawPlayerHearts(canvas: Canvas, player: Entity): Unit = {
    val heartWidth = spriteScale * uiScale
    val heartHeight = spriteScale * uiScale
    val maxHearts = player.health.max / 2
    val fullHearts = player.health.current / 2
    val hasHalfHeart = player.health.current % 2 != 0

    for (i <- 0 until maxHearts) {
      val heartX = i * heartWidth
      val heartY = 0
      val sprite = if (i < fullHearts) Sprites.fullHeartSprite
      else if (i == fullHearts && hasHalfHeart) Sprites.halfHeartSprite
      else Sprites.emptyHeartSprite

      canvas.graphicsContext2D.drawImage(
        spriteSheet,
        sprite.x * spriteScale,
        sprite.y * spriteScale,
        spriteScale,
        spriteScale,
        heartX,
        heartY,
        heartWidth,
        heartHeight
      )
    }
  }

  def drawInventory(canvas: Canvas, player: Entity): Unit = {
    val itemWidth = spriteScale * uiScale
    val itemHeight = spriteScale * uiScale

    for (i <- player.inventory.indices) {
      val itemX = i * itemWidth
      val itemY = spriteScale * uiScale
      val item = player.inventory(i)
      val sprite = item match {
        case Potion => Sprites.potionSprite
        case Key(Yellow) => Sprites.yellowKeySprite
        case Key(Blue) => Sprites.blueKeySprite
        case Key(Red) => Sprites.redKeySprite
      }

      canvas.graphicsContext2D.drawImage(
        spriteSheet,
        sprite.x * spriteScale,
        sprite.y * spriteScale,
        spriteScale,
        spriteScale,
        itemX,
        itemY,
        itemWidth,
        itemHeight
      )
    }
  }
}