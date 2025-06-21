package indigo

import Batch.toBatch
import game.StartingState
import game.entity.Movement.position
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
      
      val time = context.frame.time.running.toMillis.toLong * GameController.ticksPerSecond
      
      println(context.frame.time.delta)
      
      Outcome(model.update(optInput, time))

  override def present(context: Context[Unit], model: GameController): Outcome[SceneUpdateFragment] = {
    val spriteSheet: Graphic[Material.ImageEffects] = Graphic(0, 0, 784, 352, Material.ImageEffects(AssetName("sprites")))
    val game.Point(playerX, playerY) = model.gameState.playerEntity.position
    
    Outcome(
      SceneUpdateFragment(
        (model.gameState.dungeon.tiles.map {
          case (tilePosition, tileType) => spriteSheet.fromTile(tilePosition, tileType)
        }.toSeq ++ model.gameState.entities.flatMap(spriteSheet.fromEntity)).toBatch
      ).withCamera(Camera.LookAt(Point(playerX * spriteScale, playerY * spriteScale), Zoom(UIConfig.uiScale)))
    )
  }
}  