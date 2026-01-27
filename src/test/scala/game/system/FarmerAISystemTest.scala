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

  "FarmerAISystem" should "generate HarvestEvent for adjacent mature crop" in {
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
      NameComponent("Farmer")
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

    // 5. Verify Event
    events should contain(HarvestEvent("farmer", "crop"))
  }

  it should "handle HarvestEvent by resetting crop and adding item to inventory" in {
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

    // 2. Setup Farmer
    val farmerPos = Point(11, 10)
    val farmer = Entity(
      "farmer",
      Movement(farmerPos),
      EntityTypeComponent(Villager),
      Harvester(),
      Active(),
      Inventory(Nil, capacity = 5),
      NameComponent("Farmer")
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

    // 4. Run System with HarvestEvent
    val (newState, _) =
      FarmerAISystem.update(gameState, Seq(HarvestEvent("farmer", "crop")))

    // 5. Verify Crop Reset
    val updatedCrop = newState.getEntity("crop").get
    val updatedGrowth = updatedCrop.get[Growth].get
    updatedGrowth.currentStage should be(0)

    // 6. Verify Inventory Increased
    val updatedFarmer = newState.getEntity("farmer").get
    val inv = updatedFarmer.get[Inventory].get
    inv.itemEntityIds.size should be(1)
  }

  it should "move towards village center when inventory is full" in {
    // 1. Setup Farmer with FULL Inventory
    // We mock a full inventory by adding 5 dummy item IDs
    val fullInventory = Inventory(Seq("1", "2", "3", "4", "5"), capacity = 5)
    val farmerPos = Point(10, 10) // Far from village center (50, 50)
    val farmer = Entity(
      "farmer",
      Movement(farmerPos),
      EntityTypeComponent(Villager),
      Harvester(),
      Active(),
      fullInventory,
      NameComponent("Farmer")
    )

    // 3. Setup GameState
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
    // Should produce an input event to move
    events should have size 1
    events.head match {
      case InputEvent("farmer", ui.InputAction.Move(direction)) =>
        // Ideally should check direction, but checking type is good enough for now
        succeed
      case _ => fail("Expected InputEvent(Move)")
    }
  }

  it should "generate DropOffEvent when full and at village center" in {
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
      NameComponent("Farmer")
    )

    val player = Entity(EntityTypeComponent(EntityType.Player))
    val gameState = GameState(
      playerEntityId = player.id,
      entities = Seq(player, farmer),
      worldMap = dummyWorldMap
    )

    // 2. Run System
    val (_, events) = FarmerAISystem.update(gameState, Seq.empty)

    // 3. Verify DropOffEvent
    import game.system.event.GameSystemEvent.DropOffEvent
    events should contain(DropOffEvent("farmer"))
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
      NameComponent("Farmer")
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
