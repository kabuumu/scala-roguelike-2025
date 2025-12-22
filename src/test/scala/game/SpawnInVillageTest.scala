package game

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import game.StartingState
import map.Village
import game.entity.Movement
import game.entity.Movement.position

class SpawnInVillageTest extends AnyFreeSpec with Matchers {

  "StartingState" - {
    "should spawn the player within the bounds of the first village in Adventure mode" in {
      // Generate a new adventure state
      val gameState = StartingState.startAdventure()

      // Get the world map and player
      val worldMap = gameState.worldMap
      val player = gameState.playerEntity

      // Check if there are any villages (Adventure mode guarantees at least one in the first region)
      worldMap.villages should not be empty

      val firstVillage = worldMap.villages.head
      val villageCenter = firstVillage.centerLocation
      val playerPos = player.position

      // Define a reasonable radius for "in the village"
      // The village generation logic places buildings around the center.
      // The player should spawn near the center or within the village area.
      // Let's say within 25 tiles of the center (village bounds are roughly +/- 20 from center)
      val maxDistance = 25

      val distance = Math.sqrt(
        Math.pow(playerPos.x - villageCenter.x, 2) +
          Math.pow(playerPos.y - villageCenter.y, 2)
      )

      println(s"Village Center: $villageCenter")
      println(s"Player Position: $playerPos")
      println(s"Distance: $distance")

      // If existing logic (0,0) is used, and village is at e.g. (20,20), distance will be ~28.
      // If fixed logic is used, distance should be very small (near 0, or slightly shifted for safety).

      distance should be <= (maxDistance.toDouble)
    }
  }
}
