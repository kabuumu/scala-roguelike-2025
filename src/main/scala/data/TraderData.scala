package data

import game.entity._
import game.entity.EntityType.{Trader, PackAnimal}
import game.entity.CaravanComponent
import game.Point
import game.Sprite

object TraderData {

  def createCaravan(
      leaderId: String,
      leaderPos: Point,
      numAnimals: Int = 2
  ): Seq[Entity] = {

    // Generate IDs for animals
    val animalIds = (1 to numAnimals).map(i => s"${leaderId}-animal-${i}")
    val members = leaderId +: animalIds

    // Create Leader
    val leader = CharacterFactory.create(
      id = leaderId,
      position = leaderPos,
      sprite = Sprites.playerSprite, // Placeholder
      entityType = Trader,
      name = Some(NameComponent("Trader Caravan Leader")),
      hp = Some(100),
      initiative = Some(5),
      inventory = Some(game.entity.Inventory(Nil)),
      isActive = true,
      extraComponents = Seq(CaravanComponent(leaderId, members))
    )

    // Create Animals
    val animals = animalIds.map { id =>
      CharacterFactory.create(
        id = id,
        position = leaderPos,
        sprite = Sprites.potionSprite, // Placeholder
        entityType = PackAnimal,
        name = Some(NameComponent("Pack Animal")),
        hp = Some(50),
        initiative = Some(8), // Animals might move slightly differently?
        isActive = true // Assuming they might have AI later or need to move
      )
    }

    leader +: animals
  }
}
