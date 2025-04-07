package ui

import game.Entity

object UIState {
  sealed trait UIState

  case object Move extends UIState

  case class Attack(cursorX: Int, cursorY: Int) extends UIState

  case class AttackList(enemies: Seq[Entity], index: Int) extends UIState {
    def iterate: AttackList = AttackList(enemies, (index + 1) % enemies.length)
  }
}