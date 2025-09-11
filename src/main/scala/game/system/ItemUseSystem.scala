package game.system

import data.Entities
import data.Entities.EntityReference.{Arrow, Fireball}
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

/**
 * New unified item use system that handles all usable items through the UsableItem component.
 * Replaces both LegacyItemUseSystem and ComponentItemUseSystem with a data-driven approach.
 * Now uses type-safe functions without placeholder strings and with proper targeting context.
 */
object ItemUseSystem extends GameSystem {
  override def update(gameState: GameState, events: Seq[GameSystemEvent.GameSystemEvent]): (GameState, Seq[GameSystemEvent.GameSystemEvent]) = {
    val newEvents: Seq[GameSystemEvent] = events.flatMap {
      case GameSystemEvent.InputEvent(entityId, InputAction.UseItem(itemEntityId, item, UseContext(userId: String, target))) =>
        val optItemEffect = item.effect match {
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
          case CreateProjectile(entityReference) => for {
            targetPoint <- target match {
              case Some(entity: Entity) =>
                Some(entity.position)
              case Some(point: Point) =>
                Some(point)
              case _ =>
                None
            }
            startPosition <- gameState.getEntity(userId).map(_.position)
            entity = entityReference match {
              case Arrow =>
                Entities.arrowProjectile(
                  userId,
                  startPosition,
                  targetPoint,
                  if (gameState.getEntity(userId).exists(_.entityType == EntityType.Player)) EntityType.Enemy else EntityType.Player)
              case Fireball =>
                Entities.fireballProjectile(
                  userId,
                  startPosition,
                  targetPoint,
                  if (gameState.getEntity(userId).exists(_.entityType == EntityType.Player)) EntityType.Enemy else EntityType.Player)
            }
          } yield GameSystemEvent.SpawnEntityEvent(entity)
        }


        val optItemUsage = item.chargeType match {
          case ChargeType.SingleUse =>
            // Remove item from user's inventory
            Some(GameSystemEvent.RemoveItemEntityEvent(userId, itemEntityId))
          case ChargeType.Ammo(requiredAmmoType) =>
            // For ammo-based items, we could implement ammo consumption logic here if needed
            val ammo = gameState.getEntity(userId).flatMap(_.inventoryItems(gameState).find(_.exists[Ammo](_.ammoType == requiredAmmoType)))

            ammo match {
              case Some(ammo) =>
                // Remove one ammo from inventory
                Some(GameSystemEvent.RemoveItemEntityEvent(userId, ammo.id))
              case None =>
                // No ammo available, cannot use item
                None
            }
        }

        
        (optItemEffect, optItemUsage) match {
          case (Some(itemEffect), Some(itemUsage)) =>
            Seq(itemEffect, itemUsage) :+ GameSystemEvent.ResetInitiativeEvent(userId)
          case _ =>
            Nil

        }
      case _ =>
        Nil
    }

    (gameState, newEvents)
  }
}