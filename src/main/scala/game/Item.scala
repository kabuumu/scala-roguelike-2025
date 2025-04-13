package game

import dungeongenerator.generator.Entity.KeyColour

object Item {
  val potionValue = 5

  trait Item

  case object Potion extends Item

  case class Key(keyColour: KeyColour) extends Item
}
