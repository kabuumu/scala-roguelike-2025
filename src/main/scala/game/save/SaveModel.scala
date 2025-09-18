package game.save

import upickle.default.*
import ujson.Value
import game.*
import game.entity.*
import map.*

/**
 * DTOs used only for persistence. Keeps save/load logic isolated from gameplay logic.
 */
final case class PersistedGameState(
  playerEntityId: String,
  entities: Vector[PersistedEntity],
  messages: Vector[String],
  dungeon: PersistedDungeon
)

final case class PersistedEntity(
  id: String,
  components: Vector[SavedComponent]
)

/**
 * A single component serialized as:
 * - tag: stable string key (e.g. "Health", "Movement", ...)
 * - data: the component payload as JSON AST
 */
final case class SavedComponent(tag: String, data: Value)

// Simplified persistence models that don't depend on complex game types
final case class PersistedDungeon(
  roomGrid: Vector[(Int, Int)], // Set[Point] -> Vector[(Int, Int)]
  seed: Long
)

/**
 * Local implicit ReadWriters for save package only - automatic derivation where possible
 */
object SavePickle {
  // Automatic derivation for simple DTOs
  implicit val savedComponentRW: ReadWriter[SavedComponent] = macroRW
  implicit val persistedEntityRW: ReadWriter[PersistedEntity] = macroRW  
  implicit val persistedDungeonRW: ReadWriter[PersistedDungeon] = macroRW
  implicit val persistedGameStateRW: ReadWriter[PersistedGameState] = macroRW
}

object SaveConversions {
  import SavePickle.*

  /**
   * Convert a live GameState to a persistable DTO.
   * All components that have a registry entry will be saved.
   */
  def toPersisted(gs: GameState): PersistedGameState = {
    val persistedEntities = gs.entities.toVector.map { e =>
      val savedComps = e.components.values.toVector
        .flatMap(comp => ComponentRegistry.toSavedWithEntity(e, comp))
      PersistedEntity(e.id, savedComps)
    }

    // Convert complex Dungeon to simple PersistedDungeon
    val simpleDungeon = PersistedDungeon(
      roomGrid = gs.dungeon.roomGrid.map(p => (p.x, p.y)).toVector,
      seed = gs.dungeon.seed
    )

    PersistedGameState(
      playerEntityId = gs.playerEntityId,
      entities = persistedEntities,
      messages = gs.messages.toVector,
      dungeon = simpleDungeon
    )
  }

  /**
   * Convert a persisted DTO back to a live GameState.
   * Unknown component tags are skipped with an error collection.
   */
  def fromPersisted(pgs: PersistedGameState): Either[List[String], GameState] = {
    val (errors, entities) =
      pgs.entities.foldLeft(List.empty[String] -> Vector.empty[Entity]) {
        case ((errsAcc, entsAcc), pe) =>
          val (errs, comps) = pe.components.foldLeft(List.empty[String] -> Vector.empty[Component]) {
            case ((ce, cc), sc) =>
              ComponentRegistry.fromSaved(sc) match {
                case Right(c) => ce -> (cc :+ c)
                case Left(err) => (ce :+ err) -> cc
              }
          }
          val entity = Entity(id = pe.id, components = comps.map(c => c.getClass.asInstanceOf[Class[? <: Component]] -> c).toMap)
          (errsAcc ++ errs) -> (entsAcc :+ entity)
      }

    // Reconstruct basic Dungeon from PersistedDungeon
    val roomGrid = pgs.dungeon.roomGrid.map { case (x, y) => Point(x, y) }.toSet
    val basicDungeon = Dungeon(roomGrid = roomGrid, seed = pgs.dungeon.seed)

    if (errors.nonEmpty) {
      // Log warnings but still return a valid game state
      println(s"Save deserialization warnings: ${errors.mkString(", ")}")
    }
    
    Right(GameState(
      playerEntityId = pgs.playerEntityId,
      entities = entities,
      messages = pgs.messages,
      dungeon = basicDungeon
    ))
  }
}

/**
 * Public API for save/load JSON.
 * Self-contained within save package using automatic derivation where possible.
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
      case Left(errors) =>
        // Should not happen with current implementation but handle gracefully
        println(s"Save deserialization errors: ${errors.mkString(", ")}")
        throw new RuntimeException(s"Failed to deserialize save: ${errors.head}")
    }
  }
}