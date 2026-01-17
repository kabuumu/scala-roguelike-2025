package game.system

import game.entity.EntityType.*
import game.entity.Initiative.*
import game.entity.Movement.position
import game.entity.{Entity, EntityType, UsableItem, UseContext, Hitbox}
import game.entity.Hitbox.isWithinRangeOfHitbox
import game.entity.Inventory.usableItems
import game.entity.Targeting.EnemyActor
import game.system.event.GameSystemEvent.{GameSystemEvent, InputEvent}
import game.{GameState, Point, Direction}
import ui.InputAction
import util.Pathfinder

object EnemyAISystem extends GameSystem {

  // Helper function to determine if an enemy is a boss (2x2 entity)
  private def isBoss(enemy: Entity): Boolean = {
    enemy.get[Hitbox] match {
      case Some(hitbox) =>
        hitbox.points.size > 1 // Boss has multiple hitbox points
      case None => false
    }
  }

  // Helper function to get entity size for pathfinding
  private def getEntitySize(enemy: Entity): Point = {
    if (isBoss(enemy)) Point(2, 2) else Point(1, 1)
  }

  // Boss AI: Switches between ranged and melee based on distance and strategy
  private def getBossAction(
      enemy: Entity,
      target: Entity,
      gameState: GameState
  ): InputEvent = {
    val meleeRange = 1

    // Check if boss can attack in melee range
    if (enemy.isWithinRangeOfHitbox(target, meleeRange)) {
      // In melee range - attack!
      InputEvent(enemy.id, InputAction.Attack(target))
    } else {
      // Not in melee range - move closer using boss-sized pathfinding
      Pathfinder.getNextStepWithSize(
        enemy.position,
        target.position,
        gameState,
        Point(2, 2)
      ) match {
        case Some(nextStep) =>
          InputEvent(enemy.id, InputAction.Move(nextStep))
        case None =>
          // Pathfinding failed - try simple directional movement as fallback
          // Try all four directions to see which ones work
          val possibleDirections =
            Seq(Direction.Up, Direction.Down, Direction.Left, Direction.Right)
          val targetPosition = target.position
          val bossPosition = enemy.position

          // Choose direction that gets us closer to target
          val bestDirection =
            if (
              Math.abs(targetPosition.x - bossPosition.x) > Math.abs(
                targetPosition.y - bossPosition.y
              )
            ) {
              // Move horizontally
              if (targetPosition.x > bossPosition.x) Direction.Right
              else Direction.Left
            } else {
              // Move vertically
              if (targetPosition.y > bossPosition.y) Direction.Down
              else Direction.Up
            }

          InputEvent(enemy.id, InputAction.Move(bestDirection))
      }
    }
  }

  // Flee AI: Moves away from the nearest threat
  private def getFleeAction(
      entity: Entity,
      threats: Seq[Entity],
      gameState: GameState
  ): InputEvent = {
    // Find the nearest threat
    val entityPos = entity.position
    val nearestThreat =
      threats.minByOption(t => t.position.getChebyshevDistance(entityPos))

    nearestThreat match {
      case Some(threat) =>
        // Determine all possible moves (neighbors)
        val neighbors = entityPos.neighbors

        // Filter out blocked moves
        val validMoves =
          neighbors.filterNot(gameState.movementBlockingPoints.contains)

        if (validMoves.nonEmpty) {
          // Choose the move that maximizes distance to the threat
          val bestMove =
            validMoves.maxBy(pos => pos.getChebyshevDistance(threat.position))

          Direction.fromPoints(entityPos, bestMove) match {
            case direction => InputEvent(entity.id, InputAction.Move(direction))
          }
        } else {
          // Trapped! Wait.
          InputEvent(entity.id, InputAction.Wait)
        }
      case None =>
        // No threats? Wander randomly.
        val neighbors = entity.position.neighbors.filterNot(
          gameState.movementBlockingPoints.contains
        )
        if (neighbors.nonEmpty) {
          val randomMove = neighbors(scala.util.Random.nextInt(neighbors.size))
          InputEvent(
            entity.id,
            InputAction.Move(Direction.fromPoints(entity.position, randomMove))
          )
        } else {
          InputEvent(entity.id, InputAction.Wait)
        }
    }
  }

  override def update(
      gameState: GameState,
      events: Seq[GameSystemEvent]
  ): (GameState, Seq[GameSystemEvent]) = {
    val target = gameState.playerEntity

    // Optimization: Check if there are any ready entities first (Enemies or Animals)
    val anyActorReady = gameState.entities.exists(e =>
      (e.entityType == EntityType.Enemy || e.entityType == EntityType.Animal) && e.isReady
    )

    if (anyActorReady) {
      // Pre-compute player's visible points once for all entities
      val playerVisiblePoints = gameState.getVisiblePointsFor(target)

      // Optimization: Filter lists once
      val allEnemies =
        gameState.entities.filter(_.entityType == EntityType.Enemy)
      // val allAnimals = gameState.entities.filter(_.entityType == EntityType.Animal) // Not used currently

      val aiEvents = gameState.entities.collect {
        case entity
            if (entity.entityType == EntityType.Enemy || entity.entityType == EntityType.Animal) && entity.isReady =>
          if (entity.entityType == EntityType.Animal) {
            // Animal Behavior (Duck)
            // Check for visible threats (Player or Enemies)
            // Simple check: Is the player visible AND close enough?
            val isPlayerVisible = playerVisiblePoints.contains(
              entity.position
            ) && entity.position.getChebyshevDistance(target.position) <= 5

            // Check for nearby enemies (simulating vision range of 5)
            // Use pre-filtered list
            val visibleEnemies = allEnemies.filter(e =>
              e.position.getChebyshevDistance(entity.position) <= 5
            )

            val threats =
              if (isPlayerVisible) visibleEnemies :+ target else visibleEnemies

            if (threats.nonEmpty) {
              getFleeAction(entity, threats, gameState)
            } else {
              // Idle / Wander
              // 20% chance to move randomly if no threat
              if (scala.util.Random.nextDouble() < 0.2) {
                val neighbors = entity.position.neighbors.filterNot(
                  gameState.movementBlockingPoints.contains
                )
                if (neighbors.nonEmpty) {
                  val randomMove =
                    neighbors(scala.util.Random.nextInt(neighbors.size))
                  InputEvent(
                    entity.id,
                    InputAction.Move(
                      Direction.fromPoints(entity.position, randomMove)
                    )
                  )
                } else {
                  InputEvent(entity.id, InputAction.Wait)
                }
              } else {
                InputEvent(entity.id, InputAction.Wait)
              }
            }
          } else {
            // Existing Enemy Logic
            // Optimization: Check if enemy is in player's line of sight instead of computing enemy's LOS
            val enemyPosition = entity.position
            val enemyIsVisibleToPlayer =
              playerVisiblePoints.contains(enemyPosition)

            if (enemyIsVisibleToPlayer) {
              if (isBoss(entity)) {
                // Use boss-specific AI
                getBossAction(entity, target, gameState)
              } else {
                // Use regular enemy AI
                val rangedAbilities =
                  entity.usableItems(gameState).filter { item =>
                    item.get[UsableItem] match {
                      case Some(usableItem) =>
                        usableItem.targeting match {
                          case EnemyActor(range) =>
                            range > 1 // Ranged abilities have range > 1
                          case _ => false
                        }
                      case None => false
                    }
                  }

                // Try ranged attack first if available and target is in range
                rangedAbilities.headOption match {
                  case Some(rangedAbility) =>
                    val usableItem = rangedAbility.get[UsableItem].get
                    val range = usableItem.targeting match {
                      case EnemyActor(r) => r
                      case _             => 1
                    }
                    if (entity.isWithinRangeOfHitbox(target, range)) {
                      // Use ranged ability
                      InputEvent(
                        entity.id,
                        InputAction.UseItem(
                          rangedAbility.id,
                          usableItem,
                          UseContext(entity.id, Some(target))
                        )
                      )
                    } else {
                      // Move closer to get in range
                      Pathfinder.getNextStep(
                        entity.position,
                        target.position,
                        gameState
                      ) match {
                        case Some(nextStep) =>
                          InputEvent(entity.id, InputAction.Move(nextStep))
                        case None =>
                          InputEvent(entity.id, InputAction.Wait)
                      }
                    }
                  case None =>
                    // No ranged abilities, use melee
                    val meleeRange = 1
                    if (entity.isWithinRangeOfHitbox(target, meleeRange)) {
                      InputEvent(entity.id, InputAction.Attack(target))
                    } else {
                      Pathfinder.getNextStep(
                        entity.position,
                        target.position,
                        gameState
                      ) match {
                        case Some(nextStep) =>
                          InputEvent(entity.id, InputAction.Move(nextStep))
                        case None =>
                          InputEvent(entity.id, InputAction.Wait)
                      }
                    }
                }
              }
            } else {
              InputEvent(entity.id, InputAction.Wait)
            }
          }
      }

      (gameState, aiEvents)
    } else {
      (gameState, Nil)
    }
  }
}
