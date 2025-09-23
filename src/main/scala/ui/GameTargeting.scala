package ui

import game.{GameState, Direction, Point}
import game.entity.{Entity, EntityType}
import game.entity.EntityType.entityType
import game.entity.Health.isAlive
import game.entity.Hitbox.isWithinRangeOfHitbox
import game.entity.Movement.*
import game.entity.Equippable.isEquippable
import ui.ActionTargets.*

/**
 * Handles targeting logic for finding enemies and action targets.
 * This contains utility methods for range checking and target selection.
 */
object GameTargeting {
  
  def enemiesWithinRange(gameState: GameState, range: Int): Seq[Entity] = {
    gameState.entities.filter { enemyEntity =>
      enemyEntity.entityType == EntityType.Enemy
        &&
        // Use hitbox-aware range checking for multi-tile entities
        enemyEntity.isWithinRangeOfHitbox(gameState.playerEntity, range)
        &&
        gameState.getVisiblePointsFor(gameState.playerEntity).contains(enemyEntity.position)
        &&
        enemyEntity.isAlive
    }.sortBy(enemyEntity => enemyEntity.position.getChebyshevDistance(gameState.playerEntity.position))
  }
  
  def nearbyActionTargets(gameState: GameState): Seq[ActionTarget] = {
    val playerPosition = gameState.playerEntity.position
    val adjacentPositions = Direction.values.map(dir => playerPosition + Direction.asPoint(dir)).toSet
    
    // Get nearby enemies for attacking (range 1)
    val attackTargets = enemiesWithinRange(gameState, 1).map(AttackTarget.apply)
    
    // Get nearby equippable items
    val equipTargets = gameState.entities
      .filter(e => adjacentPositions.contains(e.position))
      .filter(_.isEquippable)
      .map(EquipTarget.apply)
    
    attackTargets ++ equipTargets
  }
}