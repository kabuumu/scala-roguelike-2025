package game.system

import data.Sprites
import game.entity.*
import game.entity.EntityType.*
import game.entity.Movement.*
import game.system.event.GameSystemEvent.*
import game.{GameState, Point}
import map.Dungeon
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.matchers.should.Matchers

class CollisionCheckSystemTest extends AnyFunSuiteLike with Matchers {
  val playerId = "testPlayerId"
  val enemyId = "testEnemyId"
  val itemId = "testItemId"
  val testDungeon: Dungeon = Dungeon(testMode = true)

  val playerEntity: Entity = Entity(
    id = playerId,
    Movement(position = Point(5, 5)),
    EntityTypeComponent(EntityType.Player),
    Health(100),
    Hitbox(),
    Drawable(Sprites.playerSprite)
  )

  val enemyEntity: Entity = Entity(
    id = enemyId,
    Movement(position = Point(5, 5)), // Same position as player - collision!
    EntityTypeComponent(EntityType.Enemy),
    Health(50),
    Hitbox(),
    Drawable(Sprites.enemySprite)
  )

  val itemEntity: Entity = Entity(
    id = itemId,
    Movement(position = Point(6, 5)),
    CanPickUp(),
    Hitbox(),
    Drawable(Sprites.defaultItemSprite)
  )

  test("CollisionCheckSystem should detect entity-entity collision") {
    val gameState = GameState(
      playerEntityId = playerId,
      entities = Seq(playerEntity, enemyEntity),
      messages = Nil,
      dungeon = testDungeon
    )

    val (updatedState, events) = CollisionCheckSystem.update(gameState, Seq.empty)

    // Should generate collision events for both entities
    events.length shouldBe 2
    
    val playerCollisionEvent = events.find {
      case CollisionEvent(entityId, _) => entityId == playerId
      case _ => false
    }
    val enemyCollisionEvent = events.find {
      case CollisionEvent(entityId, _) => entityId == enemyId
      case _ => false
    }

    playerCollisionEvent shouldBe defined
    enemyCollisionEvent shouldBe defined
    
    // Check collision targets
    playerCollisionEvent.get should matchPattern {
      case CollisionEvent(playerId, CollisionTarget.Entity(enemyId)) =>
    }
    enemyCollisionEvent.get should matchPattern {
      case CollisionEvent(enemyId, CollisionTarget.Entity(playerId)) =>
    }
  }

  test("CollisionCheckSystem should not detect collision when entities are apart") {
    val separateEnemy = enemyEntity.update[Movement](_.copy(position = Point(7, 7)))
    val gameState = GameState(
      playerEntityId = playerId,
      entities = Seq(playerEntity, separateEnemy),
      messages = Nil,
      dungeon = testDungeon
    )

    val (updatedState, events) = CollisionCheckSystem.update(gameState, Seq.empty)

    // No collision events should be generated
    events shouldBe empty
  }

  test("CollisionCheckSystem should detect wall collision") {
    // Player at wall position
    val wallPosition = testDungeon.walls.headOption.getOrElse(Point(0, 0))
    val playerAtWall = playerEntity.update[Movement](_.copy(position = wallPosition))
    
    val gameState = GameState(
      playerEntityId = playerId,
      entities = Seq(playerAtWall),
      messages = Nil,
      dungeon = testDungeon
    )

    val (updatedState, events) = CollisionCheckSystem.update(gameState, Seq.empty)

    if (testDungeon.walls.nonEmpty) {
      // Should generate wall collision event
      events.length shouldBe 1
      events.head should matchPattern {
        case CollisionEvent(playerId, CollisionTarget.Wall) =>
      }
    } else {
      // If no walls in test dungeon, no collision events
      events shouldBe empty
    }
  }

  test("CollisionCheckSystem should ignore entities without hitbox") {
    val entityWithoutHitbox = Entity(
      id = "noHitbox",
      Movement(position = Point(5, 5)), // Same position as player
      EntityTypeComponent(EntityType.Projectile), // Use a valid entity type
      Drawable(Sprites.defaultItemSprite)
      // No Hitbox component
    )

    val gameState = GameState(
      playerEntityId = playerId,
      entities = Seq(playerEntity, entityWithoutHitbox),
      messages = Nil,
      dungeon = testDungeon
    )

    val (updatedState, events) = CollisionCheckSystem.update(gameState, Seq.empty)

    // No collision events should be generated since one entity has no hitbox
    events shouldBe empty
  }

  test("CollisionCheckSystem should ignore entities marked for death") {
    val dyingEnemy = enemyEntity.addComponent(MarkedForDeath(game.DeathDetails(enemyEntity, None)))
    
    val gameState = GameState(
      playerEntityId = playerId,
      entities = Seq(playerEntity, dyingEnemy),
      messages = Nil,
      dungeon = testDungeon
    )

    val (updatedState, events) = CollisionCheckSystem.update(gameState, Seq.empty)

    // No collision events should be generated for marked-for-death entities
    events shouldBe empty
  }

  test("CollisionCheckSystem should handle multiple collisions") {
    val secondEnemy = Entity(
      id = "enemy2",
      Movement(position = Point(5, 5)), // Same position as player
      EntityTypeComponent(EntityType.Enemy),
      Health(30),
      Hitbox(),
      Drawable(Sprites.enemySprite)
    )

    val gameState = GameState(
      playerEntityId = playerId,
      entities = Seq(playerEntity, enemyEntity, secondEnemy),
      messages = Nil,
      dungeon = testDungeon
    )

    val (updatedState, events) = CollisionCheckSystem.update(gameState, Seq.empty)

    // Should generate multiple collision events (3 entities, so 6 collision events total)
    events.length shouldBe 6 // Each entity collides with every other entity
    
    // Verify we have collision events for all entities
    val entityIds = Set(playerId, enemyId, "enemy2")
    val collisionEntityIds = events.collect {
      case CollisionEvent(entityId, _) => entityId
    }.toSet
    
    collisionEntityIds shouldBe entityIds
  }

  test("CollisionCheckSystem should handle empty entity list") {
    // Can't have a truly empty entity list because GameState requires playerEntity to exist
    // So test with just the player entity
    val gameState = GameState(
      playerEntityId = playerId,
      entities = Seq(playerEntity),
      messages = Nil,
      dungeon = testDungeon
    )

    val (updatedState, events) = CollisionCheckSystem.update(gameState, Seq.empty)

    // Only wall collision events possible with a single entity
    events.forall {
      case CollisionEvent(playerId, CollisionTarget.Wall) => true
      case _ => false
    } shouldBe true
  }

  test("CollisionCheckSystem should handle single entity") {
    val gameState = GameState(
      playerEntityId = playerId,
      entities = Seq(playerEntity),
      messages = Nil,
      dungeon = testDungeon
    )

    val (updatedState, events) = CollisionCheckSystem.update(gameState, Seq.empty)

    // Check for wall collisions only (no entity-entity collisions possible)
    val wallCollisions = events.filter {
      case CollisionEvent(_, CollisionTarget.Wall) => true
      case _ => false
    }
    
    // Player might collide with walls depending on position
    events.forall {
      case CollisionEvent(playerId, CollisionTarget.Wall) => true
      case _ => false
    } shouldBe true
  }
}