package ui

import game.Point

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
    def iterateDown: ListSelect[T] = copy(index = (index - 1 + list.length) % list.length)

    val action: (UIState, Option[InputAction]) = effect(list(index))
  }

  case class MainMenu(selectedOption: Int = 0) extends UIState {
    import game.save.SaveGameSystem
    
    val options: Seq[String] = {
      val baseOptions = Seq("New Game")
      if (SaveGameSystem.hasSaveGame()) {
        baseOptions :+ "Continue Game"
      } else {
        baseOptions :+ "Continue Game (No Save)"
      }
    }
    
    def selectNext: MainMenu = copy(selectedOption = (selectedOption + 1) % options.length)
    def selectPrevious: MainMenu = copy(selectedOption = (selectedOption - 1 + options.length) % options.length)
    
    def getSelectedOption: String = options(selectedOption)
    
    def isOptionEnabled(index: Int): Boolean = {
      index match {
        case 0 => true // New Game is always enabled
        case 1 => SaveGameSystem.hasSaveGame() // Continue Game only enabled if save exists
        case _ => false
      }
    }
    
    def canConfirmCurrentSelection: Boolean = isOptionEnabled(selectedOption)
  }
}