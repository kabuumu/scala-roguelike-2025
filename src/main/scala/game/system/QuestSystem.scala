package game.system

import game.GameState
import game.entity.*
import game.entity.Coins.addCoins
import game.entity.EventMemory.*
import game.entity.Experience.addExperience
import game.entity.Inventory.{inventoryItems, removeItemEntity}
import game.quest.*
import game.system.event.GameSystemEvent
import game.system.event.GameSystemEvent.GameSystemEvent

object QuestSystem extends GameSystem {

  def hasQuestItem(
      gameState: GameState,
      itemRef: data.Items.ItemReference,
      amount: Int
  ): Boolean =
    questItemEntities(gameState, itemRef).size >= amount

  def availability(
      gameState: GameState,
      questId: String
  ): QuestAvailability =
    gameState.quests.get(questId) match {
      case Some(QuestState(QuestStatus.Active, _)) =>
        QuestAvailability.Active
      case Some(QuestState(QuestStatus.Completed, _)) =>
        QuestAvailability.Completed
      case Some(QuestState(QuestStatus.Failed, _)) =>
        QuestAvailability.Failed
      case None =>
        QuestRepository.get(questId) match {
          case Some(quest) =>
            val incomplete = quest.prerequisiteQuestIds.filterNot(
              gameState.isQuestCompleted
            )
            if (incomplete.isEmpty) QuestAvailability.Available
            else QuestAvailability.Locked(incomplete)
          case None => QuestAvailability.Locked(Set(questId))
        }
    }

  def progress(gameState: GameState, questId: String): Int =
    QuestRepository.get(questId).map(_.goal) match {
      case Some(RetrieveItemGoal(itemRef, _)) =>
        questItemEntities(gameState, itemRef).size
      case Some(KillEnemyGoal(enemyType, _)) =>
        val totalKills = matchingKillCount(gameState, enemyType)
        val baseline =
          gameState.quests.get(questId).map(_.progressBaseline).getOrElse(0)
        math.max(0, totalKills - baseline)
      case None => 0
    }

  def isGoalSatisfied(gameState: GameState, questId: String): Boolean =
    QuestRepository.get(questId).exists { quest =>
      val required = quest.goal match {
        case RetrieveItemGoal(_, amount) => amount
        case KillEnemyGoal(_, amount)    => amount
      }
      progress(gameState, questId) >= required
    }

  def acceptQuest(
      gameState: GameState,
      giver: Entity,
      questId: String
  ): (GameState, Seq[GameSystemEvent]) =
    QuestRepository.get(questId) match {
      case None =>
        (
          gameState.addMessage(s"Unknown quest: $questId"),
          Seq.empty
        )
      case Some(quest) =>
        availability(gameState, questId) match {
          case QuestAvailability.Available =>
            val baseline = quest.goal match {
              case KillEnemyGoal(enemyType, _) =>
                matchingKillCount(gameState, enemyType)
              case RetrieveItemGoal(_, _) => 0
            }
            val accepted = ensureObjectiveEnemies(
              gameState
              .acceptQuest(questId, baseline)
              .addMessage(s"Accepted quest: ${quest.title}"),
              giver,
              quest
            )
            val updated =
              if (isGoalSatisfied(accepted, questId))
                updateReadyDialogue(accepted, giver, quest)
                  .addMessage("You already meet the quest requirements!")
              else accepted
            (updated, Seq.empty)

          case QuestAvailability.Locked(incompleteIds) =>
            val titles = incompleteIds.toSeq.sorted.flatMap(
              QuestRepository.get
            ).map(_.title)
            val requirements =
              if (titles.nonEmpty) titles.mkString(", ")
              else incompleteIds.toSeq.sorted.mkString(", ")
            (
              gameState.addMessage(
                s"Complete the following quest first: $requirements"
              ),
              Seq.empty
            )

          case _ => (gameState, Seq.empty)
        }
    }

  def completeQuest(
      gameState: GameState,
      giver: Entity,
      questId: String
  ): (GameState, Seq[GameSystemEvent]) =
    if (!gameState.isQuestActive(questId)) {
      (gameState, Seq.empty)
    } else {
      QuestRepository.get(questId) match {
        case None =>
          (
            gameState.addMessage(s"Unknown quest: $questId"),
            Seq.empty
          )
        case Some(quest) if !isGoalSatisfied(gameState, questId) =>
          (
            gameState.addMessage("The quest requirements are not complete."),
            Seq.empty
          )
        case Some(quest) =>
          val (entitiesAfterConsumption, playerAfterConsumption) =
            consumeGoalItems(gameState, quest)
          val rewardedPlayer = playerAfterConsumption
            .addCoins(quest.rewards.coins)
            .addExperience(quest.rewards.experience)
          val completionText = quest.completionText.getOrElse(
            "Thank you for your help."
          )
          val choices = completionChoices(quest)
          val updatedGiver = giver.update[Conversation](_ =>
            Conversation(completionText, choices)
          )
          val completed = gameState
            .copy(entities = entitiesAfterConsumption)
            .updateEntity(rewardedPlayer.id, rewardedPlayer)
            .updateEntity(updatedGiver.id, updatedGiver)
            .completeQuest(questId)
            .addMessage(s"Completed quest: ${quest.title}!")
            .addMessage(
              s"Received ${quest.rewards.coins} coins and ${quest.rewards.experience} XP!"
            )

          (completed, Seq.empty)
      }
    }

  override def update(
      gameState: GameState,
      events: Seq[GameSystemEvent]
  ): (GameState, Seq[GameSystemEvent]) = {
    val updated = gameState.quests.foldLeft(gameState) {
      case (
            currentState,
            (questId, QuestState(QuestStatus.Active, _))
          ) if isGoalSatisfied(currentState, questId) =>
        QuestRepository.get(questId) match {
          case Some(quest) =>
            quest.giverName
              .flatMap(giverName =>
                currentState.entities.find(
                  _.get[NameComponent].exists(_.name == giverName)
                )
              )
              .map { giver =>
                val readyText = quest.readyToCompleteText.getOrElse(
                  "The quest is ready to complete."
                )
                if (giver.get[Conversation].exists(_.text != readyText)) {
                  updateReadyDialogue(currentState, giver, quest)
                    .addMessage(
                      "Quest Updated: Return to the quest giver!"
                    )
                } else currentState
              }
              .getOrElse(currentState)
          case None => currentState
        }
      case (currentState, _) => currentState
    }

    (updated, Seq.empty)
  }

  private def matchingKillCount(
      gameState: GameState,
      enemyType: String
  ): Int =
    gameState.playerEntity
      .getMemoryEventsByType[MemoryEvent.EnemyDefeated]
      .count(_.enemyType == enemyType)

  private def questItemEntities(
      gameState: GameState,
      itemRef: data.Items.ItemReference
  ): Seq[Entity] =
    gameState.playerEntity.inventoryItems(gameState).filter(
      _.get[NameComponent].exists(_.name == itemRef.name)
    )

  private def updateReadyDialogue(
      gameState: GameState,
      giver: Entity,
      quest: Quest
  ): GameState = {
    val readyText = quest.readyToCompleteText.getOrElse(
      "The quest is ready to complete."
    )
    val actionText = quest.goal match {
      case RetrieveItemGoal(_, _) => "Give Item"
      case KillEnemyGoal(_, _)    => "Complete Quest"
    }
    val updatedGiver = giver.update[Conversation](_ =>
      Conversation(
        readyText,
        Seq(
          ConversationChoice(
            actionText,
            ConversationAction.CompleteQuest(quest.id)
          ),
          ConversationChoice("Not yet", ConversationAction.CloseAction)
        )
      )
    )
    gameState.updateEntity(giver.id, updatedGiver)
  }

  private def consumeGoalItems(
      gameState: GameState,
      quest: Quest
  ): (Seq[Entity], Entity) =
    quest.goal match {
      case RetrieveItemGoal(itemRef, amount) =>
        val itemIds =
          questItemEntities(gameState, itemRef).take(amount).map(_.id).toSet
        val remainingEntities =
          gameState.entities.filterNot(entity => itemIds.contains(entity.id))
        val player = itemIds.foldLeft(gameState.playerEntity)(
          (currentPlayer, itemId) => currentPlayer.removeItemEntity(itemId)
        )
        (remainingEntities, player)
      case KillEnemyGoal(_, _) =>
        (gameState.entities, gameState.playerEntity)
    }

  private def completionChoices(quest: Quest): Seq[ConversationChoice] = {
    val followUpChoice = quest.followUpQuestId
      .flatMap(QuestRepository.get)
      .filter(_.giverName == quest.giverName)
      .map(next =>
        ConversationChoice(
          "What else can I do?",
          ConversationAction.AcceptQuest(next.id)
        )
      )
      .toSeq
    followUpChoice :+
      ConversationChoice("Goodbye", ConversationAction.CloseAction)
  }

  private def ensureObjectiveEnemies(
      gameState: GameState,
      giver: Entity,
      quest: Quest
  ): GameState =
    quest.goal match {
      case KillEnemyGoal(enemyType, amount) =>
        val existingCount = gameState.entities.count(
          _.get[EnemyTypeComponent].exists(
            _.enemyType.toString == enemyType
          )
        )
        val missingCount = math.max(0, amount - existingCount)
        val occupied =
          gameState.entities.flatMap(_.get[Movement].map(_.position)).toSet
        val origin = giver.get[Movement].map(_.position).getOrElse(
          gameState.playerEntity.get[Movement].map(_.position).get
        )
        val positions = gameState.worldMap.tiles.iterator
          .collect {
            case (point, tileType)
                if isSpawnable(tileType) && !occupied.contains(point) =>
              point
          }
          .toSeq
          .sortBy(point =>
            (
              point.getChebyshevDistance(origin),
              point.x,
              point.y
            )
          )
          .take(missingCount)

        positions.zipWithIndex.foldLeft(gameState) {
          case (state, (position, index)) =>
            val id =
              s"quest-${quest.id}-${enemyType.toLowerCase}-$index-${gameState.worldMap.seed}"
            enemyType match {
              case "Rat" =>
                state.add(data.Enemies.rat(id, position))
              case "Slimelet" =>
                state.add(data.Enemies.slimelet(id, position))
              case "Snake" =>
                val spitId = s"$id-spit"
                state
                  .add(data.Items.snakeSpit(spitId))
                  .add(data.Enemies.snake(id, position, spitId))
              case _ => state
            }
        }
      case RetrieveItemGoal(_, _) => gameState
    }

  private def isSpawnable(tileType: map.TileType): Boolean =
    tileType match {
      case map.TileType.Floor | map.TileType.MaybeFloor |
          map.TileType.Dirt | map.TileType.Grass1 |
          map.TileType.Grass2 | map.TileType.Grass3 |
          map.TileType.Bridge =>
        true
      case _ => false
    }
}
