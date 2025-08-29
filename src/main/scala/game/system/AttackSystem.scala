package game.system

import game.GameState
import game.entity.Inventory
import game.system.event.GameSystemEvent
import game.system.event.GameSystemEvent.{GameSystemEvent, SpawnEntityEvent}
import ui.InputAction
import game.entity.EntityType
import data.Sprites
import game.entity.*
import game.entity.EntityType.entityType
import game.entity.Movement.*
import game.entity.Inventory.primaryWeapon
import game.entity.WeaponItem.weaponItem
import game.entity.WeaponItem.weaponItem
import game.event.*
import game.*
import game.entity.Initiative.*

object AttackSystem extends GameSystem {
  override def update(gameState: GameState, events: Seq[GameSystemEvent]): (GameState, Seq[GameSystemEvent]) = {
    events.foldLeft((gameState, Nil)) {
      case ((currentState, currentEvents), GameSystemEvent.InputEvent(attackingEntityId, InputAction.Attack(target))) =>
        val optAttackingEntity = currentState.getEntity(attackingEntityId)
        val optWeaponEntity = currentState.getEntity(attackingEntityId).flatMap(_.primaryWeapon(currentState))
        (optAttackingEntity, optWeaponEntity) match {
          case (Some(attackingEntity), Some(weaponEntity)) =>
            weaponEntity.weaponItem match {
              case Some(weapon) if weapon.weaponType.isInstanceOf[game.entity.Ranged] =>
            val targetType = if (attackingEntity.entityType == EntityType.Player) EntityType.Enemy else EntityType.Player
            val startingPosition = attackingEntity.position
            val projectileEntity =
              Entity(
                id = s"Projectile-${System.nanoTime()}",
                Movement(position = startingPosition),
                Projectile(startingPosition, target.position, targetType, weapon.damage),
                EntityTypeComponent(EntityType.Projectile),
                Drawable(Sprites.projectileSprite),
                Collision(damage = weapon.damage, persistent = false, targetType, ""),
                Hitbox()
              )

            val newGameState = currentState
              .updateEntity(attackingEntityId, _.resetInitiative())
              .add(projectileEntity)

            (newGameState, currentEvents)
              case Some(weapon) => // Melee weapon
                val damage = weapon.damage
                val newGameState = currentState.updateEntity(attackingEntityId, _.resetInitiative())
                (newGameState, currentEvents :+ GameSystemEvent.DamageEvent(target.id, attackingEntityId, damage))
              case None => // No weapon component, shouldn't happen but fallback
                val newGameState = currentState.updateEntity(attackingEntityId, _.resetInitiative())
                (newGameState, currentEvents :+ GameSystemEvent.DamageEvent(target.id, attackingEntityId, 1))
            }
          case (Some(attackingEntity), Some(weaponEntity)) =>
            // Weapon entity exists but no weapon component - use default damage
            val newGameState = currentState.updateEntity(attackingEntityId, _.resetInitiative())
            (newGameState, currentEvents :+ GameSystemEvent.DamageEvent(target.id, attackingEntityId, 1))
          case _ =>
            // No weapon entity
            val newGameState = currentState.updateEntity(attackingEntityId, _.resetInitiative())
            (newGameState, currentEvents :+ GameSystemEvent.DamageEvent(target.id, attackingEntityId, 1))

        }
        
      case (currentState, _) =>
        currentState
    }
  }
}
