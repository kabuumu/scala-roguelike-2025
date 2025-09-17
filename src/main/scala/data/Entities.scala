package data

import data.DeathEvents.DeathEventReference.{GiveExperience, SpawnEntity}
import game.entity.*
import game.entity.Experience.experienceForLevel
import game.entity.Movement.position

object Entities {
  enum EntityReference:
    case Explosion(damage: Int, size: Int)
    case Slimelet

  def explosionEffect(creatorId: String, position: game.Point, targetType: EntityType, damage: Int = 10, size: Int = 2): Entity = {
    Entity(
      s"Explosion ($creatorId)",
      Hitbox(),
      Collision(damage = damage, persistent = true, targetType, creatorId),
      Movement(position = position),
      Drawable(Sprites.projectileSprite),
      Wave(size),
      EntityTypeComponent(EntityType.Projectile),
    )
  }
  
  def slimelet(position: game.Point): Entity = {
    val health = 5 
    val damage = 1
    Entity(
      Movement(position = position), // Position will be set by SpawnEntitySystem
      EntityTypeComponent(EntityType.Enemy),
      Health(10),
      Initiative(8),
      Inventory(Nil, None), // No weapon for slimelets, they use default 1 damage
      EventMemory(),
      Drawable(Sprites.slimeletSprite),
      Hitbox(),
      DeathEvents(Seq(GiveExperience(10))) //TODO - fix slimes
    )
  }
}
