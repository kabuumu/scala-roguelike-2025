package indigoengine

import data.Sprites
import game.entity.Movement.position
import game.entity.{Entity, SightMemory}
import game.StartingState
import generated.{PixelFont, PixelFontSmall}
import indigo.*
import indigo.Batch.toBatch
import indigoengine.SpriteExtension.*
import indigoengine.shaders.CustomShader
import indigoengine.view.Elements.*
import ui.UIConfig.*
import ui.{GameController, UIConfig, UIState}
import util.LineOfSight
import indigoengine.view.ui.InventoryMenu
import indigoengine.view.ui.CharacterScreen

import scala.scalajs.js.annotation.JSExportTopLevel

@JSExportTopLevel("IndigoGame")
object Game extends IndigoSandbox[Unit, GameController] {

  override def config: GameConfig = GameConfig.default
    .withViewport(xPixels, yPixels)
    .withMagnification(uiScale)
    .noResize

  override def assets: Set[AssetType] = Set(
    AssetType.Image(
      AssetName("sprites"),
      AssetPath("assets/sprites/sprites.png")
    ),
    AssetType.Image(
      AssetName("portraits"),
      AssetPath("assets/sprites/portraits.png")
    )
  ) ++ generated.Assets.assets.generated.assetSet

  override def fonts: Set[FontInfo] = Set(
    PixelFont.fontInfo,
    PixelFontSmall.fontInfo
  )

  override def animations: Set[Animation] = Set.empty

  override def shaders: Set[ShaderProgram] = Set(
    CustomShader.shader
  )

  override def setup(
      assetCollection: AssetCollection,
      dice: Dice
  ): Outcome[Startup[Unit]] = Outcome(
    Startup.Success(())
  )

  override def initialModel(startupData: Unit): Outcome[GameController] = {
    // Create a minimal dummy game state for the main menu (it won't be used until New Game is selected)
    val dummyDungeon = map.Dungeon(
      roomGrid = Set(game.Point(0, 0)),
      roomConnections = Set.empty,
      blockedRooms = Set.empty,
      startPoint = game.Point(0, 0),
      endpoint = None,
      items = Set.empty,
      testMode = true,
      seed = 0L
    )

    // Create a dummy world map with the dummy dungeon
    val dummyWorldMap = map.WorldMap(
      tiles = dummyDungeon.tiles,
      dungeons = Seq(dummyDungeon),
      paths = Set.empty,
      bridges = Set.empty,
      bounds = map.MapBounds(-1, 1, -1, 1)
    )

    val dummyPlayer = game.entity.Entity(
      id = "dummy",
      game.entity.Movement(position = game.Point(0, 0)),
      game.entity.EntityTypeComponent(game.entity.EntityType.Player),
      game.entity.Health(100),
      game.entity.Initiative(10),
      game.entity.Inventory(Nil),
      game.entity.Drawable(data.Sprites.playerSprite),
      game.entity.Hitbox(),
      game.entity.DeathEvents()
    )

    val dummyGameState = game.GameState(
      playerEntityId = "dummy",
      entities = Vector(dummyPlayer),
      worldMap = dummyWorldMap
    )

    Outcome(
      GameController(
        UIState.MainMenu(),
        dummyGameState
      )
    )
  }

  override def updateModel(
      context: Context[Unit],
      model: GameController
  ): GlobalEvent => Outcome[GameController] =
    _ =>
      val optInput =
        context.frame.input.mapInputsOption(InputMappings.inputMapping)
      val time = context.frame.time.running.toMillis.toLong * 1000000L

      try {
        Outcome(model.update(optInput, time))
      } catch {
        case e: Exception =>
          println(s"Error during model update: ${e.getMessage}")
          e.printStackTrace()
          Outcome(model)
      }

  override def present(
      context: Context[Unit],
      model: GameController
  ): Outcome[SceneUpdateFragment] = {
    model.uiState match {
      case _: UIState.MainMenu =>
        // Render main menu screen
        Outcome(
          SceneUpdateFragment(
            Layer.Content(mainMenu(model))
          )
        )
      case gameOver: UIState.GameOver =>
        // Render game over screen
        Outcome(
          SceneUpdateFragment(
            Layer.Content(gameOverScreen(model, gameOver.player))
          )
        )
      case UIState.WorldMap =>
        Outcome(
          worldMapView(model)
        )
      case _: UIState.Inventory | _: UIState.InventoryActionState =>
        Outcome(
          presentGame(context, model) |+| SceneUpdateFragment(
            Layer.Content(
              InventoryMenu.render(
                model,
                Graphic(0, 0, 784, 352, Material.Bitmap(AssetName("sprites")))
              )
            )
          )
        )
      case UIState.Character =>
        Outcome(
          presentGame(context, model) |+| SceneUpdateFragment(
            Layer.Content(
              CharacterScreen.render(
                model,
                Graphic(0, 0, 784, 352, Material.Bitmap(AssetName("sprites")))
              )
            )
          )
        )

      case _ =>
        Outcome(presentGame(context, model))
    }
  }

  def presentGame(
      context: Context[Unit],
      model: GameController
  ): SceneUpdateFragment = {
    // Render normal game
    val spriteSheet =
      Graphic(0, 0, 784, 352, Material.Bitmap(AssetName("sprites")))
    val player = model.gameState.playerEntity
    val game.Point(playerX, playerY) = player.position
    val visiblePoints = model.gameState.getVisiblePointsFor(player)
    val sightMemory = player.get[SightMemory].toSet.flatMap(_.seenPoints)

    // Combine filtering and mapping for tileSprites
    // Optimize: Only iterate over visible viewport instead of entire world map
    val rangeX =
      (UIConfig.xTiles / 2) + 2 // +2 buffer to ensure no edges visible
    val rangeY = (UIConfig.yTiles / 2) + 2

    val minX = playerX - rangeX
    val maxX = playerX + rangeX
    val minY = playerY - rangeY
    val maxY = playerY + rangeY

    val tileSprites = (for {
      x <- minX to maxX
      y <- minY to maxY
    } yield {
      val tilePosition = game.Point(x, y)
      model.gameState.worldMap.getTile(tilePosition).map { tileType =>
        if (
          sightMemory.contains(
            tilePosition
          ) || UIConfig.ignoreLineOfSight
        ) {
          val tileSprite = spriteSheet.fromTile(tilePosition, tileType)
          if (visiblePoints.contains(tilePosition)) Some(tileSprite)
          else
            Some(
              tileSprite
                .asInstanceOf[Graphic[Material.Bitmap]]
                .modifyMaterial(_.toImageEffects.withTint(RGBA.SlateGray))
            )
        } else {
          None
        }
      }
    }).flatten.flatten.toSeq

    // Filter and map entities in one pass
    val entitySprites: Batch[SceneNode] = model.gameState.entities
      .filter(e => visiblePoints.contains(e.position))
      .toBatch
      .flatMap(entity =>
        spriteSheet.fromEntity(entity) ++ enemyHealthBar(entity)
      )

    val cursor = drawUIElements(spriteSheet, model)

    SceneUpdateFragment(
      Layer
        .Content((tileSprites ++ cursor).toBatch ++ entitySprites)
        .withCamera(
          Camera
            .LookAt(Point(playerX * spriteScale, playerY * spriteScale))
        ),
      Layer.Content(
        healthBar(model)
          ++ experienceBar(model)
          ++ coins(model, spriteSheet)
          ++ perkSelection(model)
          ++ keys(model, spriteSheet)
          ++ equipmentPaperdoll(model, spriteSheet)
          ++ tradeItemDisplay(model, spriteSheet)
          ++ conversationWindow(model, spriteSheet)
          ++ messageWindow(model)
          ++ versionInfo(model)
          ++ renderDebugMenu(model)
          ++ debugItemSelection(model, spriteSheet)
          ++ debugPerkSelection(model)
      )
    )
  }

  private def drawUIElements(
      spriteSheet: Graphic[?],
      model: GameController
  ): Seq[SceneNode] = {
    // Don't show targeting for trade-related lists (buying/selling)
    val optCursorTargetInfo = model.uiState match {
      case scrollSelect: UIState.ScrollSelectState =>
        Some((scrollSelect.cursor, None)) // Position only, no entity

      // Skip targeting for buying/selling - these are inventory/shop UIs
      case _: UIState.BuyItemSelect | _: UIState.SellItemSelect =>
        None

      // Handle action target selection (for attacks, equipping, trading, etc.)
      case actionTargetSelect: UIState.ActionTargetSelect =>
        if (actionTargetSelect.list.nonEmpty) {
          val actionTarget = actionTargetSelect.currentItem
          Some((actionTarget.entity.position, Some(actionTarget.entity)))
        } else {
          None
        }

      // Handle enemy targeting (for ranged attacks, spells, etc.)
      case enemyTargetSelect: UIState.EnemyTargetSelect =>
        if (enemyTargetSelect.list.nonEmpty) {
          val targetEntity = enemyTargetSelect.currentItem
          Some((targetEntity.position, Some(targetEntity)))
        } else {
          None
        }

      case _ =>
        None
    }

    val playerPosition = model.gameState.playerEntity.position

    optCursorTargetInfo.toSeq.flatMap {
      case (game.Point(cursorX, cursorY), optTargetEntity) =>
        val line = LineOfSight
          .getBresenhamLine(playerPosition, game.Point(cursorX, cursorY))
          .dropRight(1)
          .map { point =>
            Shape.Box(
              Rectangle(
                Point(point.x * spriteScale, point.y * spriteScale),
                Size(spriteScale)
              ),
              Fill.Color(RGBA.Red.withAlpha(0.5f))
            )
          }

        // Get cursor positions based on entity hitbox or single position
        val cursorPositions = optTargetEntity match {
          case Some(entity) =>
            // For entities with hitboxes, highlight all hitbox points
            import game.entity.Hitbox.*
            entity.get[game.entity.Hitbox] match {
              case Some(hitbox) =>
                hitbox.points.map(hitboxPoint =>
                  game.Point(cursorX + hitboxPoint.x, cursorY + hitboxPoint.y)
                )
              case None =>
                Set(game.Point(cursorX, cursorY))
            }
          case None =>
            // For scroll select or non-entity targets, just use the position
            Set(game.Point(cursorX, cursorY))
        }

        val cursorSprite = Sprites.cursorSprite

        // Draw cursor sprite at each position
        val cursorSprites = cursorPositions.map { pos =>
          spriteSheet
            .fromSprite(cursorSprite)
            .moveTo(pos.x * spriteScale, pos.y * spriteScale)
        }

        // Draw red highlight boxes for multi-tile entities
        val redHighlights = optTargetEntity match {
          case Some(entity) =>
            import game.entity.Hitbox.*
            entity.get[game.entity.Hitbox] match {
              case Some(hitbox) if hitbox.points.size > 1 =>
                // Multi-tile entity: draw red box for each hitbox point
                hitbox.points.map { hitboxPoint =>
                  val highlightPos =
                    game.Point(cursorX + hitboxPoint.x, cursorY + hitboxPoint.y)
                  Shape.Box(
                    Rectangle(
                      Point(
                        highlightPos.x * spriteScale,
                        highlightPos.y * spriteScale
                      ),
                      Size(spriteScale)
                    ),
                    Fill.Color(RGBA.Red.withAlpha(0.5f))
                  )
                }
              case _ =>
                Set.empty
            }
          case None =>
            Set.empty
        }

        line ++ cursorSprites ++ redHighlights
    }
  }
}
