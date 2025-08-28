package indigoengine

import game.Direction
import game.Input.*
import indigo.shared.events.Combo.KeyInputs
import indigo.*

object InputMappings {
  val inputMapping: InputMapping[Input] = InputMapping[game.Input.Input](
    KeyInputs(Key.KEY_W)        -> Move(Direction.Up),
    KeyInputs(Key.ARROW_UP)     -> Move(Direction.Up),
    KeyInputs(Key.KEY_A)        -> Move(Direction.Left),
    KeyInputs(Key.ARROW_LEFT)   -> Move(Direction.Left),
    KeyInputs(Key.KEY_S)        -> Move(Direction.Down),
    KeyInputs(Key.ARROW_DOWN)   -> Move(Direction.Down),
    KeyInputs(Key.KEY_D)        -> Move(Direction.Right),
    KeyInputs(Key.ARROW_RIGHT)  -> Move(Direction.Right),
    KeyInputs(Key.KEY_Z)        -> Attack(PrimaryAttack),
    KeyInputs(Key.KEY_X)        -> Attack(SecondaryAttack),
    KeyInputs(Key.KEY_E)        -> Interact,
    KeyInputs(Key.KEY_U)        -> UseItem,
    KeyInputs(Key.KEY_Q)        -> Equip,
    KeyInputs(Key.KEY_C)        -> Cancel,
    KeyInputs(Key.ESCAPE)       -> Cancel,
    KeyInputs(Key.SPACE)        -> Confirm,
    KeyInputs(Key.ENTER)        -> Confirm,
    KeyInputs(Key.KEY_L)        -> LevelUp
  )
}
