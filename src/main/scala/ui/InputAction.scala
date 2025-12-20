package ui

import game.{Direction, GameState}
import game.entity.{
  Entity,
  UsableItem,
  UseContext,
  ConversationAction as ActionDetails
}
import game.status.StatusEffect

enum InputAction {
  case Move(direction: Direction)
  case LevelUp(chosenPerk: StatusEffect)
  case UseItem[T](
      itemId: String,
      itemType: UsableItem,
      useContext: UseContext[T]
  )
  case Attack(target: Entity)
  case Wait
  case Equip
  case EquipSpecific(target: Entity)
  case LoadGame
  case DescendStairs
  case Trade(trader: Entity)
  case BuyItem(trader: Entity, itemRef: data.Items.ItemReference)
  case SellItem(trader: Entity, itemEntity: Entity)
  case ConversationAction(entity: Entity, action: ActionDetails)
  case NewAdventure
  case NewGauntlet
}
