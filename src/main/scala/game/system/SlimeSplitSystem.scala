package game.system

import data.Sprites
import game.entity.*
import game.entity.Experience.experienceForLevel
import game.system.event.GameSystemEvent
import game.system.event.GameSystemEvent.*
import game.{Direction, GameState, Point}

object SlimeSplitSystem extends GameSystem {
  override def update(gameState: GameState, events: Seq[GameSystemEvent]): (GameState, Seq[GameSystemEvent]) = {
    val slimeSplitEvents = events.collect {
      case event: SlimeSplitEvent => event
    }
    
    val newEvents = slimeSplitEvents.flatMap { slimeSplitEvent =>
      createSlimelets(slimeSplitEvent.slimePosition, slimeSplitEvent.killerId, gameState)
    }
    
    (gameState, newEvents)
  }
  
  private def createSlimelets(slimePosition: Point, killerId: Option[String], gameState: GameState): Seq[GameSystemEvent] = {
    // Get all adjacent positions
    val adjacentPositions = Seq(Direction.Up, Direction.Down, Direction.Left, Direction.Right)
      .map(dir => slimePosition + Direction.asPoint(dir))
    
    // Filter to only empty positions (not blocked by movement)
    val emptyPositions = adjacentPositions.filterNot(gameState.movementBlockingPoints.contains)
    
    // Create up to 2 slimelets at empty positions
    val slimeletPositions = emptyPositions.take(2)
    val slimeletEvents = slimeletPositions.zipWithIndex.map { case (position, index) =>
      val slimeletId = s"Slimelet-${System.currentTimeMillis()}-$index"
      
      val slimelet = Entity(
        id = slimeletId,
        Movement(position = position),
        EntityTypeComponent(EntityType.Enemy),
        Health(10),
        Initiative(8),
        Inventory(Nil, None), // No weapon for slimelets, they use default 1 damage
        Drawable(Sprites.slimeletSprite),
        Hitbox(),
        DeathEvents(deathDetails => deathDetails.killerId.map {
          killerId => AddExperienceEvent(killerId, experienceForLevel(1) / 4)
        }.toSeq)
      )
      
      SpawnEntityEvent(slimelet)
    }
    
    slimeletEvents
  }
}