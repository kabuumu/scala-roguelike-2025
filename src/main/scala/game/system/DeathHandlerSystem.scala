package game.system

import data.DeathEvents.DeathEventReference.{GiveExperience, SpawnEntity, DropCoins}
import data.Entities.EntityReference
import game.GameState
import game.entity.{Collision, DeathEvents, EntityType, MarkedForDeath, Movement}
import game.entity.EntityType.entityType
import game.system.event.GameSystemEvent
import game.system.event.GameSystemEvent.GameSystemEvent

object DeathHandlerSystem extends GameSystem {
  override def update(gameState: GameState, events: Seq[GameSystemEvent]): (GameState, Seq[GameSystemEvent]) = {
    val entitiesMarkedForDeath = gameState.entities.filter(_.has[MarkedForDeath])
    
    entitiesMarkedForDeath.foldLeft((gameState, Seq.empty[GameSystemEvent])) {
      case ((currentGameState, currentEvents), entity) =>
        // Process death events only for non-player entities
        (entity.get[DeathEvents], entity.get[MarkedForDeath]) match {
          case (optDeathEvents, Some(markedForDeath)) if entity.entityType != EntityType.Player =>
            // If the entity is marked for death, process the death events
            optDeathEvents match {
              case Some(DeathEvents(deathEvents)) =>
                val newEvents: Seq[GameSystemEvent] = deathEvents.flatMap {
                  case GiveExperience(amount) =>
                    markedForDeath.deathDetails.killerId match {
                      case Some(killerId) => Some(GameSystemEvent.AddExperienceEvent(killerId, amount))
                      case None => None
                    }
                  case SpawnEntity(entityReference, forceSpawn) =>
                    val creator = markedForDeath.deathDetails.victim.get[Collision].map(_.creatorId).flatMap(currentGameState.getEntity).getOrElse(entity)
                    
                    markedForDeath.deathDetails.victim.get[Movement].map(_.position) match {
                    case Some(victimPosition) =>
                      Some(GameSystemEvent.SpawnEntityEvent(entityReference, creator, victimPosition, forceSpawn))
                    case None =>
                      throw new Exception("Cannot spawn entity on death: victim has no position")
                  }
                  case DropCoins(amount) =>
                    markedForDeath.deathDetails.victim.get[Movement].map(_.position) match {
                      case Some(victimPosition) =>
                        Some(GameSystemEvent.SpawnEntityEvent(EntityReference.Coin, entity, victimPosition, forceSpawn = true))
                      case None =>
                        throw new Exception("Cannot drop coins on death: victim has no position")
                    }
                }
                // Trigger death events and remove the entity
                (currentGameState.remove(entity.id), 
                  currentEvents ++ newEvents)
              case None =>
                // If no death events are defined, just remove the entity
                (currentGameState.remove(entity.id), 
                  currentEvents)
            }
          case _ =>
            // If the entity is not marked for death, do nothing
            (currentGameState, currentEvents)
        }
    }
  }
}
