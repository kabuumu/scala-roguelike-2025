package game.system

import game.{GameState, StartingState}
import game.entity.*
import game.entity.Movement.position
import game.entity.Inventory.inventoryItems
import game.system.event.GameSystemEvent
import game.system.event.GameSystemEvent.GameSystemEvent
import ui.InputAction
import map.{Dungeon, MapGenerator, WorldMapGenerator, WorldMapConfig, WorldConfig, MapBounds, RiverConfig, DungeonConfig}

/**
 * Handles the player descending stairs to the next dungeon floor.
 * Generates a new dungeon with increased difficulty based on the current floor.
 */
object DescendStairsSystem extends GameSystem {
  override def update(gameState: GameState, events: Seq[GameSystemEvent]): (GameState, Seq[GameSystemEvent]) = {
    val descendEvents = events.collect {
      case GameSystemEvent.InputEvent(entityId, InputAction.DescendStairs) if entityId == gameState.playerEntityId =>
        entityId
    }
    
    if (descendEvents.isEmpty) {
      (gameState, Nil)
    } else {
      // Generate new dungeon for next floor using the new world map system
      val newFloor = gameState.dungeonFloor + 1
      val seed = System.currentTimeMillis() + newFloor
      
      val worldBounds = MapBounds(-20, 20, -20, 20)
      
      val worldMap = WorldMapGenerator.generateWorldMap(
        WorldMapConfig(
          worldConfig = WorldConfig(
            bounds = worldBounds,
            grassDensity = 0.60,
            treeDensity = 0.15,
            dirtDensity = 0.15,
            ensureWalkablePaths = true,
            perimeterTrees = true,
            seed = seed
          ),
          dungeonConfigs = Seq(
            // Dungeon size, items, and locked doors are automatically calculated from bounds
            DungeonConfig(
              bounds = worldBounds,
              seed = seed
            )
          ),
          riverConfigs = Seq(
            RiverConfig(
              startPoint = game.Point(-200, -150),
              flowDirection = (1, 1),
              length = 200,
              width = 1,
              curviness = 0.3,
              bounds = worldBounds,
              seed = seed
            ),
            RiverConfig(
              startPoint = game.Point(200, -150),
              flowDirection = (-1, 1),
              length = 200,
              width = 1,
              curviness = 0.25,
              bounds = worldBounds,
              seed = seed + 1
            )
          ),
          generatePathsToDungeons = false,
          generatePathsBetweenDungeons = true,
          pathsPerDungeon = 2,
          pathWidth = 1,
          minDungeonSpacing = 10
        )
      )
      
      // Use the dungeon with combined tiles from the world map
      val dungeon = worldMap.dungeons.head
      
      // Preserve player state but move to new dungeon start
      val currentPlayer = gameState.playerEntity
      val playerInventoryEntityIds = currentPlayer.inventoryItems(gameState).map(_.id).toSeq
      
      // Create new player entity at dungeon start with preserved stats
      val newPlayerPosition = game.Point(
        dungeon.startPoint.x * Dungeon.roomSize + Dungeon.roomSize / 2,
        dungeon.startPoint.y * Dungeon.roomSize + Dungeon.roomSize / 2
      )

      val newPlayer = currentPlayer.update[Movement](_.copy(position = newPlayerPosition))
      
      // Generate enemies for new floor using depth-based system with floor multiplier
      val (newEnemies, newAbilities) = {
        val dungeonRoomsOnly = dungeon.roomGrid.filterNot(room => 
          room == dungeon.startPoint
        )
        
        val enemiesAndAbilities = dungeonRoomsOnly.zipWithIndex.map { case (roomPoint, index) =>
          if (dungeon.hasBossRoom && dungeon.endpoint.contains(roomPoint)) {
            // Boss room
            StartingState.EnemyGeneration.createEnemiesForRoom(roomPoint, Int.MaxValue, index)
          } else {
            // Scale depth by floor number for increased difficulty
            val baseDepth = dungeon.roomDepths.getOrElse(roomPoint, 1)
            val scaledDepth = baseDepth + (newFloor - 1) * 3 // Add 3 depth levels per floor
            StartingState.EnemyGeneration.createEnemiesForRoom(roomPoint, scaledDepth, index)
          }
        }
        
        val allEnemies = enemiesAndAbilities.flatMap(_._1)
        val combinedAbilities = enemiesAndAbilities.flatMap(_._2).toMap
        
        (allEnemies.toSet, combinedAbilities)
      }
      
      // Generate items for new floor (using same logic as StartingState)
      val newItems: Set[Entity] = dungeon.items.zipWithIndex.map {
        case ((point, itemReference), index) =>
          val basePosition = game.Point(
            point.x * Dungeon.roomSize + Dungeon.roomSize / 2,
            point.y * Dungeon.roomSize + Dungeon.roomSize / 2
          )
          
          val itemEntity = itemReference.createEntity(s"item-floor${newFloor}-$index")
          val placedEntity = itemEntity.addComponent(Movement(position = basePosition))
          
          itemReference match {
            case data.Items.ItemReference.YellowKey => 
              placedEntity.addComponent(EntityTypeComponent(EntityType.Key(KeyColour.Yellow)))
            case data.Items.ItemReference.BlueKey => 
              placedEntity.addComponent(EntityTypeComponent(EntityType.Key(KeyColour.Blue)))
            case data.Items.ItemReference.RedKey => 
              placedEntity.addComponent(EntityTypeComponent(EntityType.Key(KeyColour.Red)))
            case _ =>
              placedEntity
          }
      }.toSet
      
      // Generate locked doors
      val newLockedDoors: Set[Entity] = dungeon.lockedDoors.map {
        case (point, lockedDoor) =>
          Entity(
            Movement(position = game.Point(point.x, point.y)),
            EntityTypeComponent(lockedDoor),
            Hitbox(),
            lockedDoor.keyColour match {
              case KeyColour.Yellow => Drawable(data.Sprites.yellowDoorSprite)
              case KeyColour.Blue => Drawable(data.Sprites.blueDoorSprite)
              case KeyColour.Red => Drawable(data.Sprites.redDoorSprite)
            }
          )
      }
      
      // Get player's inventory items from current game state
      val playerInventoryEntities = gameState.entities.filter(e => playerInventoryEntityIds.contains(e.id))
      
      // Spawn a trader in the dedicated trader room
      val newTrader: Entity = {
        val traderRoomPoint = dungeon.traderRoom.getOrElse(dungeon.startPoint)
        
        val traderPos = game.Point(
          traderRoomPoint.x * Dungeon.roomSize + Dungeon.roomSize / 2,
          traderRoomPoint.y * Dungeon.roomSize + Dungeon.roomSize / 2
        )
        data.Entities.trader(s"trader-floor-$newFloor", traderPos)
      }
      
      // Create new game state with new world map and preserved player
      val newGameState = GameState(
        playerEntityId = newPlayer.id,
        entities = Vector(newPlayer) ++ playerInventoryEntities ++ newItems ++ newEnemies ++ newLockedDoors ++ newAbilities.values :+ newTrader,
        worldMap = worldMap,
        dungeonFloor = newFloor,
        messages = Seq(s"Descended to dungeon floor $newFloor. Enemies grow stronger...")
      )
      
      (newGameState, Nil)
    }
  }
}
