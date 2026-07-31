package game.quest

import data.Items.ItemReference

enum QuestStatus:
  case Active, Completed, Failed

case class QuestState(
    status: QuestStatus,
    progressBaseline: Int = 0
)

enum QuestAvailability:
  case Locked(incompletePrerequisiteIds: Set[String])
  case Available, Active, Completed, Failed

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
    completionText: Option[String] = None,
    prerequisiteQuestIds: Set[String] = Set.empty,
    followUpQuestId: Option[String] = None
)

object QuestRepository {
  val quests: Map[String, Quest] = data.Quests.quests

  require(
    validate(quests).isRight,
    validate(quests).left.getOrElse("Invalid quest repository")
  )

  def get(id: String): Option[Quest] = quests.get(id)

  def validate(quests: Map[String, Quest]): Either[String, Unit] = {
    val keyMismatch = quests.collectFirst {
      case (key, quest) if key != quest.id =>
        s"Quest map key '$key' does not match quest id '${quest.id}'"
    }
    val invalidGoal = quests.values.collectFirst {
      case quest @ Quest(_, _, _, RetrieveItemGoal(_, amount), _, _, _, _, _, _, _)
          if amount <= 0 =>
        s"Quest '${quest.id}' goal amount must be positive"
      case quest @ Quest(_, _, _, KillEnemyGoal(_, amount), _, _, _, _, _, _, _)
          if amount <= 0 =>
        s"Quest '${quest.id}' goal amount must be positive"
    }
    val invalidRewards = quests.values.collectFirst {
      case quest
          if quest.rewards.experience < 0 || quest.rewards.coins < 0 =>
        s"Quest '${quest.id}' rewards must be non-negative"
    }
    val missingPrerequisite = quests.values.iterator
      .flatMap(quest =>
        quest.prerequisiteQuestIds.iterator
          .filterNot(quests.contains)
          .map(missing =>
            s"Quest '${quest.id}' has missing prerequisite '$missing'"
          )
      )
      .toSeq
      .headOption
    val missingFollowUp = quests.values.collectFirst {
      case quest
          if quest.followUpQuestId.exists(id => !quests.contains(id)) =>
        s"Quest '${quest.id}' has missing follow-up '${quest.followUpQuestId.get}'"
    }
    val inconsistentFollowUp = quests.values.collectFirst {
      case quest
          if quest.followUpQuestId.exists(nextId =>
            !quests(nextId).prerequisiteQuestIds.contains(quest.id)
          ) =>
        s"Quest '${quest.id}' follow-up must declare it as a prerequisite"
    }

    keyMismatch
      .orElse(invalidGoal)
      .orElse(invalidRewards)
      .orElse(missingPrerequisite)
      .orElse(missingFollowUp)
      .orElse(inconsistentFollowUp)
      .orElse(findDependencyCycle(quests))
      .toLeft(())
  }

  private def findDependencyCycle(
      quests: Map[String, Quest]
  ): Option[String] = {
    def visit(
        id: String,
        visiting: Set[String],
        visited: Set[String]
    ): Either[String, Set[String]] =
      if (visiting.contains(id)) {
        Left(s"Quest dependency cycle detected at '$id'")
      } else if (visited.contains(id)) {
        Right(visited)
      } else {
        quests(id).prerequisiteQuestIds.foldLeft[
          Either[String, Set[String]]
        ](Right(visited)) { (result, prerequisiteId) =>
          result.flatMap(currentVisited =>
            visit(prerequisiteId, visiting + id, currentVisited)
          )
        }.map(_ + id)
      }

    quests.keys
      .foldLeft[Either[String, Set[String]]](Right(Set.empty)) {
        (result, id) =>
          result.flatMap(visited => visit(id, Set.empty, visited))
      }
      .left
      .toOption
  }
}
