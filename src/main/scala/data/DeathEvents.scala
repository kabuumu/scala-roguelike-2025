package data

import data.Entities.EntityReference
import game.entity.Entity
import game.system.event.GameSystemEvent.{AddExperienceEvent, GameSystemEvent}

object DeathEvents {
  enum DeathEventReference:
    case GiveExperience(amount: Int)
    case SpawnEntity(
        entityReference: EntityReference,
        forceSpawn: Boolean = true
    )
    case DropCoins(amount: Int)
    case SpawnProjectile(
        projectile: data.Projectiles.ProjectileReference,
        strategies: Set[SpawnStrategy]
    )
}
