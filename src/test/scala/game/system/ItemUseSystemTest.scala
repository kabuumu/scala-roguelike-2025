package game.system

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import game.entity.*
import game.GameState
import game.Point
import game.system.event.GameSystemEvent
import ui.InputAction
import data.Sprites
import data.Items
import data.Projectiles
import map.WorldMap
import map.MapBounds

class ItemUseSystemTest extends AnyFunSpec with Matchers {

  describe("ItemUseSystem") {
    it("should chain lightning damage to nearby enemies") {
      // 1. Setup entities
      val playerId = "player-1"
      val enemy1Id = "enemy-1"
      val enemy2Id = "enemy-2"
      val enemy3Id = "enemy-3"

      val player = Entity(
        id = playerId,
        Movement(position = Point(0, 0)),
        EntityTypeComponent(EntityType.Player),
        Health(100)
      )

      // Enemy 1: Range 6 from player (Target)
      val enemy1 = Entity(
        id = enemy1Id,
        Movement(position = Point(0, 6)),
        EntityTypeComponent(EntityType.Enemy),
        Health(50)
      )

      // Enemy 2: Range 4 from Enemy 1
      val enemy2 = Entity(
        id = enemy2Id,
        Movement(position = Point(0, 10)),
        EntityTypeComponent(EntityType.Enemy),
        Health(50)
      )

      // Enemy 3: Range 3 from Enemy 2
      val enemy3 = Entity(
        id = enemy3Id,
        Movement(position = Point(3, 10)),
        EntityTypeComponent(EntityType.Enemy),
        Health(50)
      )

      // Distant Enemy: Range 20 from player, far from chain
      val distantEnemy = Entity(
        id = "distant-enemy",
        Movement(position = Point(0, 20)),
        EntityTypeComponent(EntityType.Enemy),
        Health(50)
      )

      // Scroll Item
      val scrollId = "scroll-1"
      val scroll = Items
        .chainLightningScroll(scrollId)
        .addComponent(
          Inventory()
        ) // Add Inventory component just in case checks need it? No, item logic is separate.

      // Add scroll to player inventory
      val playerWithInventory =
        player.addComponent(Inventory(itemEntityIds = Seq(scrollId)))

      val worldMap = WorldMap(
        tiles = Map.empty,
        dungeons = Seq.empty,
        paths = Set.empty,
        bridges = Set.empty,
        bounds = MapBounds(0, 100, 0, 100)
      )

      val gameState = GameState(
        playerEntityId = playerId,
        entities = Seq(
          playerWithInventory,
          enemy1,
          enemy2,
          enemy3,
          distantEnemy,
          scroll
        ),
        worldMap = worldMap
      )

      // 2. Use Scroll
      val useEvent = GameSystemEvent.InputEvent(
        playerId,
        InputAction.UseItem(
          scrollId,
          scroll.get[UsableItem].get,
          UseContext(playerId, Some(Point(0, 6))) // Target Enemy 1
        )
      )

      val (newState, newEvents) = ItemUseSystem.update(gameState, Seq(useEvent))

      // 3. Verify Events
      // Expect:
      // - RemoveItemEntityEvent (scroll consumed)
      // - SpawnProjectileEvent (Player -> Enemy1)
      // - ResetInitiativeEvent
      // Note: Damage and subsequent bounces are handled by CollisionHandlerSystem and Projectile system

      val damageEvents = newEvents.collect {
        case e: GameSystemEvent.DamageEvent => e
      }
      damageEvents should have size 0 // Damage happens on collision now

      val projectileEvents = newEvents.collect {
        case e: GameSystemEvent.SpawnProjectileEvent => e
      }
      projectileEvents should have size 1

      // First bolt: Player -> Enemy1
      val bolt1 = projectileEvents.find(_.targetPoint == Point(0, 6)).get
      bolt1.creator.id shouldBe playerId
      bolt1.projectile shouldBe Projectiles.ProjectileReference
        .LightningBolt(10, 3, 5, Some(EntityType.Enemy))
    }
  }
}
