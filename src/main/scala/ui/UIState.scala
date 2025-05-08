package ui

import game.Item.Weapon
import game.Point
import game.entity._
import game.Item.Item

object UIState {
  sealed trait UIState

  case object Move extends UIState

  case class ScrollSelect(cursor: Point) extends UIState {
    val cursorX: Int = cursor.x
    val cursorY: Int = cursor.y
  }

  case class Attack(enemies: Seq[Entity], index: Int = 0, optWeapon: Option[Weapon]) extends UIState {
    def iterate: Attack = Attack(enemies, (index + 1) % enemies.length, optWeapon)

    val position: Point = enemies(index)[Movement].position
  }

  case class SelectItem(items: Seq[Item], index: Int = 0) extends UIState {
    def iterate: SelectItem = SelectItem(items, (index + 1) % items.length)

    val selectedItem: Item = items(index)
  }
}