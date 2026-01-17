package ui

import game.GameState
import game.GameMode
import game.Point
import game.entity.*
import game.entity.EntityType
import data.Enemies
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import map.WorldMap

class GameTargetingTest extends AnyFreeSpec with Matchers {

  "GameTargeting" - {
    "enemiesWithinRange" - {
      "should include Animals" in {
        // Setup simple game state
        val playerPos = Point(5, 5)
        val duckPos = Point(6, 5) // Adjacent

        val player = Entity(
          id = "player",
          Movement(playerPos),
          EntityTypeComponent(EntityType.Player),
          Hitbox(),
          SightMemory(seenPoints = Set(playerPos, duckPos)) // Can see duck
        )

        val duck = Enemies.duck("duck", duckPos).addComponent(Hitbox())

        // Mock map (all floors for visibility/movement)
        val worldMap = TestMapHelper.emptyWorld(
          10,
          10
        ) // Assuming empty map has floors? Actually need to check WorldMap.empty behaviour or mock it
        // Or just use minimal state since targeting logic relies on entity positions and visibility which we might need to mock or setup
        // GameTargeting.enemiesWithinRange uses `gameState.getVisiblePointsFor(player)`
        // Currently getVisiblePointsFor relies on LineOfSight.getVisiblePoints(entityPosition, blockingPoints, 10).
        // If map is empty, no blocking points.

        val entities = Vector(player, duck)
        val gameState = GameState(
          playerEntityId = player.id,
          entities = entities,
          worldMap = TestMapHelper.emptyWorld(
            20,
            20
          ), // Placeholder if exists or construct minimal
          dungeonFloor = 0,
          gameMode = GameMode.Adventure
        )

        val targets = GameTargeting.enemiesWithinRange(gameState, range = 5)

        targets should contain(duck)
      }
    }
  }
}

object TestMapHelper {
  // Helper to create empty world map if needed, or rely on existing mocks/factories
  def emptyWorld(width: Int, height: Int): WorldMap = {
    WorldMap(
      tiles = (for {
        x <- 0 until width
        y <- 0 until height
      } yield Point(x, y) -> map.TileType.Floor).toMap,
      dungeons = Seq.empty,
      paths = Set.empty,
      bridges = Set.empty,
      bounds = map.MapBounds(0, width / 10, 0, height / 10)
    )
  }
}
