package scalafx
import game.Direction
import game.Input.*
import scalafx.scene.input.KeyCode

object InputTranslator {
  def translateKeyCode(keyCode: KeyCode): Input = {
    keyCode match {
      case KeyCode.W | KeyCode.Up => Move(Direction.Up)
      case KeyCode.A | KeyCode.Left => Move(Direction.Left)
      case KeyCode.S | KeyCode.Down => Move(Direction.Down)
      case KeyCode.D | KeyCode.Right => Move(Direction.Right)
      case KeyCode.Z => Attack(PrimaryAttack)
      case KeyCode.X => Attack(SecondaryAttack)
      case KeyCode.E => Interact
      case KeyCode.U => UseItem
      case KeyCode.C | KeyCode.Escape => Cancel
      case _ => Wait
    }
  }
}
