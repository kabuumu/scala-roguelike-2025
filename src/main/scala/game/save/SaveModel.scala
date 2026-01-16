package game.save

import upickle.default.*
import ujson.Value
import game.*
import game.entity.*
import map.*

/** DTOs used only for persistence. Keeps save/load logic isolated from gameplay
  * logic.
  */
final case class PersistedGameState(
    playerEntityId: String,
    entities: Vector[PersistedEntity],
    messages: Vector[String],
    dungeon: PersistedDungeon,
    dungeonFloor: Int = 1,
    gameMode: String = "Adventure",
    worldSeed: Long = 0L
)

final case class PersistedEntity(
    id: String,
    components: Vector[SavedComponent]
)

/** A single component serialized as:
  *   - tag: stable string key (e.g. "Health", "Movement", ...)
  *   - data: the component payload as JSON AST
  */
final case class SavedComponent(tag: String, data: Value)

final case class PersistedRoomConnection(
    originRoom: (Int, Int),
    direction: String,
    destinationRoom: (Int, Int),
    isLocked: Boolean,
    keyColour: Option[String]
)

// Simplified persistence models that don't depend on complex game types
final case class PersistedDungeon(
    roomGrid: Vector[(Int, Int)], // Set[Point] -> Vector[(Int, Int)]
    roomConnections: Vector[PersistedRoomConnection],
    startPoint: (Int, Int),
    endpoint: Option[(Int, Int)],
    items: Vector[((Int, Int), String)],
    entranceSide: String,
    traderRoom: Option[(Int, Int)],
    hasBossRoom: Boolean,
    seed: Long
)

/** Local implicit ReadWriters for save package only - automatic derivation
  * where possible
  */
object SavePickle {
  // Automatic derivation for simple DTOs
  implicit val savedComponentRW: ReadWriter[SavedComponent] = macroRW
  implicit val persistedEntityRW: ReadWriter[PersistedEntity] = macroRW
  implicit val persistedRoomConnectionRW: ReadWriter[PersistedRoomConnection] =
    macroRW
  implicit val persistedDungeonRW: ReadWriter[PersistedDungeon] = macroRW
  implicit val persistedGameStateRW: ReadWriter[PersistedGameState] = macroRW
}

object SaveConversions {
  import SavePickle.*

  /** Convert a live GameState to a persistable DTO. All components that have a
    * registry entry will be saved.
    */
  def toPersisted(gs: GameState): PersistedGameState = {
    val persistedEntities = gs.entities.toVector.map { e =>
      val savedComps = e.components.values.toVector
        .flatMap(comp => ComponentRegistry.toSavedWithEntity(e, comp))
      PersistedEntity(e.id, savedComps)
    }

    // Convert complex WorldMap to simple PersistedDungeon
    // Extract primary dungeon if available, otherwise create a simple one
    val simpleDungeon = gs.worldMap.primaryDungeon match {
      case Some(dungeon) =>
        PersistedDungeon(
          roomGrid = dungeon.roomGrid.map(p => (p.x, p.y)).toVector,
          roomConnections = dungeon.roomConnections.map { rc =>
            PersistedRoomConnection(
              originRoom = (rc.originRoom.x, rc.originRoom.y),
              direction = rc.direction.toString,
              destinationRoom = (rc.destinationRoom.x, rc.destinationRoom.y),
              isLocked = rc.optLock.isDefined,
              keyColour = rc.optLock.map(_.keyColour.toString)
            )
          }.toVector,
          startPoint = (dungeon.startPoint.x, dungeon.startPoint.y),
          endpoint = dungeon.endpoint.map(p => (p.x, p.y)),
          items = dungeon.items.map { case (p, itemRef) =>
            ((p.x, p.y), itemRef.toString)
          }.toVector,
          entranceSide = dungeon.entranceSide.toString,
          traderRoom = dungeon.traderRoom.map(p => (p.x, p.y)),
          hasBossRoom = dungeon.hasBossRoom,
          seed = dungeon.seed
        )
      case None =>
        // No dungeon in world map, create minimal placeholder
        PersistedDungeon(
          roomGrid = Vector((0, 0)),
          roomConnections = Vector.empty,
          startPoint = (0, 0),
          endpoint = None,
          items = Vector.empty,
          entranceSide = "Left",
          traderRoom = None,
          hasBossRoom = false,
          seed = System.currentTimeMillis()
        )
    }

    PersistedGameState(
      playerEntityId = gs.playerEntityId,
      entities = persistedEntities,
      messages = gs.messages.toVector,
      dungeon = simpleDungeon,
      dungeonFloor = gs.dungeonFloor,
      gameMode = gs.gameMode.toString,
      worldSeed = gs.worldMap.seed
    )
  }

  /** Convert a persisted DTO back to a live GameState. Unknown component tags
    * are skipped with an error collection.
    */
  def fromPersisted(
      pgs: PersistedGameState
  ): Either[List[String], GameState] = {
    val (errors, entities) =
      pgs.entities.foldLeft(List.empty[String] -> Vector.empty[Entity]) {
        case ((errsAcc, entsAcc), pe) =>
          val (errs, comps) = pe.components.foldLeft(
            List.empty[String] -> Vector.empty[Component]
          ) { case ((ce, cc), sc) =>
            ComponentRegistry.fromSaved(sc) match {
              case Right(c)  => ce -> (cc :+ c)
              case Left(err) => (ce :+ err) -> cc
            }
          }
          val entity = Entity(
            id = pe.id,
            components = comps
              .map(c => c.getClass.asInstanceOf[Class[? <: Component]] -> c)
              .toMap
          )
          (errsAcc ++ errs) -> (entsAcc :+ entity)
      }

    // Reconstruct basic Dungeon from PersistedDungeon and wrap in WorldMap
    val roomGrid = pgs.dungeon.roomGrid.map { case (x, y) => Point(x, y) }.toSet

    val roomConnections = pgs.dungeon.roomConnections.map { prc =>
      import game.entity.EntityType.LockedDoor
      import game.entity.KeyColour

      val msgDirection = Direction.values
        .find(_.toString == prc.direction)
        .getOrElse(Direction.Right)
      val optLock: Option[LockedDoor] = if (prc.isLocked) {
        val colour = prc.keyColour
          .flatMap(c =>
            List(KeyColour.Red, KeyColour.Blue, KeyColour.Yellow)
              .find(_.toString == c)
          )
          .getOrElse(KeyColour.Red)
        Some(LockedDoor(colour))
      } else None

      map.RoomConnection(
        originRoom = Point(prc.originRoom._1, prc.originRoom._2),
        direction = msgDirection,
        destinationRoom = Point(prc.destinationRoom._1, prc.destinationRoom._2),
        optLock = optLock
      )
    }.toSet

    val items = pgs.dungeon.items.flatMap { case ((x, y), refStr) =>
      data.Items.ItemReference.values.find(_.toString == refStr).map { ref =>
        Point(x, y) -> ref
      }
    }.toSet

    val entranceSide = Direction.values
      .find(_.toString == pgs.dungeon.entranceSide)
      .getOrElse(Direction.Left)

    val basicDungeon = Dungeon(
      roomGrid = roomGrid,
      roomConnections = roomConnections,
      startPoint = Point(pgs.dungeon.startPoint._1, pgs.dungeon.startPoint._2),
      endpoint = pgs.dungeon.endpoint.map(p => Point(p._1, p._2)),
      items = items,
      entranceSide = entranceSide,
      traderRoom = pgs.dungeon.traderRoom.map(p => Point(p._1, p._2)),
      hasBossRoom = pgs.dungeon.hasBossRoom,
      seed = pgs.dungeon.seed
    )

    // Create a WorldMap that wraps the dungeon
    val worldMap = if (pgs.gameMode == "Gauntlet") {
      // Gauntlet mode: Just the dungeon, no outside world
      WorldMap(
        tiles = basicDungeon.tiles,
        dungeons = Seq(basicDungeon),
        paths = Set.empty,
        bridges = Set.empty,
        bounds =
          MapBounds(-20, 20, -20, 20), // Default bounds sufficient for gauntlet
        seed = pgs.worldSeed
      )
    } else {
      // Adventure mode: Regenerate the world terrain
      val worldBounds = MapBounds(-20, 20, -20, 20) // Match StartingState
      val config = WorldMapConfig(
        worldConfig = WorldConfig(
          bounds = worldBounds,
          grassDensity = 0.65,
          treeDensity = 0.20,
          dirtDensity = 0.10,
          ensureWalkablePaths = true,
          perimeterTrees = false,
          seed = pgs.worldSeed
        )
        // Defaults for dungeons/villages will apply
      )

      val baseWorldMap = WorldMapGenerator.generateWorldMap(config)

      // Merge the restored dungeon into the generated world
      // We replace the generated primary dungeon with our restored one
      // And we overlay the restored dungeon's tiles onto the world tiles

      // Note: We currently only persist one dungeon. If the world generated multiple,
      // we might want to keep the others? For now, we follow the pattern of
      // trusting the saved dungeon is the relevant one.

      baseWorldMap.copy(
        dungeons = Seq(basicDungeon) ++ baseWorldMap.dungeons.drop(
          1
        ), // Keep other dungeons if any?
        tiles = baseWorldMap.tiles ++ basicDungeon.tiles
      )
    }

    if (errors.nonEmpty) {
      // Log warnings but still return a valid game state
      println(s"Save deserialization warnings: ${errors.mkString(", ")}")
    }

    Right(
      GameState(
        playerEntityId = pgs.playerEntityId,
        entities = entities,
        messages = pgs.messages,
        worldMap = worldMap,
        dungeonFloor = pgs.dungeonFloor,
        gameMode =
          if (pgs.gameMode == "Gauntlet") GameMode.Gauntlet
          else GameMode.Adventure
      )
    )
  }
}

/** Public API for save/load JSON. Self-contained within save package using
  * automatic derivation where possible.
  */
object SaveGameJson {
  import SaveConversions.*
  import SavePickle.*

  def serialize(gs: GameState): String = {
    val persisted = toPersisted(gs)
    write(persisted)
  }

  def deserialize(json: String): GameState = {
    val persisted = read[PersistedGameState](json)
    fromPersisted(persisted) match {
      case Right(gameState) => gameState
      case Left(errors)     =>
        // Should not happen with current implementation but handle gracefully
        println(s"Save deserialization errors: ${errors.mkString(", ")}")
        throw new RuntimeException(
          s"Failed to deserialize save: ${errors.head}"
        )
    }
  }
}
