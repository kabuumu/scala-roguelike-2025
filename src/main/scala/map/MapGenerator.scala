package map

import game.{Direction, Point}
import scala.annotation.tailrec

object MapGenerator {
  
  /** Maximum iterations for dungeon generation before declaring configuration impossible */
  private val MaxGenerationIterations = 10000
  
  /**
   * Generates a dungeon with configurable parameters.
   * This is the new parameterized API that supports bounds and entrance side configuration.
   * 
   * @param config DungeonConfig specifying all dungeon parameters
   * @return Generated Dungeon with outdoor area
   */
  def generateDungeon(config: DungeonConfig): Dungeon = {
    val startTime = System.currentTimeMillis()

    val mutators: Set[DungeonMutator] = Set(
      new EndPointMutator(config.size),
      new TraderRoomMutator(config.size),
      new KeyLockMutator(config.lockedDoorCount, config.size),
      new TreasureRoomMutator(config.itemCount, config.size),
      new BossRoomMutator(config.size)
    )

    @tailrec
    def recursiveGenerator(openDungeons: Set[Dungeon], iterations: Int = 0): Dungeon = {
      if (openDungeons.isEmpty) {
        throw new IllegalStateException(
          s"Cannot generate dungeon: bounds too restrictive or configuration impossible. " +
          s"Bounds: ${config.bounds.map(_.describe).getOrElse("None")}, size: ${config.size}"
        )
      }
      
      if (iterations > MaxGenerationIterations) {
        throw new IllegalStateException(
          s"Dungeon generation exceeded maximum iterations ($MaxGenerationIterations). " +
          s"Configuration may be impossible to satisfy: ${config.bounds.map(_.describe).getOrElse("None")}, size: ${config.size}"
        )
      }
      
      val currentDungeon: Dungeon = openDungeons.maxBy( dungeon =>
        dungeon.roomGrid.size + dungeon.lockedDoorCount + dungeon.nonKeyItems.size
      )

      // Generate mutations that respect bounds configuration
      val newOpenDungeons: Set[Dungeon] = for {
        mutator <- mutators
        possibleDungeon <- mutator.getPossibleMutations(currentDungeon, config)
      } yield possibleDungeon
      
      newOpenDungeons.find(dungeon =>
        dungeon.lockedDoorCount == config.lockedDoorCount
          && dungeon.nonKeyItems.size == config.itemCount
          && dungeon.roomGrid.size == config.size
          && dungeon.traderRoom.isDefined
          && dungeon.hasBossRoom
      ) match {
        case Some(completedDungeon) =>
          completedDungeon
        case None =>
          recursiveGenerator(newOpenDungeons ++ openDungeons - currentDungeon, iterations + 1)
      }
    }

    // Start from configured entrance room if bounds are specified
    val startRoom = config.bounds match {
      case Some(_) => config.getEntranceRoom
      case None => Point(0, 0)
    }
    
    val baseDungeon = recursiveGenerator(Set(Dungeon(startPoint = startRoom, seed = config.seed)))

    println("Generating Dungeon (Configured)")
    println(s"  Bounds: ${config.bounds.map(_.describe).getOrElse("Unbounded")}")
    println(s"  Entrance side: ${config.entranceSide}")
    println(s"  Generated dungeon with ${baseDungeon.roomGrid.size} rooms, " +
      s"${baseDungeon.roomConnections.size} connections, " +
      s"${baseDungeon.lockedDoorCount} locked doors, " +
      s"${baseDungeon.nonKeyItems.size} items")

    println(s"  Completed dungeon with config took ${System.currentTimeMillis() - startTime}ms")

    baseDungeon
  }
  
  /**
   * Generates a dungeon without the hardcoded outdoor rooms.
   * This is used by WorldMapGenerator to place dungeons directly on procedural terrain.
   * 
   * @param config DungeonConfig specifying all dungeon parameters
   * @return Generated Dungeon without outdoor rooms
   */
  def generateDungeonWithoutOutdoorRooms(config: DungeonConfig): Dungeon = {
    val mutators: Set[DungeonMutator] = Set(
      new EndPointMutator(config.size),
      new TraderRoomMutator(config.size),
      new KeyLockMutator(config.lockedDoorCount, config.size),
      new TreasureRoomMutator(config.itemCount, config.size),
      new BossRoomMutator(config.size)
    )

    @tailrec
    def recursiveGenerator(openDungeons: Set[Dungeon], iterations: Int = 0): Dungeon = {
      if (openDungeons.isEmpty) {
        throw new IllegalStateException(
          s"Cannot generate dungeon: configuration impossible. " +
          s"Size: ${config.size}"
        )
      }
      
      if (iterations > MaxGenerationIterations) {
        throw new IllegalStateException(
          s"Dungeon generation exceeded maximum iterations ($MaxGenerationIterations). " +
          s"Configuration may be impossible to satisfy: size: ${config.size}"
        )
      }
      
      val currentDungeon: Dungeon = openDungeons.maxBy( dungeon =>
        dungeon.roomGrid.size + dungeon.lockedDoorCount + dungeon.nonKeyItems.size
      )

      val newOpenDungeons: Set[Dungeon] = for {
        mutator <- mutators
        possibleDungeon <- mutator.getPossibleMutations(currentDungeon)
      } yield possibleDungeon
      
      newOpenDungeons.find(dungeon =>
        dungeon.lockedDoorCount == config.lockedDoorCount
          && dungeon.nonKeyItems.size == config.itemCount
          && dungeon.roomGrid.size == config.size
          && dungeon.traderRoom.isDefined
          && dungeon.hasBossRoom
      ) match {
        case Some(completedDungeon) =>
          completedDungeon
        case None =>
          recursiveGenerator(newOpenDungeons ++ openDungeons - currentDungeon, iterations + 1)
      }
    }

    // Generate dungeon without outdoor rooms - it will sit directly on the terrain
    val baseDungeon = recursiveGenerator(Set(Dungeon(seed = config.seed)))
    
    // Find an edge room to use as the entrance
    // An edge room is one that has fewer connections than interior rooms
    val edgeRoom = findEdgeRoomForEntrance(baseDungeon)
    
    // Update the dungeon to use the edge room as the start point
    baseDungeon.copy(startPoint = edgeRoom)
  }
  
  /**
   * Finds a room on the edge of the dungeon to use as the entrance.
   * An edge room is defined as a room with fewer than 3 connections to other rooms.
   */
  private def findEdgeRoomForEntrance(dungeon: Dungeon): Point = {
    // Count connections for each room
    val roomConnectionCounts = dungeon.roomGrid.map { room =>
      val connectionCount = dungeon.roomConnections.count(_.originRoom == room)
      (room, connectionCount)
    }
    
    // Find rooms with the fewest connections (edge rooms)
    val minConnections = roomConnectionCounts.map(_._2).min
    val edgeRooms = roomConnectionCounts.filter(_._2 == minConnections).map(_._1).toSeq
    
    // If current startPoint is already an edge room, keep it
    if (edgeRooms.contains(dungeon.startPoint)) {
      dungeon.startPoint
    } else {
      // Otherwise, pick an edge room (preferably one closest to origin for consistency)
      edgeRooms.minBy(room => room.x * room.x + room.y * room.y)
    }
  }
  
  /**
   * Backward-compatible dungeon generation API.
   * This maintains compatibility with existing code that uses the old signature.
   */
  def generateDungeon(dungeonSize: Int, lockedDoorCount: Int = 0, itemCount: Int = 0, seed: Long = System.currentTimeMillis()): Dungeon = {
    val startTime = System.currentTimeMillis()

    val mutators: Set[DungeonMutator] = Set(
      new EndPointMutator(dungeonSize),
      new TraderRoomMutator(dungeonSize),
      new KeyLockMutator(lockedDoorCount, dungeonSize),
      new TreasureRoomMutator(itemCount, dungeonSize),
      new BossRoomMutator(dungeonSize)
    )

    @tailrec
    def recursiveGenerator(openDungeons: Set[Dungeon]): Dungeon = {
      val currentDungeon: Dungeon = openDungeons.maxBy( dungeon =>
        dungeon.roomGrid.size + dungeon.lockedDoorCount + dungeon.nonKeyItems.size
      )

      val newOpenDungeons: Set[Dungeon] = for {
        mutator <- mutators
        possibleDungeon <- mutator.getPossibleMutations(currentDungeon)
      } yield possibleDungeon
      
      newOpenDungeons.find(dungeon =>
        dungeon.lockedDoorCount == lockedDoorCount
          && dungeon.nonKeyItems.size == itemCount
//          && dungeon.dungeonPath.size == dungeonPathSize
          && dungeon.roomGrid.size == dungeonSize
          && dungeon.traderRoom.isDefined
          && dungeon.hasBossRoom
      ) match {
        case Some(completedDungeon) =>
          completedDungeon
        case None =>
          recursiveGenerator(newOpenDungeons ++ openDungeons - currentDungeon)
      }
    }

    val dungeon = recursiveGenerator(Set(Dungeon(seed = seed)))

    println("Generating Dungeon")
    println(s"  Generated dungeon with ${dungeon.roomGrid.size} rooms, " +
      s"${dungeon.roomConnections.size} connections, " +
      s"${dungeon.lockedDoorCount} locked doors, " +
      s"${dungeon.nonKeyItems.size} items")

    println(s"  Completed dungeon with config took ${System.currentTimeMillis() - startTime}ms")

    dungeon
  }
}
