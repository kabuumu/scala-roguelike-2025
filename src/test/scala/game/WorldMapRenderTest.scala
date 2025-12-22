package game

import org.scalatest.funsuite.AnyFunSuite
import indigoengine.view.ui.WorldMapUI
import indigo.SceneUpdateFragment
import map.TileType

class WorldMapRenderTest extends AnyFunSuite {

  test("WorldMapUI.worldMapView should render without errors") {
    val worldTiles = Map(
      game.Point(0, 0) -> TileType.Floor,
      game.Point(1, 0) -> TileType.Wall,
      game.Point(0, 1) -> TileType.Water
    )
    val canvasWidth = 800
    val canvasHeight = 600
    val playerPos = game.Point(0, 0)
    val seenPoints = Set(game.Point(0, 0), game.Point(1, 0)) // (0,1) is unseen

    // Should not throw exception
    val fragment = WorldMapUI.worldMapView(
      worldTiles,
      canvasWidth,
      canvasHeight,
      playerPos,
      seenPoints
    )

    // Basic assertions could be checked here if we could inspect SceneUpdateFragment easily
    // For now, satisfied if it runs.
    assert(fragment != null)
  }

  test("WorldMapUI renders empty if no seen points") {
    val worldTiles = Map(
      game.Point(0, 0) -> TileType.Floor
    )
    val fragment = WorldMapUI.worldMapView(
      worldTiles,
      800,
      600,
      game.Point(0, 0),
      Set.empty
    )
    assert(fragment == SceneUpdateFragment.empty)
  }
}
