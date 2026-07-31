package game.quest

import data.Items.ItemReference

enum QuestStatus:
  case Active, Completed, Failed

sealed trait QuestGoal
case class RetrieveItemGoal(itemReference: ItemReference, amount: Int = 1)
    extends QuestGoal
case class KillEnemyGoal(enemyType: String, amount: Int = 1)
    extends QuestGoal

case class QuestRewards(experience: Int, coins: Int)

case class Quest(
    id: String,
    title: String,
    description: String,
    goal: QuestGoal,
    status: QuestStatus = QuestStatus.Active,
    rewards: QuestRewards,
    giverName: Option[String] = None,
    readyToCompleteText: Option[String] = None,
    completionText: Option[String] = None
)

object QuestRepository {
  val quests: Map[String, Quest] = data.Quests.quests

  def get(id: String): Option[Quest] = quests.get(id)
}
