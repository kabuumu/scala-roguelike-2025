package ui

import game.Point
import game.entity.Entity

import scala.reflect.ClassTag

object UIState {
  sealed trait UIState

  case object Move extends UIState
  
  case class GameOver(player: Entity) extends UIState

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
      val withSave = if (SaveGameSystem.hasSaveGame()) {
        baseOptions :+ "Continue Game"
      } else {
        baseOptions :+ "Continue Game (No Save)"
      }
      withSave :+ "Debug Menu"
    }
    
    def selectNext: MainMenu = copy(selectedOption = (selectedOption + 1) % options.length)
    def selectPrevious: MainMenu = copy(selectedOption = (selectedOption - 1 + options.length) % options.length)
    
    def getSelectedOption: String = options(selectedOption)
    
    def isOptionEnabled(index: Int): Boolean = {
      index match {
        case 0 => true // New Game is always enabled
        case 1 => SaveGameSystem.hasSaveGame() // Continue Game only enabled if save exists
        case 2 => true // Debug Menu is always enabled
        case _ => false
      }
    }
    
    def canConfirmCurrentSelection: Boolean = isOptionEnabled(selectedOption)
  }
  
  case class DebugMenu(spriteIndex: Int = 0) extends UIState {
    // Calculate total number of sprites in the sprite sheet
    // 49 columns x 22 rows = 1078 sprites total
    val totalSprites: Int = 49 * 22
    
    def nextSprite: DebugMenu = copy(spriteIndex = (spriteIndex + 1) % totalSprites)
    def previousSprite: DebugMenu = copy(spriteIndex = (spriteIndex - 1 + totalSprites) % totalSprites)
    
    def getCurrentCoordinates: (Int, Int) = {
      val x = spriteIndex % 49
      val y = spriteIndex / 49
      (x, y)
    }
  }
}