package ui

import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.matchers.should.Matchers

import testsupport.*
import testsupport.Given.*
import game.{Direction, Input, Point}
import game.entity.*
import game.entity.Experience.*
import game.system.event.GameSystemEvent.AddExperienceEvent
import ui.UIState.Move

class GameControllerStoryTest extends AnyFunSuiteLike with Matchers {

  test("Initiative decreases over time") {
    val story = Given
      .thePlayerAt(4, 4)
      .modifyPlayer[Initiative](_.copy(maxInitiative = 1, currentInitiative = 1))
      .beginStory()

    story
      .when.timePasses(1)
      .`then`.thePlayer.component[Initiative].is(Initiative(1, 0))
  }

  test("Player moves when given a move action") {
    val story = Given
      .thePlayerAt(4, 4)
      .beginStory()

    story
      .when.thePlayer.moves(Direction.Up)
      .`then`.thePlayer.isAt(4, 3)
  }

  test("Player heals when using a potion") {
    val potion = items.potion("test-potion-1")
    
    val story = Given
      .thePlayerAt(4, 4)
      .modifyPlayer[Health](_ => Health(5, 10)) // wounded player with 5/10 HP
      .withItems(potion)
      .beginStory()

    story
      .`then`.thePlayer.hasHealth(5)
      .when.thePlayer.opensItems()
      .and.thePlayer.confirmsSelection() // select the potion
      .`then`.thePlayer.hasHealth(10) // should be fully healed
  }

  test("Using a fireball scroll creates a projectile after targeting") {
    val scroll = items.scroll("test-scroll-1")
    
    val story = Given
      .thePlayerAt(4, 4)
      .withItems(scroll)
      .beginStory()

    story
      .when.thePlayer.opensItems()
      .and.thePlayer.confirmsSelection() // select the scroll
      .and.cursor.moves(Direction.Up, 3) // move target cursor up
      .and.uiIsScrollTargetAt(4, 1)
      .and.projectilesAre(0)
      .and.cursor.confirm() // confirm target
      .and.projectilesAre(1)
  }

  test("Firing an arrow damages an enemy") {
    val bow = items.bow("test-bow-1")
    val arrow = items.arrow("test-arrow-1")
    val enemies = Given.enemies.basic("enemyId", 4, 7, health = 10, withWeapons = true)
    
    val story = Given
      .thePlayerAt(4, 4)
      .modifyPlayer[Initiative](_.copy(maxInitiative = 10, currentInitiative = 0))
      .withItems(bow, arrow)
      .withEntities(enemies*)
      .beginStory()

    story
      .when.thePlayer.opensItems()
      .and.thePlayer.confirmsSelection() // select the bow
      .and.projectilesAre(0)
      .and.thePlayer.confirmsSelection() // confirm bow target (auto-targets enemy)
      .and.projectilesAre(1)
      .and.timePasses(3) // let projectile travel and hit
      .`then`.enemy("enemyId").hasHealth(2) // bow does 8 damage, enemy had 10 health
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
    
    val story = Given
      .thePlayerAt(4, 4)
      .modifyPlayer[Initiative](_.copy(maxInitiative = 10, currentInitiative = 0))
      .withItems(scroll)
      .withEntities((enemy1Entities ++ enemy2Entities)*)
      .beginStory()

    story
      .when.thePlayer.opensItems()
      .and.thePlayer.confirmsSelection() // select the scroll
      .and.cursor.moves(Direction.Down) // move target to (4, 5)
      .and.uiIsScrollTargetAt(4, 5)
      .and.cursor.confirm() // confirm fireball target
      .and.timePasses(10) // process explosion, collisions, and experience
      .and.entityMissing("enemy1")
      .and.entityMissing("enemy2")
      .`then`.thePlayer.component[Experience].satisfies(
        _.currentExperience == experienceForLevel(2), 
        "player should have gained full level 2 experience"
      )
  }
}