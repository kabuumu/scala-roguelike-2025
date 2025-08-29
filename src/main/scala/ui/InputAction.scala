package ui

import game.{Direction, GameState}
import game.entity.Entity
import game.event.Event
import game.status.StatusEffect

enum InputAction {
  case Move(direction: Direction)
  case LevelUp(chosenPerk: StatusEffect)
  case UseItem(itemEffect: Entity => GameState => Seq[Event])
  case UseComponentItem(itemEntityId: String)
  case UseComponentItemAtPoint(itemEntityId: String, targetPoint: game.Point)
  case UseComponentItemOnEntity(itemEntityId: String, targetEntityId: String)
  case Attack(target: Entity)
  case Wait
  case Equip
}