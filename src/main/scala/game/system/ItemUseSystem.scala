package game.system

import data.{Entities, Projectiles}
import data.Projectiles.ProjectileReference.{Arrow, Fireball}
import game.entity.*
import game.entity.EntityType.entityType
import game.entity.GameEffect.{CreateProjectile, Heal}
import game.entity.Health.*
import game.entity.Inventory.inventoryItems
import game.entity.Movement.position
import game.system.event.GameSystemEvent
import game.system.event.GameSystemEvent.*
import game.{GameState, Point}
import ui.InputAction

object ItemUseSystem extends GameSystem {
  override def update(gameState: GameState, events: Seq[GameSystemEvent.GameSystemEvent]): (GameState, Seq[GameSystemEvent.GameSystemEvent]) = {
    val newEvents: Seq[GameSystemEvent] = events.flatMap {
      case GameSystemEvent.InputEvent(userId, InputAction.UseItem(itemEntityId, item, UseContext(_, target))) =>
        val user = gameState.getEntity(userId)

        // First check if we have required ammo (if needed)
        val hasRequiredResources = item.chargeType match {
          case ChargeType.Ammo(requiredAmmoType) =>
            gameState.getEntity(userId).flatMap(_.inventoryItems(gameState).find(_.exists[Ammo](_.ammoType == requiredAmmoType))).isDefined
          case _ => true // SingleUse and InfiniteUse always have required resources
        }

        if (!hasRequiredResources) {
          // No required resources (ammo), cannot use item at all
          Nil
        } else {
          (for {
            user <- gameState.getEntity(userId)
            itemEffect <- item.effect match {
              case Heal(healAmount) =>
                for {
                  targetId <- target match {
                    case None => Some(userId)
                    case Some(entity: Entity) => Some(entity.id)
                    case _ => None
                  }
                  targetEntity <- gameState.getEntity(targetId)
                  if !targetEntity.hasFullHealth
                } yield GameSystemEvent.HealEvent(targetId, healAmount)
              case CreateProjectile(projectileReference) => for {
                targetPoint <- target match {
                  case Some(entity: Entity) =>
                    Some(entity.position)
                  case Some(point: Point) =>
                    Some(point)
                  case _ =>
                    None
                }
              } yield GameSystemEvent.SpawnProjectileEvent(projectileReference, user, targetPoint)
            }
          } yield {
            val itemUsageEvents = item.chargeType match {
              case ChargeType.SingleUse =>
                // Remove item from user's inventory
                Seq(GameSystemEvent.RemoveItemEntityEvent(userId, itemEntityId))
              case ChargeType.InfiniteUse =>
                // Keep item in inventory, no removal needed
                Seq.empty
              case ChargeType.Ammo(requiredAmmoType) =>
                // We already checked ammo exists, so find and remove it
                val ammo = gameState.getEntity(userId).flatMap(_.inventoryItems(gameState).find(_.exists[Ammo](_.ammoType == requiredAmmoType)))
                ammo match {
                  case Some(ammo) =>
                    // Remove one ammo from inventory
                    Seq(GameSystemEvent.RemoveItemEntityEvent(userId, ammo.id))
                  case None =>
                    // This shouldn't happen since we checked above, but safety fallback
                    Seq.empty
                }
            }
            val allEvents = Seq(itemEffect) ++ itemUsageEvents
            // Always reset initiative when an item is successfully used
            allEvents ++ Seq(GameSystemEvent.ResetInitiativeEvent(userId))
          }).getOrElse(Nil)
        }
      case _ =>
        Nil
    }

    (gameState, newEvents)
  }
}