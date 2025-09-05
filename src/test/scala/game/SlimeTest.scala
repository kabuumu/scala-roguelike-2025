package game

import data.Sprites
import game.entity.*
import game.entity.Experience.experienceForLevel
import game.entity.Health.*
import game.entity.Movement.position
import game.system.DeathHandlerSystem
import game.system.event.GameSystemEvent.{AddExperienceEvent, SpawnEntityWithCollisionCheckEvent}
import game.{DeathDetails, Direction, GameState, Point}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class SlimeTest extends AnyFunSuite with Matchers {

  test("Slime should generate SpawnEntityWithCollisionCheckEvent when killed") {
    // Create a player entity for the test
    val player = Entity(
      id = "Player ID",
      Movement(position = Point(5, 5)),
      EntityTypeComponent(EntityType.Player),
      Health(100),
      Drawable(Sprites.playerSprite),
      Hitbox()
    )
    
    // Create a test slime using the actual StartingState helper function
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
        // Create slimelet spawn events with collision checking
        val slimeletEvents = (0 until 2).map { index =>
          val slimeletId = s"Slimelet-${System.currentTimeMillis()}-$index"
          val adjacentPositions = Seq(
            // Cardinal directions
            deathDetails.victim.position + Point(0, -1),  // Up
            deathDetails.victim.position + Point(0, 1),   // Down
            deathDetails.victim.position + Point(-1, 0),  // Left
            deathDetails.victim.position + Point(1, 0),   // Right
            // Diagonal directions
            deathDetails.victim.position + Point(-1, -1), // UpLeft
            deathDetails.victim.position + Point(1, -1),  // UpRight
            deathDetails.victim.position + Point(-1, 1),  // DownLeft
            deathDetails.victim.position + Point(1, 1)    // DownRight
          )
          
          val slimeletTemplate = Entity(
            id = slimeletId,
            Movement(position = Point(0, 0)),
            EntityTypeComponent(EntityType.Enemy),
            Health(10),
            Initiative(8),
            Inventory(Nil, None),
            Drawable(Sprites.slimeletSprite),
            Hitbox(),
            DeathEvents(deathDetails => deathDetails.killerId.map {
              killerId => AddExperienceEvent(killerId, experienceForLevel(1) / 4)
            }.toSeq)
          )
          
          SpawnEntityWithCollisionCheckEvent(slimeletTemplate, adjacentPositions)
        }
        experienceEvent ++ slimeletEvents
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

    // Verify events were generated (experience + 2 spawn events)
    events should have size 3
    events.count(_.isInstanceOf[AddExperienceEvent]) shouldBe 1
    events.count(_.isInstanceOf[SpawnEntityWithCollisionCheckEvent]) shouldBe 2

    // Verify experience event
    val experienceEvent = events.find(_.isInstanceOf[AddExperienceEvent]).get.asInstanceOf[AddExperienceEvent]
    experienceEvent.entityId shouldBe "Player ID"
    experienceEvent.experience shouldBe experienceForLevel(2) / 4

    // Verify spawn events contain adjacent positions (including diagonals)
    val spawnEvents = events.filter(_.isInstanceOf[SpawnEntityWithCollisionCheckEvent]).map(_.asInstanceOf[SpawnEntityWithCollisionCheckEvent])
    spawnEvents.foreach { event =>
      // Check that preferred positions are adjacent to slime position (10, 10) - including diagonals
      event.preferredPositions.foreach { pos =>
        val xDistance = math.abs(pos.x - 10)
        val yDistance = math.abs(pos.y - 10)
        // Adjacent positions have max distance of 1 in both x and y (Chebyshev distance)
        xDistance should be <= 1
        yDistance should be <= 1
        // Must be at least 1 unit away (not the same position)
        (xDistance + yDistance) should be >= 1
      }
    }
  }

  test("SpawnEntitySystem should spawn slimelets only on empty tiles") {
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

    // Create a collision-checked spawn event for position (10, 10)
    val adjacentPositions = Seq(
      // Cardinal directions
      Point(10, 9), Point(10, 11), Point(9, 10), Point(11, 10), // Up, Down, Left, Right
      // Diagonal directions  
      Point(9, 9), Point(11, 9), Point(9, 11), Point(11, 11)   // UpLeft, UpRight, DownLeft, DownRight
    )
    
    val slimeletTemplate = Entity(
      id = "Test Slimelet",
      Movement(position = Point(0, 0)), // Will be set by system
      EntityTypeComponent(EntityType.Enemy),
      Health(10),
      Initiative(8),
      Inventory(Nil, None),
      Drawable(Sprites.slimeletSprite),
      Hitbox(),
      DeathEvents()
    )
    
    val spawnEvent = SpawnEntityWithCollisionCheckEvent(slimeletTemplate, adjacentPositions)

    // Process through SpawnEntitySystem
    val (finalState, finalEvents) = game.system.SpawnEntitySystem.update(gameState, Seq(spawnEvent))

    // Verify slimelet was spawned
    val spawnedSlimelets = finalState.entities.filter(_.id == "Test Slimelet")
    spawnedSlimelets should have size 1
    
    val slimeletPosition = spawnedSlimelets.head.get[Movement].get.position
    // Should not spawn on blocked positions
    slimeletPosition `should` not be Point(11, 10) // Player position
    slimeletPosition `should` not be Point(9, 10)  // Blocking entity position
    
    // Should be on one of the empty adjacent positions (including diagonals)
    slimeletPosition should (be(Point(10, 9)) or be(Point(10, 11)) or 
                           be(Point(9, 9)) or be(Point(11, 9)) or 
                           be(Point(9, 11)) or be(Point(11, 11)))
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

    // Verify only experience event was generated (no spawn events)
    events should have size 1
    events.count(_.isInstanceOf[AddExperienceEvent]) shouldBe 1
    events.count(_.isInstanceOf[SpawnEntityWithCollisionCheckEvent]) shouldBe 0
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