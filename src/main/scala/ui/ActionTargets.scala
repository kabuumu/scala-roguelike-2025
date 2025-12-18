package ui

import game.entity.{Entity, Health, Equippable, EquipmentSlot}
import game.entity.Health.*
import game.entity.NameComponent.name

/** Defines action targets for the unified action system. These represent
  * different types of interactions the player can perform.
  */
object ActionTargets {
  // Unified action target for the new input system
  sealed trait ActionTarget {
    def entity: Entity
    def description: String
  }

  case class AttackTarget(entity: Entity) extends ActionTarget {
    def description: String = {
      val healthText = if (entity.has[Health]) {
        s" (${entity.currentHealth}/${entity.maxHealth} HP)"
      } else ""
      s"Attack ${entity.name.getOrElse("Enemy")}$healthText"
    }
  }

  case class EquipTarget(entity: Entity) extends ActionTarget {
    def description: String = {
      entity.get[Equippable] match {
        case Some(equippable) =>
          if (equippable.slot == EquipmentSlot.Weapon) {
            s"Equip ${equippable.itemName} (Damage bonus +${equippable.damageBonus})"
          } else {
            s"Equip ${equippable.itemName} (Damage reduction +${equippable.damageReduction})"
          }
        case None => s"Equip ${entity.name.getOrElse("Item")}"
      }
    }
  }

  case class DescendStairsTarget(entity: Entity) extends ActionTarget {
    def description: String = "Descend stairs to next floor"
  }

  case class TradeTarget(entity: Entity) extends ActionTarget {
    def description: String = s"Trade with ${entity.name.getOrElse("Trader")}"
  }

  case class ConversationTarget(entity: Entity) extends ActionTarget {
    def description: String = s"Talk to ${entity.name.getOrElse("Person")}"
  }
}
