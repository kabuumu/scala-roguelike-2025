package data

import game.quest.{Quest, KillEnemyGoal, RetrieveItemGoal, QuestRewards}
import data.Items.ItemReference

object Quests {
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
    ),
    "kill_rats" -> Quest(
      id = "kill_rats",
      title = "Rat Infestation",
      description = "Kill 3 rats near the village and return to the questgiver.",
      goal = KillEnemyGoal("Rat", 3),
      rewards = QuestRewards(experience = 300, coins = 50),
      giverName = Some("Quest Giver"),
      readyToCompleteText = Some("You killed them? Thank you!"),
      completionText = Some("The village is safe from rats now.")
    )
  )
}
