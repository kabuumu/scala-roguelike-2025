package game

object Item {
  val potionValue = 5

  trait Item

  case object Potion extends Item

  case object Key extends Item
}
