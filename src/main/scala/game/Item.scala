package game

import data.Sprites
import game.Item.ItemEffect.PointTargeted
import game.entity.Health.*
import game.entity.Initiative.*
import game.entity.EntityType.*
import game.entity.UpdateAction.{CollisionCheckAction, ProjectileUpdateAction}
import game.entity.{Collision, Drawable, Entity, EntityType, EntityTypeComponent, Hitbox, Inventory, Movement, UpdateController}

object Item {
  val potionValue = 5

  //Types of item
  // Unusable (e.g. key, melee weapon, armour or ammo - has uses but not by direct use)
  //    This is assuming that equipping is a separate action from using
  //    This is a bit of a misnomer, as you can use a key to unlock a door, but the use action is simply walking into the door
  //    Weapons could be considered as usable, but currently they are used by a separate attack action
  // Single use usable (e.g. potion or scroll)
  // Multi use usable (e.g. wand or staff) would need a charges mechanic
  // Usable with ammo (e.g. bow or gun) would need linking to an ammo item

  sealed trait Item

  sealed trait UsableItem extends Item {
    def itemEffect: ItemEffect
  }

  sealed trait UnusableItem extends Item


  enum ItemEffect {
    case EntityTargeted(effect: Entity => Entity => GameState => GameState)
    case PointTargeted(effect: Point => Entity => GameState => GameState)
    case NonTargeted(effect: Entity => GameState => GameState)
  }

  enum TargetType:
    case Self, Point


  case object Potion extends UsableItem {
    override def itemEffect: ItemEffect =
      ItemEffect.NonTargeted {
        entity =>
          gameState =>
            if (entity.hasFullHealth)
              gameState
                .addMessage(s"${System.nanoTime()}: ${entity[EntityTypeComponent]} is already at full health")
            else
              gameState.updateEntity(
                entity.id,
                _.heal(Item.potionValue)
              ).updateEntity(
                entity.id,
                _.update[Inventory](_ - this)
                  .resetInitiative()
              ).addMessage(
                s"${System.nanoTime()}: ${entity[EntityTypeComponent]} used a potion to heal ${Item.potionValue} health"
              )
      }
  }

  case object Scroll extends UsableItem {

    override def itemEffect: ItemEffect =
      PointTargeted { target =>
        entity =>
          gameState => {
            val scrollDamage = 3

            val targetType = if (entity.entityType == EntityType.Player) EntityType.Enemy else EntityType.Player
            val startingPosition = entity[Movement].position
            val fireballEntity =
              Entity(
                id = s"Projectile-${System.nanoTime()}",
                Movement(position = startingPosition),
                game.entity.Projectile(startingPosition, target, targetType, scrollDamage),
                UpdateController(ProjectileUpdateAction, CollisionCheckAction),
                EntityTypeComponent(EntityType.Projectile),
                Drawable(Sprites.projectileSprite),
                Collision(damage = scrollDamage, explodes = true, persistent = false, targetType),
                Hitbox()
              )

            gameState
              .add(fireballEntity)
              .updateEntity(
                entity.id,
                _.update[Inventory](_ - Scroll)
                  .resetInitiative(),
              ).addMessage(
                s"${System.nanoTime()}: ${entity[EntityTypeComponent]} used a Scroll to attack $target"
              )
          }
      }
  }

  case class Key(keyColour: KeyColour) extends UnusableItem

  case class Weapon(damage: Int, weaponType: WeaponType) extends UnusableItem {
    val range: Int = weaponType match {
      case Melee => 1
      case Ranged(range) => range
    }
  }

  sealed trait WeaponType
  case object Melee extends WeaponType
  case class Ranged(range: Int) extends WeaponType

  enum KeyColour {
    case Yellow, Red, Blue
  }
}
