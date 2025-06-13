package ui

import game.Point
import game.perk.Perk

import scala.reflect.ClassTag

object UIState {
  sealed trait UIState

  case object Move extends UIState

  case class ScrollSelect(cursor: Point, effect: Point => (UIState, Option[InputAction])) extends UIState {
    val cursorX: Int = cursor.x
    val cursorY: Int = cursor.y

    val action: (UIState, Option[InputAction]) = effect(cursor)
  }

  case class ListSelect[T: ClassTag](list: Seq[T], index: Int = 0, effect: T => (UIState, Option[InputAction])) extends UIState {
    def iterate: ListSelect[T] = copy(index = (index + 1) % list.length)

    val action: (UIState, Option[InputAction]) = effect(list(index))
  }
}