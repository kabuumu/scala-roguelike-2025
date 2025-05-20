package game.action

import data.Sprites
import game.Item.*
import game.entity.*
import game.entity.EntityType.entityType
import game.entity.Initiative.*
import game.entity.UpdateAction.{CollisionCheckAction, ProjectileUpdateAction}
import game.{Item, *}
import Health.*

case class AttackAction(targetPosition: Point, optWeapon: Option[Weapon]) extends Action {
  def apply(attackingEntity: Entity, gameState: GameState): GameState = {
    optWeapon match {
      case Some(Weapon(damage, Item.Ranged(_))) =>
        val targetType = if (attackingEntity.entityType == EntityType.Player) EntityType.Enemy else EntityType.Player
        val startingPosition = attackingEntity[Movement].position
        val projectileEntity =
          Entity(
            id = s"Projectile-${System.nanoTime()}",
            Movement(position = startingPosition),
            Projectile(startingPosition, targetPosition, targetType, damage),
            UpdateController(ProjectileUpdateAction, CollisionCheckAction),
            EntityTypeComponent(EntityType.Projectile),
            Drawable(Sprites.projectileSprite),
            Collision(damage = damage, explodes = false, persistent = false, targetType),
            Hitbox()
          )

        gameState
          .add(projectileEntity)
          .updateEntity(
            attackingEntity.id,
            attackingEntity.resetInitiative()
          )
      case _ =>
        val damage = optWeapon match {
          case Some(weapon) => weapon.damage
          case None => 1
        }
        gameState.getActor(targetPosition) match {
          case Some(target) =>
            gameState
              .updateEntity(
                target.id, target.damage(damage)
              )
              .updateEntity(
                attackingEntity.id,
                attackingEntity.resetInitiative()
              )
          case _ =>
            throw new Exception(s"No target found at $targetPosition")
        }
    }
  }
}
