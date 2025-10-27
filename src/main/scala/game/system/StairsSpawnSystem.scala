package game.system

import data.Sprites
import game.{GameState, Point}
import game.entity.*
import game.entity.EntityType.entityType
import game.entity.EnemyTypeComponent.enemyType
import game.system.event.GameSystemEvent
import game.system.event.GameSystemEvent.GameSystemEvent
import data.Enemies.EnemyReference
import map.Dungeon

/**
 * Spawns stairs when the boss is defeated.
 * Stairs allow the player to descend to the next dungeon floor.
 */
object StairsSpawnSystem extends GameSystem {
  override def update(gameState: GameState, events: Seq[GameSystemEvent]): (GameState, Seq[GameSystemEvent]) = {
    // Check if stairs already exist
    val stairsAlreadyExist = gameState.entities.exists(_.entityType == EntityType.Stairs)
    
    if (stairsAlreadyExist) {
      // Stairs already spawned, nothing to do
      (gameState, Nil)
    } else {
      // Check if boss was defeated by looking for all enemies
      // If boss room exists but no boss entity, boss was defeated
      val hasBossRoom = gameState.worldMap.primaryDungeon.exists(_.hasBossRoom)
      val bossExists = gameState.entities.exists(entity =>
        entity.entityType == EntityType.Enemy && entity.enemyType.contains(EnemyReference.Boss)
      )
      
      if (hasBossRoom && !bossExists) {
        // Boss was defeated, spawn stairs at endpoint room center
        gameState.worldMap.primaryDungeon.flatMap(_.endpoint) match {
          case Some(endpoint) =>
            val stairsPosition = Point(
              endpoint.x * Dungeon.roomSize + Dungeon.roomSize / 2,
              endpoint.y * Dungeon.roomSize + Dungeon.roomSize / 2
            )
            
            val stairsEntity = Entity(
              id = s"stairs-floor-${gameState.dungeonFloor}",
              Movement(position = stairsPosition),
              EntityTypeComponent(EntityType.Stairs),
              Drawable(Sprites.stairsSprite),
              NameComponent("Stairs")
            )
            
            val updatedState = gameState.add(stairsEntity)
            val message = s"Stairs appeared! Press Space when adjacent to descend to floor ${gameState.dungeonFloor + 1}."
            (updatedState.addMessage(message), Nil)
          case None =>
            (gameState, Nil)
        }
      } else {
        (gameState, Nil)
      }
    }
  }
}
