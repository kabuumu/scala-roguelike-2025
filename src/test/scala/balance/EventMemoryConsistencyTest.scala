package balance

import org.scalatest.funsuite.AnyFunSuite
import game.entity.*
import game.entity.Health.*
import game.system.{DamageSystem, EventMemorySystem}
import game.system.event.GameSystemEvent
import game.Point
import data.Items
import game.GameState

class EventMemoryConsistencyTest extends AnyFunSuite {

  private def mkPlayer(): Entity =
    Entity(
      id = "player",
      Movement(Point(0,0)),
      EntityTypeComponent(EntityType.Player),
      Health(70),
      Initiative(0),
      Inventory(),
      Equipment(),
      EventMemory(),
      Drawable(data.Sprites.playerSprite),
      Hitbox()
    )

  private def mkEnemy(): (Entity, Entity) = {
    val weapon = Items.weapon("enemy-weapon", 7, game.system.event.GameSystemEvent.DamageSource.Melee)
    val enemy = Entity(
      id = "enemy",
      Movement(Point(0,1)),
      EntityTypeComponent(EntityType.Enemy),
      Health(30),
      Initiative(0),
      Inventory(Seq(weapon.id)),
      Equipment(),
      EventMemory(),
      Drawable(data.Sprites.enemySprite),
      Hitbox()
    )
    (weapon, enemy)
  }

  test("Recorded damage equals applied HP loss and unified formula") {
    val player = mkPlayer()
    val (weapon, enemy) = mkEnemy()
    val dungeon = map.MapGenerator.generateDungeon(dungeonSize = 3, lockedDoorCount = 0, itemCount = 0)
    val state0 = GameState(player.id, Seq(player, enemy, weapon), Nil, dungeon)

    val beforeHP = player.currentHealth
    val events = Seq(GameSystemEvent.DamageEvent(player.id, enemy.id, baseDamage = 7))
    val (afterDamage, _) = DamageSystem.update(state0, events)
    val (afterMemory, _) = EventMemorySystem.update(afterDamage, events)

    val appliedHP = beforeHP - afterDamage.getEntity(player.id).get.currentHealth
    val mem = afterMemory.getEntity(player.id).get.get[EventMemory].get
    val taken = mem.getEventsByType[MemoryEvent.DamageTaken].headOption
      .getOrElse(throw new AssertionError("No DamageTaken memory event"))

    assert(taken.damage == appliedHP, "Mismatch between recorded and actual damage")
    val recomputed = math.max(1, taken.baseDamage + taken.attackerBonus - taken.defenderResistance)
    assert(taken.damage == recomputed, "Breakdown fields do not recompute to final damage")
  }
}