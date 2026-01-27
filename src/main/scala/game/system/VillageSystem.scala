package game.system

import game.GameState
import game.system.event.GameSystemEvent.{GameSystemEvent, MessageEvent}
import map.Village

object VillageSystem extends GameSystem {

  // Consumption logic
  // Update every 10 seconds (roughly 600 frames at 60fps)
  // But GameSystem update is per frame. We need a timer in GameState or internal to system (stateless systems tough).
  // Actually, we can use `gameState.messages` or store time in GameState?
  // Wait, `GrowthSystem` uses components on entities. Village is in WorldMap, not an entity.
  // We can just query `System.currentTimeMillis()` for "real time" updates or use frame count if available.
  // GameState doesn't seem to have valid frame count.
  // Let's use a simple counter attached to something? No.
  // Let's rely on standard update but keep state in... where?
  // We can update `Village` to have `lastUpdateTick`? No, immutable.
  // Let's just consume a tiny fraction every tick or use a probability.
  // Probability: 1/600 chance per frame per person to consume 1 food?
  // 600 frames = 10s.
  // If pop=10, 10 * (1/600) chance ~ 1 food every 60 frames (1s).

  // Better: Store `consumptionAccumulator` in Village? (Double)
  // Let's add `consumptionAccumulator` to Village in next pass if needed, or just use probability for now as it's easier.
  // Probability = 1.0 / (10 * 60) per person per frame.

  private val SecondsPerFood = 10
  private val UpdatesPerSecond = 60
  private val ConsumptionChance = 1.0 / (SecondsPerFood * UpdatesPerSecond)

  override def update(
      gameState: GameState,
      events: Seq[GameSystemEvent]
  ): (GameState, Seq[GameSystemEvent]) = {

    val worldMap = gameState.worldMap

    // Process each village
    // Check if map changed to avoid copying if no changes
    var mapChanged = false
    var messages = Seq.empty[GameSystemEvent]

    val newVillages = worldMap.villages.map { village =>
      // Calculate consumption for this tick
      // Simulate for each person? Or just bulk.
      // Expected consumption per tick = Population * Chance
      // We can interpret this as "Probability that ONE unit is consumed by the village"
      // is Pop * Chance. (Valid for small Pop * Chance << 1)

      val shouldConsume = scala.util.Random
        .nextDouble() < (village.population * ConsumptionChance)

      if (shouldConsume) {
        val newConsumed = village.cropsConsumed + 1
        val newStockpile = Math.max(0, village.stockpile - 1)

        mapChanged = true

        // Optional: Message if stockpile runs out?
        if (village.stockpile == 0 && newStockpile == 0) {
          // Starving? For now just track stats.
        }

        village.copy(
          stockpile = newStockpile,
          cropsConsumed = newConsumed
        )
      } else {
        village
      }
    }

    if (mapChanged) {
      val newState =
        gameState.copy(worldMap = worldMap.copy(villages = newVillages))

      // Log accumulated stats occasionally? No, UI will handle display.
      // But user requested "Keep the rate fairly consistent based on tests to check how many are consumed"
      // So verification is key.

      (newState, messages)
    } else {
      (gameState, Seq.empty)
    }
  }
}
