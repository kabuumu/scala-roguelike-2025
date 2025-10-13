package testsupport

import data.{Items, Sprites}
import game.{GameState, Point}
import game.entity.*
import game.entity.EntityType.*
import game.entity.Equipment.*
import game.entity.Equippable.*
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
    def modifyPlayer[C <: Component](f: C => C)(implicit ct: ClassTag[C]): World =
      copy(player = player.update[C](f))

    def setPlayer[C <: Component](c: C)(implicit ct: ClassTag[C]): World =
      copy(player = player.update[C](_ => c))

    // Generic, component-based entity modification by id
    def modifyEntity[C <: Component](id: String)(f: C => C)(implicit ct: ClassTag[C]): World =
      copy(entities = entities.map(e => if (e.id == id) e.update[C](f) else e))

    def setEntityComponent[C <: Component](id: String, c: C)(implicit ct: ClassTag[C]): World =
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
    val startingSword = Items.basicSword("test-sword")
    val startingArmor = Items.chainmailArmor("test-armor")

    val player = Entity(
      id = id,
      Movement(position = Point(x, y)),
      EntityTypeComponent(EntityType.Player),
      Health(10),
      Initiative(0),
      Inventory(Seq(startingSword.id, startingArmor.id)),
      Equipment(
        weapon = Some(Equippable.weapon(3, "Basic Sword")),
        armor = Some(Equippable.armor(EquipmentSlot.Armor, 5, "Chainmail Armor"))
      ),
      SightMemory(),
      EventMemory(),
      Drawable(Sprites.playerSprite),
      Hitbox(),
      Experience(),
      Coins()
    )

    val dungeon = Dungeon(testMode = true)
    World(player, Seq(startingSword, startingArmor, player), dungeon)
  }

  def thePlayerInBossRoom(x: Int, y: Int, id: String = "testPlayerId"): World = {
    val startingSword = Items.basicSword("test-sword")
    val startingArmor = Items.chainmailArmor("test-armor")

    val player = Entity(
      id = id,
      Movement(position = Point(x, y)),
      EntityTypeComponent(EntityType.Player),
      Health(10),
      Initiative(0),
      Inventory(Seq(startingSword.id, startingArmor.id)),
      Equipment(
        weapon = Some(Equippable.weapon(3, "Basic Sword")),
        armor = Some(Equippable.armor(EquipmentSlot.Armor, 5, "Chainmail Armor"))
      ),
      SightMemory(),
      EventMemory(),
      Drawable(Sprites.playerSprite),
      Hitbox(),
      Experience(),
      Coins()
    )

    // Create a simple 2-room dungeon with boss room
    val dungeon = Dungeon(
      roomGrid = Set(Point(0, 0), Point(1, 0)),
      startPoint = Point(0, 0),
      endpoint = Some(Point(1, 0)),
      hasBossRoom = true,
      testMode = true
    )
    World(player, Seq(startingSword, startingArmor, player), dungeon)
  }

  object items {
    def potion(id: String): Entity = Items.healingPotion(id)
    def scroll(id: String): Entity = Items.fireballScroll(id)
    def bow(id: String): Entity = Items.bow(id)
    def arrow(id: String): Entity = Items.arrow(id)
  }

  object enemies {
    def basic(id: String, x: Int, y: Int, health: Int = 10, withWeapons: Boolean = false, deathEvents: Option[DeathEvents] = None): Seq[Entity] = {
      val baseComponents = Seq(
        Movement(position = Point(x, y)),
        EntityTypeComponent(EntityType.Enemy),
        Health(health),
        Initiative(10),
        Inventory(),
        SightMemory(),
        Drawable(Sprites.enemySprite),
        Hitbox()
      )

      val allComponents = deathEvents match {
        case Some(de) => baseComponents :+ de
        case None => baseComponents
      }

      val base = Entity(id, allComponents*)

      // withWeapons parameter is ignored in new system - enemies use melee attacks
      Seq(base)
    }
  }
}