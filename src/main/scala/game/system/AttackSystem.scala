package game.system

import game.GameState
import game.entity.Inventory
import game.system.event.GameSystemEvent
import game.system.event.GameSystemEvent.{GameSystemEvent, SpawnEntityEvent}
import ui.InputAction
import game.entity.EntityType
import game.Item.Weapon
import data.Sprites
import game.Item.*
import game.entity.*
import game.entity.EntityType.entityType
import game.entity.Movement.*
import game.event.*
import game.{Item, *}
import game.entity.Initiative.*

object AttackSystem extends GameSystem {
  override def update(gameState: GameState, events: Seq[GameSystemEvent]): (GameState, Seq[GameSystemEvent]) = {
    events.foldLeft((gameState, events)) {
      case ((currentState, currentEvents), GameSystemEvent.InputEvent(attackingEntityId, InputAction.Attack(target))) =>
        val optAttackingEntity = currentState.getEntity(attackingEntityId)
        val optWeapon = currentState.getEntity(attackingEntityId).flatMap(_.get[Inventory].flatMap(_.primaryWeapon))
        (optAttackingEntity, optWeapon) match {
          case (Some(attackingEntity), Some(Weapon(damage, Item.Ranged(_)))) =>
            val targetType = if (attackingEntity.entityType == EntityType.Player) EntityType.Enemy else EntityType.Player
            val startingPosition = attackingEntity.position
            val projectileEntity =
              Entity(
                id = s"Projectile-${System.nanoTime()}",
                Movement(position = startingPosition),
                Projectile(startingPosition, target.position, targetType, damage),
                EntityTypeComponent(EntityType.Projectile),
                Drawable(Sprites.projectileSprite),
                Collision(damage = damage, persistent = false, targetType, ""),
                Hitbox()
              )

            val newGameState = currentState
              .updateEntity(attackingEntityId, _.resetInitiative())
              .add(projectileEntity)

            (newGameState, currentEvents)
          case _ =>
            val damage = optWeapon match {
              case Some(weapon) => weapon.damage
              case None => 1
            }

            val newGameState = currentState.updateEntity(attackingEntityId, _.resetInitiative())

            (newGameState, currentEvents :+ GameSystemEvent.DamageEvent(target.id, attackingEntityId, damage))

        }
        
      case (currentState, _) =>
        currentState
    }
  }
}
