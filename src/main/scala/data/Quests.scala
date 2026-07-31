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
      completionText = Some("Thank you so much! Our village is safe."),
      followUpQuestId = Some("kill_rats")
    ),
    "kill_rats" -> Quest(
      id = "kill_rats",
      title = "Rat Infestation",
      description = "Kill 3 rats near the village and return to the questgiver.",
      goal = KillEnemyGoal("Rat", 3),
      rewards = QuestRewards(experience = 300, coins = 50),
      giverName = Some("Quest Giver"),
      readyToCompleteText = Some("You killed them? Thank you!"),
      completionText = Some("The village is safe from rats now."),
      prerequisiteQuestIds = Set("retrieve_statue"),
      followUpQuestId = Some("slime_cleanup")
    ),
    "slime_cleanup" -> Quest(
      id = "slime_cleanup",
      title = "Slime Cleanup",
      description =
        "Clear 3 slimelets from the nearby dungeons and return to the quest giver.",
      goal = KillEnemyGoal("Slimelet", 3),
      rewards = QuestRewards(experience = 400, coins = 75),
      giverName = Some("Quest Giver"),
      readyToCompleteText = Some("The slimelets are gone? Excellent work!"),
      completionText = Some("Our dungeon routes are safer already."),
      prerequisiteQuestIds = Set("kill_rats"),
      followUpQuestId = Some("snake_hunt")
    ),
    "snake_hunt" -> Quest(
      id = "snake_hunt",
      title = "Snake Hunt",
      description =
        "Defeat 2 snakes in the deeper dungeons and return to the quest giver.",
      goal = KillEnemyGoal("Snake", 2),
      rewards = QuestRewards(experience = 600, coins = 125),
      giverName = Some("Quest Giver"),
      readyToCompleteText = Some("You survived the snakes? Remarkable!"),
      completionText = Some("You have made these lands much safer."),
      prerequisiteQuestIds = Set("slime_cleanup")
    )
  )
}
