package data

import game.entity._
import game.Point
import game.entity.EntityType.Villager

object Farmer {
  def create(id: String, position: Point): Entity = {
    CharacterFactory.create(
      id = id,
      position = position,
      sprite = Sprites.farmerSprite,
      entityType = Villager,
      name = Some(NameComponent("Farmer")),
      hp = Some(10),
      initiative = Some(5),
      inventory = Some(Inventory(Nil, capacity = 5)),
      isActive = true,
      extraComponents = Seq(Harvester())
    )
  }
}
