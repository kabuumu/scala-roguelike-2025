package indigoengine.view

import game.entity.{Entity, Equipment}
import game.entity.Equipment.*
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
    import game.entity.{PotionItem, ScrollItem, BowItem}
    import data.Sprites
    
    val player = model.gameState.playerEntity
    val usableItems = player.usableItems(model.gameState)
    
    if (usableItems.isEmpty) {
      Batch.empty
    } else {
      val startX = defaultBorderSize
      val startY = uiYOffset + 150 // Position below health bar
      val itemSize = spriteScale
      val itemSpacing = itemSize + defaultBorderSize
      
      // Title
      val title = text("Usable Items", startX, startY - defaultBorderSize * 2)
      
      // Display each usable item
      val itemDisplays = usableItems.zipWithIndex.map { case (item, index) =>
        val itemX = startX + (index * itemSpacing)
        val itemY = startY
        
        // Determine sprite based on item type
        val sprite = if (item.has[PotionItem]) Sprites.potionSprite
                    else if (item.has[ScrollItem]) Sprites.scrollSprite  
                    else if (item.has[BowItem]) Sprites.bowSprite
                    else Sprites.defaultItemSprite
        
        spriteSheet.fromSprite(sprite).moveTo(itemX, itemY)
      }
      
      Seq(title).toBatch ++ itemDisplays.toBatch
    }
  }
  
  def keys(model: GameController, spriteSheet: Graphic[?]): Batch[SceneNode] = {
    // TODO: Reimplement keys display for new component system  
    Batch.empty
  }

  def equipmentPaperdoll(model: GameController, spriteSheet: Graphic[?]): Batch[SceneNode] = {
    import game.entity.Equipment.*

    val player = model.gameState.playerEntity
    val equipment = player.equipment
    
    // Position paperdoll on the right side of the screen with more space
    val paperdollWidth = spriteScale * 4
    val paperdollHeight = spriteScale * 5
    val paperdollX = canvasWidth - paperdollWidth - defaultBorderSize
    val paperdollY = uiYOffset
    
    // Create background for paperdoll
    val background = BlockBar.getBlockBar(
      Rectangle(Point(paperdollX - defaultBorderSize, paperdollY - defaultBorderSize), 
                Size(paperdollWidth + (defaultBorderSize * 2), paperdollHeight + (defaultBorderSize * 2))),
      RGBA.SlateGray.withAlpha(0.3f)
    )
    
    // Title
    val title = text("Equipment", paperdollX, paperdollY - (defaultBorderSize * 3))
    
    // Helmet slot with better spacing
    val helmetY = paperdollY + spriteScale
    val helmetSlotX = paperdollX + spriteScale * 3
    val helmetSlot = BlockBar.getBlockBar(
      Rectangle(Point(helmetSlotX, helmetY), Size(spriteScale, spriteScale)),
      RGBA.SlateGray.withAlpha(0.5f)
    )
    val helmetLabel = text("Helmet:", paperdollX + defaultBorderSize, helmetY + (spriteScale / 4))
    
    val helmetItem = equipment.helmet.map { helmet =>
      val sprite = helmet.itemName match {
        case "Leather Helmet" => data.Sprites.leatherHelmetSprite
        case "Iron Helmet" => data.Sprites.ironHelmetSprite
        case _ => data.Sprites.defaultItemSprite
      }
      spriteSheet.fromSprite(sprite).moveTo(helmetSlotX, helmetY)
    }.toSeq
    
    // Armor slot with better spacing  
    val armorY = paperdollY + (spriteScale * 2) + defaultBorderSize
    val armorSlotX = paperdollX + spriteScale * 3
    val armorSlot = BlockBar.getBlockBar(
      Rectangle(Point(armorSlotX, armorY), Size(spriteScale, spriteScale)),
      RGBA.SlateGray.withAlpha(0.5f)
    )
    val armorLabel = text("Armor:", paperdollX + defaultBorderSize, armorY + (spriteScale / 4))
    
    val armorItem = equipment.armor.map { armor =>
      val sprite = armor.itemName match {
        case "Chainmail Armor" => data.Sprites.chainmailArmorSprite
        case "Plate Armor" => data.Sprites.plateArmorSprite
        case _ => data.Sprites.defaultItemSprite
      }
      spriteSheet.fromSprite(sprite).moveTo(armorSlotX, armorY)
    }.toSeq
    
    // Equipment stats with better positioning
    val totalDamageReduction = equipment.getTotalDamageReduction
    val statsY = paperdollY + (spriteScale * 4)
    val statsText = text(s"Total DR: $totalDamageReduction", paperdollX + defaultBorderSize, statsY)
    
    Seq(background, title, helmetSlot, helmetLabel, armorSlot, armorLabel, statsText).toBatch ++ 
    helmetItem.toBatch ++ armorItem.toBatch
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
