package game.system

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import game.entity.*
import game.GameState
import game.Point
import game.Direction
import game.system.event.GameSystemEvent
import ui.InputAction
import map.WorldMap
import map.MapBounds

class EnemyAISystemTest extends AnyFunSpec with Matchers {

  describe("EnemyAISystem - Animal Behavior") {

    // Helper to create a game state
    def createGameState(entities: Seq[Entity]): GameState = {
      val worldMap = WorldMap(
        tiles = Map.empty,
        dungeons = Seq.empty,
        paths = Set.empty,
        bridges = Set.empty,
        bounds = MapBounds(0, 100, 0, 100)
      )

      GameState(
        playerEntityId = "player",
        entities = entities,
        worldMap = worldMap
      )
    }

    it("should wander (random move or wait) when no threats are nearby") {
      val animal = Entity(
        id = "duck",
        Movement(position = Point(10, 10)),
        EntityTypeComponent(EntityType.Animal),
        Initiative(10, 0), // Ready to act
        Hitbox(),
        Active()
      )

      val player = Entity(
        id = "player",
        Movement(position = Point(100, 100)), // Far away
        EntityTypeComponent(EntityType.Player),
        SightMemory(Set.empty), // Player sees nothing
        Hitbox(),
        Active()
      )

      val gameState = createGameState(Seq(animal, player))

      val (newState, events) = EnemyAISystem.update(gameState, Seq.empty)

      val moveEvents = events.collect {
        case GameSystemEvent.InputEvent("duck", InputAction.Move(_)) => true
        case GameSystemEvent.InputEvent("duck", InputAction.Wait)    => true
      }

      moveEvents should not be empty
    }

    it("should flee from player when visible") {
      val animalPos = Point(10, 10)
      val playerPos = Point(10, 9) // Player is North of Duck

      val animal = Entity(
        id = "duck",
        Movement(position = Point(10, 10)),
        EntityTypeComponent(EntityType.Animal),
        Initiative(10, 0), // Ready to act
        Hitbox(),
        Active()
      )

      val player = Entity(
        id = "player",
        Movement(position = playerPos),
        EntityTypeComponent(EntityType.Player),
        SightMemory(Set(animalPos)), // Player sees animal
        Hitbox(),
        Active()
      )

      val gameState = createGameState(Seq(animal, player))

      val (newState, events) = EnemyAISystem.update(gameState, Seq.empty)

      val moveEvent = events.collectFirst {
        case GameSystemEvent.InputEvent("duck", InputAction.Move(dir)) => dir
      }

      // Duck is at (10, 10), Player at (10, 9). Duck should move South (Down).
      moveEvent shouldBe Some(Direction.Down)
    }

    it("should flee from nearby enemies") {
      val animalPos = Point(10, 10)
      val enemyPos = Point(9, 10) // Enemy is West of Duck

      val animal = Entity(
        id = "duck",
        Movement(position = Point(10, 10)),
        EntityTypeComponent(EntityType.Animal),
        Initiative(10, 0), // Ready to act
        Hitbox(),
        Active()
      )

      val player = Entity(
        id = "player",
        Movement(position = Point(100, 100)), // Far away
        EntityTypeComponent(EntityType.Player),
        SightMemory(
          Set(animalPos)
        ),
        Hitbox(),
        Active()
      )

      val enemy = Entity(
        id = "wolf",
        Movement(position = enemyPos),
        EntityTypeComponent(EntityType.Enemy),
        Hitbox(),
        Active()
      )

      val gameState = createGameState(Seq(animal, player, enemy))

      val (newState, events) = EnemyAISystem.update(gameState, Seq.empty)

      val moveEvent = events.collectFirst {
        case GameSystemEvent.InputEvent("duck", InputAction.Move(dir)) => dir
      }

      // Duck at (10, 10), Enemy at (9, 10). Duck should move East (Right).
      moveEvent shouldBe Some(Direction.Right)
    }
  }
}
