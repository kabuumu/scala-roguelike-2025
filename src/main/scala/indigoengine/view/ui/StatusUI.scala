package indigoengine.view.ui

import indigo.*
import indigo.Batch.toBatch
import indigoengine.view.Elements
import indigoengine.view.UIUtils
import indigoengine.view.BlockBar
import ui.UIConfig.*
import ui.GameController
import game.entity.Entity

object StatusUI {

  // UI Row system - each row takes up consistent vertical space
  private val uiRowHeight = spriteScale + (defaultBorderSize * 2)
  private def uiRowY(rowIndex: Int): Int = uiYOffset + (rowIndex * uiRowHeight)

  def healthBar(model: GameController): Batch[SceneNode] = {
    import game.entity.Health.*

    val currentHealth = model.gameState.playerEntity.currentHealth
    val maxHealth = model.gameState.playerEntity.maxHealth

    val barWidth = spriteScale * 6 // Total width of the health bar
    val barHeight = (spriteScale / 4) * 3 // Height of the health bar
    val xOffset = uiXOffset // X position of the bar
    val yOffset = uiRowY(0) // Row 0

    val filledWidth = (currentHealth * barWidth) / maxHealth

    BlockBar.attributeBar(
      Rectangle(Point(xOffset, yOffset), Size(barWidth, barHeight)),
      filledWidth,
      RGBA.Green,
      RGBA.Crimson
    ) :+ UIUtils.text(
      s"$currentHealth/$maxHealth",
      xOffset + barWidth + defaultBorderSize,
      yOffset
    )
  }

  def experienceBar(model: GameController): Batch[SceneNode] = {
    import game.entity.Experience.*

    val player = model.gameState.playerEntity

    val currentExp = player.experience
    val nextLevelExp = player.nextLevelExperience

    val drawableCurrentExperience = currentExp - player.previousLevelExperience
    val drawableNextLevelExperience =
      nextLevelExp - player.previousLevelExperience

    val barWidth = spriteScale * 6 // Total width of the experience bar
    val barHeight = spriteScale / 2 // Height of the experience bar
    val xOffset = uiXOffset // X position of the bar
    val yOffset = uiRowY(1) // Row 1

    val filledWidth: Int =
      if (player.canLevelUp) barWidth
      else (drawableCurrentExperience * barWidth) / drawableNextLevelExperience

    BlockBar.attributeBar(
      Rectangle(Point(xOffset, yOffset), Size(barWidth, barHeight)),
      filledWidth,
      RGBA.Orange,
      RGBA.SlateGray
    ) ++ (if (player.canLevelUp)
            Some(
              UIUtils.text(
                "Press 'L' to level up!",
                xOffset + barWidth + defaultBorderSize,
                yOffset - defaultBorderSize
              )
            )
          else None).toSeq.toBatch
  }

  def enemyHealthBar(enemyEntity: Entity): Batch[SceneNode] = {
    import game.entity.EntityType.*
    import game.entity.Health.*
    import game.entity.Movement.*
    import game.entity.Hitbox.*

    if (enemyEntity.entityType == game.entity.EntityType.Enemy) {
      val hitboxCount =
        enemyEntity.get[game.entity.Hitbox].map(_.points.size).getOrElse(1)
      val entitySize = Math.max(1, hitboxCount / 2)

      val game.Point(xPosition, yPosition) = enemyEntity.position

      val currentHealth = enemyEntity.currentHealth
      val maxHealth = enemyEntity.maxHealth

      val barWidth = entitySize * spriteScale // Total width of the health bar
      val barHeight = spriteScale / 5 // Height of the health bar

      // Check if this is a multi-tile entity (more than just the default (0,0) hitbox point)
      val hitboxPoints = enemyEntity
        .get[game.entity.Hitbox]
        .map(_.points)
        .getOrElse(Set(game.Point(0, 0)))
      val isMultiTile = hitboxPoints.size > 1

      val xOffset =
        (xPosition * spriteScale) + ((entitySize * spriteScale - barWidth) / 2)
      val yOffset = (yPosition * spriteScale) + (entitySize * spriteScale)

      val filledWidth =
        if (maxHealth > 0) (currentHealth * barWidth) / maxHealth else 0

      BlockBar.attributeBar(
        bounds = Rectangle(Point(xOffset, yOffset), Size(barWidth, barHeight)),
        filledWidth = filledWidth,
        fullColour = RGBA.Green,
        emptyColour = RGBA.Crimson,
        borderWidth = 1
      )
    } else Batch.empty
  }
}
