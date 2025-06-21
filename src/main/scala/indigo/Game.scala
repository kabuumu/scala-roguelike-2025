package indigo

import Batch.toBatch
import data.Sprites
import game.entity.Entity
import game.entity.Movement.position
import game.{LineOfSight, StartingState}
import indigo.*
import indigo.SpriteExtension.*
import ui.UIConfig.*
import ui.{GameController, UIConfig, UIState}

import scala.scalajs.js.annotation.JSExportTopLevel

@JSExportTopLevel("IndigoGame")
object Game extends IndigoSandbox[Unit, GameController] {

  override def config: GameConfig = GameConfig.default.withViewport(240, 480)

  override def assets: Set[AssetType] = Set(
    AssetType.Image(AssetName("sprites"), AssetPath("assets/sprites/sprites.png"))
  )

  override def fonts: Set[FontInfo] = Set.empty

  override def animations: Set[Animation] = Set.empty

  override def shaders: Set[ShaderProgram] = Set.empty

  override def setup(assetCollection: AssetCollection, dice: Dice): Outcome[Startup[Unit]] = Outcome(
    Startup.Success(())
  )

  override def initialModel(startupData: Unit): Outcome[GameController] = {
    Outcome(GameController(
      UIState.Move,
      StartingState.startingGameState
    ))
  }

  override def updateModel(context: Context[Unit], model: GameController): GlobalEvent => Outcome[GameController] =
    _ =>
      val optInput = context.frame.input.mapInputsOption(InputMappings.inputMapping)
      val time = context.frame.time.running.toMillis.toLong * 1000000L
      
      Outcome(model.update(optInput, time))

  override def present(context: Context[Unit], model: GameController): Outcome[SceneUpdateFragment] = {
    val spriteSheet: Graphic[Material.ImageEffects] = Graphic(0, 0, 784, 352, Material.ImageEffects(AssetName("sprites")))
    val game.Point(playerX, playerY) = model.gameState.playerEntity.position

    val tileSprites = model.gameState.dungeon.tiles.map {
      case (tilePosition, tileType) => spriteSheet.fromTile(tilePosition, tileType)
    }.toSeq

    val entitySprites = model.gameState.entities.flatMap(spriteSheet.fromEntity)

    val cursor = drawUIElements(spriteSheet, model)
    
    Outcome(
      SceneUpdateFragment(
        (tileSprites ++ entitySprites ++ cursor).toBatch
      ).withCamera(Camera.LookAt(Point(playerX * spriteScale, playerY * spriteScale), Zoom.x3))
    )
  }

  private def drawUIElements(spriteSheet: Graphic[Material.ImageEffects], model: GameController): Seq[SceneNode] = {
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
          .withCrop(
            cursorSprite.x * spriteScale,
            cursorSprite.y * spriteScale,
            spriteScale,
            spriteScale
          )
          .moveTo(cursorX * spriteScale, cursorY * spriteScale)

        line :+ sprite
    }
  }
}  