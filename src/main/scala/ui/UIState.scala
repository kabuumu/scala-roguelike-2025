package ui

import game.Entity

object UIState {
  sealed trait UIState

  case object Move extends UIState

  case class Attack(cursorX: Int, cursorY: Int) extends UIState

  case class AttackList(enemies: Seq[Entity], position: Int) extends UIState {
    def iterate: AttackList = AttackList(enemies, (position + 1) % enemies.length)
  }
}