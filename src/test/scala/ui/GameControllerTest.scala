package ui

import data.Sprites
import game.Direction.{Down, Up}
import game.Item.*
import game.entity.*
import game.entity.EntityType.*
import game.entity.Experience.*
import game.entity.Health.*
import game.entity.Inventory.*
import game.{GameState, Input, Point}
import map.Dungeon
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.matchers.should.Matchers
import ui.GameController.frameTime
import ui.UIState.{Move, ScrollSelect}
import game.system.event.GameSystemEvent.AddExperienceEvent


class GameControllerTest extends AnyFunSuiteLike with Matchers {
  val playerId = "testPlayerId"
  
  val testDungeon: Dungeon = Dungeon(testMode = true)
  
  // Helper function to create item entities
  def createItemEntity(item: Item, itemId: String): Entity = {
    val baseEntity = Entity(
      id = itemId,
      ItemType(item),
      CanPickUp(),
      Hitbox()
    )
    
    item match {
      case equippable: EquippableItem =>
        baseEntity.addComponent(Equippable.fromEquippableItem(equippable))
      case _ => baseEntity
    }
  }
  
  val playerEntity: Entity = Entity(
    id = playerId,
    Movement(position = Point(4, 4)),
    EntityTypeComponent(EntityType.Player),
    Health(10),
    Initiative(0),
    Inventory(Seq(), Some(Weapon(2, Melee)), Some(Weapon(1, Ranged(6)))),
    SightMemory(),
    Drawable(Sprites.playerSprite),
    Hitbox(),
    Experience(),
  )

  test("Player initiative should decrease when not 0") {
    val unreadyPlayerEntity = playerEntity.update[Initiative](_.copy(maxInitiative = 1, currentInitiative = 1))

    val gameState = GameState(playerEntityId = playerId, entities = Seq(unreadyPlayerEntity), messages = Nil, dungeon = testDungeon)
    val gameController = GameController(Move, gameState)

    val updatedGameState = gameController.update(None, Long.MaxValue)
    updatedGameState.gameState.playerEntity.get[Initiative] should contain(Initiative(1, 0))
  }

  test("Player should move when given a move action") {
    val gameState = GameState(playerEntityId = playerId, entities = Seq(playerEntity), messages = Nil, dungeon = testDungeon)
    val gameController = GameController(Move, gameState)

    val updatedGameState = gameController.update(Some(Input.Move(Up)), Long.MaxValue)
    updatedGameState.gameState.playerEntity.get[Movement] should contain(Movement(Point(4, 3)))
  }

  test("Player should heal when using a potion") {
    // Create a potion entity
    val potionEntity = createItemEntity(Potion, "test-potion-1")
    
    // Create wounded player with potion in inventory
    val woundedPlayer = playerEntity
      .damage(5, "")
      .update[Inventory](_.addItemEntityId(potionEntity.id))

    val gameState = GameState(
      playerEntityId = playerId, 
      entities = Seq(woundedPlayer, potionEntity), 
      messages = Nil, 
      dungeon = testDungeon
    )
    val gameController = GameController(Move, gameState)

    gameController.gameState.playerEntity.currentHealth shouldBe 5

    val updatedGameState =
      gameController
        .update(Some(Input.UseItem), frameTime) //To enter the use item state
        .update(Some(Input.UseItem), frameTime * 2) //To select the potion

    updatedGameState.gameState.playerEntity.currentHealth shouldBe 10
  }

  test("Player using a scroll fireball scroll should create a projectile") {
    // Create a scroll entity
    val scrollEntity = createItemEntity(Scroll, "test-scroll-1")
    
    // Create player with scroll in inventory
    val playerWithScroll = playerEntity.update[Inventory](_.addItemEntityId(scrollEntity.id))

    val gameState = GameState(
      playerEntityId = playerId, 
      entities = Seq(playerWithScroll, scrollEntity), 
      messages = Nil, 
      dungeon = testDungeon
    )
    val gameController = GameController(Move, gameState)

    val beforeSelectingFireball =
      gameController
        .update(Some(Input.UseItem), frameTime) //To enter the use item state
        .update(Some(Input.UseItem), frameTime * 2) //To select the scroll
        .update(Some(Input.Move(Up)), frameTime * 3) //To move the target cursor up
        .update(Some(Input.Move(Up)), frameTime * 4) //To move the target cursor up
        .update(Some(Input.Move(Up)), frameTime * 5) //To move the target cursor up

    beforeSelectingFireball.gameState.entities.count(_.entityType == EntityType.Projectile) shouldBe 0

    val afterSelectingFireball =
      beforeSelectingFireball
        .update(Some(Input.UseItem), frameTime * 6) //To select the target

    afterSelectingFireball.gameState.entities.count(_.entityType == EntityType.Projectile) shouldBe 1
  }

  test("Player firing an arrow at an enemy entity") {
    // Create bow and arrow entities
    val bowEntity = createItemEntity(Bow, "test-bow-1")
    val arrowEntity = createItemEntity(Arrow, "test-arrow-1")
    
    // Create player with bow and arrow in inventory
    val playerWithBowAndArrow = playerEntity
      .update[Inventory](_.addItemEntityId(bowEntity.id).addItemEntityId(arrowEntity.id))
      .update[Initiative](_.copy(maxInitiative = 10, currentInitiative = 0))

    val enemyEntity = Entity(
      id = "enemyId",
      Movement(position = Point(4, 7)),
      EntityTypeComponent(EntityType.Enemy),
      Health(10),
      Initiative(10),
      Inventory(Seq(), Some(Weapon(2, Melee)), Some(Weapon(1, Ranged(6)))),
      SightMemory(),
      Drawable(Sprites.enemySprite),
      Hitbox()
    )

    val gameState = GameState(
      playerEntityId = playerId, 
      entities = Seq(playerWithBowAndArrow, bowEntity, arrowEntity, enemyEntity), 
      messages = Nil, 
      dungeon = testDungeon
    )
    val gameController = GameController(Move, gameState)

    val beforeFiringArrow =
      gameController
        .update(Some(Input.UseItem), frameTime) //To enter the use item state
        .update(Some(Input.UseItem), frameTime * 2) //To select the bow

    beforeFiringArrow.gameState.entities.count(_.entityType == EntityType.Projectile) shouldBe 0

    val afterFiringArrow =
      beforeFiringArrow
        .update(Some(Input.UseItem), frameTime * 3) //To select the bow target

    afterFiringArrow.gameState.entities.count(_.entityType == EntityType.Projectile) shouldBe 1

    val afterCollision =
      afterFiringArrow
        .update(None, frameTime * 4)
        .update(None, frameTime * 5)
        .update(None, frameTime * 6)


//    afterCollision.gameState.entities.count(_.entityType == EntityType.Projectile) shouldBe 0
    afterCollision.gameState.entities.find(_.id == enemyEntity.id).get.currentHealth shouldBe 2
  }

  test("Player fires a fireball that hits multiple enemies, removing them and granting experience") {
    // Create a scroll entity
    val scrollEntity = createItemEntity(Scroll, "test-scroll-2")
    
    // Create player with scroll in inventory
    val playerWithScroll = playerEntity
      .update[Inventory](_.addItemEntityId(scrollEntity.id))
      .update[Initiative](_.copy(maxInitiative = 10, currentInitiative = 0))


    val enemy1 = Entity(
      id = "enemy1",
      Movement(position = Point(4, 7)),
      EntityTypeComponent(EntityType.Enemy),
      Health(2),
      Initiative(10),
      Inventory(),
      SightMemory(),
      Drawable(Sprites.enemySprite),
      Hitbox(),
      DeathEvents(deathDetails =>
        deathDetails.killerId.map {
          killerId => AddExperienceEvent(killerId, experienceForLevel(2) / 2)
        }.toSeq
      )
    )

    val enemy2 = Entity(
      id = "enemy2",
      Movement(position = Point(5, 7)),
      EntityTypeComponent(EntityType.Enemy),
      Health(2),
      Initiative(10),
      Inventory(),
      SightMemory(),
      Drawable(Sprites.enemySprite),
      Hitbox(),
      DeathEvents(deathDetails =>
        deathDetails.killerId.map {
          killerId => AddExperienceEvent(killerId, experienceForLevel(2) / 2)
        }.toSeq
      )
    )

    val gameState = GameState(
      playerEntityId = playerId,
      entities = Seq(playerWithScroll, scrollEntity, enemy1, enemy2),
      messages = Nil,
      dungeon = testDungeon
    )
    val gameController = GameController(Move, gameState)

    // Simulate using the fireball scroll and targeting the enemies' position
    val beforeFiringFireball = gameController
      .update(Some(Input.UseItem), frameTime) // Enter use item state
      .update(Some(Input.UseItem), frameTime * 2) // Select scroll
      .update(Some(Input.Move(Down)), frameTime * 3) // Move target cursor down

    // Check that the UI state is ScrollSelect as expected
    beforeFiringFireball.uiState should be (a[ScrollSelect])
    beforeFiringFireball.uiState.asInstanceOf[ScrollSelect].cursor shouldBe Point(4, 5)
    
    val afterFiringFireball = beforeFiringFireball
      .update(Some(Input.UseItem), frameTime * 4) // Confirm fireball target
      
    val fireballMoving = afterFiringFireball
      .update(None, frameTime * 5) // Process explosion/collision
      .update(None, frameTime * 6) // Process entity removals and experience
      .update(None, frameTime * 7) // Process entity removals and experience
      .update(None, frameTime * 8) 
      
    val afterCollision = afterFiringFireball  // Process entity removals and experience
      .update(None, frameTime * 9) // Process entity removals and experience
      .update(None, frameTime * 10) // Process entity removals and experience
      .update(None, frameTime * 11) // Process entity removals and experience
      .update(None, frameTime * 12) // Process entity removals and experience
      .update(None, frameTime * 13) // Process entity removals and experience
      .update(None, frameTime * 14) // Process entity removals and experience

    // Both enemies should be removed
    afterCollision.gameState.entities.find(_.id == "enemy1") shouldBe empty
    afterCollision.gameState.entities.find(_.id == "enemy2") shouldBe empty

    // Player should have gained experience for both enemies
    val expectedExp = experienceForLevel(2)
    afterCollision.gameState.playerEntity.experience shouldBe expectedExp
    afterCollision.gameState.playerEntity.canLevelUp shouldBe true
  }
}
