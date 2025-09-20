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

  def rat(id: String, position: game.Point): Entity = {
    Entity(
      id = id,
      Movement(position = position),
      EntityTypeComponent(EntityType.Enemy),
      Health(25),
      Initiative(12),
      Inventory(Nil),
      Equipment(weapon = Some(Equippable.weapon(7, "Rat Claws"))), // 1 base + 7 bonus = 8 total damage
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
      Equipment(weapon = Some(Equippable.weapon(5, "Snake Fangs"))), // 1 base + 5 bonus = 6 total damage
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
      Equipment(weapon = Some(Equippable.weapon(5, "Slime Acid"))), // 1 base + 5 bonus = 6 total damage
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
}