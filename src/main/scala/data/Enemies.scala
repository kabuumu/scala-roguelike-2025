package data

import data.DeathEvents.DeathEventReference.{
  GiveExperience,
  SpawnEntity,
  DropCoins
}
import data.Entities.EntityReference.Slimelet
import game.entity.*
import game.entity.Experience.experienceForLevel
import game.Point

object Enemies {
  enum EnemyReference:
    case Rat
    case Snake
    case Slime
    case Slimelet
    case Duck
    case Boss

  /** Enemy difficulty values for dungeon depth progression. Used to determine
    * appropriate groups of enemies per depth.
    */
  object EnemyDifficulty {
    val SLIMELET = 1
    val SLIME = 2
    val RAT = 3
    val SNAKE = 4
    val BOSS = 10
    val DUCK = 1

    def difficultyFor(enemyRef: EnemyReference): Int = enemyRef match {
      case EnemyReference.Slimelet => SLIMELET
      case EnemyReference.Slime    => SLIME
      case EnemyReference.Rat      => RAT
      case EnemyReference.Snake    => SNAKE
      case EnemyReference.Boss     => BOSS
      case EnemyReference.Duck     => DUCK
    }
  }

  def rat(id: String, position: game.Point): Entity = {
    CharacterFactory.create(
      id,
      position,
      Sprites.ratSprite,
      EntityType.Enemy,
      hp = Some(25),
      initiative = Some(6),
      inventory = Some(Inventory(Nil)),
      extraComponents = Seq(
        EnemyTypeComponent(EnemyReference.Rat),
        Equipment(weapon =
          Some(EquippedItem(s"$id-weapon", Equippable.weapon(8, "Rat Claws")))
        ),
        EventMemory(),
        DeathEvents(
          Seq(
            GiveExperience(experienceForLevel(2) / 4),
            DropCoins(3)
          )
        )
      )
    )
  }

  def snake(id: String, position: game.Point, spitAbilityId: String): Entity = {
    CharacterFactory.create(
      id,
      position,
      Sprites.snakeSprite,
      EntityType.Enemy,
      hp = Some(18),
      initiative = Some(12),
      inventory = Some(Inventory(Seq(spitAbilityId))),
      extraComponents = Seq(
        EnemyTypeComponent(EnemyReference.Snake),
        Equipment(weapon =
          Some(EquippedItem(s"$id-weapon", Equippable.weapon(6, "Snake Fangs")))
        ),
        EventMemory(),
        DeathEvents(
          Seq(
            GiveExperience(experienceForLevel(2) / 4),
            DropCoins(3)
          )
        )
      )
    )
  }

  def slime(id: String, position: game.Point): Entity = {
    CharacterFactory.create(
      id,
      position,
      Sprites.slimeSprite,
      EntityType.Enemy,
      hp = Some(20),
      initiative = Some(8),
      inventory = Some(Inventory(Nil)),
      extraComponents = Seq(
        EnemyTypeComponent(EnemyReference.Slime),
        Equipment(weapon =
          Some(EquippedItem(s"$id-weapon", Equippable.weapon(6, "Slime Acid")))
        ),
        EventMemory(),
        DeathEvents(
          Seq(
            GiveExperience(experienceForLevel(2) / 4),
            SpawnEntity(Slimelet, forceSpawn = false),
            SpawnEntity(Slimelet, forceSpawn = false),
            DropCoins(5)
          )
        )
      )
    )
  }

  def slimelet(id: String, position: game.Point): Entity = {
    CharacterFactory.create(
      id,
      position,
      Sprites.slimeletSprite,
      EntityType.Enemy,
      hp = Some(10),
      initiative = Some(4),
      inventory = Some(Inventory(Nil)),
      extraComponents = Seq(
        EnemyTypeComponent(EnemyReference.Slimelet),
        EventMemory(),
        DeathEvents(
          Seq(
            GiveExperience(experienceForLevel(2) / 5),
            DropCoins(1)
          )
        )
      )
    )
  }

  def boss(
      id: String,
      position: game.Point,
      rangedAbilityId: String
  ): Entity = {
    CharacterFactory.create(
      id,
      position,
      Sprites.bossSpriteTL, // Base sprite, will be overridden by extraComponents
      EntityType.Enemy,
      hp = Some(120),
      initiative = Some(10),
      inventory =
        Some(Inventory(Nil)), // TODO - Temporarily removed boss ranged attack
      extraComponents = Seq(
        EnemyTypeComponent(EnemyReference.Boss),
        Equipment(weapon =
          Some(EquippedItem(s"$id-weapon", Equippable.weapon(15, "Boss Claws")))
        ),
        EventMemory(),
        // Override Drawable for multi-tile sprite
        Drawable(
          Set(
            Point(0, 0) -> Sprites.bossSpriteTL,
            Point(1, 0) -> Sprites.bossSpriteTR,
            Point(0, 1) -> Sprites.bossSpriteBL,
            Point(1, 1) -> Sprites.bossSpriteBR
          )
        ),
        // Override Hitbox for 2x2 size
        Hitbox(points =
          Set(Point(0, 0), Point(1, 0), Point(0, 1), Point(1, 1))
        ),
        DeathEvents(
          Seq(
            GiveExperience(experienceForLevel(5)),
            DropCoins(50)
          )
        )
      )
    )
  }

  def duck(id: String, position: game.Point): Entity = {
    CharacterFactory.create(
      id,
      position,
      Sprites.duckSprite,
      EntityType.Animal,
      hp = Some(5),
      initiative = Some(8),
      inventory = Some(Inventory(Nil)),
      extraComponents = Seq(
        EnemyTypeComponent(EnemyReference.Duck),
        EventMemory(),
        DeathEvents(
          Seq(
            SpawnEntity(Entities.EntityReference.Meat),
            GiveExperience(experienceForLevel(1) / 4)
          )
        )
      )
    )
  }
}
