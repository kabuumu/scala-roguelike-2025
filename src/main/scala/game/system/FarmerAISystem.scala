package game.system

import game.GameState
import game.entity.{
  Entity,
  Growth,
  Harvester,
  Inventory,
  Active,
  Movement,
  EntityType,
  Drawable,
  NameComponent,
  Health,
  Initiative
}
import game.entity.EntityType.Plant
import game.entity.Initiative.*
import game.system.event.GameSystemEvent.{
  GameSystemEvent,
  InputEvent,
  HarvestEvent,
  MessageEvent,
  DropOffEvent
}
import game.Point
import ui.InputAction
import util.Pathfinder
import game.Direction
import data.Sprites
import game.entity.Movement.position
import game.entity.EntityType.entityType

object FarmerAISystem extends GameSystem {

  // Harvest range (adjacent)
  private val HarvestRange = 1

  override def update(
      gameState: GameState,
      events: Seq[GameSystemEvent]
  ): (GameState, Seq[GameSystemEvent]) = {

    // 1. Process State Change Events (from other systems)
    val stateAfterEvents = events.foldLeft(gameState) {
      case (state, DropOffEvent(farmerId)) =>
        handleDropOff(state, farmerId)
      case (state, _) => state
    }

    // 2. Process AI (Decision Making) - only for farmers who are ready (initiative == 0)
    // We now directly modify state for harvests instead of emitting events
    val (finalState, aiEvents) = stateAfterEvents.entities.foldLeft(
      (stateAfterEvents, Seq.empty[GameSystemEvent])
    ) {
      case ((currentState, accEvents), farmer)
          if farmer.has[Harvester] && farmer.has[Active] && farmer.isReady =>
        val (newState, newEvents) = processFarmer(farmer, currentState)
        (newState, accEvents ++ newEvents)
      case ((currentState, accEvents), _) =>
        (currentState, accEvents)
    }

    (finalState, aiEvents)
  }

  private def handleHarvest(
      gameState: GameState,
      harvesterId: String,
      cropId: String
  ): GameState = {
    // Verify both entities exist
    (gameState.getEntity(harvesterId), gameState.getEntity(cropId)) match {
      case (Some(harvester), Some(crop)) =>
        // Verify crop is harvestable
        if (
          crop.entityType == Plant && crop
            .get[Growth]
            .exists(g => g.currentStage == g.maxStage)
        ) {
          // Reset crop growth and sprite
          val resetCrop = crop
            .update[Growth](g => g.copy(currentStage = 0))
            .update[Drawable](_ => Drawable(resetCropSprite(crop)))
            .resetInitiative() // Reset crop's initiative so it can grow again

          // Create item (abstracted as coin/wheat for now)
          val harvestedItem = data.Items.coin(
            s"wheat-${System.currentTimeMillis()}-${scala.util.Random.nextInt(1000)}"
          )

          // Update inventory and reset farmer's initiative
          val updatedHarvester = harvester
            .update[Inventory] { inv =>
              inv.addItemEntityId(harvestedItem.id)
            }
            .resetInitiative() // Reset initiative after action

          gameState
            .updateEntity(cropId, resetCrop)
            .updateEntity(harvesterId, updatedHarvester)
            .add(harvestedItem) // Add item to world so it exists
            .addMessage(
              s"${harvester.get[NameComponent].map(_.name).getOrElse("Farmer")} harvested ${crop.get[NameComponent].map(_.name).getOrElse("Crop")}"
            )

        } else gameState

      case _ => gameState
    }
  }

  private def handleDropOff(
      gameState: GameState,
      farmerId: String
  ): GameState = {
    gameState.getEntity(farmerId) match {
      case Some(farmer) =>
        val inventory = farmer.get[Inventory].getOrElse(Inventory())
        val itemCount = inventory.itemEntityIds.size

        if (itemCount > 0) {
          // Clear inventory and reset initiative
          val updatedFarmer =
            farmer
              .update[Inventory](_.copy(itemEntityIds = Nil))
              .resetInitiative()

          // Update Village Stats
          val farmerPos = farmer.position
          val newWorldMap = gameState.worldMap.villages
            .find(v =>
              farmerPos.x >= v.bounds.minRoomX && farmerPos.x <= v.bounds.maxRoomX &&
                farmerPos.y >= v.bounds.minRoomY && farmerPos.y <= v.bounds.maxRoomY
            )
            .map { village =>
              val updatedVillage = village.copy(
                stockpile = village.stockpile + itemCount,
                cropsHarvested = village.cropsHarvested + itemCount
              )
              val updatedVillages = gameState.worldMap.villages.map {
                case v if v == village => updatedVillage
                case v                 => v
              }
              gameState.worldMap.copy(villages = updatedVillages)
            }
            .getOrElse(gameState.worldMap)

          // Remove item entities from world
          val stateWithRemovedItems =
            inventory.itemEntityIds.foldLeft(gameState) { (state, id) =>
              state.remove(id)
            }

          stateWithRemovedItems
            .copy(worldMap = newWorldMap)
            .updateEntity(farmerId, updatedFarmer)
            .addMessage(s"Farmer deposited $itemCount crops at the village.")

        } else gameState

      case None => gameState
    }
  }

  private def resetCropSprite(crop: Entity): game.Sprite = {
    crop
      .get[Growth]
      .flatMap(_.stageSprites.get(0))
      .getOrElse(Sprites.dirtSprite)
  }

  // Returns (updated state, events to emit)
  private def processFarmer(
      farmer: Entity,
      gameState: GameState
  ): (GameState, Seq[GameSystemEvent]) = {
    farmer.get[Inventory] match {
      case Some(inventory) if inventory.isFull =>
        // 1. Inventory Full -> Return to Village
        findNearestVillageCenter(farmer.position, gameState) match {
          case Some(center) =>
            if (farmer.position.getChebyshevDistance(center) <= 1) {
              // At center -> Drop off directly
              val newState = handleDropOff(gameState, farmer.id)
              (newState, Seq.empty)
            } else {
              // Move to center
              Pathfinder.getNextStep(farmer.position, center, gameState) match {
                case Some(nextStep) =>
                  (
                    gameState,
                    Seq(InputEvent(farmer.id, InputAction.Move(nextStep)))
                  )
                case None =>
                  wander(farmer, gameState)
              }
            }
          case None =>
            wander(farmer, gameState)
        }

      case _ =>
        // 2. Inventory Check OK -> Harvest Logic
        processHarvesting(farmer, gameState)
    }
  }

  private def findNearestVillageCenter(
      pos: Point,
      gameState: GameState
  ): Option[Point] = {
    if (gameState.worldMap.villages.isEmpty) None
    else {
      Some(
        gameState.worldMap.villages
          .minBy(_.centerLocation.getChebyshevDistance(pos))
          .centerLocation
      )
    }
  }

  // Returns (updated state, events to emit)
  private def processHarvesting(
      farmer: Entity,
      gameState: GameState
  ): (GameState, Seq[GameSystemEvent]) = {
    // Scan for mature crops
    val allPlants = gameState.entities.filter(_.entityType == Plant)
    val matureCrops = allPlants.filter { e =>
      e.get[Growth].exists(g => g.currentStage == g.maxStage)
    }

    if (matureCrops.nonEmpty) {
      // Find nearest
      val nearestCrop =
        matureCrops.minBy(_.position.getChebyshevDistance(farmer.position))

      val distance = farmer.position.getChebyshevDistance(nearestCrop.position)

      if (distance <= HarvestRange) {
        // Harvest directly - don't emit event
        val newState = handleHarvest(gameState, farmer.id, nearestCrop.id)
        (newState, Seq.empty)
      } else {
        // Move towards crop
        Pathfinder.getNextStep(
          farmer.position,
          nearestCrop.position,
          gameState
        ) match {
          case Some(nextStep) =>
            (gameState, Seq(InputEvent(farmer.id, InputAction.Move(nextStep))))
          case None =>
            wander(farmer, gameState)
        }
      }
    } else {
      // Wander if no mature crops
      wander(farmer, gameState)
    }
  }

  private def wander(
      farmer: Entity,
      gameState: GameState
  ): (GameState, Seq[GameSystemEvent]) = {
    val neighbors = farmer.position.neighbors.filterNot(pos =>
      gameState.worldMap.staticMovementBlockingPoints.contains(pos) ||
        gameState.dynamicMovementBlockingPoints.contains(pos)
    )
    if (neighbors.nonEmpty) {
      val randomMove = neighbors(
        scala.util.Random.nextInt(neighbors.size)
      )
      (
        gameState,
        Seq(
          InputEvent(
            farmer.id,
            InputAction.Move(
              Direction.fromPoints(farmer.position, randomMove)
            )
          )
        )
      )
    } else {
      (gameState, Seq(InputEvent(farmer.id, InputAction.Wait)))
    }
  }

}
