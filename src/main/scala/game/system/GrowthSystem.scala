package game.system

import game.GameState
import game.entity.{Growth, Drawable, Initiative}
import game.entity.Initiative.*
import game.system.event.GameSystemEvent.GameSystemEvent

object GrowthSystem extends GameSystem {
  override def update(
      gameState: GameState,
      events: Seq[GameSystemEvent]
  ): (GameState, Seq[GameSystemEvent]) = {

    // Process all entities with Growth component that are ready (initiative == 0)
    val updatedEntities = gameState.entities.map { entity =>
      entity.get[Growth] match {
        case Some(growth)
            if growth.currentStage < growth.maxStage && entity.isReady =>
          // Time to grow!
          val grownGrowth = growth.grow

          // Update entity with new Growth component AND new Sprite
          val spriteUpdate =
            grownGrowth.currentSprite.map(s => Drawable(s))

          // Apply updates and reset initiative for next growth cycle
          entity
            .update[Growth](_ => grownGrowth)
            .update[Drawable](_ =>
              spriteUpdate.getOrElse(
                entity.get[Drawable].getOrElse(Drawable(game.Sprite(0, 0, 0)))
              )
            )
            .resetInitiative()

        case _ => entity
      }
    }

    (gameState.copy(entities = updatedEntities), Seq.empty)
  }
}
