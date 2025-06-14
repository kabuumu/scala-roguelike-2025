package game.action

import data.Sprites
import game.Item.*
import game.entity.*
import game.entity.EntityType.entityType
import game.entity.Movement.*
import game.event.*
import game.{Item, *}

case class AttackAction(targetPosition: Point, optWeapon: Option[Weapon]) extends Action {
  def apply(attackingEntity: Entity, gameState: GameState): Seq[Event] = {
    optWeapon match {
      case Some(Weapon(damage, Item.Ranged(_))) =>
        val targetType = if (attackingEntity.entityType == EntityType.Player) EntityType.Enemy else EntityType.Player
        val startingPosition = attackingEntity.position
        val projectileEntity =
          Entity(
            id = s"Projectile-${System.nanoTime()}",
            Movement(position = startingPosition),
            Projectile(startingPosition, targetPosition, targetType, damage),
            EntityTypeComponent(EntityType.Projectile),
            Drawable(Sprites.projectileSprite),
            Collision(damage = damage, persistent = false, targetType, ""),
            Hitbox()
          )

        Seq(
          AddEntityEvent(projectileEntity),
          ResetInitiativeEvent(attackingEntity.id)
        )
      case _ =>
        val damage = optWeapon match {
          case Some(weapon) => weapon.damage
          case None => 1
        }
        gameState.getActor(targetPosition) match {
          case Some(target) =>
            Seq(
              DamageEntityEvent(target.id, damage, attackingEntity.id),
              ResetInitiativeEvent(attackingEntity.id)
            )
          case _ =>
            throw new Exception(s"No target found at $targetPosition")
        }
    }
  }
}
