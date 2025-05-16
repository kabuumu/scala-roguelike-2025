package game.entity

import game.Item
import game.Item.{Item, UsableItem, Weapon}

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
    def usableItems: Seq[Item] = items.filterNot(_.isInstanceOf[Item.Key])
    def groupedUsableItems: Map[UsableItem, Int] = usableItems.collect {
      case usableItem: UsableItem => usableItem
    }.groupBy(identity).view.mapValues(_.size).toMap
    def addItem(item: Item): Entity = entity.update[Inventory](_ + item)
  }
}
