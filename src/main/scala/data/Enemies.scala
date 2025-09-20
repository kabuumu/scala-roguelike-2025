package data

import data.DeathEvents.DeathEventReference.{GiveExperience, SpawnEntity}
import data.Entities.EntityReference.Slimelet
import game.entity.*
import game.entity.Experience.experienceForLevel

object Enemies {
  enum EnemyReference:
    case Rat
    case Snake
    case Slime
    case Slimelet

  /**
   * Enemy difficulty values for dungeon depth progression.
   * Used to determine appropriate groups of enemies per depth.
   */
  object EnemyDifficulty {
    val SLIMELET = 1
    val SLIME = 2  
    val RAT = 3
    val SNAKE = 4
    
    def difficultyFor(enemyRef: EnemyReference): Int = enemyRef match {
      case EnemyReference.Slimelet => SLIMELET
      case EnemyReference.Slime => SLIME
      case EnemyReference.Rat => RAT
      case EnemyReference.Snake => SNAKE
    }
  }

  def rat(id: String, position: game.Point): Entity = {
    Entity(
      id = id,
      Movement(position = position),
      EntityTypeComponent(EntityType.Enemy),
      Health(25),
      Initiative(12),
      Inventory(Nil),
      Equipment(weapon = Some(Equippable.weapon(8, "Rat Claws"))), // 1 base + 8 bonus = 9 total damage
      EventMemory(),
      Drawable(Sprites.ratSprite),
      Hitbox(),
      DeathEvents(Seq(GiveExperience(experienceForLevel(2) / 4)))
    )
  }

  def snake(id: String, position: game.Point, spitAbilityId: String): Entity = {
    Entity(
      id = id,
      Movement(position = position),
      EntityTypeComponent(EntityType.Enemy),
      Health(18),
      Initiative(25),
      Inventory(Seq(spitAbilityId)), // Give snake the spit ability
      Equipment(weapon = Some(Equippable.weapon(6, "Snake Fangs"))), // 1 base + 6 bonus = 7 total damage
      EventMemory(),
      Drawable(Sprites.snakeSprite),
      Hitbox(),
      DeathEvents(Seq(GiveExperience(experienceForLevel(2) / 4)))
    )
  }

  def slime(id: String, position: game.Point): Entity = {
    Entity(
      id = id,
      Movement(position = position),
      EntityTypeComponent(EntityType.Enemy),
      Health(20),
      Initiative(15),
      Inventory(Nil),
      Equipment(weapon = Some(Equippable.weapon(6, "Slime Acid"))), // 1 base + 6 bonus = 7 total damage
      EventMemory(),
      Drawable(Sprites.slimeSprite),
      Hitbox(),
      DeathEvents(Seq(
        GiveExperience(experienceForLevel(2) / 4),
        SpawnEntity(Slimelet, forceSpawn = false),
        SpawnEntity(Slimelet, forceSpawn = false)
      ))
    )
  }

  def slimelet(id: String, position: game.Point): Entity = {
    Entity(
      id = id,
      Movement(position = position),
      EntityTypeComponent(EntityType.Enemy),
      Health(10),
      Initiative(8),
      Inventory(Nil), // No weapon for slimelets, they use default 1 damage
      EventMemory(),
      Drawable(Sprites.slimeletSprite),
      Hitbox(),
      DeathEvents(Seq(GiveExperience(experienceForLevel(2) / 5)))
    )
  }
}