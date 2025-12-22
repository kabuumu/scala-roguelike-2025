package game.system

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import game.entity.*
import game.GameState
import game.Point
import game.system.event.GameSystemEvent
import ui.InputAction
import data.Sprites
import game.StartingState
import map.WorldMap
import map.MapBounds

class InventorySystemTest extends AnyFunSpec with Matchers {

  describe("InventorySystem") {
    it(
      "should preserve Drawable component when dropping and picking up an item"
    ) {
      // 1. Setup GameState with a player and an item in inventory
      val playerId = "player-1"
      val itemEntityId = "item-1"

      val itemEntity = Entity(
        id = itemEntityId,
        Drawable(Sprites.chainmailArmorSprite), // The problematic sprite
        Inventory(), // Should not need inventory but just in case
        CanPickUp(),
        game.entity.NameComponent("Chainmail Armor", "Test Armor")
      )

      val playerEntity = Entity(
        id = playerId,
        Movement(position = Point(0, 0)),
        Inventory(itemEntityIds = Seq(itemEntityId)),
        EntityTypeComponent(EntityType.Player),
        game.entity.Equipment()
      )

      val worldMap = WorldMap(
        tiles = Map(
          Point(0, 0) -> map.TileType.Floor,
          Point(0, 1) -> map.TileType.Floor // Adjacent spot for drop
        ),
        dungeons = Seq.empty,
        paths = Set.empty,
        bridges = Set.empty,
        bounds = MapBounds(0, 10, 0, 10)
      )

      val initialState = GameState(
        playerEntityId = playerId,
        entities = Seq(playerEntity, itemEntity),
        worldMap = worldMap
      )

      // 2. Drop the item
      val dropEvent =
        GameSystemEvent.InputEvent(playerId, InputAction.DropItem(itemEntityId))

      val (stateAfterDrop, _) =
        InventorySystem.update(initialState, Seq(dropEvent))

      val droppedItem = stateAfterDrop.getEntity(itemEntityId).get

      // Verify item moved to adjacent tile
      droppedItem.has[Movement] shouldBe true
      droppedItem.get[Movement].get.position should not be Point(
        0,
        0
      ) // Should be nearby

      // Verify Drawable is still there
      droppedItem.has[Drawable] shouldBe true

      val expectedSprite = Sprites.chainmailArmorSprite
      val hasSprite =
        droppedItem.get[Drawable].get.sprites.exists(_._2 == expectedSprite)
      hasSprite shouldBe true

      // 3. Pick up the item
      // Simulate collision
      val pickupEvent = GameSystemEvent.CollisionEvent(
        playerId,
        GameSystemEvent.CollisionTarget.Entity(itemEntityId)
      )

      // Move player to item position first to simulate real conditions
      val dropPos = droppedItem.get[Movement].get.position
      val playerAtItem = stateAfterDrop
        .getEntity(playerId)
        .get
        .update[Movement](_.copy(position = dropPos))
      val stateReadyForPickup =
        stateAfterDrop.updateEntity(playerId, playerAtItem)

      val (stateAfterPickup, _) =
        InventorySystem.update(stateReadyForPickup, Seq(pickupEvent))

      val pickedUpItem = stateAfterPickup.getEntity(itemEntityId).get

      // Verify Movement is gone
      pickedUpItem.has[Movement] shouldBe false

      // Verify Drawable is STILL there
      pickedUpItem.has[Drawable] shouldBe true
      val hasSpriteAfterPickup =
        pickedUpItem.get[Drawable].get.sprites.exists(_._2 == expectedSprite)
      hasSpriteAfterPickup shouldBe true
    }
  }
}
