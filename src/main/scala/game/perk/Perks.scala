package game.perk

import game.entity.{Component, Entity, EventMemory}
import game.entity.EventMemory.*
import game.entity.Experience.level
import game.entity.MemoryEvent
import game.status.StatusEffect
import game.status.StatusEffect.EffectType.*
import game.status.StatusEffect.statusEffects

import scala.util.Random

object Perks {
  // Basic perks (always available)
  final val IncreaseMaxHealthPerk = StatusEffect(
    IncreaseMaxHealth(20),
    name = "Hearty",
    description = "Increases maximum health by 20."
  )
  
  final val ReduceIncomingDamagePerk = StatusEffect(
    ReduceIncomingDamage(5),
    name = "Fortified",
    description = "Reduces incoming damage by 5."
  )
  
  final val IncreaseDamagePerk = StatusEffect(
    IncreaseDamage(5),
    name = "Berserker",
    description = "Increases damage dealt by 5."
  )

  // Specialized perks based on event history
  final val SlimeBanePerk = StatusEffect(
    IncreaseDamageVsEnemyType("Slime", 3),
    name = "Slime Bane",
    description = "Deal +3 damage against slimes."
  )

  final val SlimeletBanePerk = StatusEffect(
    IncreaseDamageVsEnemyType("Slimelet", 2),
    name = "Slimelet Bane", 
    description = "Deal +2 damage against slimelets."
  )

  final val BowMasteryPerk = StatusEffect(
    IncreaseDamageWithWeaponType("Bow", 4),
    name = "Bow Mastery",
    description = "Deal +4 damage with bow attacks."
  )

  final val ScrollMasteryPerk = StatusEffect(
    IncreaseDamageWithItemType("Fireball Scroll", 3),
    name = "Scroll Mastery", 
    description = "Deal +3 damage with scroll attacks."
  )

  final val BattleHardenedPerk = StatusEffect(
    ReduceIncomingDamage(3),
    name = "Battle Hardened",
    description = "Reduces incoming damage by 3 (stacks with Fortified)."
  )

  final val ImprovedFortificationPerk = StatusEffect(
    ReduceIncomingDamage(8),
    name = "Improved Fortification",
    description = "Reduces incoming damage by 8."
  )

  // Define perk requirements
  sealed trait PerkRequirement {
    def isMet(entity: Entity): Boolean
  }

  case class EnemiesKilledRequirement(enemyType: String, count: Int) extends PerkRequirement {
    def isMet(entity: Entity): Boolean = {
      val defeated = entity.getMemoryEventsByType[MemoryEvent.EnemyDefeated]
      defeated.count(_.enemyType.contains(enemyType)) >= count
    }
  }

  case class ItemUsageRequirement(itemType: String, count: Int) extends PerkRequirement {
    def isMet(entity: Entity): Boolean = {
      val itemsUsed = entity.getMemoryEventsByType[MemoryEvent.ItemUsed]
      itemsUsed.count(_.itemType.contains(itemType)) >= count
    }
  }

  case class DamageTakenRequirement(totalDamage: Int) extends PerkRequirement {
    def isMet(entity: Entity): Boolean = {
      val damageTaken = entity.getMemoryEventsByType[MemoryEvent.DamageTaken]
      damageTaken.map(_.damage).sum >= totalDamage
    }
  }

  case class LevelRequirement(level: Int) extends PerkRequirement {
    def isMet(entity: Entity): Boolean = {
      entity.level >= level
    }
  }

  case class HasPerkRequirement(perk: StatusEffect) extends PerkRequirement {
    def isMet(entity: Entity): Boolean = {
      entity.statusEffects.contains(perk)
    }
  }

  // Define all available perks with their requirements
  case class PerkDefinition(
    perk: StatusEffect,
    requirements: Seq[PerkRequirement]
  )

  val allPerks: Seq[PerkDefinition] = Seq(
    // Basic perks (no requirements beyond leveling up)
    PerkDefinition(IncreaseMaxHealthPerk, Seq.empty),
    PerkDefinition(ReduceIncomingDamagePerk, Seq.empty),
    PerkDefinition(IncreaseDamagePerk, Seq.empty),

    // Specialized perks with requirements
    PerkDefinition(
      SlimeBanePerk,
      Seq(EnemiesKilledRequirement("Slime", 5))
    ),
    PerkDefinition(
      SlimeletBanePerk,
      Seq(EnemiesKilledRequirement("Slimelet", 8))
    ),
    PerkDefinition(
      BowMasteryPerk,
      Seq(ItemUsageRequirement("Bow", 10))
    ),
    PerkDefinition(
      ScrollMasteryPerk,
      Seq(ItemUsageRequirement("Fireball Scroll", 5))
    ),
    PerkDefinition(
      BattleHardenedPerk,
      Seq(DamageTakenRequirement(50))
    ),
    PerkDefinition(
      ImprovedFortificationPerk,
      Seq(
        LevelRequirement(3),
        HasPerkRequirement(ReduceIncomingDamagePerk)
      )
    )
  )

  def getAvailablePerks(entity: Entity): Seq[StatusEffect] = {
    allPerks
      .filter(perkDef => perkDef.requirements.forall(_.isMet(entity)))
      .filter(perkDef => !entity.statusEffects.contains(perkDef.perk)) // Don't offer perks already owned
      .map(_.perk)
  }

  def getRandomPerks(entity: Entity, count: Int = 3): Seq[StatusEffect] = {
    val available = getAvailablePerks(entity)
    if (available.length <= count) {
      available
    } else {
      Random.shuffle(available).take(count)
    }
  }
}
