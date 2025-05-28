package game.status

import game.entity.Entity

trait StatusEffect {
  def apply(entity: Entity): Entity
}
