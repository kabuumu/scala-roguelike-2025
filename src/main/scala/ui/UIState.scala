package ui

import game.Point
import game.action.Action

import scala.reflect.ClassTag

object UIState {
  sealed trait UIState

  case object Move extends UIState

  case class ScrollSelect(cursor: Point) extends UIState {
    val cursorX: Int = cursor.x
    val cursorY: Int = cursor.y
  }

  case class ListSelect[T: ClassTag](list: Seq[T], index: Int = 0, effect: T => (UIState, Option[Action])) extends UIState {
    def iterate: ListSelect[T] = copy(index = (index + 1) % list.length)

    val action: (UIState, Option[Action]) = effect(list(index))
  }
}