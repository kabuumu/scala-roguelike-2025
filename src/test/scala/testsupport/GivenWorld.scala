package testsupport

import data.Sprites
import game.{GameState, Point}
import game.entity.*
import game.entity.EntityType.*
import map.Dungeon
import ui.UIState
import ui.UIState.{Move, UIState}

import scala.reflect.ClassTag

object Given {
  final case class World(
    player: Entity,
    entities: Seq[Entity],
    dungeon: Dungeon
  ) {
    // Generic, component-based player modification using ECS API
    def modifyPlayer[C](f: C => C)(implicit ct: ClassTag[C]): World =
      copy(player = player.update[C](f))

    def setPlayer[C](c: C)(implicit ct: ClassTag[C]): World =
      copy(player = player.update[C](_ => c))

    // Generic, component-based entity modification by id
    def modifyEntity[C](id: String)(f: C => C)(implicit ct: ClassTag[C]): World =
      copy(entities = entities.map(e => if (e.id == id) e.update[C](f) else e))

    def setEntityComponent[C](id: String, c: C)(implicit ct: ClassTag[C]): World =
      copy(entities = entities.map(e => if (e.id == id) e.update[C](_ => c) else e))

    // Inventory and population helpers
    def withItems(items: Entity*): World = {
      val inv = player.get[Inventory].getOrElse(Inventory())
      val updatedInv = items.foldLeft(inv)((acc, it) => acc.addItemEntityId(it.id))
      copy(player = player.update[Inventory](_ => updatedInv), entities = entities ++ items)
    }

    def withEntities(extra: Entity*): World =
      copy(entities = entities ++ extra)

    // Build state and begin story
    def buildGameState(): GameState =
      GameState(player.id, entities.filterNot(_.id == player.id) :+ player, Nil, dungeon)

    def beginStory(initialUI: UIState = Move, startTick: Int = 0): GameStory =
      GameStory.begin(initialUI, buildGameState(), startTick)
  }

  def thePlayerAt(x: Int, y: Int, id: String = "testPlayerId"): World = {
    val primary = ItemFactory.createWeapon("primary-weapon", 2, Melee)
    val secondary = ItemFactory.createWeapon("secondary-weapon", 1, game.entity.Ranged(6))

    val player = Entity(
      id = id,
      Movement(position = Point(x, y)),
      EntityTypeComponent(EntityType.Player),
      Health(10),
      Initiative(0),
      Inventory(Seq(), Some(primary.id), Some(secondary.id)),
      SightMemory(),
      Drawable(Sprites.playerSprite),
      Hitbox(),
      Experience()
    )

    val dungeon = Dungeon(testMode = true)
    World(player, Seq(primary, secondary, player), dungeon)
  }

  object items {
    def potion(id: String): Entity = ItemFactory.createPotion(id)
    def scroll(id: String): Entity = ItemFactory.createScroll(id)
    def bow(id: String): Entity = ItemFactory.createBow(id)
    def arrow(id: String): Entity = ItemFactory.createArrow(id)
    def weapon(id: String, damage: Int, wt: WeaponType): Entity = ItemFactory.createWeapon(id, damage, wt)
  }

  object enemies {
    def basic(id: String, x: Int, y: Int, health: Int = 10, withWeapons: Boolean = false, deathEvents: Option[DeathEvents] = None): Seq[Entity] = {
      val base = Entity(
        id = id,
        Movement(position = Point(x, y)),
        EntityTypeComponent(EntityType.Enemy),
        Health(health),
        Initiative(10),
        Inventory(),
        SightMemory(),
        Drawable(Sprites.enemySprite),
        Hitbox()
      )

      val withDeath = deathEvents match {
        case Some(de) => base.update[DeathEvents](_ => de)
        case None => base
      }

      if (!withWeapons) Seq(withDeath)
      else {
        val p = ItemFactory.createWeapon(s"$id-primary-weapon", 2, Melee)
        val s = ItemFactory.createWeapon(s"$id-secondary-weapon", 1, game.entity.Ranged(6))
        val withInv = withDeath.update[Inventory](_ => Inventory(Seq(), Some(p.id), Some(s.id)))
        Seq(withInv, p, s)
      }
    }
  }
}