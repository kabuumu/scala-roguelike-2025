package data

import game.entity._
import game.{Point, Sprite}
import game.entity.EntityType.Plant

object Crops {

  val CropGrowthInitiative = 500

  def wheat(id: String, position: Point): Entity = {
    val growthComponent = Growth(
      currentStage = 0,
      maxStage = 3,
      growthTimer = 0, // Not used with initiative system
      timePerStage = 0, // Not used with initiative system
      stageSprites = Map(
        0 -> Sprites.dirtSprite, // Seed/Planted
        1 -> Sprites.cropStage1Sprite, // Sprout
        2 -> Sprites.cropStage2Sprite, // Growing
        3 -> Sprites.cropStage3Sprite // Mature
      )
    )

    Entity(
      id,
      Movement(position = position),
      growthComponent,
      Initiative(CropGrowthInitiative), // Use initiative for turn-based growth
      NameComponent("Wheat"),
      Drawable(growthComponent.stageSprites(0)),
      EntityTypeComponent(EntityType.Plant)
    )
  }
}
