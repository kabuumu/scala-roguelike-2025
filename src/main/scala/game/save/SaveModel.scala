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
 * Implicit ReadWriters contained within the save package to avoid polluting game code
 */
object SavePickle {
  // Basic ReadWriters for game types used only within save package
  implicit val pointRW: ReadWriter[Point] = readwriter[ujson.Value].bimap[Point](
    point => ujson.Obj("x" -> ujson.Num(point.x), "y" -> ujson.Num(point.y)),
    json => {
      val obj = json.obj
      Point(obj("x").num.toInt, obj("y").num.toInt)
    }
  )
  
  implicit val spriteRW: ReadWriter[Sprite] = readwriter[ujson.Value].bimap[Sprite](
    sprite => ujson.Obj("x" -> ujson.Num(sprite.x), "y" -> ujson.Num(sprite.y), "layer" -> ujson.Num(sprite.layer)),
    json => {
      val obj = json.obj
      Sprite(obj("x").num.toInt, obj("y").num.toInt, obj("layer").num.toInt)
    }
  )
  
  implicit val directionRW: ReadWriter[Direction] = readwriter[String].bimap[Direction](
    _.toString,
    str => Direction.valueOf(str)
  )
  
  implicit val entityTypeRW: ReadWriter[EntityType] = readwriter[String].bimap[EntityType](
    _.toString,
    str => str match {
      case "Player" => EntityType.Player
      case "Enemy" => EntityType.Enemy
      case "Wall" => EntityType.Wall
      case "Floor" => EntityType.Floor
      case "Projectile" => EntityType.Projectile
      case s if s.startsWith("LockedDoor(") =>
        val colorStr = s.substring(11, s.length - 1)
        val keyColour = colorStr match {
          case "Red" => KeyColour.Red
          case "Blue" => KeyColour.Blue
          case "Yellow" => KeyColour.Yellow
          case _ => KeyColour.Red
        }
        EntityType.LockedDoor(keyColour)
      case s if s.startsWith("Key(") =>
        val colorStr = s.substring(4, s.length - 1)
        val keyColour = colorStr match {
          case "Red" => KeyColour.Red
          case "Blue" => KeyColour.Blue
          case "Yellow" => KeyColour.Yellow
          case _ => KeyColour.Red
        }
        EntityType.Key(keyColour)
      case _ => EntityType.Player
    }
  )

  implicit val keyColourRW: ReadWriter[KeyColour] = readwriter[String].bimap[KeyColour](
    _.toString,
    str => str match {
      case "Red" => KeyColour.Red
      case "Blue" => KeyColour.Blue
      case "Yellow" => KeyColour.Yellow
      case _ => KeyColour.Red
    }
  )

  // ReadWriters for the persistence DTOs
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
 * Self-contained within save package using local ReadWriters.
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