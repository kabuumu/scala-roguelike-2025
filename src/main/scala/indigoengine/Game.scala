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
    Outcome(GameController(
      UIState.Move,
      StartingState.startingGameState
    ).init())
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
        )
      )
    )
  }

  private def drawUIElements(spriteSheet: Graphic[?], model: GameController): Seq[SceneNode] = {
    val optCursorPosition = model.uiState match {
      case UIState.ScrollSelect(cursor, _) =>
        Some(cursor)
      case list: UIState.ListSelect[Entity] if list.list.head.isInstanceOf[Entity] =>
        val position = list.list(list.index).position
        Some(position)
      case _ =>
        None
    }

    val playerPosition = model.gameState.playerEntity.position

    optCursorPosition.toSeq.flatMap {
      case game.Point(cursorX, cursorY) =>
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


        val cursorSprite = Sprites.cursorSprite

        val sprite = spriteSheet
          .fromSprite(cursorSprite)
          .moveTo(cursorX * spriteScale, cursorY * spriteScale)

        line :+ sprite
    }
  }
}  