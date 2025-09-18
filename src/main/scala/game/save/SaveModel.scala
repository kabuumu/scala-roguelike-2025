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
  dungeon: Dungeon
) derives ReadWriter

final case class PersistedEntity(
  id: String,
  components: Vector[SavedComponent]
) derives ReadWriter

/**
 * A single component serialized as:
 * - tag: stable string key (e.g. "Health", "Movement", ...)
 * - data: the component payload as JSON AST
 */
final case class SavedComponent(tag: String, data: Value) derives ReadWriter

object SaveConversions {

  /**
   * Convert a live GameState to a persistable DTO.
   * All components that have a registry entry will be saved.
   */
  def toPersisted(gs: GameState): PersistedGameState = {
    val persistedEntities = gs.entities.toVector.map { e =>
      val savedComps =
        e.components.values.toVector
          .flatMap(comp => ComponentRegistry.toSavedWithEntity(e, comp)) // Use entity-aware method for Health
      PersistedEntity(e.id, savedComps)
    }

    PersistedGameState(
      playerEntityId = gs.playerEntityId,
      entities = persistedEntities,
      messages = gs.messages.toVector,
      dungeon = gs.dungeon
    )
  }

  /**
   * Convert a persisted DTO back to a live GameState.
   * Unknown component tags are skipped with an error collection; you can change this to hard-fail if you prefer.
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

    if (errors.nonEmpty) Left(errors)
    else Right(
      GameState(
        playerEntityId = pgs.playerEntityId,
        entities = entities,
        messages = pgs.messages,
        dungeon = pgs.dungeon
      )
    )
  }
}

/**
 * Public API for save/load JSON.
 * You can swap uPickle for another JSON lib if desired; only this file would change.
 */
object SaveGameJson {

  import SaveConversions.*

  def serialize(gs: GameState): String = {
    val persisted = toPersisted(gs)
    write(persisted)
  }

  def deserialize(json: String): GameState = {
    val persisted = read[PersistedGameState](json)
    fromPersisted(persisted) match {
      case Right(gameState) => gameState
      case Left(errors) =>
        // For now, log errors but don't fail - matches original behavior
        println(s"Warning: Some components could not be deserialized: ${errors.mkString(", ")}")
        // Return the game state anyway, just missing some components
        val persistedWithoutErrors = persisted
        GameState(
          playerEntityId = persistedWithoutErrors.playerEntityId,
          entities = persistedWithoutErrors.entities.map(pe => 
            Entity(pe.id, pe.components.flatMap(sc => ComponentRegistry.fromSaved(sc).toOption)
              .map(c => c.getClass.asInstanceOf[Class[? <: Component]] -> c).toMap)
          ),
          messages = persistedWithoutErrors.messages,
          dungeon = persistedWithoutErrors.dungeon
        )
    }
  }
}