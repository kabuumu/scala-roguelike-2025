package game.combat

import game.GameState
import game.entity.Entity
import game.entity.Equipment.*
import game.status.StatusEffect
import game.status.StatusEffect.*
import game.status.StatusEffect.EffectType
import game.system.event.GameSystemEvent

final case class DamageBreakdown(
  baseDamage: Int,
  attackerBonus: Int,
  defenderResistance: Int,
  finalDamage: Int
)

object DamageCalculator {

  def compute(baseDamage: Int, attacker: Entity, defender: Entity, gameState: GameState, source: GameSystemEvent.DamageSource = GameSystemEvent.DamageSource.Melee): DamageBreakdown = {
    val statusDamageBonus = attacker.statusEffects.collect { case StatusEffect(EffectType.IncreaseDamage(b), _, _) => b }.sum
    
    // Only apply equipment damage bonus for melee attacks, not projectiles
    val equipmentDamageBonus = source match {
      case GameSystemEvent.DamageSource.Melee => attacker.getTotalDamageBonus
      case GameSystemEvent.DamageSource.Projectile => 0
    }
    
    // Apply specialized perk bonuses
    val specializedBonuses = calculateSpecializedBonuses(attacker, defender, source, gameState)
    
    val attackerBonus = statusDamageBonus + equipmentDamageBonus + specializedBonuses

    val statusDamageResistance = defender.statusEffects.collect { case StatusEffect(EffectType.ReduceIncomingDamage(r), _, _) => r }.sum
    val equipmentDamageResistance = defender.getTotalDamageReduction
    val defenderResistance = statusDamageResistance + equipmentDamageResistance

    val finalDamage = math.max(1, baseDamage + attackerBonus - defenderResistance)
    DamageBreakdown(baseDamage, attackerBonus, defenderResistance, finalDamage)
  }

  private def calculateSpecializedBonuses(attacker: Entity, defender: Entity, source: GameSystemEvent.DamageSource, gameState: GameState): Int = {
    var totalBonus = 0

    // Get defender's enemy type from sprite
    val enemyType = getEnemyTypeFromEntity(defender)

    // Apply enemy-specific damage bonuses
    attacker.statusEffects.foreach {
      case StatusEffect(EffectType.IncreaseDamageVsEnemyType(targetEnemyType, bonus), _, _) =>
        if (enemyType == targetEnemyType) {
          totalBonus += bonus
        }
      case _ => // Not an enemy-specific bonus
    }

    // Apply weapon/item specific bonuses based on damage source
    // This would require context from the attacking system about what weapon/item was used
    // For now, we'll implement basic logic based on damage source
    source match {
      case GameSystemEvent.DamageSource.Projectile =>
        // For projectile attacks, check if it's from bow or scroll
        // This is a simplified implementation - ideally we'd pass more context
        attacker.statusEffects.foreach {
          case StatusEffect(EffectType.IncreaseDamageWithWeaponType("Bow", bonus), _, _) =>
            totalBonus += bonus
          case StatusEffect(EffectType.IncreaseDamageWithItemType("Fireball Scroll", bonus), _, _) =>
            totalBonus += bonus
          case _ => // Not a weapon/item-specific bonus
        }
      case _ => // Melee attacks don't get projectile bonuses
    }

    totalBonus
  }

  private def getEnemyTypeFromEntity(entity: Entity): String = {
    entity.get[game.entity.Drawable] match {
      case Some(drawable) =>
        drawable.sprites.headOption.map(_._2) match {
          case Some(sprite) if sprite == data.Sprites.ratSprite => "Rat"
          case Some(sprite) if sprite == data.Sprites.slimeletSprite => "Slimelet"
          case Some(sprite) if sprite == data.Sprites.slimeSprite => "Slime"
          case Some(sprite) if sprite == data.Sprites.snakeSprite => "Snake"
          case Some(sprite) if sprite == data.Sprites.bossSpriteTL => "Boss"
          case _ => "Enemy"
        }
      case None => "Enemy"
    }
  }
}