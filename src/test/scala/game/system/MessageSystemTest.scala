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

class MessageSystemTest extends AnyFunSuiteLike with Matchers {
  val playerId = "testPlayerId"
  val testDungeon: Dungeon = Dungeon(testMode = true)

  val playerEntity: Entity = Entity(
    id = playerId,
    Movement(position = Point(5, 5)),
    EntityTypeComponent(EntityType.Player),
    Health(100),
    Drawable(Sprites.playerSprite),
    Hitbox()
  )

  test("MessageSystem should add message to game state") {
    val gameState = GameState(
      playerEntityId = playerId,
      entities = Seq(playerEntity),
      messages = Nil,
      dungeon = testDungeon
    )

    val messageEvent = MessageEvent("Test message")
    val (updatedState, events) = MessageSystem.update(gameState, Seq(messageEvent))

    // Message should be added to the game state
    updatedState.messages should contain("Test message")
    updatedState.messages.length shouldBe 1
    events shouldBe empty
  }

  test("MessageSystem should add multiple messages") {
    val gameState = GameState(
      playerEntityId = playerId,
      entities = Seq(playerEntity),
      messages = Seq("Existing message"),
      dungeon = testDungeon
    )

    val messageEvents = Seq(
      MessageEvent("First new message"),
      MessageEvent("Second new message")
    )
    val (updatedState, events) = MessageSystem.update(gameState, messageEvents)

    // All messages should be present
    updatedState.messages should contain("Existing message")
    updatedState.messages should contain("First new message")
    updatedState.messages should contain("Second new message")
    updatedState.messages.length shouldBe 3
    events shouldBe empty
  }

  test("MessageSystem should maintain message order") {
    val gameState = GameState(
      playerEntityId = playerId,
      entities = Seq(playerEntity),
      messages = Seq("First", "Second"),
      dungeon = testDungeon
    )

    val messageEvent = MessageEvent("Third")
    val (updatedState, events) = MessageSystem.update(gameState, Seq(messageEvent))

    // New message should be appended
    updatedState.messages shouldBe Seq("First", "Second", "Third")
    events shouldBe empty
  }

  test("MessageSystem should ignore non-message events") {
    val gameState = GameState(
      playerEntityId = playerId,
      entities = Seq(playerEntity),
      messages = Seq("Original message"),
      dungeon = testDungeon
    )

    val nonMessageEvent = DamageEvent(playerId, "enemy", 10)
    val (updatedState, events) = MessageSystem.update(gameState, Seq(nonMessageEvent))

    // Game state should be unchanged
    updatedState shouldBe gameState
    events shouldBe empty
  }

  test("MessageSystem should handle empty message string") {
    val gameState = GameState(
      playerEntityId = playerId,
      entities = Seq(playerEntity),
      messages = Nil,
      dungeon = testDungeon
    )

    val messageEvent = MessageEvent("")
    val (updatedState, events) = MessageSystem.update(gameState, Seq(messageEvent))

    // Empty message should still be added
    updatedState.messages should contain("")
    updatedState.messages.length shouldBe 1
    events shouldBe empty
  }

  test("MessageSystem should handle mixed event types") {
    val gameState = GameState(
      playerEntityId = playerId,
      entities = Seq(playerEntity),
      messages = Nil,
      dungeon = testDungeon
    )

    val mixedEvents = Seq(
      DamageEvent(playerId, "enemy", 10),
      MessageEvent("Damage dealt"),
      HealEvent(playerId, 5),
      MessageEvent("Player healed")
    )
    val (updatedState, events) = MessageSystem.update(gameState, mixedEvents)

    // Only message events should be processed
    updatedState.messages should contain("Damage dealt")
    updatedState.messages should contain("Player healed")
    updatedState.messages.length shouldBe 2
    events shouldBe empty
  }

  test("MessageSystem should handle no events") {
    val gameState = GameState(
      playerEntityId = playerId,
      entities = Seq(playerEntity),
      messages = Seq("Existing message"),
      dungeon = testDungeon
    )

    val (updatedState, events) = MessageSystem.update(gameState, Seq.empty)

    // Game state should be unchanged
    updatedState shouldBe gameState
    events shouldBe empty
  }

  test("MessageSystem should handle long messages") {
    val gameState = GameState(
      playerEntityId = playerId,
      entities = Seq(playerEntity),
      messages = Nil,
      dungeon = testDungeon
    )

    val longMessage = "This is a very long message that contains a lot of text to test how the message system handles longer strings"
    val messageEvent = MessageEvent(longMessage)
    val (updatedState, events) = MessageSystem.update(gameState, Seq(messageEvent))

    // Long message should be added normally
    updatedState.messages should contain(longMessage)
    updatedState.messages.length shouldBe 1
    events shouldBe empty
  }
}