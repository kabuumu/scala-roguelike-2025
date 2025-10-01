package game.system

import game.{GameState, StartingState}
import game.entity.*
import game.entity.Movement.position
import game.entity.Inventory.inventoryItems
import game.system.event.GameSystemEvent
import game.system.event.GameSystemEvent.GameSystemEvent
import ui.InputAction
import map.{Dungeon, MapGenerator}

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
      // Generate new dungeon for next floor
      val newFloor = gameState.dungeonFloor + 1
      val dungeon = MapGenerator.generateDungeon(
        dungeonSize = 20, 
        lockedDoorCount = 3, 
        itemCount = 6
      )
      
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
        val nonStartRooms = dungeon.roomGrid - dungeon.startPoint
        val roomDepths = dungeon.roomDepths
        
        val enemiesAndAbilities = nonStartRooms.zipWithIndex.map { case (roomPoint, index) =>
          if (dungeon.hasBossRoom && dungeon.endpoint.contains(roomPoint)) {
            // Boss room
            StartingState.EnemyGeneration.createEnemiesForRoom(roomPoint, Int.MaxValue, index)
          } else {
            // Scale depth by floor number for increased difficulty
            val baseDepth = roomDepths.getOrElse(roomPoint, 1)
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
      
      // Create new game state with new dungeon and preserved player
      val newGameState = GameState(
        playerEntityId = newPlayer.id,
        entities = Vector(newPlayer) ++ playerInventoryEntities ++ newItems ++ newEnemies ++ newLockedDoors ++ newAbilities.values,
        dungeon = dungeon,
        dungeonFloor = newFloor,
        messages = Seq(s"Descended to dungeon floor $newFloor. Enemies grow stronger...")
      )
      
      (newGameState, Nil)
    }
  }
}
