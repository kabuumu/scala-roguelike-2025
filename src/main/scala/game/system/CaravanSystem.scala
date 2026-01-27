package game.system

import game.GameState
import game.system.event.GameSystemEvent.{GameSystemEvent, InputEvent}
import game.entity.{
  Entity,
  CaravanComponent,
  CaravanState,
  Movement,
  NameComponent
}
import ui.InputAction
import util.Pathfinder
import game.Point
import map.Village
import game.entity.EntityType
import game.Direction
import game.entity.Initiative.*
import game.entity.Active
import game.entity.Movement.position

object CaravanSystem extends GameSystem {

  override def update(
      gameState: GameState,
      events: Seq[GameSystemEvent]
  ): (GameState, Seq[GameSystemEvent]) = {

    // Process Caravans - only those that are Active and Ready (initiative == 0)
    val caravans = gameState.entities.collect {
      case e if e.has[CaravanComponent] && e.has[Active] && e.isReady => e
    }

    if (caravans.isEmpty) return (gameState, Seq.empty)

    // Calculate actions for each caravan leader
    val (stateWithTrades, aiEvents) =
      caravans.foldLeft((gameState, Seq.empty[GameSystemEvent])) {
        case ((currentState, currentEvents), leader) =>
          val caravan = leader.get[CaravanComponent].get // Safe due to collect

          caravan.state match {
            case CaravanState.Idle =>
              handleIdleState(currentState, leader, caravan) match {
                case (newState, newEvents) =>
                  (newState, currentEvents ++ newEvents)
              }

            case CaravanState.Moving =>
              handleMovingState(currentState, leader, caravan) match {
                case (newState, newEvents) =>
                  (newState, currentEvents ++ newEvents)
              }

            case CaravanState.Trading =>
              handleTradingState(currentState, leader, caravan) match {
                case (newState, newEvents) =>
                  (newState, currentEvents ++ newEvents)
              }
          }
      }

    // Handle Followers (Pack Animals) - only those that are Active and Ready
    val followerEvents = handleFollowers(stateWithTrades, caravans)

    (stateWithTrades, aiEvents ++ followerEvents)
  }

  private def handleIdleState(
      gameState: GameState,
      leader: Entity,
      caravan: CaravanComponent
  ): (GameState, Seq[GameSystemEvent]) = {
    // Find a target village that is NOT the current one (if at one)
    val currentPos = leader.position

    // Find a random village to go to
    val villages = gameState.worldMap.villages
    if (villages.size < 2)
      return (gameState, Seq.empty) // Need at least 2 villages to trade between

    // Choose one that is decently far away or just random different one
    // Filter out villages very close to current pos (assume we are at one)
    val differentVillages = villages.filter(v =>
      v.centerLocation.getChebyshevDistance(currentPos) > 20
    )

    if (differentVillages.nonEmpty) {
      val target = differentVillages(
        scala.util.Random.nextInt(differentVillages.size)
      )

      val updatedCaravan = caravan.copy(
        state = CaravanState.Moving,
        targetVillageCenter = Some(target.centerLocation),
        waitTimer = 0
      )

      val updatedLeader = leader.update[CaravanComponent](_ => updatedCaravan)
      (gameState.updateEntity(leader.id, updatedLeader), Seq.empty)
    } else {
      (gameState, Seq.empty)
    }
  }

  private def handleMovingState(
      gameState: GameState,
      leader: Entity,
      caravan: CaravanComponent
  ): (GameState, Seq[GameSystemEvent]) = {
    val targetPos = caravan.targetVillageCenter.getOrElse(leader.position)

    // Check if arrived (within reasonable range of center)
    if (leader.position.getChebyshevDistance(targetPos) < 10) {
      // Arrived! Switch to Trading
      val updatedCaravan = caravan.copy(
        state = CaravanState.Trading,
        waitTimer = CaravanComponent.TradingDuration
      )
      val updatedLeader = leader.update[CaravanComponent](_ => updatedCaravan)

      return (gameState.updateEntity(leader.id, updatedLeader), Seq.empty)
    }

    // Move towards target
    // Use Pathfinder
    Pathfinder.getNextStep(leader.position, targetPos, gameState) match {
      case Some(nextStep) =>
        (gameState, Seq(InputEvent(leader.id, InputAction.Move(nextStep))))
      case None =>
        // Stuck? Wait.
        (gameState, Seq(InputEvent(leader.id, InputAction.Wait)))
    }
  }

  private def handleTradingState(
      gameState: GameState,
      leader: Entity,
      caravan: CaravanComponent
  ): (GameState, Seq[GameSystemEvent]) = {

    if (caravan.waitTimer > 0) {
      // Perform trade logic ONCE upon arrival (or periodically?)
      // Doing it once when timer is at max value for simplicity
      var updatedState = gameState

      if (caravan.waitTimer == CaravanComponent.TradingDuration) {
        // Execute Trade
        // Find the village we are at
        updatedState = gameState.worldMap.villages
          .find(v =>
            v.centerLocation.getChebyshevDistance(leader.position) < 20
          )
          .map { village =>
            // Buy Logic: If stockpile > 20, buy 5 items (or max available)
            val buyThreshold = 20
            val amountToBuy = 5

            if (village.stockpile > buyThreshold) {
              val amount = Math.min(
                amountToBuy,
                village.stockpile - buyThreshold
              ) // Keep at least threshold
              if (amount > 0) {
                // Update Village
                val newStockpile = village.stockpile - amount
                val wealthGain = amount * 2 // Arbitrary price 2 coins per crop
                val newWealth = village.wealth + wealthGain

                val updatedVillage =
                  village.copy(stockpile = newStockpile, wealth = newWealth)

                // Log message?
                val msg = s"Trader bought $amount crops from ${village.name}."

                val newMap = gameState.worldMap
                  .copy(villages = gameState.worldMap.villages.map {
                    case v if v == village => updatedVillage
                    case v                 => v
                  })

                gameState.copy(worldMap = newMap).addMessage(msg)
              } else gameState
            } else gameState
          }
          .getOrElse(gameState)
      }

      val updatedCaravan = caravan.copy(waitTimer = caravan.waitTimer - 1)
      val updatedLeader = leader.update[CaravanComponent](_ => updatedCaravan)

      (
        updatedState.updateEntity(leader.id, updatedLeader),
        Seq(InputEvent(leader.id, InputAction.Wait))
      )
    } else {
      // Done trading, switch to Idle to pick new target
      val updatedCaravan = caravan.copy(state = CaravanState.Idle)
      val updatedLeader = leader.update[CaravanComponent](_ => updatedCaravan)
      (gameState.updateEntity(leader.id, updatedLeader), Seq.empty)
    }
  }

  private def handleFollowers(
      gameState: GameState,
      caravans: Seq[Entity]
  ): Seq[GameSystemEvent] = {
    caravans.flatMap { leader =>
      val caravan = leader.get[CaravanComponent].get
      val followerIds = caravan.members.filterNot(_ == leader.id)

      followerIds.flatMap { fid =>
        gameState.getEntity(fid).map { follower =>
          // Follow Logic:
          // If distance to leader > 2, move towards leader
          val dist = follower.position.getChebyshevDistance(leader.position)

          if (dist > 2) {
            Pathfinder.getNextStep(
              follower.position,
              leader.position,
              gameState
            ) match {
              case Some(nextStep) =>
                InputEvent(follower.id, InputAction.Move(nextStep))
              case None => InputEvent(follower.id, InputAction.Wait)
            }
          } else {
            InputEvent(follower.id, InputAction.Wait)
          }
        }
      }
    }
  }
}
