package ui

import game.{Direction, GameState}
import game.Item.Item
import game.entity.Entity
import game.event.Event
import game.perk.Perk

enum InputAction {
  case Move(direction: Direction)
  case LevelUp(chosenPerk: Perk)
  case UseItem(itemEffect: Entity => GameState => Seq[Event])
  case Attack(target: Entity)
  case Wait
}