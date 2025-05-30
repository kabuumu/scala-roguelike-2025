package scalafx

import data.Sprites
import game.*
import game.Item.Item
import game.entity.*
import game.entity.Drawable.*
import game.entity.EntityType.*
import game.entity.Health.*
import game.entity.Inventory.*
import game.entity.Movement.*
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
import scalafx.scene.paint.Color
import scalafx.scene.text.Font
import ui.UIState.{ScrollSelect, UIState}
import ui.{GameController, UIState}

import scala.language.postfixOps

object App extends JFXApp3 {
  val uiScale = 2
  val spriteScale = 16
  val canvasX: Int = 90 / uiScale
  val canvasY: Int = 45 / uiScale
  val debugOmniscience: Boolean = false

  override def start(): Unit = {
    val canvas = new Canvas(spriteScale * uiScale * canvasX, spriteScale * uiScale * canvasY)
    val messageArea = new TextArea {
      editable = false
      prefWidth = spriteScale * uiScale * canvasX
      prefHeight = spriteScale * uiScale * 4.5
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
      if (controller.gameState.playerEntity.currentHealth <= 0) {
        println("Game Over")
      }
      val newController = controller.update(keyCodes.headOption.map(InputTranslator.translateKeyCode), currentTime)

      val fps = 1000000000 / (currentTime - controller.lastUpdateTime)

      if(newController.gameState.messages != controller.gameState.messages) {
        updateMessageArea(newController, messageArea)
      }

      if (newController.gameState.drawableChanges != controller.gameState.drawableChanges
        || newController.uiState != controller.uiState
        || newController.gameState.messages != controller.gameState.messages) {
        updateCanvas(newController, canvas, spriteSheet)
      }

      controller = newController

    }.start()
  }

  private def updateCanvas(state: GameController, canvas: Canvas, spriteSheet: Image): Unit = {
    canvas.graphicsContext2D.clearRect(0, 0, canvas.width.value, canvas.height.value)
    canvas.graphicsContext2D.setImageSmoothing(false)

    //Update to draw entities relative to the player
    val player = state.gameState.playerEntity
    val Point(playerX, playerY) = player.position

    val (xOffset, yOffset) = (playerX - (canvasX / 2), playerY - (canvasY / 2))

    val playerVisiblePoints = state.gameState.playerVisiblePoints
    val visibleEntities = state.gameState.entities.filter {
      entity =>
        (entity.position.getChebyshevDistance(player.position) <= canvasX / 2)
          && (player.exists[SightMemory](_.seenPoints.contains(entity.position)) || debugOmniscience)
    }


    val visibleTiles = for {
      x <- playerX - (canvasX / 2) to playerX + (canvasX / 2)
      y <- playerY - (canvasY / 2) to playerY + (canvasY / 2)
      tilePosition = Point(x, y)
      if player.exists[SightMemory](_.seenPoints.contains(tilePosition)) || debugOmniscience
      tile <- state.gameState.dungeon.tiles.get(tilePosition)
    } yield tilePosition -> tile

    visibleTiles.foreach {
      case (tilePosition, tileType) =>
        val visible = playerVisiblePoints.contains(tilePosition)

        drawTile(tileType, canvas, spriteSheet, tilePosition.x - xOffset, tilePosition.y - yOffset, visible)
    }


    visibleEntities.sortBy {
      entity =>
        entity.get[Drawable].map(_.sprites.head._2.layer).getOrElse(0)
    }.foreach {
      entity =>
        val visible = playerVisiblePoints.contains(entity.position)

        //Do not draw dynamic entities that are not visible
        if (entity.entityType.isStatic || visible) {
          drawEntity(entity, canvas, spriteSheet, xOffset, yOffset, visible)
        }
    }

    drawUiElements(state.uiState, canvas, spriteSheet, xOffset, yOffset, player.position)
    drawHealthBar(canvas, player)
    drawInventory(canvas, player, state.uiState)
    drawKeys(canvas, player)
    drawExperienceBar(canvas, player)
  }

  private def updateMessageArea(state: GameController, messageArea: TextArea): Unit = {
    val messages = state.gameState.messages.take(6).mkString("\n")

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
    if (!visible) {
      canvas.graphicsContext2D.setGlobalAlpha(0.5)
    } else {
      canvas.graphicsContext2D.setGlobalAlpha(1)
    }

    entity.sprites.foreach {
      case (Point(x, y), entitySprite) =>
        canvas.graphicsContext2D.drawImage(
          spriteSheet,
          entitySprite.x * spriteScale,
          entitySprite.y * spriteScale,
          spriteScale,
          spriteScale,
          (x - xOffset) * spriteScale * uiScale,
          (y - yOffset) * spriteScale * uiScale,
          spriteScale * uiScale,
          spriteScale * uiScale
        )
    }
  }

  private def drawUiElements(uiState: UIState, canvas: Canvas, spriteSheet: Image, xOffset: Int, yOffset: Int, playerPosition: Point): Unit = {
    val optCursorPosition = uiState match {
      case ScrollSelect(cursor, _) =>
        Some(cursor)
      case list: UIState.ListSelect[Entity] if list.list.head.isInstanceOf[Entity] =>
        val position = list.list(list.index).position
        Some(position)
      case _ =>
        None
    }

    optCursorPosition.foreach {
      case Point(cursorX, cursorY) =>
        val offsetCursorX = (cursorX - xOffset) * spriteScale * uiScale
        val offsetCursorY = (cursorY - yOffset) * spriteScale * uiScale

        val line = LineOfSight.getBresenhamLine(playerPosition, Point(cursorX, cursorY))

        canvas.graphicsContext2D.save()
        line.dropRight(1).foreach {
          point =>
            val lineX = (point.x - xOffset) * spriteScale * uiScale
            val lineY = (point.y - yOffset) * spriteScale * uiScale

            canvas.graphicsContext2D.setGlobalAlpha(0.5)
            canvas.graphicsContext2D.setFill(Color.Red)
            canvas.graphicsContext2D.fillRect(lineX, lineY, spriteScale * uiScale, spriteScale * uiScale)
        }
        canvas.graphicsContext2D.restore()

        val cursorSprite = Sprites.cursorSprite

        canvas.graphicsContext2D.setGlobalAlpha(1)

        canvas.graphicsContext2D.drawImage(
          spriteSheet,
          cursorSprite.x * spriteScale,
          cursorSprite.y * spriteScale,
          spriteScale,
          spriteScale,
          offsetCursorX,
          offsetCursorY,
          spriteScale * uiScale,
          spriteScale * uiScale
        )
    }
  }

  private def drawHealthBar(canvas: Canvas, player: Entity): Unit = {
    canvas.graphicsContext2D.setGlobalAlpha(1)

    val barWidth = (spriteScale * uiScale) * 4 // Total width of the health bar
    val barHeight = (spriteScale * uiScale) / 2 // Height of the health bar
    val xOffset = (spriteScale * uiScale) / 2 // X position of the bar
    val yOffset = (spriteScale * uiScale) / 4 // Y position of the bar

    val currentHealth = player.currentHealth
    val maxHealth = player.maxHealth

    // Calculate the width of the filled portion of the health bar
    val filledWidth = (currentHealth.toDouble / maxHealth) * barWidth

    val gc = canvas.graphicsContext2D

    // Draw the background of the health bar (gray)
    gc.setFill(Color.Gray)
    gc.fillRect(xOffset, yOffset, barWidth, barHeight)

    // Draw the filled portion of the health bar (red)
    gc.setFill(Color.Red)
    gc.fillRect(xOffset, yOffset, filledWidth, barHeight)

    // Draw the border around the health bar
    gc.setStroke(Color.White) // Set border color
    gc.setLineWidth(uiScale) // Set border thickness
    gc.strokeRect(xOffset, yOffset, barWidth, barHeight)

    // Draw the health numbers to the right of the bar
    gc.setFill(Color.White)
    gc.setFont(pixelFont)
    gc.fillText(
      s"$currentHealth/$maxHealth",
      xOffset + barWidth + xOffset,
      yOffset + barHeight - uiScale)
  }

  private def drawInventory(canvas: Canvas, player: Entity, uiState: UIState): Unit = {
    val itemWidth = spriteScale * uiScale
    val itemHeight = spriteScale * uiScale

    canvas.graphicsContext2D.setGlobalAlpha(1)

    // Group items and count their occurrences
    val groupedItems = player.groupedUsableItems

    for (((item, quantity), index) <- groupedItems.zipWithIndex) {
      val itemX = (itemWidth / 2) + index * (itemWidth * 1.5)
      val itemY = spriteScale * uiScale * 1.5
      val sprite = Sprites.itemSprites(item)

      uiState match {
        case UIState.ListSelect(list, selectedIndex, _) if selectedIndex == index && list.head.isInstanceOf[Item] =>
          canvas.graphicsContext2D.setGlobalAlpha(0.5)
          canvas.graphicsContext2D.setFill(Color.Red)
          canvas.graphicsContext2D.fillRect(itemX, itemY, itemWidth, itemHeight)
        case _ =>
          canvas.graphicsContext2D.setGlobalAlpha(1)
      }

      // Draw the item sprite
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

      // Draw the quantity number
      canvas.graphicsContext2D.setFill(Color.White)
      canvas.graphicsContext2D.setFont(pixelFont)
      canvas.graphicsContext2D.fillText(
        quantity.toString,
        itemX + itemWidth + (spriteScale * uiScale / 8),
        itemY + itemHeight - (spriteScale * uiScale / 8)
      )
    }
  }


  def drawKeys(canvas: Canvas, player: Entity): Unit = {
    val itemWidth = spriteScale * uiScale
    val itemHeight = spriteScale * uiScale

    canvas.graphicsContext2D.setGlobalAlpha(1)
    val keys = player.items.filter(_.isInstanceOf[Item.Key])

    for (i <- keys.indices) {
      val itemX = (itemWidth / 2) + i * itemWidth
      val itemY = spriteScale * uiScale * 2.5
      val key = keys(i)
      val sprite = Sprites.itemSprites(key)

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

  def drawExperienceBar(canvas: Canvas, player: Entity): Unit = {
    import Experience.*

    val barWidth = (spriteScale * uiScale) * 4 // Total width of the experience bar
    val barHeight = (spriteScale * uiScale) / 4 // Height of the experience bar
    val xOffset = (spriteScale * uiScale) / 2 // X position of the bar
    val yOffset = (spriteScale * uiScale) // Y position of the bar

    val currentExp = player.experience
    val nextLevelExp = player.nextLevelExperience

    val drawableCurrentExperience = currentExp - player.previousLevelExperience
    val drawableNextLevelExperience = nextLevelExp - player.previousLevelExperience

    // Calculate the width of the filled portion of the experience bar
    val filledWidth: Double = if (player.canLevelUp) barWidth else (drawableCurrentExperience.toDouble / drawableNextLevelExperience) * barWidth

    val gc = canvas.graphicsContext2D

    // Draw the background of the experience bar (gray)
    gc.setFill(Color.Gray)
    gc.fillRect(xOffset, yOffset, barWidth, barHeight)

    // Draw the filled portion of the experience bar (blue)
    gc.setFill(Color.Yellow)
    gc.fillRect(xOffset, yOffset, filledWidth, barHeight)

    // Draw the border around the experience bar
    gc.setStroke(Color.White) // Set border color
    gc.setLineWidth(uiScale) // Set border thickness
    gc.strokeRect(xOffset, yOffset, barWidth, barHeight)

    if (player.canLevelUp)
      //Display text saying "Press 'L' to level up!"
      gc.fillText(
        "Press 'L' to level up!",
        xOffset + barWidth + xOffset,
        yOffset + barHeight
      )

  }
}