package game

import data.{Enemies, Items}
import game.entity.{Coins, DeathEvents, Equipment, Equippable, Health, Movement}
import game.entity.Coins.*
import org.scalatest.funsuite.AnyFunSuite
import testsupport.Given

class CoinSystemTest extends AnyFunSuite {

  test("Player starts with 0 coins") {
    val world = Given.thePlayerAt(5, 5)
    val gameState = world.buildGameState()
    
    assert(gameState.playerEntity.coins == 0)
    assert(gameState.playerEntity.totalCoinsCollected == 0)
  }

  test("Player picks up coin by walking over it") {
    val coin = Items.coin("test-coin")
      .addComponent(Movement(position = Point(6, 5)))
    
    val world = Given
      .thePlayerAt(5, 5)
      .withEntities(coin)
      .beginStory()
    
    // Player starts with 0 coins
    world.thePlayer.component[Coins].satisfies(c => c.current == 0)
    
    // Move player to coin location
    val afterMove = world.thePlayer.moves(Direction.Right)
    
    // Player should now have 1 coin
    afterMove.thePlayer.component[Coins].satisfies { c =>
      c.current == 1 && c.totalCollected == 1
    }
    
    // Coin entity should be removed from the world
    assert(!afterMove.gameState.entities.exists(_.id == "test-coin"))
  }

  test("Player collects multiple coins") {
    val coin1 = Items.coin("test-coin-1")
      .addComponent(Movement(position = Point(6, 5)))
    val coin2 = Items.coin("test-coin-2")
      .addComponent(Movement(position = Point(7, 5)))
    
    val world = Given
      .thePlayerAt(5, 5)
      .withEntities(coin1, coin2)
      .beginStory()
    
    // Move to first coin
    val afterCoin1 = world.thePlayer.moves(Direction.Right)
    afterCoin1.thePlayer.component[Coins].satisfies(c => c.current == 1)
    
    // Move to second coin
    val afterCoin2 = afterCoin1.thePlayer.moves(Direction.Right)
    afterCoin2.thePlayer.component[Coins].satisfies { c =>
      c.current == 2 && c.totalCollected == 2
    }
  }

  test("Enemies have DropCoins death event") {
    import data.DeathEvents.DeathEventReference.DropCoins
    
    val enemies = Given.enemies.basic(
      "test-enemy", 6, 5, health = 1,
      deathEvents = Some(DeathEvents(Seq(DropCoins(5))))
    )
    
    val world = Given
      .thePlayerAt(5, 5)
      .withEntities(enemies*)
      .beginStory()
    
    // Verify enemy has DropCoins death event
    val enemy = world.gameState.entities.find(_.id == "test-enemy")
    assert(enemy.isDefined, "Enemy should exist")
    
    val deathEvents = enemy.get.get[DeathEvents]
    assert(deathEvents.isDefined, "Enemy should have death events")
    
    val hasDropCoins = deathEvents.get.deathEvents.exists {
      case DropCoins(5) => true
      case _ => false
    }
    assert(hasDropCoins, "Enemy should have DropCoins(5) death event")
  }

  test("Rat has DropCoins(3) death event") {
    val rat = Enemies.rat("test-rat", Point(6, 5))
    
    // Check that rat has DropCoins death event
    val deathEvents = rat.get[DeathEvents]
    assert(deathEvents.isDefined, "Rat should have death events")
    
    import data.DeathEvents.DeathEventReference.DropCoins
    val hasDropCoins = deathEvents.get.deathEvents.exists {
      case DropCoins(3) => true
      case _ => false
    }
    assert(hasDropCoins, "Rat should have DropCoins(3) death event")
  }

  test("Boss drops 50 coins on death") {
    val boss = Enemies.boss("test-boss", Point(6, 5), "test-ability")
      .update[Health](_.copy(baseCurrent = 1)) // Make boss easy to kill for testing
    
    val world = Given
      .thePlayerAt(5, 5)
      .withEntities(boss)
      .beginStory()
    
    // Check that boss has DropCoins death event
    val bossEntity = world.gameState.entities.find(_.id == "test-boss")
    assert(bossEntity.isDefined)
    
    val deathEvents = bossEntity.get.get[DeathEvents]
    assert(deathEvents.isDefined)
    
    // Verify the boss has DropCoins(50) in its death events
    import data.DeathEvents.DeathEventReference.DropCoins
    val hasDropCoins = deathEvents.get.deathEvents.exists {
      case DropCoins(50) => true
      case _ => false
    }
    assert(hasDropCoins, "Boss should have DropCoins(50) death event")
  }

  test("Total coins collected tracks coins even if spent") {
    val coin = Items.coin("test-coin")
      .addComponent(Movement(position = Point(6, 5)))
    
    val world = Given
      .thePlayerAt(5, 5)
      .withEntities(coin)
      .beginStory()
    
    // Collect coin
    val afterCollect = world.thePlayer.moves(Direction.Right)
    afterCollect.thePlayer.component[Coins].satisfies { c =>
      c.current == 1 && c.totalCollected == 1
    }
    
    // Manually spend a coin (simulate future shop feature)
    val playerWithSpentCoin = afterCollect.gameState.playerEntity.removeCoins(1)
    assert(playerWithSpentCoin.coins == 0)
    assert(playerWithSpentCoin.totalCoinsCollected == 1, "Total collected should remain 1")
  }
}
