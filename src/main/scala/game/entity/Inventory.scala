package game.entity

import game.Item
import game.Item.ChargeType.{Ammo, SingleUse}
import game.Item.{Item, UnusableItem, UsableItem, Weapon}

case class Inventory(items: Seq[Item] = Nil, primaryWeapon: Option[Weapon] = None, secondaryWeapon: Option[Weapon] = None) extends Component {
  def contains(item: Item): Boolean = items.contains(item)

  def -(item: Item): Inventory = {
    val index = items.indexOf(item)
    if (index != -1) {
      copy(items = items.patch(index, Nil, 1))
    } else {
      this
    }
  }

  def +(item: Item): Inventory = {
    copy(items = items :+ item)
  }

  val isEmpty: Boolean = items.isEmpty
}

object Inventory {
  extension (entity: Entity) {
    def items: Seq[Item] = entity.get[Inventory].toSeq.flatMap(_.items)

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

    def removeItem(item: Item): Entity = entity.update[Inventory](_ - item)
  }
}
