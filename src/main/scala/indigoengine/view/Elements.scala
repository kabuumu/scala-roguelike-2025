package indigoengine.view

import game.Item.Item
import game.entity.Entity
import game.status.StatusEffect
import generated.{Assets, PixelFont, PixelFontSmall}
import indigo.*
import indigo.Batch.toBatch
import indigoengine.SpriteExtension.*
import ui.UIConfig.*
import ui.{GameController, UIState}

object Elements {
  def text(text: String, x: Int, y: Int): SceneNode = Text(
    text,
    x,
    y,
    PixelFont.fontKey,
    Assets.assets.generated.PixelFontMaterial
  )

  def wrapText(text: String, maxLineLength: Int): Seq[String] = {
    text.split("\\s+").foldLeft(Seq("")) { (lines, word) =>
      val currentLine = lines.last
      if (currentLine.isEmpty) lines.init :+ word
      else if ((currentLine.length + 1 + word.length) <= maxLineLength)
        lines.init :+ (currentLine + " " + word)
      else
        lines :+ word
    }
  }

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
    ) :+ text(s"$currentHealth/$maxHealth", xOffset + barWidth + defaultBorderSize, yOffset)
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
      Some(text("Press 'L' to level up!", xOffset + barWidth + defaultBorderSize, yOffset - defaultBorderSize))
    else None).toSeq.toBatch
  }

  def usableItems(model: GameController, spriteSheet: Graphic[?]): Batch[SceneNode] = {
    import game.entity.Inventory.*
    // Group items and count their occurrences
    val groupedItems = model.gameState.playerEntity.groupedUsableItems

    val itemSprites = for {((item, quantity), index) <- groupedItems.zipWithIndex} yield {
      val itemX: Int = uiXOffset + index * ((spriteScale * 3) / 2)
      val itemY: Int = uiYOffset + (spriteScale / 2) + spriteScale + defaultBorderSize
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
  
  def keys(model: GameController, spriteSheet: Graphic[?]): Batch[SceneNode] = {
    import game.entity.Inventory.*

    // Get the keys from the player's inventory
    val keys = model.gameState.playerEntity.keys

    val keySprites = for {(key, index) <- keys.zipWithIndex} yield {
      val itemX: Int = uiXOffset + index * uiItemScale
      val itemY: Int = uiYOffset + (spriteScale / 2) + spriteScale + spriteScale + (defaultBorderSize * 2)
      val sprite = data.Sprites.itemSprites(key)

      Seq(
        spriteSheet.fromSprite(sprite)
          .moveTo(itemX.toInt, itemY.toInt),
      )
    }
    keySprites.flatten.toSeq.toBatch
  }


  def perkSelection(model: GameController): Batch[SceneNode] = {
    model.uiState match {
      case uiState@UIState.ListSelect(list, _, _) if list.head.isInstanceOf[StatusEffect] =>
        val perkCardWidth = spriteScale * 4 // Width of the perk card
        val perkCardHeight = spriteScale * 6 // Height of the perk card

        // Get the possible perks for the player
        val perks = uiState.list.asInstanceOf[Seq[StatusEffect]]

        val numPerks = perks.size
        val spacing = spriteScale * 2
        val totalWidth = numPerks * perkCardWidth + (numPerks - 1) * spacing
        val startX = (canvasWidth - totalWidth) / 2

        (for {
          (perk, index) <- perks.zipWithIndex
        } yield {
          val index = perks.indexOf(perk)
          val isChosenPerk: Boolean = uiState.index == index

          val itemX = startX + index * (perkCardWidth + spacing)
          val itemY = spriteScale * 3

          // Draw the perk rectangle
          Seq(
            BlockBar.getBlockBar(
              Rectangle(Point(itemX.toInt, itemY.toInt), Size(perkCardWidth, perkCardHeight)),
              if (isChosenPerk) RGBA.Orange else RGBA.SlateGray
            ),
            Text(
              perk.name,
              itemX + defaultBorderSize,
              itemY + defaultBorderSize,
              PixelFont.fontKey,
              Assets.assets.generated.PixelFontMaterial
            ),
            // Draw the perk description and wrap to fit within the card width
            Text(
              //Wrap the description text if full words are longer than 14 characters on a line
              wrapText(perk.description, 13).mkString("\n"),
              itemX + defaultBorderSize,
              itemY + spriteScale + defaultBorderSize,
              PixelFontSmall.fontKey,
              Assets.assets.generated.PixelFontSmallMaterial
            )
          )
        }).flatten.toBatch
      case _ =>
        Batch.empty
    }
  }

  def enemyHealthBar(enemyEntity: Entity): Batch[SceneNode] = {
    import game.entity.EntityType.*
    import game.entity.Health.*
    import game.entity.Movement.*

    if (enemyEntity.entityType == game.entity.EntityType.Enemy) {
      val game.Point(xPosition, yPosition) = enemyEntity.position
      
      val currentHealth = enemyEntity.currentHealth
      val maxHealth = enemyEntity.maxHealth

      val barWidth = spriteScale // Total width of the health bar
      val barHeight = spriteScale / 5 // Height of the health bar
      val xOffset = xPosition * spriteScale + uiXOffset - (spriteScale / 2) // X position of the bar
      val yOffset = yPosition * spriteScale + uiYOffset + (spriteScale / 2)// Y position of the bar

      val filledWidth = (currentHealth * barWidth) / maxHealth

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
