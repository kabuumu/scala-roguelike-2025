package ui

import game.Item.Weapon
import game.Point
import game.entity._

object UIState {
  sealed trait UIState

  case object Move extends UIState

  case class FreeSelect(cursorX: Int, cursorY: Int) extends UIState

  case class Attack(enemies: Seq[Entity], index: Int = 0, optWeapon: Option[Weapon]) extends UIState {
    def iterate: Attack = Attack(enemies, (index + 1) % enemies.length, optWeapon)

    val position: Point = enemies(index)[Movement].position
  }
}