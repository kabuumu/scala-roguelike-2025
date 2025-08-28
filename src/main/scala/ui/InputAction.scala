package ui

import game.{Direction, GameState}
import game.Item.Item
import game.entity.Entity
import game.event.Event
import game.status.StatusEffect

enum InputAction {
  case Move(direction: Direction)
  case LevelUp(chosenPerk: StatusEffect)
  case UseItem(itemEffect: Entity => GameState => Seq[Event])
  case Attack(target: Entity)
  case Wait
  case Equip
}