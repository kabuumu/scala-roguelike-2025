package game.quest

import data.Items.ItemReference

enum QuestStatus:
  case Active, Completed, Failed

sealed trait QuestGoal
case class RetrieveItemGoal(itemReference: ItemReference, amount: Int = 1)
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
  val quests: Map[String, Quest] = Map(
    "retrieve_statue" -> Quest(
      id = "retrieve_statue",
      title = "The Missing Statue",
      description = "Retrieve the Golden Statue from the nearby cave.",
      goal = RetrieveItemGoal(ItemReference.GoldenStatue, 1),
      rewards = QuestRewards(experience = 500, coins = 100),
      giverName = Some("Elder"),
      readyToCompleteText = Some("You found it! Please, give it to me."),
      completionText = Some("Thank you so much! Our village is safe.")
    )
  )

  def get(id: String): Option[Quest] = quests.get(id)
}
