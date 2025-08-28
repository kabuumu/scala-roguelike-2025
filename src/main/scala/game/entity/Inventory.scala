package game.entity

import game.Item
import game.Item.ChargeType.{Ammo, SingleUse}
import game.Item.{Item, UnusableItem, UsableItem, Weapon}

// Simple wrapper to track items with optional entity IDs for state preservation
case class ItemWithId(item: Item, entityId: Option[String] = None)

case class Inventory(
  items: Seq[ItemWithId] = Nil, 
  primaryWeapon: Option[Weapon] = None, 
  secondaryWeapon: Option[Weapon] = None
) extends Component {
  
  // Helper methods for backward compatibility
  def itemsOnly: Seq[Item] = items.map(_.item)
  
  def contains(item: Item): Boolean = itemsOnly.contains(item)

  def -(item: Item): Inventory = {
    val index = items.indexWhere(_.item == item)
    if (index != -1) {
      copy(items = items.patch(index, Nil, 1))
    } else {
      this
    }
  }

  def +(item: Item): Inventory = {
    copy(items = items :+ ItemWithId(item))
  }
  
  // New method to add item with entity tracking
  def addWithEntityId(item: Item, entityId: String): Inventory = {
    copy(items = items :+ ItemWithId(item, Some(entityId)))
  }
  
  // Method to get entity ID for an item (for dropping)
  def getEntityIdForItem(item: Item): Option[String] = {
    items.find(_.item == item).flatMap(_.entityId)
  }

  val isEmpty: Boolean = items.isEmpty
}

object Inventory {
  extension (entity: Entity) {
    def items: Seq[Item] = entity.get[Inventory].toSeq.flatMap(_.itemsOnly)

    def keys: Seq[Item.Key] = items.collect {
      case key: Item.Key => key
    }
    
    def groupedUsableItems: Map[UsableItem, Int] = items.collect {
      case usableItem: UsableItem => usableItem
    }.groupBy(identity).view.map {
      case (item, list) =>
        item.chargeType match {
          case SingleUse => item -> list.size
          case Ammo(ammoType) =>
            item -> items.count(_ == ammoType)
        }

    }.toMap

    def groupedUnusableItems: Map[UnusableItem, Int] = items.collect {
      case unusableItem: UnusableItem => unusableItem
    }.groupBy(identity).view.mapValues(_.size).toMap

    def addItem(item: Item): Entity = entity.update[Inventory](_ + item)
    
    // New method to add item while tracking its entity ID
    def addItemWithEntityId(item: Item, entityId: String): Entity = 
      entity.update[Inventory](_.addWithEntityId(item, entityId))

    def removeItem(item: Item): Entity = entity.update[Inventory](_ - item)
    
    // Get entity ID for dropping items with preserved state
    def getItemEntityId(item: Item): Option[String] = 
      entity.get[Inventory].flatMap(_.getEntityIdForItem(item))
  }
}
