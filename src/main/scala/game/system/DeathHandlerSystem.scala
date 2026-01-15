package game.system

import data.DeathEvents.DeathEventReference.{
  GiveExperience,
  SpawnEntity,
  DropCoins
}
import data.Entities.EntityReference
import game.GameState
import game.entity.{
  Collision,
  DeathEvents,
  EntityType,
  MarkedForDeath,
  Movement
}
import game.entity.EntityType.entityType
import game.system.event.GameSystemEvent
import game.system.event.GameSystemEvent.GameSystemEvent
import game.entity.Health.isAlive
import game.entity.Movement.position

object DeathHandlerSystem extends GameSystem {
  override def update(
      gameState: GameState,
      events: Seq[GameSystemEvent]
  ): (GameState, Seq[GameSystemEvent]) = {
    val entitiesMarkedForDeath =
      gameState.entities.filter(_.has[MarkedForDeath])

    entitiesMarkedForDeath.foldLeft((gameState, Seq.empty[GameSystemEvent])) {
      case ((currentGameState, currentEvents), entity) =>
        // Process death events only for non-player entities
        (entity.get[DeathEvents], entity.get[MarkedForDeath]) match {
          case (optDeathEvents, Some(markedForDeath))
              if entity.entityType != EntityType.Player =>
            // If the entity is marked for death, process the death events
            optDeathEvents match {
              case Some(DeathEvents(deathEvents)) =>
                val newEvents: Seq[GameSystemEvent] = deathEvents.flatMap {
                  case GiveExperience(amount) =>
                    markedForDeath.deathDetails.killerId match {
                      case Some(killerId) =>
                        Some(
                          GameSystemEvent.AddExperienceEvent(killerId, amount)
                        )
                      case None => None
                    }
                  case SpawnEntity(entityReference, forceSpawn) =>
                    val creator = markedForDeath.deathDetails.victim
                      .get[Collision]
                      .map(_.creatorId)
                      .flatMap(currentGameState.getEntity)
                      .getOrElse(entity)

                    markedForDeath.deathDetails.victim
                      .get[Movement]
                      .map(_.position) match {
                      case Some(victimPosition) =>
                        Some(
                          GameSystemEvent.SpawnEntityEvent(
                            entityReference,
                            creator,
                            victimPosition,
                            forceSpawn
                          )
                        )
                      case None =>
                        throw new Exception(
                          "Cannot spawn entity on death: victim has no position"
                        )
                    }
                  case DropCoins(amount) =>
                    markedForDeath.deathDetails.victim
                      .get[Movement]
                      .map(_.position) match {
                      case Some(victimPosition) =>
                        Some(
                          GameSystemEvent.SpawnEntityEvent(
                            EntityReference.Coin,
                            entity,
                            victimPosition,
                            forceSpawn = true
                          )
                        )
                      case None =>
                        throw new Exception(
                          "Cannot drop coins on death: victim has no position"
                        )
                    }
                  case data.DeathEvents.DeathEventReference
                        .SpawnProjectile(projectileRef, strategies) =>
                    import data.SpawnStrategy
                    import game.entity.Health
                    import game.entity.EntityTypeComponent

                    val victim = markedForDeath.deathDetails.victim
                    val killerId = markedForDeath.deathDetails.killerId

                    victim.get[Movement].map(_.position).flatMap {
                      victimPosition =>
                        // Filter potential targets based on strategies
                        val potentialTargets =
                          currentGameState.entities.filter { e =>
                            e.has[Health] && e.isAlive && e.id != victim.id
                          }

                        // Apply filters
                        val filteredTargets =
                          strategies.foldLeft(potentialTargets) {
                            (targets, strategy) =>
                              strategy match {
                                case SpawnStrategy.ExcludeKiller =>
                                  killerId
                                    .map(kid => targets.filter(_.id != kid))
                                    .getOrElse(targets)
                                case SpawnStrategy.ExcludeCreator =>
                                  victim
                                    .get[Collision]
                                    .map(_.creatorId)
                                    .map(cid => targets.filter(_.id != cid))
                                    .getOrElse(targets)
                                case SpawnStrategy.ExcludeSpecific(id) =>
                                  targets.filter(_.id != id)
                                case SpawnStrategy.TargetNearestEnemy(range) =>
                                  targets.filter(t =>
                                    t.position.getChebyshevDistance(
                                      victimPosition
                                    ) <= range
                                  )
                                case SpawnStrategy.TargetRandomEnemy(range) =>
                                  targets.filter(t =>
                                    t.position.getChebyshevDistance(
                                      victimPosition
                                    ) <= range
                                  )
                              }
                          }

                        // Select target
                        val selectedTarget = strategies.collectFirst {
                          case SpawnStrategy.TargetNearestEnemy(_) =>
                            filteredTargets.minByOption(
                              _.position
                                .getChebyshevDistance(victimPosition)
                                .asInstanceOf[Double]
                            )
                          case SpawnStrategy.TargetRandomEnemy(_) =>
                            if (filteredTargets.nonEmpty)
                              Some(
                                filteredTargets(
                                  scala.util.Random
                                    .nextInt(filteredTargets.size)
                                )
                              )
                            else None
                        }.flatten

                        selectedTarget.map { target =>
                          // For recursive projectiles (like lightning chain), we ideally want to preserve the ORIGINAL creator.
                          // But DeathHandlerSystem doesn't know the full history.
                          // Currently, we use the dying entity (victim) as the creator of the new projectile.
                          // This is acceptable as long as damage attribution logic follows the chain or resets.
                          // In Chain Lightning's specific case, `CollisionHandlerSystem` logic wasn't fully portable.
                          // However, if we make the new projectile inherit the `creatorId` from the victim's `creatorId`,
                          // we can chain attribution.

                          // BUT `SpawnProjectileEvent` takes an `Entity` creator.
                          // If we pass `victim`, SpawnProjectileSystem uses `victim.id` as creator.
                          // To Chain attribution, we'd need to modify SpawnProjectileSystem or pass a proxi.
                          // For now, let's pass `victim` and assume `SpawnProjectileEvent` logic is sufficient or acceptable.
                          // Wait, if projectile A (creator=Player) hits Enemy B. Projectile A dies.
                          // New Projectile C is spawned. Creator = Projectile A.
                          // Projectile C hits Enemy D. Creator = Projectile A.
                          // Damage event: Attacker = Projectile A.
                          // If Projectile A is gone, `DamageSystem` might fail to resolve attacker -> null/no stats.
                          // This is a risk.

                          // Fix: Use the Victim's creator as the creator of the new projectile if possible?
                          // The victim IS the projectile in this case.
                          // victim.get[Collision].creatorId is the Player.
                          // We can look up that entity.

                          val originalCreator = victim
                            .get[Collision]
                            .map(_.creatorId)
                            .flatMap(currentGameState.getEntity)
                            .getOrElse(victim)

                          GameSystemEvent.SpawnProjectileEvent(
                            projectileRef,
                            originalCreator,
                            target.position,
                            Some(victimPosition)
                          )
                        }
                    }
                }
                // Trigger death events and remove the entity
                (currentGameState.remove(entity.id), currentEvents ++ newEvents)
              case None =>
                // If no death events are defined, just remove the entity
                (currentGameState.remove(entity.id), currentEvents)
            }
          case _ =>
            // If the entity is not marked for death, do nothing
            (currentGameState, currentEvents)
        }
    }
  }
}
