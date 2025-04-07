package scalafx
import game.Direction
import game.Input.*
import scalafx.scene.input.KeyCode

object InputTranslator {
  def translateKeyCode(keyCode: KeyCode): Input = {
    keyCode match {
      case KeyCode.W => Move(Direction.Up)
      case KeyCode.A => Move(Direction.Left)
      case KeyCode.S => Move(Direction.Down)
      case KeyCode.D => Move(Direction.Right)
      case KeyCode.Space => Attack
      case KeyCode.E => Interact
      case KeyCode.Q => UseItem
      case KeyCode.C => Cancel
      case _ => Wait
    }
  }
}
