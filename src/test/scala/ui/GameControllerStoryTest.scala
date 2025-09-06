package ui

import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.matchers.should.Matchers

import testsupport.*
import testsupport.Given.*
import game.{Direction, Input, Point}
import game.entity.*
import game.entity.Health.*
import game.entity.Experience.*
import game.system.event.GameSystemEvent.AddExperienceEvent
import ui.UIState.Move
import ui.GameController
import ui.GameController.frameTime
import game.save.TestSaveGameSystem
import scala.util.Success

class GameControllerStoryTest extends AnyFunSuiteLike with Matchers {

  test("Initiative decreases over time") {
    Given
      .thePlayerAt(4, 4)
      .modifyPlayer[Initiative](_.copy(maxInitiative = 1, currentInitiative = 1))
      .beginStory()
      .timePasses(1)
      .thePlayer.component[Initiative].is(Initiative(1, 0))
  }

  test("Player moves when given a move action") {
    Given
      .thePlayerAt(4, 4)
      .beginStory()
      .thePlayer.moves(Direction.Up)
      .thePlayer.isAt(4, 3)
  }

  test("Player heals when using a potion") {
    val potion = items.potion("test-potion-1")
    
    val world = Given
      .thePlayerAt(4, 4)
      .withItems(potion)
    
    // Damage the player using the extension method like in original test
    val damagedWorld = world.copy(player = world.player.damage(5, ""))
    
    damagedWorld
      .beginStory()
      .thePlayer.hasHealth(5)
      .thePlayer.opensItems()       // enter use-item state (ListSelect)
      .thePlayer.confirmsSelection() // select the potion (immediately used)
      .thePlayer.hasHealth(10)      // should be fully healed
  }

  test("Using a fireball scroll creates a projectile after targeting") {
    val scroll = items.scroll("test-scroll-1")
    
    Given
      .thePlayerAt(4, 4)
      .withItems(scroll)
      .beginStory()
      .thePlayer.opensItems()        // enter use-item state (ListSelect)
      .uiIsListSelect()             // verify we're in item selection
      .thePlayer.confirmsSelection() // select the scroll (enters ScrollSelect)
      .cursor.moves(Direction.Up, 3) // move target cursor up
      .uiIsScrollTargetAt(4, 1)     // verify cursor position
      .projectilesAre(0)            // no projectile yet
      .cursor.confirm()             // confirm target
      .projectilesAre(1)            // projectile created
  }

  test("Firing an arrow damages an enemy") {
    val bow = items.bow("test-bow-1")
    val arrow = items.arrow("test-arrow-1")
    val enemies = Given.enemies.basic("enemyId", 4, 7, health = 10, withWeapons = true)
    
    Given
      .thePlayerAt(4, 4)
      .modifyPlayer[Initiative](_.copy(maxInitiative = 10, currentInitiative = 0))
      .withItems(bow, arrow)
      .withEntities(enemies*)
      .beginStory()
      .thePlayer.opensItems()       // enter use-item state (ListSelect with items)
      .uiIsListSelect()            // verify item selection state
      .thePlayer.confirmsSelection() // select the bow (enters ListSelect with enemies)
      .uiIsListSelect()            // verify enemy selection state  
      .projectilesAre(0)           // no projectile yet
      .thePlayer.confirmsSelection() // select target enemy
      .projectilesAre(1)           // projectile created
      .timePasses(10)               // let projectile travel and hit
      .projectilesAre(0)
      .enemy("enemyId").hasHealth(2) // bow does 8 damage, enemy had 10 health
  }

  test("Fireball hits multiple enemies, removes them, and awards experience") {
    val scroll = items.scroll("test-scroll-2")
    
    // Create enemies with death events that award experience
    val enemy1DeathEvents = DeathEvents(deathDetails =>
      deathDetails.killerId.map {
        killerId => AddExperienceEvent(killerId, experienceForLevel(2) / 2)
      }.toSeq
    )
    val enemy2DeathEvents = DeathEvents(deathDetails =>
      deathDetails.killerId.map {
        killerId => AddExperienceEvent(killerId, experienceForLevel(2) / 2)
      }.toSeq
    )
    
    val enemy1Entities = Given.enemies.basic("enemy1", 4, 7, health = 2, deathEvents = Some(enemy1DeathEvents))
    val enemy2Entities = Given.enemies.basic("enemy2", 5, 7, health = 2, deathEvents = Some(enemy2DeathEvents))
    
    Given
      .thePlayerAt(4, 4)
      .modifyPlayer[Initiative](_.copy(maxInitiative = 10, currentInitiative = 0))
      .withItems(scroll)
      .withEntities((enemy1Entities ++ enemy2Entities)*)
      .beginStory()
      .thePlayer.opensItems()       // enter use-item state (ListSelect)
      .uiIsListSelect()            // verify item selection state
      .thePlayer.confirmsSelection() // select the scroll (enters ScrollSelect)
      .cursor.moves(Direction.Down) // move target to (4, 5)
      .uiIsScrollTargetAt(4, 5)    // verify cursor position
      .cursor.confirm()            // confirm fireball target
      .timePasses(15)              // process explosion, collisions, and experience
      .entityMissing("enemy1")
      .entityMissing("enemy2")
      .thePlayer.component[Experience].satisfies(
        _.currentExperience == experienceForLevel(2), 
        "player should have gained full level 2 experience"
      )
  }

  // Comprehensive save/load functionality tests
  test("Save and load preserves player health correctly") {
    TestSaveGameSystem.clearStorage()
    
    val helmet = Equippable(EquipmentSlot.Helmet, 2, "Iron Helmet")
    val potion = items.potion("test-potion-save")
    val enemy = Given.enemies.basic("enemy-save", 10, 10, health = 50)
    
    val worldWithoutEquipment = Given
      .thePlayerAt(5, 5)
      .withItems(potion)
      .withEntities(enemy*)
      .modifyPlayer[Health](_ => Health(100)) // Start with 100 health
    
    val originalWorld = worldWithoutEquipment.copy(player = worldWithoutEquipment.player.addComponent(Equipment(Some(helmet), None)))
    
    // Damage the player to 84/100 health
    val damagedWorld = originalWorld.copy(player = originalWorld.player.damage(16, ""))
    
    val story = damagedWorld.beginStory()
    
    // Verify original state - damaged health, equipped helmet, enemy present
    story
      .thePlayer.hasHealth(84)
      .thePlayer.component[Equipment].satisfies(_.helmet.isDefined, "should have helmet equipped")
      .thePlayer.component[Equipment].satisfies(_.getTotalDamageReduction == 2, "should have damage reduction")
      .entity("enemy-save").component[EntityTypeComponent].satisfies(_.entityType == EntityType.Enemy)
      .entity("enemy-save").hasHealth(50)
    
    // Save the game
    val saveResult = TestSaveGameSystem.saveGame(story.controller.gameState)
    saveResult shouldBe a[Success[?]]
    
    // Load the game
    val loadResult = TestSaveGameSystem.loadGame()
    loadResult shouldBe a[Success[?]]
    val loadedGameState = loadResult.get
    
    // Create new story from loaded state
    val loadedStory = GameStory.begin(UIState.Move, loadedGameState)
    
    // Verify all critical state is preserved
    loadedStory
      .thePlayer.hasHealth(84) // CRITICAL: Health should be 84/100, not 100/100
      .thePlayer.component[Equipment].satisfies(_.helmet.isDefined, "helmet should be preserved")
      .thePlayer.component[Equipment].satisfies(_.getTotalDamageReduction == 2, "damage reduction preserved")
      .entity("enemy-save").component[EntityTypeComponent].satisfies(_.entityType == EntityType.Enemy, "enemy should remain enemy")
      .entity("enemy-save").hasHealth(50) // Enemy health preserved
      .thePlayer.component[Drawable].satisfies(_.sprites.nonEmpty, "player should remain visible")
    
    TestSaveGameSystem.clearStorage()
  }

  test("Save and load preserves all entities and prevents disappearance") {
    TestSaveGameSystem.clearStorage()
    
    val enemies = Given.enemies.basic("enemy1", 8, 8, health = 30) ++ 
                  Given.enemies.basic("enemy2", 12, 12, health = 40) ++
                  Given.enemies.basic("enemy3", 6, 10, health = 25)
    
    val originalWorld = Given
      .thePlayerAt(10, 10)
      .withEntities(enemies*)
    
    val story = originalWorld.beginStory()
    
    // Verify original state has all entities
    story
      .entity("enemy1").component[EntityTypeComponent].satisfies(_.entityType == EntityType.Enemy)
      .entity("enemy2").component[EntityTypeComponent].satisfies(_.entityType == EntityType.Enemy)
      .entity("enemy3").component[EntityTypeComponent].satisfies(_.entityType == EntityType.Enemy)
    
    // Save and load
    TestSaveGameSystem.saveGame(story.controller.gameState)
    val loadedGameState = TestSaveGameSystem.loadGame().get
    val loadedStory = GameStory.begin(UIState.Move, loadedGameState)
    
    // Verify NO entities disappeared
    loadedStory
      .entity("enemy1").component[EntityTypeComponent].satisfies(_.entityType == EntityType.Enemy, "enemy1 should not disappear")
      .entity("enemy2").component[EntityTypeComponent].satisfies(_.entityType == EntityType.Enemy, "enemy2 should not disappear") 
      .entity("enemy3").component[EntityTypeComponent].satisfies(_.entityType == EntityType.Enemy, "enemy3 should not disappear")
      .entity("enemy1").hasHealth(30) // Health preserved
      .entity("enemy2").hasHealth(40)
      .entity("enemy3").hasHealth(25)
    
    TestSaveGameSystem.clearStorage()
  }

  test("Save and load preserves equipment and inventory completely") {
    TestSaveGameSystem.clearStorage()
    
    val helmet = Equippable(EquipmentSlot.Helmet, 3, "Steel Helmet")
    val armor = Equippable(EquipmentSlot.Armor, 4, "Chain Mail")
    val potion1 = items.potion("save-potion-1")
    val potion2 = items.potion("save-potion-2")
    val scroll = items.scroll("save-scroll-1")
    val bow = items.bow("save-bow-1")
    
    val worldWithoutEquipment = Given
      .thePlayerAt(7, 7)
      .withItems(potion1, potion2, scroll, bow)
      .modifyPlayer[Inventory](inv => inv.copy(primaryWeaponId = Some("save-bow-1")))
    
    val originalWorld = worldWithoutEquipment.copy(player = worldWithoutEquipment.player.addComponent(Equipment(Some(helmet), Some(armor))))
    
    val story = originalWorld.beginStory()
    
    // Verify original equipment and inventory
    story
      .thePlayer.component[Equipment].satisfies(_.helmet.isDefined, "should have helmet")
      .thePlayer.component[Equipment].satisfies(_.armor.isDefined, "should have armor") 
      .thePlayer.component[Equipment].satisfies(_.getTotalDamageReduction == 7, "total DR should be 7")
      .thePlayer.component[Inventory].satisfies(_.itemEntityIds.length == 4, "should have 4 items")
      .thePlayer.component[Inventory].satisfies(_.primaryWeaponId.isDefined, "should have primary weapon")
    
    // Save and load
    TestSaveGameSystem.saveGame(story.controller.gameState)
    val loadedGameState = TestSaveGameSystem.loadGame().get
    val loadedStory = GameStory.begin(UIState.Move, loadedGameState)
    
    // Verify equipment and inventory preserved
    loadedStory
      .thePlayer.component[Equipment].satisfies(_.helmet.isDefined, "helmet should be preserved")
      .thePlayer.component[Equipment].satisfies(_.armor.isDefined, "armor should be preserved")
      .thePlayer.component[Equipment].satisfies(_.getTotalDamageReduction == 7, "total DR should remain 7")
      .thePlayer.component[Inventory].satisfies(_.itemEntityIds.length == 4, "all 4 items should be preserved")
      .thePlayer.component[Inventory].satisfies(_.primaryWeaponId.isDefined, "primary weapon should be preserved")
    
    TestSaveGameSystem.clearStorage()
  }

  test("Save and load preserves player visibility and rendering") {
    TestSaveGameSystem.clearStorage()
    
    val originalWorld = Given
      .thePlayerAt(15, 15)
    
    val story = originalWorld.beginStory()
    
    // Verify player has drawable component
    story
      .thePlayer.component[Drawable].satisfies(_.sprites.nonEmpty, "player should have visible sprites")
    
    // Save and load
    TestSaveGameSystem.saveGame(story.controller.gameState)
    val loadedGameState = TestSaveGameSystem.loadGame().get
    val loadedStory = GameStory.begin(UIState.Move, loadedGameState)
    
    // Verify player remains visible after loading
    loadedStory
      .thePlayer.component[Drawable].satisfies(_.sprites.nonEmpty, "player should remain visible after loading")
      .thePlayer.isAt(15, 15) // Position preserved
    
    TestSaveGameSystem.clearStorage()
  }

  test("Save and load preserves dungeon layout correctly") {
    TestSaveGameSystem.clearStorage()
    
    val originalWorld = Given
      .thePlayerAt(20, 20)
    
    val story = originalWorld.beginStory()
    val originalDungeon = story.controller.gameState.dungeon
    
    // Save and load
    TestSaveGameSystem.saveGame(story.controller.gameState)
    val loadedGameState = TestSaveGameSystem.loadGame().get
    val loadedDungeon = loadedGameState.dungeon
    
    // Verify dungeon structure is preserved
    loadedDungeon.seed should equal(originalDungeon.seed)
    loadedDungeon.testMode should equal(originalDungeon.testMode)
    loadedDungeon.roomGrid should equal(originalDungeon.roomGrid)
    loadedDungeon.roomConnections should equal(originalDungeon.roomConnections)
    loadedDungeon.startPoint should equal(originalDungeon.startPoint)
    loadedDungeon.endpoint should equal(originalDungeon.endpoint)
    
    TestSaveGameSystem.clearStorage()
  }

  test("Complete save/load integration with complex game state") {
    TestSaveGameSystem.clearStorage()
    
    // Create complex game state with everything
    val helmet = Equippable(EquipmentSlot.Helmet, 2, "Complex Helmet")
    val armor = Equippable(EquipmentSlot.Armor, 3, "Complex Armor")
    val potions = (1 to 3).map(i => items.potion(s"complex-potion-$i"))
    val enemies = (1 to 5).flatMap(i => Given.enemies.basic(s"complex-enemy-$i", 5 + i, 5 + i, health = 20 + i * 5))
    
    val worldWithoutEquipment = Given
      .thePlayerAt(25, 25)
      .withItems(potions*)
      .withEntities(enemies*)
      .modifyPlayer[Health](_ => Health(100)) // Start with 100 health
      .modifyPlayer[Experience](_.copy(currentExperience = 150, levelUp = true))
      .modifyPlayer[Initiative](_.copy(maxInitiative = 20, currentInitiative = 15))
    
    val originalWorld = worldWithoutEquipment.copy(player = worldWithoutEquipment.player.addComponent(Equipment(Some(helmet), Some(armor))))
    
    // Damage player to test health preservation
    val damagedWorld = originalWorld.copy(player = originalWorld.player.damage(25, ""))
    
    val story = damagedWorld.beginStory()
    
    // Verify complex original state
    story
      .thePlayer.hasHealth(75) // 100 - 25 = 75
      .thePlayer.component[Equipment].satisfies(_.getTotalDamageReduction == 5)
      .thePlayer.component[Experience].satisfies(_.currentExperience == 150)
      .thePlayer.component[Experience].satisfies(_.levelUp == true)
      .thePlayer.component[Initiative].satisfies(_.maxInitiative == 20)
      .thePlayer.component[Initiative].satisfies(_.currentInitiative == 15)
      .thePlayer.component[Inventory].satisfies(_.itemEntityIds.length == 3)
    
    // Verify all enemies present
    (1 to 5).foreach { i =>
      story.entity(s"complex-enemy-$i").component[EntityTypeComponent].satisfies(_.entityType == EntityType.Enemy)
    }
    
    // Save and load
    TestSaveGameSystem.saveGame(story.controller.gameState)
    val loadedGameState = TestSaveGameSystem.loadGame().get
    val loadedStory = GameStory.begin(UIState.Move, loadedGameState)
    
    // Verify ALL state preserved perfectly
    loadedStory
      .thePlayer.hasHealth(75) // CRITICAL: Health must be exactly 75, not 100
      .thePlayer.component[Equipment].satisfies(_.getTotalDamageReduction == 5)
      .thePlayer.component[Experience].satisfies(_.currentExperience == 150)
      .thePlayer.component[Experience].satisfies(_.levelUp == true)
      .thePlayer.component[Initiative].satisfies(_.maxInitiative == 20)
      .thePlayer.component[Initiative].satisfies(_.currentInitiative == 15)
      .thePlayer.component[Inventory].satisfies(_.itemEntityIds.length == 3)
      .thePlayer.component[Drawable].satisfies(_.sprites.nonEmpty)
      .thePlayer.isAt(25, 25)
    
    // Verify all enemies still present and correct
    (1 to 5).foreach { i =>
      loadedStory
        .entity(s"complex-enemy-$i").component[EntityTypeComponent].satisfies(_.entityType == EntityType.Enemy, s"enemy $i should remain enemy")
        .entity(s"complex-enemy-$i").hasHealth(20 + i * 5) // Health preserved
    }
    
    TestSaveGameSystem.clearStorage()
  }
}