package game

import org.scalatest.funsuite.AnyFunSuite
import data.Items

class StartingStateTest extends AnyFunSuite {

  test(
    "startAdventure should guarantee Golden Statue placement in closest dungeon"
  ) {
    // Generate a new game state
    val gameState = StartingState.startAdventure()

    // Check if Golden Statue exists in simple item list
    val allItems = gameState.worldMap.allItems
    val hasGoldenStatue = allItems.exists { case (_, itemRef) =>
      itemRef == Items.ItemReference.GoldenStatue
    }

    assert(
      hasGoldenStatue,
      "Golden Statue must be present in worldMap.allItems"
    )

    // Trace where it is
    val (itemPos, _) =
      allItems.find(_._2 == Items.ItemReference.GoldenStatue).get
    println(s"Golden Statue found at Room Coordinates: $itemPos")

    // Verify it is in the closest dungeon
    val closestDungeon = gameState.worldMap.dungeons.minBy { d =>
      val dx = d.startPoint.x
      val dy = d.startPoint.y
      dx * dx + dy * dy
    }

    println(s"Closest Dungeon Start: ${closestDungeon.startPoint}")
    println(s"Closest Dungeon Endpoint: ${closestDungeon.endpoint}")

    // The item should be in the closest dungeon
    val itemInClosestDungeon = closestDungeon.items.exists { case (pos, ref) =>
      ref == Items.ItemReference.GoldenStatue
    }

    assert(
      itemInClosestDungeon,
      s"Golden Statue should be in the closest dungeon (Start: ${closestDungeon.startPoint})"
    )
  }
}
