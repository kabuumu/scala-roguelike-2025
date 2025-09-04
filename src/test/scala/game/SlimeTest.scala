package game

import data.Sprites
import game.entity.*
import game.entity.Experience.experienceForLevel
import game.entity.Health.*
import game.entity.Movement.position
import game.system.DeathHandlerSystem
import game.system.event.GameSystemEvent.{AddExperienceEvent, SpawnEntityEvent, SlimeSplitEvent}
import game.{DeathDetails, GameState, Point}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class SlimeTest extends AnyFunSuite with Matchers {

  test("Slime should generate SlimeSplitEvent when killed") {
    // Create a player entity for the test
    val player = Entity(
      id = "Player ID",
      Movement(position = Point(5, 5)),
      EntityTypeComponent(EntityType.Player),
      Health(100),
      Drawable(Sprites.playerSprite),
      Hitbox()
    )
    
    // Create a test slime with the new death behavior
    val slime = Entity(
      id = "Test Slime",
      Movement(position = Point(10, 10)),
      EntityTypeComponent(EntityType.Enemy),
      Health(20),
      Initiative(15),
      Inventory(Nil, None),
      Drawable(Sprites.slimeSprite),
      Hitbox(),
      MarkedForDeath(DeathDetails(Entity(
        id = "Test Slime Victim",
        Movement(position = Point(10, 10)),
        EntityTypeComponent(EntityType.Enemy),
        Health(20),
        Drawable(Sprites.slimeSprite),
        Hitbox()
      ), Some("Player ID"))),
      DeathEvents(deathDetails => {
        val experienceEvent = deathDetails.killerId.map {
          killerId => AddExperienceEvent(killerId, experienceForLevel(2) / 4)
        }.toSeq
        // Use SlimeSplitEvent to handle splitting with empty position checking
        val splitEvent = SlimeSplitEvent(deathDetails.victim.position, deathDetails.killerId)
        experienceEvent :+ splitEvent
      })
    )

    val gameState = GameState(
      playerEntityId = "Player ID",
      entities = Vector(player, slime),
      dungeon = map.MapGenerator.generateDungeon(10, 0, 0)
    )

    // Process death through DeathHandlerSystem
    val (updatedState, events) = DeathHandlerSystem.update(gameState, Seq.empty)

    // Verify slime was removed
    updatedState.entities.find(_.id == "Test Slime") shouldBe None

    // Verify events were generated (experience + split event)
    events should have size 2
    events.count(_.isInstanceOf[AddExperienceEvent]) shouldBe 1
    events.count(_.isInstanceOf[SlimeSplitEvent]) shouldBe 1

    // Verify experience event
    val experienceEvent = events.find(_.isInstanceOf[AddExperienceEvent]).get.asInstanceOf[AddExperienceEvent]
    experienceEvent.entityId shouldBe "Player ID"
    experienceEvent.experience shouldBe experienceForLevel(2) / 4

    // Verify split event contains correct position and killer
    val splitEvent = events.find(_.isInstanceOf[SlimeSplitEvent]).get.asInstanceOf[SlimeSplitEvent]
    splitEvent.slimePosition shouldBe Point(10, 10)
    splitEvent.killerId shouldBe Some("Player ID")
  }

  test("SlimeSplitSystem should spawn slimelets only on empty tiles") {
    // Create a player entity at position (11, 10) to block one adjacent position
    val player = Entity(
      id = "Player ID",
      Movement(position = Point(11, 10)), // This will block the right position
      EntityTypeComponent(EntityType.Player),
      Health(100),
      Drawable(Sprites.playerSprite),
      Hitbox()
    )
    
    // Create another entity to block another position
    val blockingEntity = Entity(
      id = "Blocking Entity",
      Movement(position = Point(9, 10)), // This will block the left position
      EntityTypeComponent(EntityType.Enemy),
      Health(100),
      Drawable(Sprites.ratSprite),
      Hitbox()
    )

    val gameState = GameState(
      playerEntityId = "Player ID",
      entities = Vector(player, blockingEntity),
      dungeon = map.MapGenerator.generateDungeon(10, 0, 0)
    )

    // Create a SlimeSplitEvent for position (10, 10)
    val splitEvent = SlimeSplitEvent(Point(10, 10), Some("Player ID"))

    // Process through SlimeSplitSystem directly
    val (finalState, finalEvents) = game.system.SlimeSplitSystem.update(gameState, Seq(splitEvent))

    // Verify only spawn events for unblocked positions were created
    val spawnEvents = finalEvents.filter(_.isInstanceOf[SpawnEntityEvent]).map(_.asInstanceOf[SpawnEntityEvent])
    
    // Should have spawned at most 2 slimelets, and they should not be at blocked positions
    spawnEvents.size `should` be <= 2
    spawnEvents.foreach { event =>
      val slimeletPosition = event.newEntity.get[Movement].get.position
      // Should not spawn on player position or blocking entity position
      slimeletPosition `should` not be Point(11, 10)
      slimeletPosition `should` not be Point(9, 10)
      // Should be adjacent to slime position
      val distance = math.abs(slimeletPosition.x - 10) + math.abs(slimeletPosition.y - 10)
      distance shouldBe 1
    }
  }

  test("Slimelets should not spawn more slimelets when killed") {
    // Create a player entity for the test
    val player = Entity(
      id = "Player ID",
      Movement(position = Point(5, 5)),
      EntityTypeComponent(EntityType.Player),
      Health(100),
      Drawable(Sprites.playerSprite),
      Hitbox()
    )
    
    // Create a test slimelet with basic death behavior (no splitting)
    val slimelet = Entity(
      id = "Test Slimelet",
      Movement(position = Point(10, 10)),
      EntityTypeComponent(EntityType.Enemy),
      Health(10),
      Initiative(8),
      Drawable(Sprites.slimeletSprite),
      Hitbox(),
      MarkedForDeath(DeathDetails(Entity(
        id = "Test Slimelet Victim",
        Movement(position = Point(10, 10)),
        EntityTypeComponent(EntityType.Enemy),
        Health(10),
        Drawable(Sprites.slimeletSprite),
        Hitbox()
      ), Some("Player ID"))),
      DeathEvents(deathDetails => deathDetails.killerId.map {
        killerId => AddExperienceEvent(killerId, experienceForLevel(1) / 4)
      }.toSeq)
    )

    val gameState = GameState(
      playerEntityId = "Player ID",
      entities = Vector(player, slimelet),
      dungeon = map.MapGenerator.generateDungeon(10, 0, 0)
    )

    // Process death through DeathHandlerSystem
    val (updatedState, events) = DeathHandlerSystem.update(gameState, Seq.empty)

    // Verify slimelet was removed
    updatedState.entities.find(_.id == "Test Slimelet") shouldBe None

    // Verify only experience event was generated (no split events)
    events should have size 1
    events.count(_.isInstanceOf[AddExperienceEvent]) shouldBe 1
    events.count(_.isInstanceOf[SlimeSplitEvent]) shouldBe 0
  }

  test("StartingState should create slimes with correct stats") {
    val startingState = StartingState.startingGameState
    
    // Find slimes in the starting state
    val slimes = startingState.entities.filter(_.id.startsWith("Slime"))
    
    // Verify slimes exist
    slimes should not be empty
    
    // Verify slime stats
    slimes.foreach { slime =>
      slime.maxHealth shouldBe 20
      slime.currentHealth shouldBe 20
      slime.get[Initiative].map(_.currentInitiative) shouldBe Some(15)
      slime.get[Drawable].isDefined shouldBe true
      slime.get[EntityTypeComponent].map(_.entityType) shouldBe Some(EntityType.Enemy)
    }
  }
}