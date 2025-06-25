package game.system

import game.GameState
import game.entity.Inventory
import game.system.event.GameSystemEvent
import game.system.event.GameSystemEvent.GameSystemEvent
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

@deprecated
object LegacyAttackSystem extends GameSystem {
  override def update(gameState: GameState, events: Seq[GameSystemEvent]): (GameState, Seq[GameSystemEvent]) = {
    val updatedGameState = events.foldLeft(gameState) {
      case (currentState, GameSystemEvent.InputEvent(entityId, InputAction.Attack(target))) =>
        val events = for {
          attackingEntity <- currentState.getEntity(entityId).toSeq
          optWeapon = attackingEntity.get[Inventory].flatMap(_.primaryWeapon)
          targetPosition <- target.get[Movement].map(_.position).toSeq
          events <- optWeapon match {
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
        }} yield events
        
        gameState.handleEvents(events)
      case (currentState, _) =>
        currentState
    }
    
    (updatedGameState, Nil)
  }

}
