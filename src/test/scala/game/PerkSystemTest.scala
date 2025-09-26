package game

import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.matchers.should.Matchers
import testsupport.{GameStory, Given}
import game.{Direction, Input, Point}
import game.entity.*
import game.entity.EventMemory.*
import game.entity.Experience.{level, getPossiblePerks}
import game.perk.Perks
import game.perk.Perks.*
import game.status.StatusEffect.{statusEffects, addStatusEffect}

class PerkSystemTest extends AnyFunSuiteLike with Matchers {

  test("Basic perks are always available at level up") {
    Given
      .thePlayerAt(4, 4)
      .modifyPlayer[Experience](_.copy(currentExperience = Experience.experienceForLevel(2), levelUp = true))
      .beginStory()
      .thePlayer.component[Experience].satisfies(_.levelUp, "should be ready to level up")
      .thePlayer.component[Experience].satisfies(exp => {
        val player = Given.thePlayerAt(4, 4).modifyPlayer[Experience](_ => exp).player
        player.getPossiblePerks.length == 3
      }, "should have 3 perk options")
      .thePlayer.component[Experience].satisfies(exp => {
        val player = Given.thePlayerAt(4, 4).modifyPlayer[Experience](_ => exp).player
        player.getPossiblePerks.contains(IncreaseMaxHealthPerk)
      }, "should include basic health perk")
  }

  test("Random perk selection returns exactly 3 perks when more are available") {
    val player = Given.thePlayerAt(4, 4)
      .modifyPlayer[Experience](_.copy(currentExperience = Experience.experienceForLevel(2), levelUp = true))
      .player

    val randomPerks = Perks.getRandomPerks(player)
    randomPerks.length shouldBe 3

    val availablePerks = Perks.getAvailablePerks(player)
    randomPerks.forall(availablePerks.contains) shouldBe true
  }

  test("Already owned perks are not offered again") {
    val playerWithPerk = Given.thePlayerAt(4, 4)
      .modifyPlayer[Experience](_.copy(currentExperience = Experience.experienceForLevel(2), levelUp = true))
      .player
      .addStatusEffect(IncreaseMaxHealthPerk)

    val availablePerks = Perks.getAvailablePerks(playerWithPerk)
    availablePerks should not contain IncreaseMaxHealthPerk
  }

  test("Battle Hardened perk becomes available after taking damage") {
    val playerWithDamage = (1 to 10).foldLeft(Given.thePlayerAt(4, 4).player) { (entity, i) =>
      entity.addMemoryEvent(MemoryEvent.DamageTaken(System.nanoTime(), 10, 10, 0, 0, "test"))
    }

    val availablePerks = Perks.getAvailablePerks(playerWithDamage)
    availablePerks should contain(BattleHardenedPerk)
  }

  test("Improved Fortification requires level 3 and Fortified perk") {
    val level3Player = Given.thePlayerAt(4, 4)
      .modifyPlayer[Experience](_.copy(currentExperience = Experience.experienceForLevel(3), levelUp = true))
      .player

    // Without Fortified perk
    val availableWithoutFortified = Perks.getAvailablePerks(level3Player)
    availableWithoutFortified should not contain ImprovedFortificationPerk

    // With Fortified perk
    val playerWithFortified = level3Player.addStatusEffect(ReduceIncomingDamagePerk)
    val availableWithFortified = Perks.getAvailablePerks(playerWithFortified)
    availableWithFortified should contain(ImprovedFortificationPerk)
  }

  test("Slime Bane perk becomes available after killing slimes") {
    val playerWithKills = (1 to 5).foldLeft(Given.thePlayerAt(4, 4).player) { (entity, i) =>
      entity.addMemoryEvent(MemoryEvent.EnemyDefeated(System.nanoTime(), "Slime", "combat"))
    }

    val availablePerks = Perks.getAvailablePerks(playerWithKills)
    availablePerks should contain(SlimeBanePerk)
  }

  test("Bow Mastery perk becomes available after bow usage") {
    val playerWithBowUse = (1 to 10).foldLeft(Given.thePlayerAt(4, 4).player) { (entity, i) =>
      entity.addMemoryEvent(MemoryEvent.ItemUsed(System.nanoTime(), "Bow", None))
    }

    val availablePerks = Perks.getAvailablePerks(playerWithBowUse)
    availablePerks should contain(BowMasteryPerk)
  }

  test("Scroll Mastery perk becomes available after scroll usage") {
    val playerWithScrollUse = (1 to 5).foldLeft(Given.thePlayerAt(4, 4).player) { (entity, i) =>
      entity.addMemoryEvent(MemoryEvent.ItemUsed(System.nanoTime(), "Fireball Scroll", None))
    }

    val availablePerks = Perks.getAvailablePerks(playerWithScrollUse)
    availablePerks should contain(ScrollMasteryPerk)
  }

  test("Perk requirements check all conditions correctly") {
    val player = Entity(
      "test-player",
      Movement(position = Point(4, 4)),
      Experience(Experience.experienceForLevel(3)),
      EventMemory(Seq(
        MemoryEvent.EnemyDefeated(System.nanoTime(), "Slime", "combat"),
        MemoryEvent.EnemyDefeated(System.nanoTime(), "Slime", "combat"),
        MemoryEvent.EnemyDefeated(System.nanoTime(), "Slime", "combat"),
        MemoryEvent.EnemyDefeated(System.nanoTime(), "Slime", "combat"),
        MemoryEvent.EnemyDefeated(System.nanoTime(), "Slime", "combat")
      ))
    )

    // Test individual requirement checking
    EnemiesKilledRequirement("Slime", 5).isMet(player) shouldBe true
    EnemiesKilledRequirement("Slime", 6).isMet(player) shouldBe false
    LevelRequirement(3).isMet(player) shouldBe true
    LevelRequirement(4).isMet(player) shouldBe false
  }

  test("Leveling up shows random selection of available perks") {
    Given
      .thePlayerAt(4, 4)
      .modifyPlayer[Experience](_.copy(currentExperience = Experience.experienceForLevel(2), levelUp = true))
      .beginStory()
      .thePlayer.component[Experience].satisfies(_.levelUp, "should be ready to level up")
      .step(Some(Input.LevelUp))
      .uiIsListSelect()
      .thePlayer.confirmsSelection()
      .thePlayer.component[Experience].satisfies(!_.levelUp, "level up should be processed")
  }
}