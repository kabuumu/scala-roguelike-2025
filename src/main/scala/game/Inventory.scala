package game

import game.Item.{Item, Weapon}

case class Inventory(items: Seq[Item] = Nil, primaryWeapon: Option[Weapon] = None, secondaryWeapon: Option[Weapon] = None) {
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
