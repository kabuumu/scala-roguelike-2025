package game.system

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import game.entity.*
import game.GameState
import game.Point
import game.system.event.GameSystemEvent
import data.Projectiles
import map.WorldMap
import map.MapBounds
import game.entity.Health.*

class ChainLightningTest extends AnyFunSpec with Matchers {

  describe("Chain Lightning Bug Reproduction") {
    it("should damage the second target even if the first target dies") {
      // 1. Setup
      // Player (creator)
      val playerId = "player-1"
      val player = Entity(
        playerId,
        Movement(Point(0, 0)),
        EntityTypeComponent(EntityType.Player)
      )

      // Enemy 1 (First target, will die)
      val enemy1Id = "enemy-1"
      val enemy1 = Entity(
        id = enemy1Id,
        Movement(position = Point(0, 6)),
        EntityTypeComponent(EntityType.Enemy),
        Health(10) // Low health, will be killed by 10 damage
      )

      // Enemy 2 (Second target)
      val enemy2Id = "enemy-2"
      val enemy2 = Entity(
        id = enemy2Id,
        Movement(position = Point(0, 10)),
        EntityTypeComponent(EntityType.Enemy),
        Health(50)
      )

      // Projectile (First bolt from Player -> Enemy 1)
      val projectileId = "proj-1"
      val projectile = Entity(
        id = projectileId,
        Movement(Point(0, 6)), // Start at Enemy 1 position (impact point)
        EntityTypeComponent(EntityType.Projectile),
        Collision(
          damage = 10,
          persistent = false,
          target = EntityType.Enemy,
          creatorId = playerId
        ),
        // Replicating what lightningBoltProjectile does in Projectiles.scala
        DeathEvents(
          Seq(
            data.DeathEvents.DeathEventReference.SpawnProjectile(
              data.Projectiles.ProjectileReference.LightningBolt(
                10,
                2, // bouncesLeft
                5,
                Some(EntityType.Enemy)
              ),
              Set(
                data.SpawnStrategy.TargetNearestEnemy(5),
                data.SpawnStrategy.ExcludeKiller,
                data.SpawnStrategy.ExcludeCreator
              )
            )
          )
        )
      )

      val worldMap = WorldMap(
        tiles = Map.empty,
        dungeons = Seq.empty,
        paths = Set.empty,
        bridges = Set.empty,
        bounds = MapBounds(0, 20, 0, 20)
      )

      val gameState = GameState(
        playerEntityId = playerId,
        entities = Seq(player, enemy1, enemy2, projectile),
        worldMap = worldMap
      )

      // 2. Collision Check: Projectile hits Enemy 1
      val collisionEvent = GameSystemEvent.CollisionEvent(
        projectileId,
        GameSystemEvent.CollisionTarget.Entity(enemy1Id)
      )

      val (afterCollisionState, collisionEvents) =
        CollisionHandlerSystem.update(gameState, Seq(collisionEvent))

      // Verify Enemy 1 DamageEvent
      val damageEvent1 = collisionEvents.collectFirst {
        case e: GameSystemEvent.DamageEvent => e
      }.get

      // Verify Projectile is MarkedForDeath with killerId = Enemy 1 (collided entity)
      // This is crucial for ExcludeKiller strategy
      println("Checking MarkedForDeath...")
      val markedProjectile = afterCollisionState.getEntity(projectileId).get
      markedProjectile.has[MarkedForDeath] shouldBe true
      markedProjectile
        .get[MarkedForDeath]
        .get
        .deathDetails
        .killerId shouldBe Some(enemy1Id)

      // 3. Process Death via DeathHandlerSystem (simulating next frame phase 1)
      val (stateAfterDeath, deathEvents) =
        DeathHandlerSystem.update(
          afterCollisionState,
          Seq.empty
        )

      // Check for SpawnProjectileEvent
      val spawnEvent = deathEvents.collectFirst {
        case e: GameSystemEvent.SpawnProjectileEvent => e
      }

      spawnEvent shouldBe defined
      val bounceProjectileRef = spawnEvent.get.projectile

      // Verify projectile reference
      bounceProjectileRef shouldBe a[
        data.Projectiles.ProjectileReference.LightningBolt
      ]

      // 4. Simulate Spawning the Bounce Projectile
      val (stateWithBounce, _) =
        SpawnProjectileSystem.update(stateAfterDeath, Seq(spawnEvent.get))

      val bounceProjectileEntity = stateWithBounce.entities.find { e =>
        e.id != playerId && e.id != enemy1Id && e.id != enemy2Id && e.id != projectileId
      }.get

      // Verify creator ID logic (should be attributed to original player)
      bounceProjectileEntity.get[Collision].get.creatorId shouldBe playerId

      // 5. Cleanup Enemy 1 (Death)
      // Manually remove enemy 1 to simulate it dying from damage if we fully simulated loop
      val stateWithBounceNoEnemy1 = stateWithBounce.copy(
        entities = stateWithBounce.entities.filter(_.id != enemy1Id)
      )

      // 6. Collision Check: Bounce Projectile hits Enemy 2
      val bounceCollisionEvent = GameSystemEvent.CollisionEvent(
        bounceProjectileEntity.id,
        GameSystemEvent.CollisionTarget.Entity(enemy2Id)
      )

      val (_, secondCollisionEvents) =
        CollisionHandlerSystem.update(
          stateWithBounceNoEnemy1,
          Seq(bounceCollisionEvent)
        )

      // Should have DamageEvent for Enemy 2
      val damageEvent2 = secondCollisionEvents.collectFirst {
        case e: GameSystemEvent.DamageEvent => e
      }
      damageEvent2 shouldBe defined

      // 7. Process DamageEvent with DamageSystem
      val (finalState, _) =
        DamageSystem.update(stateWithBounceNoEnemy1, Seq(damageEvent2.get))

      // Enemy 2 should have taken damage
      val enemy2Final = finalState.getEntity(enemy2Id).get
      enemy2Final.currentHealth should be < 50
    }
  }
}
