package ui

import game.{Direction, GameState}
import game.entity.{Entity, UsableItem, UseContext}
import game.status.StatusEffect

enum InputAction {
  case Move(direction: Direction)
  case LevelUp(chosenPerk: StatusEffect)
  case UseItem[T](itemId: String, itemType: UsableItem, useContext: UseContext[T])
  case Attack(target: Entity)
  case Wait
  case Equip
  case LoadGame
}