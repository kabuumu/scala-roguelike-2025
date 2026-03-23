package game.system

import game.GameState
import game.entity._
import game.system.event.GameSystemEvent.{
  GameSystemEvent,
  HarvestEvent,
  InputEvent,
  DropOffEvent
}
import data.Sprites
import game.Sprite
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import game.entity.Movement
import game.Point
import game.entity.EntityType.{Plant, Villager}
import game.entity.Inventory.inventoryItems
import map.{WorldMap, MapBounds}

class FarmerAISystemTest extends AnyFlatSpec with Matchers {

  // Dummy buildings to satisfy Village requirement
  val dummyBuilding1 =
    map.Building(Point(40, 40), 5, 5, map.BuildingType.Generic)
  val dummyBuilding2 =
    map.Building(Point(60, 60), 5, 5, map.BuildingType.Generic)

  // Minimal dummy village
  val dummyVillage = map.Village(
    buildings = Seq(dummyBuilding1, dummyBuilding2),
    centerLocation = Point(50, 50),
    paths = Set.empty,
    name = "Test Village",
    bounds = MapBounds(40, 60, 40, 60), // Fits center 50,50
    population = 10,
    stockpile = 0,
    cropsHarvested = 0
  )

  // Minimal dummy world map with village
  val dummyWorldMap = WorldMap(
    tiles = Map.empty,
    dungeons = Nil,
    shop = None,
    villages = Seq(dummyVillage),
    paths = Set.empty,
    bridges = Set.empty,
    bounds = MapBounds(0, 0, 100, 100),
    chunks = Map.empty,
    seed = 123L,
    processedRegions = Set.empty,
    lastCenterChunk = None
  )

  "FarmerAISystem" should "harvest adjacent mature crop directly (state change)" in {
    // 1. Setup Crop (Mature)
    val cropGrowth = Growth(
      currentStage = 3,
      maxStage = 3,
      growthTimer = 0,
      timePerStage = 10,
      stageSprites = Map(3 -> Sprites.cropStage3Sprite, 0 -> Sprites.dirtSprite)
    )
    val cropPos = Point(10, 10)
    val crop = Entity(
      "crop",
      Movement(cropPos),
      cropGrowth,
      EntityTypeComponent(Plant),
      Drawable(Sprites.cropStage3Sprite),
      NameComponent("Crop")
    )

    // 2. Setup Farmer (Adjacent)
    val farmerPos = Point(11, 10) // 1 tile away (adjacent)
    val farmer = Entity(
      "farmer",
      Movement(farmerPos),
      EntityTypeComponent(Villager),
      Harvester(),
      Active(),
      Inventory(Nil, capacity = 5),
      NameComponent("Farmer"),
      Initiative(10, 0) // Ready to act
    )

    // 3. Setup GameState
    val player = Entity(EntityTypeComponent(EntityType.Player))
    val gameState = GameState(
      playerEntityId = player.id,
      entities = Seq(
        player,
        crop,
        farmer
      ),
      worldMap = dummyWorldMap
    )

    // 4. Run System
    val (newState, _) = FarmerAISystem.update(gameState, Seq.empty)

    // 5. Verify crop was reset (system handles harvest directly now)
    val updatedCrop = newState.getEntity("crop").get
    val updatedGrowth = updatedCrop.get[Growth].get
    updatedGrowth.currentStage should be(0)

    // 6. Verify farmer inventory has 1 item
    val updatedFarmer = newState.getEntity("farmer").get
    val inv = updatedFarmer.get[Inventory].get
    inv.itemEntityIds.size should be(1)
  }

  it should "move towards nearest mature crop when not adjacent" in {
    // 1. Setup Crop (Mature) far from farmer
    val cropGrowth = Growth(
      currentStage = 3,
      maxStage = 3,
      growthTimer = 0,
      timePerStage = 10,
      stageSprites = Map(3 -> Sprites.cropStage3Sprite, 0 -> Sprites.dirtSprite)
    )
    val cropPos = Point(15, 10)
    val crop = Entity(
      "crop",
      Movement(cropPos),
      cropGrowth,
      EntityTypeComponent(Plant),
      Drawable(Sprites.cropStage3Sprite),
      NameComponent("Crop")
    )

    // 2. Setup Farmer (Far from crop)
    val farmerPos = Point(10, 10) // 5 tiles away
    val farmer = Entity(
      "farmer",
      Movement(farmerPos),
      EntityTypeComponent(Villager),
      Harvester(),
      Active(),
      Inventory(Nil, capacity = 5),
      NameComponent("Farmer"),
      Initiative(10, 0)
    )

    // 3. Setup GameState
    val player = Entity(EntityTypeComponent(EntityType.Player))
    val gameState = GameState(
      playerEntityId = player.id,
      entities = Seq(
        player,
        crop,
        farmer
      ),
      worldMap = dummyWorldMap
    )

    // 4. Run System
    val (_, events) = FarmerAISystem.update(gameState, Seq.empty)

    // 5. Verify movement event emitted
    events should have size 1
    events.head match {
      case InputEvent("farmer", ui.InputAction.Move(_)) =>
        succeed
      case _ => fail("Expected InputEvent(Move)")
    }
  }

  it should "move towards village center when inventory is full" in {
    // 1. Setup Farmer with FULL Inventory
    val fullInventory = Inventory(Seq("1", "2", "3", "4", "5"), capacity = 5)
    val farmerPos = Point(10, 10) // Far from village center (50, 50)
    val farmer = Entity(
      "farmer",
      Movement(farmerPos),
      EntityTypeComponent(Villager),
      Harvester(),
      Active(),
      fullInventory,
      NameComponent("Farmer"),
      Initiative(10, 0)
    )

    // 2. Setup GameState
    val player = Entity(EntityTypeComponent(EntityType.Player))
    val gameState = GameState(
      playerEntityId = player.id,
      entities = Seq(
        player,
        farmer
      ),
      worldMap = dummyWorldMap
    )

    // 3. Run System
    val (_, events) = FarmerAISystem.update(gameState, Seq.empty)

    // 4. Verify Movement Event
    events should have size 1
    events.head match {
      case InputEvent("farmer", ui.InputAction.Move(direction)) =>
        succeed
      case _ => fail("Expected InputEvent(Move)")
    }
  }

  it should "drop off items directly when full and at village center" in {
    // 1. Setup Farmer with FULL Inventory AT village center
    val fullInventory = Inventory(Seq("1", "2", "3", "4", "5"), capacity = 5)
    val farmerPos = Point(50, 50) // On top of village center
    val farmer = Entity(
      "farmer",
      Movement(farmerPos),
      EntityTypeComponent(Villager),
      Harvester(),
      Active(),
      fullInventory,
      NameComponent("Farmer"),
      Initiative(10, 0)
    )

    val player = Entity(EntityTypeComponent(EntityType.Player))
    val gameState = GameState(
      playerEntityId = player.id,
      entities = Seq(player, farmer),
      worldMap = dummyWorldMap
    )

    // 2. Run System — system handles drop-off directly via state change
    val (newState, _) = FarmerAISystem.update(gameState, Seq.empty)

    // 3. Verify Inventory Cleared
    val updatedFarmer = newState.getEntity("farmer").get
    val inv = updatedFarmer.get[Inventory].get
    inv.itemEntityIds should be(empty)

    // 4. Verify Village Stats Updated
    val updatedVillage = newState.worldMap.villages.head
    updatedVillage.stockpile should be(5)
    updatedVillage.cropsHarvested should be(5)
  }

  it should "handle DropOffEvent by clearing inventory and updating village stats" in {
    // 1. Setup Farmer with FULL Inventory
    val fullInventory = Inventory(Seq("1", "2", "3", "4", "5"), capacity = 5)
    val farmerPos = Point(50, 50)
    val farmer = Entity(
      "farmer",
      Movement(farmerPos),
      EntityTypeComponent(Villager),
      Harvester(),
      Active(),
      fullInventory,
      NameComponent("Farmer"),
      Initiative(10, 0)
    )

    val player = Entity(EntityTypeComponent(EntityType.Player))
    val gameState = GameState(
      playerEntityId = player.id,
      entities = Seq(player, farmer),
      worldMap = dummyWorldMap
    )

    // 2. Run System with DropOffEvent
    import game.system.event.GameSystemEvent.DropOffEvent
    val (newState, _) =
      FarmerAISystem.update(gameState, Seq(DropOffEvent("farmer")))

    // 3. Verify Inventory Cleared
    val updatedFarmer = newState.getEntity("farmer").get
    val inv = updatedFarmer.get[Inventory].get
    inv.itemEntityIds should be(empty)

    // 4. Verify Village Stats Updated
    val updatedVillage = newState.worldMap.villages.head
    updatedVillage.stockpile should be(5)
    updatedVillage.cropsHarvested should be(5)
  }
}
