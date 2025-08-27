package game

import data.Sprites
import game.Item.ItemEffect.{EntityTargeted, PointTargeted}
import game.entity.*
import game.entity.EntityType.*
import game.entity.Health.*
import game.entity.Movement.*
import game.event.*
import game.system.event.GameSystemEvent.SpawnEntityEvent

import scala.util.Random


object Item {
  val potionValue = 40

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
    def chargeType: ChargeType
  }

  sealed trait UnusableItem extends Item

  enum ChargeType {
    case SingleUse
    case Ammo(ammoItem: Item)
  }

  enum ItemEffect {
    case EntityTargeted(effect: Entity => Entity => GameState => Seq[Event])
    case PointTargeted(effect: Point => Entity => GameState => Seq[Event])
    case NonTargeted(effect: Entity => GameState => Seq[Event])
  }

  case object Potion extends UsableItem {
    override def chargeType: ChargeType = ChargeType.SingleUse
    override def itemEffect: ItemEffect =
      ItemEffect.NonTargeted {
        itemUser =>
          gameState =>
            if (itemUser.hasFullHealth)
              Seq(
                MessageEvent(s"${System.nanoTime()}: ${itemUser.entityType} is already at full health")
              )
            else
              Seq(
                HealEvent(itemUser.id, Item.potionValue),
                RemoveItemEvent(itemUser.id, this),
                ResetInitiativeEvent(itemUser.id),
                MessageEvent(s"${System.nanoTime()}: ${itemUser.entityType} used a potion to heal ${Item.potionValue} health")
              )
      }
  }

  case object Scroll extends UsableItem {
    override def chargeType: ChargeType = ChargeType.SingleUse
    override def itemEffect: ItemEffect =
      PointTargeted { target =>
        entity =>
          gameState => {
            val scrollDamage = 30

            val targetType = if (entity.entityType == EntityType.Player) EntityType.Enemy else EntityType.Player
            val startingPosition = entity.position
              def explosionEntity(parentEntity: Entity) = Entity(
                s"explosion ${Random.nextInt()}",
                Hitbox(Set(Point(0, 0))),
                Collision(damage = scrollDamage, persistent = true, targetType, entity.id),
                Movement(position = parentEntity.position),
                Drawable(Sprites.projectileSprite),
                Wave(2),
                EntityTypeComponent(EntityType.Projectile)
              )

            val fireballEntity =
              Entity(
                id = s"Projectile-${System.nanoTime()}",
                Movement(position = startingPosition),
                game.entity.Projectile(startingPosition, target, targetType, 0),
                EntityTypeComponent(EntityType.Projectile),
                Drawable(Sprites.projectileSprite),
                Collision(damage = scrollDamage, persistent = false, targetType, entity.id),
                Hitbox(),
                DeathEvents(
                  deathDetails =>
                    Seq(
                      SpawnEntityEvent(explosionEntity(deathDetails.victim))
                    )
                )
              )

            Seq(
              AddEntityEvent(fireballEntity),
              RemoveItemEvent(entity.id, this),
              ResetInitiativeEvent(entity.id),
              MessageEvent(s"${System.nanoTime()}: ${entity.entityType} threw a fireball at ${target}"),
            )
          }
      }
  }

  case object Arrow extends UnusableItem

  case object Bow extends UsableItem {
    override def chargeType: ChargeType = ChargeType.Ammo(Arrow)
    override def itemEffect: ItemEffect = EntityTargeted {
      target =>
        entity =>
          gameState => {
            val bowDamage = 8
            val targetType = if (entity.entityType == EntityType.Player) EntityType.Enemy else EntityType.Player
            val startingPosition = entity.position
            val projectileEntity =
              Entity(
                id = s"Projectile-${System.nanoTime()}",
                Movement(position = startingPosition),
                game.entity.Projectile(startingPosition, target.position, targetType, bowDamage),
                EntityTypeComponent(EntityType.Projectile),
                Drawable(Sprites.projectileSprite),
                Collision(damage = bowDamage, persistent = false, targetType, entity.id),
                Hitbox()
              )

            Seq(
              AddEntityEvent(projectileEntity),
              RemoveItemEvent(entity.id, Arrow),
              ResetInitiativeEvent(entity.id),
              MessageEvent(s"${System.nanoTime()}: ${entity.entityType} used a Bow to attack ${target.id}"),
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
