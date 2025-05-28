package game.event

import game.entity.Entity
import game.perk.Perk

case class ApplyPerkEvent(entityId: String, perk: Perk) extends EntityEvent {
  override def action: Entity => Entity = _.addStatusEffect(perk)
    
}
