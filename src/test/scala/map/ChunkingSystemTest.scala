package map

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import game.{GameState, Point, GameMode}
import game.entity.{Entity, Active, Movement}
import game.system.CullingSystem
import map.{ChunkManager, WorldMap, WorldConfig, MapBounds}

class ChunkingSystemTest extends AnyFunSuite with Matchers {

  test("ChunkManager should load and unload chunks based on player position") {
    val seed = 12345L
    val config = WorldConfig(
      bounds = MapBounds(-100, 100, -100, 100),
      grassDensity = 0.3,
      treeDensity = 0.3,
      dirtDensity = 0.3,
      perimeterTrees = false
    )

    val initialWorldMap = WorldMap(
      tiles = Map.empty,
      dungeons = Seq.empty,
      villages = Seq.empty,
      paths = Set.empty,
      bridges = Set.empty,
      bounds = config.bounds,
      chunks = Map.empty,
      seed = seed,
      processedRegions = Set.empty
    )

    // Player at 0,0
    val playerPos = Point(0, 0)

    val mapStep1 =
      ChunkManager.updateChunks(playerPos, initialWorldMap, config, seed)

    // Check loaded chunks. Radius is 5. So -5 to 5 chunks in both dims.
    // Chunk size 16.
    val expectedChunksSide = 11 // -5 to 5 = 11 chunks
    // mapStep1.chunks.size should be 11*11 = 121
    mapStep1.chunks.size shouldBe 121

    // Move player FAR away. Chunk size 16. Move 20 chunks away.
    // x = 20 * 16 = 320
    val playerPosFar = Point(320, 0)

    val mapStep2 =
      ChunkManager.updateChunks(playerPosFar, mapStep1, config, seed)

    // Should have new chunks around 320,0
    // And UNLOADED chunks around 0,0

    val centerChunkX = 20 // 320 / 16
    mapStep2.chunks.keys.foreach { case (cx, cy) =>
      cx should be >= (centerChunkX - 7) // Loading radius 5 + buffer 2 = 7
      cx should be <= (centerChunkX + 7)
    }

    // Ensure 0,0 is NOT loaded (it's at chunk 0, center is 20, dist is 20 > 7)
    mapStep2.chunks.contains((0, 0)) shouldBe false
  }

  test("CullingSystem should mark entities active/inactive") {
    val player = Entity("player", Movement(Point(0, 0)))
    val closeEnemy = Entity("close", Movement(Point(5, 5)))
    val farEnemy = Entity("far", Movement(Point(100, 100))) // Dist > 25

    val entities = Seq(player, closeEnemy, farEnemy)

    val gameState = GameState(
      playerEntityId = player.id,
      entities = entities,
      worldMap = WorldMap(
        Map.empty,
        Nil,
        None,
        Nil,
        Set.empty,
        Set.empty,
        MapBounds(0, 0, 0, 0)
      )
    )

    // Run CullingSystem
    val (newState, _) = CullingSystem.update(gameState, Seq.empty)

    newState.getEntity(player.id).get.has[Active] shouldBe true
    newState.getEntity(closeEnemy.id).get.has[Active] shouldBe true
    newState.getEntity(farEnemy.id).get.has[Active] shouldBe false

    // Move player to far enemy
    val playerMovedState = newState.updateEntity(
      player.id,
      e => e.update[Movement](m => m.copy(position = Point(100, 100)))
    )

    val (newState2, _) = CullingSystem.update(playerMovedState, Seq.empty)

    newState2.getEntity(farEnemy.id).get.has[Active] shouldBe true
    newState2.getEntity(closeEnemy.id).get.has[Active] shouldBe false
  }
}
