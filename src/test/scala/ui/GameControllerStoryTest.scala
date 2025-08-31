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
      .timePasses(3)               // let projectile travel and hit
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
}