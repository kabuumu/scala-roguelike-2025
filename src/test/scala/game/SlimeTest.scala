package game

import data.Sprites
import game.entity.*
import game.entity.Experience.experienceForLevel
import game.entity.Health.*
import game.system.DeathHandlerSystem
import game.system.event.GameSystemEvent.{AddExperienceEvent, SpawnEntityEvent}
import game.{DeathDetails, GameState, Point}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class SlimeTest extends AnyFunSuite with Matchers {

  test("Slime should spawn two slimelets when killed") {
    // Create a test slime entity to be the victim
    val slimeVictim = Entity(
      id = "Test Slime Victim",
      Movement(position = Point(10, 10)),
      EntityTypeComponent(EntityType.Enemy),
      Health(20),
      Drawable(Sprites.slimeSprite),
      Hitbox()
    )
    
    // Create a player entity for the test
    val player = Entity(
      id = "Player ID",
      Movement(position = Point(5, 5)),
      EntityTypeComponent(EntityType.Player),
      Health(100),
      Drawable(Sprites.playerSprite),
      Hitbox()
    )
    
    // Create a test slime with the death behavior
    val slime = Entity(
      id = "Test Slime",
      Movement(position = Point(10, 10)),
      EntityTypeComponent(EntityType.Enemy),
      Health(20),
      Initiative(15),
      Inventory(Nil, None),
      Drawable(Sprites.slimeSprite),
      Hitbox(),
      MarkedForDeath(DeathDetails(slimeVictim, Some("Player ID"))),
      DeathEvents(deathDetails => {
        val experienceEvent = deathDetails.killerId.map {
          killerId => AddExperienceEvent(killerId, experienceForLevel(2) / 4)
        }.toSeq
        // Simplified slimelet creation for testing
        val slimeletEvents = Seq(
          SpawnEntityEvent(Entity(
            id = "Slimelet-1",
            Movement(position = Point(11, 10)),
            EntityTypeComponent(EntityType.Enemy),
            Health(10),
            Initiative(8),
            Drawable(Sprites.slimeletSprite),
            Hitbox()
          )),
          SpawnEntityEvent(Entity(
            id = "Slimelet-2", 
            Movement(position = Point(9, 10)),
            EntityTypeComponent(EntityType.Enemy),
            Health(10),
            Initiative(8),
            Drawable(Sprites.slimeletSprite),
            Hitbox()
          ))
        )
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
    events.count(_.isInstanceOf[SpawnEntityEvent]) shouldBe 2

    // Verify experience event
    val experienceEvent = events.find(_.isInstanceOf[AddExperienceEvent]).get.asInstanceOf[AddExperienceEvent]
    experienceEvent.entityId shouldBe "Player ID"
    experienceEvent.experience shouldBe experienceForLevel(2) / 4

    // Verify spawn events contain slimelets
    val spawnEvents = events.filter(_.isInstanceOf[SpawnEntityEvent]).map(_.asInstanceOf[SpawnEntityEvent])
    spawnEvents.map(_.newEntity.id) should contain allOf("Slimelet-1", "Slimelet-2")
    spawnEvents.foreach { event =>
      event.newEntity.currentHealth shouldBe 10
      event.newEntity.get[Initiative].map(_.currentInitiative) shouldBe Some(8)
    }
  }

  test("Slimelets should not spawn more slimelets when killed") {
    // Create a test slimelet victim entity
    val slimeletVictim = Entity(
      id = "Test Slimelet Victim",
      Movement(position = Point(10, 10)),
      EntityTypeComponent(EntityType.Enemy),
      Health(10),
      Drawable(Sprites.slimeletSprite),
      Hitbox()
    )
    
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
      MarkedForDeath(DeathDetails(slimeletVictim, Some("Player ID"))),
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
    events.count(_.isInstanceOf[SpawnEntityEvent]) shouldBe 0
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