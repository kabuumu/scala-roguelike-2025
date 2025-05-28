package game.perk

import game.entity.{Entity, Health}

case class IncreaseMaxHealthPerk(amount: Int) extends Perk {
  override def apply(entity: Entity): Entity = entity.update[Health](
    health =>
      health.copy(
        max = health.max + amount,
        current = health.current + amount
      )
  )
}
