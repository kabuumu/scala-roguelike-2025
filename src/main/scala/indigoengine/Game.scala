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

import scala.scalajs.js.annotation.JSExportTopLevel

@JSExportTopLevel("IndigoGame")
object Game extends IndigoSandbox[Unit, GameController] {

  override def config: GameConfig = GameConfig.default
    .withViewport(xPixels, yPixels)
    .withMagnification(uiScale)
    .noResize

  override def assets: Set[AssetType] = Set(
    AssetType.Image(AssetName("sprites"), AssetPath("assets/sprites/sprites.png"))
  ) ++ generated.Assets.assets.generated.assetSet

  override def fonts: Set[FontInfo] = Set(
    PixelFont.fontInfo,
    PixelFontSmall.fontInfo
  )

  override def animations: Set[Animation] = Set.empty

  override def shaders: Set[ShaderProgram] = Set(
    CustomShader.shader,
  )

  override def setup(assetCollection: AssetCollection, dice: Dice): Outcome[Startup[Unit]] = Outcome(
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
      dungeon = dummyDungeon
    )
    
    Outcome(GameController(
      UIState.MainMenu(),
      dummyGameState
    ))
  }

  override def updateModel(context: Context[Unit], model: GameController): GlobalEvent => Outcome[GameController] =
    _ =>
      val optInput = context.frame.input.mapInputsOption(InputMappings.inputMapping)
      val time = context.frame.time.running.toMillis.toLong * 1000000L

      try {
        Outcome(model.update(optInput, time))
      } catch {
        case e: Exception =>
          println(s"Error during model update: ${e.getMessage}")
          e.printStackTrace()
          Outcome(model)
      }

  override def present(context: Context[Unit], model: GameController): Outcome[SceneUpdateFragment] = {
    model.uiState match {
      case _: UIState.MainMenu =>
        // Render main menu screen
        Outcome(
          SceneUpdateFragment(
            Layer.Content(mainMenu(model))
          )
        )
      case _ =>
        // Render normal game
        val spriteSheet = Graphic(0, 0, 784, 352, Material.Bitmap(AssetName("sprites")))
        val player = model.gameState.playerEntity
        val game.Point(playerX, playerY) = player.position
        val visiblePoints = model.gameState.getVisiblePointsFor(player)
        val sightMemory = player.get[SightMemory].toSet.flatMap(_.seenPoints)

        // Combine filtering and mapping for tileSprites
        val tileSprites = model.gameState.dungeon.tiles.iterator.collect {
          case (tilePosition, tileType) if sightMemory.contains(tilePosition) =>
            val tileSprite = spriteSheet.fromTile(tilePosition, tileType)
            if (visiblePoints.contains(tilePosition)) tileSprite
            else tileSprite.asInstanceOf[Graphic[Material.Bitmap]]
              .modifyMaterial(_.toImageEffects.withTint(RGBA.SlateGray))
        }.toSeq

        // Filter and map entities in one pass
        val entitySprites: Batch[SceneNode] = model.gameState.entities
          .filter(e => visiblePoints.contains(e.position))
          .toBatch
          .flatMap(entity =>
            spriteSheet.fromEntity(entity) ++ enemyHealthBar(entity)
          )
    
        val cursor = drawUIElements(spriteSheet, model)

        Outcome(
          SceneUpdateFragment(
            Layer.Content((tileSprites ++ cursor).toBatch ++ entitySprites)
              .withCamera(Camera.LookAt(Point(playerX * spriteScale, playerY * spriteScale))),
            Layer.Content(
              healthBar(model)
                ++ experienceBar(model)
                ++ usableItems(model, spriteSheet)
                ++ perkSelection(model)
                ++ keys(model, spriteSheet)
                ++ equipmentPaperdoll(model, spriteSheet)
                ++ messageWindow(model)
                ++ versionInfo(model)
            )
          )
        )
    }
  }

  private def drawUIElements(spriteSheet: Graphic[?], model: GameController): Seq[SceneNode] = {
    val optCursorTargetInfo = model.uiState match {
      case UIState.ScrollSelect(cursor, _) =>
        Some((cursor, None)) // Position only, no entity
      case list: UIState.ListSelect[_] if list.list.nonEmpty =>
        val selectedItem = list.list(list.index)
        selectedItem match {
          // Handle ActionTarget from unified action system
          case actionTarget: ui.GameController.ActionTarget =>
            Some((actionTarget.entity.position, Some(actionTarget.entity)))
          // Handle direct Entity objects (for attack lists, etc.)
          case entity: game.entity.Entity =>
            // Only show cursor for entities that have meaningful map positions (enemies, not inventory items)
            import game.entity.UsableItem
            if (!entity.has[UsableItem]) {
              Some((entity.position, Some(entity)))
            } else {
              None
            }
          case _ =>
            None
        }
      case _ =>
        None
    }

    val playerPosition = model.gameState.playerEntity.position

    optCursorTargetInfo.toSeq.flatMap {
      case (game.Point(cursorX, cursorY), optTargetEntity) =>
        val line = LineOfSight.getBresenhamLine(playerPosition, game.Point(cursorX, cursorY)).dropRight(1)
          .map {
            point =>
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

        line ++ cursorSprites
    }
  }
}
