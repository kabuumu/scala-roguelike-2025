package game

object Input {
  sealed trait Input
  case class Move(direction: Direction) extends Input
  case class Attack(attackType: AttackType) extends Input
  sealed trait AttackType
  case object PrimaryAttack extends AttackType
  case object SecondaryAttack extends AttackType
  case object Wait extends Input
  case object Cancel extends Input
  case object UseItem extends Input
  case object Interact extends Input
}
