package game

object Input {
  sealed trait Input
  case class Move(direction: Direction) extends Input
  case object Attack extends Input
  case object Wait extends Input
  case object Cancel extends Input
  case object UseItem extends Input
  case object Interact extends Input
}
