package game

import data.Enemies
import game.entity.*
import game.entity.Health.maxHealth
import game.{Direction, Point}
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.matchers.should.Matchers
import testsupport.{GameStory, Given}

class BatEnemyTest extends AnyFunSuiteLike with Matchers {

  test("Bat has fast initiative but low health") {
    val bat = Enemies.bat("test-bat", Point(5, 5))
    
    // Bat should have low health (8)
    bat.maxHealth shouldBe 8
    
    // Bat should be very fast (initiative 30, higher than snake at 25)
    bat.get[Initiative] match {
      case Some(initiative) => initiative.maxInitiative shouldBe 30
      case None => fail("Bat should have initiative component")
    }
  }

  test("Bat has weak attack damage") {
    val bat = Enemies.bat("test-bat", Point(5, 5))
    
    // Check weapon - should have low damage (2 bonus)
    bat.get[Equipment] match {
      case Some(equipment) => 
        equipment.weapon match {
          case Some(weapon) => 
            weapon.damageBonus shouldBe 2
            weapon.itemName shouldBe "Bat Bite"
          case None => fail("Bat should have weapon")
        }
      case None => fail("Bat should have equipment component")
    }
  }

  test("Bat can be placed in game and has correct type") {
    val bat = Enemies.bat("test-bat", Point(5, 5))
    
    Given
      .thePlayerAt(4, 4)
      .withEntities(bat)
      .beginStory()
      .thePlayer.isAt(4, 4)
      .entity("test-bat").component[EnemyTypeComponent].satisfies(_.enemyType == data.Enemies.EnemyReference.Bat)
  }

  test("Bat is faster than all other normal enemies") {
    val bat = Enemies.bat("test-bat", Point(5, 5))
    val snake = Enemies.snake("test-snake", Point(6, 6), "spit-1")
    val rat = Enemies.rat("test-rat", Point(7, 7))
    val slime = Enemies.slime("test-slime", Point(8, 8))
    val slimelet = Enemies.slimelet("test-slimelet", Point(9, 9))
    
    val batInitiative = bat.get[Initiative].map(_.maxInitiative).getOrElse(0)
    val snakeInitiative = snake.get[Initiative].map(_.maxInitiative).getOrElse(0)
    val ratInitiative = rat.get[Initiative].map(_.maxInitiative).getOrElse(0)
    val slimeInitiative = slime.get[Initiative].map(_.maxInitiative).getOrElse(0)
    val slimeletInitiative = slimelet.get[Initiative].map(_.maxInitiative).getOrElse(0)
    
    batInitiative should be > snakeInitiative
    batInitiative should be > ratInitiative
    batInitiative should be > slimeInitiative
    batInitiative should be > slimeletInitiative
  }

  test("Bat is weaker than most other enemies") {
    val bat = Enemies.bat("test-bat", Point(5, 5))
    val snake = Enemies.snake("test-snake", Point(6, 6), "spit-1")
    val rat = Enemies.rat("test-rat", Point(7, 7))
    val slime = Enemies.slime("test-slime", Point(8, 8))
    val slimelet = Enemies.slimelet("test-slimelet", Point(9, 9))
    
    val batHealth = bat.maxHealth
    val snakeHealth = snake.maxHealth
    val ratHealth = rat.maxHealth
    val slimeHealth = slime.maxHealth
    val slimeletHealth = slimelet.maxHealth
    
    batHealth should be < snakeHealth
    batHealth should be < ratHealth
    batHealth should be < slimeHealth
    batHealth should be < slimeletHealth
  }

  test("Bat drops appropriate rewards on death") {
    val bat = Enemies.bat("test-bat", Point(5, 5))
    
    bat.get[DeathEvents] match {
      case Some(deathEvents) =>
        deathEvents.deathEvents.size should be >= 2
        // Should give some experience
        deathEvents.deathEvents.exists(_.isInstanceOf[data.DeathEvents.DeathEventReference.GiveExperience]) shouldBe true
        // Should drop coins
        deathEvents.deathEvents.exists(_.isInstanceOf[data.DeathEvents.DeathEventReference.DropCoins]) shouldBe true
      case None => fail("Bat should have death events")
    }
  }

  test("Bat has correct difficulty level") {
    import data.Enemies.EnemyDifficulty._
    import data.Enemies.EnemyReference._
    
    difficultyFor(Bat) shouldBe 1
    difficultyFor(Bat) shouldBe difficultyFor(Slimelet)
  }
}
