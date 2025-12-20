package ui

import game.Point
import game.entity.{Entity, Conversation, ConversationChoice, Trader}
import game.entity.Conversation.conversation
import game.entity.ConversationAction
import game.entity.ConversationAction.HealAction
import data.Items.ItemReference

object UIState {
  sealed trait UIState

  case object Move extends UIState

  case class GameOver(player: Entity) extends UIState

  // Base trait for scroll select states
  sealed trait ScrollSelectState extends UIState {
    def cursor: Point
    def effect: Point => (UIState, Option[InputAction])
    def cursorX: Int = cursor.x
    def cursorY: Int = cursor.y
    def action: (UIState, Option[InputAction]) = effect(cursor)
  }

  // Concrete scroll select implementation
  case class ScrollSelect(
      cursor: Point,
      effect: Point => (UIState, Option[InputAction])
  ) extends ScrollSelectState

  // Base trait for list select states
  sealed trait ListSelectState extends UIState {
    def index: Int
    def listLength: Int
    def action: (UIState, Option[InputAction])

    def nextIndex: Int = (index + 1) % listLength
    def prevIndex: Int = (index - 1 + listLength) % listLength
  }

  // Concrete list select for usable items
  case class UseItemSelect(
      list: Seq[Entity],
      index: Int = 0,
      effect: Entity => (UIState, Option[InputAction])
  ) extends ListSelectState {
    def iterate: UseItemSelect = copy(index = nextIndex)
    def iterateDown: UseItemSelect = copy(index = prevIndex)
    def listLength: Int = list.length
    def action: (UIState, Option[InputAction]) = effect(list(index))
    def currentItem: Entity = list(index)
  }

  // Concrete list select for buying items (ItemReference)
  case class BuyItemSelect(
      list: Seq[data.Items.ItemReference],
      index: Int = 0,
      effect: data.Items.ItemReference => (UIState, Option[InputAction])
  ) extends ListSelectState {
    def iterate: BuyItemSelect = copy(index = nextIndex)
    def iterateDown: BuyItemSelect = copy(index = prevIndex)
    def listLength: Int = list.length
    def action: (UIState, Option[InputAction]) = effect(list(index))
    def currentItem: data.Items.ItemReference = list(index)
  }

  // Concrete list select for selling items (Entity)
  case class SellItemSelect(
      list: Seq[Entity],
      index: Int = 0,
      effect: Entity => (UIState, Option[InputAction])
  ) extends ListSelectState {
    def iterate: SellItemSelect = copy(index = nextIndex)
    def iterateDown: SellItemSelect = copy(index = prevIndex)
    def listLength: Int = list.length
    def action: (UIState, Option[InputAction]) = effect(list(index))
    def currentItem: Entity = list(index)
  }

  // Concrete list select for status effects/perks
  case class StatusEffectSelect(
      list: Seq[game.status.StatusEffect],
      index: Int = 0,
      effect: game.status.StatusEffect => (UIState, Option[InputAction])
  ) extends ListSelectState {
    def iterate: StatusEffectSelect = copy(index = nextIndex)
    def iterateDown: StatusEffectSelect = copy(index = prevIndex)
    def listLength: Int = list.length
    def action: (UIState, Option[InputAction]) = effect(list(index))
    def currentItem: game.status.StatusEffect = list(index)
  }

  // Concrete list select for action targets (Attack, Equip, Trade, etc.)
  case class ActionTargetSelect(
      list: Seq[ActionTargets.ActionTarget],
      index: Int = 0,
      effect: ActionTargets.ActionTarget => (UIState, Option[InputAction])
  ) extends ListSelectState {
    def iterate: ActionTargetSelect = copy(index = nextIndex)
    def iterateDown: ActionTargetSelect = copy(index = prevIndex)
    def listLength: Int = list.length
    def action: (UIState, Option[InputAction]) = effect(list(index))
    def currentItem: ActionTargets.ActionTarget = list(index)
  }

  // Concrete list select for enemy targeting (for ranged attacks, spells, etc.)
  case class EnemyTargetSelect(
      list: Seq[Entity],
      index: Int = 0,
      effect: Entity => (UIState, Option[InputAction])
  ) extends ListSelectState {
    def iterate: EnemyTargetSelect = copy(index = nextIndex)
    def iterateDown: EnemyTargetSelect = copy(index = prevIndex)
    def listLength: Int = list.length
    def action: (UIState, Option[InputAction]) = effect(list(index))
    def currentItem: Entity = list(index)
  }

  case class InteractionState(
      entity: Entity,
      message: String,
      options: Seq[(String, ConversationAction)],
      selectedOption: Int = 0
  ) extends UIState {

    def selectNext: InteractionState =
      copy(selectedOption = (selectedOption + 1) % options.length)
    def selectPrevious: InteractionState = copy(selectedOption =
      (selectedOption - 1 + options.length) % options.length
    )

    def getSelectedOption: (String, ConversationAction) = options(
      selectedOption
    )
  }

  case class MainMenu(selectedOption: Int = 0) extends UIState {
    import game.save.SaveGameSystem

    val options: Seq[String] = {
      val baseOptions = Seq("New Adventure", "New Gauntlet")
      if (SaveGameSystem.hasSaveGame()) {
        baseOptions :+ "Continue Game"
      } else {
        baseOptions :+ "Continue Game (No Save)"
      }
    }

    def selectNext: MainMenu =
      copy(selectedOption = (selectedOption + 1) % options.length)
    def selectPrevious: MainMenu = copy(selectedOption =
      (selectedOption - 1 + options.length) % options.length
    )

    def getSelectedOption: String = options(selectedOption)

    def isOptionEnabled(index: Int): Boolean = {
      index match {
        case 0 | 1 => true // New Game options are always enabled
        case 2     =>
          SaveGameSystem
            .hasSaveGame() // Continue Game only enabled if save exists
        case _ => false
      }
    }

    def canConfirmCurrentSelection: Boolean = isOptionEnabled(selectedOption)
  }

  case object WorldMap extends UIState
}
