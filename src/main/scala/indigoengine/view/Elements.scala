package indigoengine.view

import indigo.*
import ui.{GameController, UIState}
import ui.UIConfig.*
import indigoengine.SpriteExtension.*
import indigo.Batch.toBatch
import game.Item.Item
import generated.PixelFont
import generated.Assets

object Elements {
  def healthBar(model: GameController): Batch[SceneNode] = {
    import game.entity.Health.*

    val currentHealth = model.gameState.playerEntity.currentHealth
    val maxHealth = model.gameState.playerEntity.maxHealth

    val barWidth = spriteScale * 6 // Total width of the health bar
    val barHeight = (spriteScale / 4) * 3 // Height of the health bar
    val xOffset = uiXOffset // X position of the bar
    val yOffset = uiYOffset // Y position of the bar

    val filledWidth = (currentHealth * barWidth) / maxHealth

    BlockBar.attributeBar(
      Rectangle(Point(xOffset, yOffset), Size(barWidth, barHeight)),
      filledWidth,
      RGBA.Green,
      RGBA.Crimson,
    ) :+ text(s"$currentHealth/$maxHealth", xOffset + barWidth + borderSize, yOffset)
  }
  
  def experienceBar(model: GameController): Batch[SceneNode] = {
    import game.entity.Experience.*

    val player = model.gameState.playerEntity
    
    val currentExp = player.experience
    val nextLevelExp = player.nextLevelExperience

    val drawableCurrentExperience = currentExp - player.previousLevelExperience
    val drawableNextLevelExperience = nextLevelExp - player.previousLevelExperience

    // Calculate the width of the filled portion of the experience bar

    val barWidth = spriteScale * 6 // Total width of the experience bar
    val barHeight = spriteScale / 2 // Height of the experience bar
    val xOffset = uiXOffset // X position of the bar
    val yOffset = uiYOffset + spriteScale // Y position of the bar

    val filledWidth: Int = if (player.canLevelUp) barWidth 
    else (drawableCurrentExperience * barWidth) / drawableNextLevelExperience

    BlockBar.attributeBar(
      Rectangle(Point(xOffset, yOffset), Size(barWidth, barHeight)),
      filledWidth,
      RGBA.Orange,
      RGBA.SlateGray,
    ) ++ (if(player.canLevelUp)
      Some(text("Press 'L' to level up!", xOffset + barWidth + borderSize, yOffset - borderSize))
    else None).toSeq.toBatch
  }

  def usableItems(model: GameController, spriteSheet: Graphic[?]): Batch[SceneNode] = {
    import game.entity.Inventory.*
    // Group items and count their occurrences
    val groupedItems = model.gameState.playerEntity.groupedUsableItems

    val itemSprites = for {((item, quantity), index) <- groupedItems.zipWithIndex} yield {
      val itemX: Int = uiXOffset + index * ((spriteScale * 3) / 2)
      val itemY: Int = uiYOffset + (spriteScale / 2) + spriteScale + borderSize
      val sprite = data.Sprites.itemSprites(item)

      Seq(
        spriteSheet.fromSprite(sprite)
        .moveTo(itemX.toInt, itemY.toInt),
        text(s"$quantity", itemX + spriteScale, itemY + 8)
      ) ++ (model.uiState match {
        case UIState.ListSelect(list, selectedIndex, _) if selectedIndex == index && list.head.isInstanceOf[Item] =>
          Some(
            BlockBar.getBlockBar(
              Rectangle(Point(itemX.toInt, itemY.toInt), Size(spriteScale, spriteScale)),
              RGBA.Yellow.withAlpha(0.5f)
            )
          )
        case _ =>
          None
      })
    }
    itemSprites.flatten.toSeq.toBatch
  }
  
  def text(text: String, x: Int, y: Int): SceneNode = Text(
    text,
    x,
    y,
    PixelFont.fontKey,
    Assets.assets.generated.PixelFontMaterial
  )
}
