package game.system

import game.GameState
import game.entity._
import game.system.event.GameSystemEvent.GameSystemEvent
import data.Sprites
import game.Sprite
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class GrowthSystemTest extends AnyFlatSpec with Matchers {

  "GrowthSystem" should "not grow when initiative is not ready" in {
    val growth = Growth(
      currentStage = 0,
      maxStage = 3,
      growthTimer = 0,
      timePerStage = 0,
      stageSprites = Map(0 -> Sprites.dirtSprite, 1 -> Sprites.cropStage1Sprite)
    )

    val entity = Entity(
      "test-crop",
      growth,
      Initiative(10, 5), // Not ready (currentInitiative != 0)
      Drawable(Sprites.dirtSprite),
      EntityTypeComponent(EntityType.Plant)
    )

    // Create a dummy player entity
    val player = Entity(EntityTypeComponent(EntityType.Player))

    val gameState = GameState(
      player.id,
      Seq(player, entity),
      worldMap = null
    )

    val (newState, _) = GrowthSystem.update(gameState, Seq.empty)

    val updatedEntity = newState.getEntity("test-crop").get
    val updatedGrowth = updatedEntity.get[Growth].get

    // Should not have grown
    updatedGrowth.currentStage should be(0)
  }

  it should "advance stage when initiative is ready" in {
    val growth = Growth(
      currentStage = 0,
      maxStage = 3,
      growthTimer = 0,
      timePerStage = 0,
      stageSprites = Map(0 -> Sprites.dirtSprite, 1 -> Sprites.cropStage1Sprite)
    )

    val entity = Entity(
      "test-crop",
      growth,
      Initiative(10, 0), // Ready (currentInitiative == 0)
      Drawable(Sprites.dirtSprite),
      EntityTypeComponent(EntityType.Plant)
    )

    // Create a dummy player entity
    val player = Entity(EntityTypeComponent(EntityType.Player))

    val gameState = GameState(
      playerEntityId = player.id,
      entities = Seq(player, entity),
      worldMap = null
    )

    val (newState, _) = GrowthSystem.update(gameState, Seq.empty)

    val updatedEntity = newState.getEntity("test-crop").get
    val updatedGrowth = updatedEntity.get[Growth].get

    // Should have grown
    updatedGrowth.currentStage should be(1)

    // Initiative should be reset
    val updatedInitiative = updatedEntity.get[Initiative].get
    updatedInitiative.currentInitiative should be(10) // Reset to max

    // Check Sprite update
    val drawable = updatedEntity.get[Drawable].get
    drawable.sprites.head._2 should be(Sprites.cropStage1Sprite)
  }

  it should "not grow past max stage" in {
    val growth = Growth(
      currentStage = 3, // Already at max
      maxStage = 3,
      growthTimer = 0,
      timePerStage = 0,
      stageSprites = Map(3 -> Sprites.cropStage3Sprite)
    )

    val entity = Entity(
      "test-crop",
      growth,
      Initiative(10, 0), // Ready
      Drawable(Sprites.cropStage3Sprite),
      EntityTypeComponent(EntityType.Plant)
    )

    val player = Entity(EntityTypeComponent(EntityType.Player))

    val gameState = GameState(
      playerEntityId = player.id,
      entities = Seq(player, entity),
      worldMap = null
    )

    val (newState, _) = GrowthSystem.update(gameState, Seq.empty)

    val updatedEntity = newState.getEntity("test-crop").get
    val updatedGrowth = updatedEntity.get[Growth].get

    // Should NOT have grown past max
    updatedGrowth.currentStage should be(3)
  }
}
