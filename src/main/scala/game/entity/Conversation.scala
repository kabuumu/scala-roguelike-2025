package game.entity

import game.entity.Entity

enum ConversationAction:
  case HealAction(amount: Int, cost: Int)
  case AcceptQuest(questId: String)
  case CompleteQuest(questId: String)

  case TradeAction // Keeping for backward compatibility if needed, but likely replaced by Buy/Sell
  case BuyAction
  case SellAction
  case CloseAction

case class ConversationChoice(text: String, action: ConversationAction)

case class Conversation(
    text: String,
    choices: Seq[ConversationChoice] = Seq(
      ConversationChoice("Leave", ConversationAction.CloseAction)
    )
) extends Component

object Conversation {
  extension (entity: Entity) {
    def conversation: Option[Conversation] = entity.get[Conversation]
    def hasConversation: Boolean = entity.has[Conversation]
  }
}
