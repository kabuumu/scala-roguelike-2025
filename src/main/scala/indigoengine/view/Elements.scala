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
    import game.entity.{UsableItem, Ammo, Targeting}
    import game.entity.UsableItem.isUsableItem
    import game.entity.Ammo.isAmmo
    import game.system.event.GameSystemEvent.{HealEvent, CreateProjectileEvent}
    import data.Sprites
    import indigoengine.view.BlockBar
    
    val player = model.gameState.playerEntity
    val usableItems = player.usableItems(model.gameState)
    val allInventoryItems = player.inventoryItems(model.gameState)
    
    // Check if we're in item selection mode
    val selectedItemIndex = model.uiState match {
      case listSelect: UIState.ListSelect[?] if listSelect.list.nonEmpty && listSelect.list.head.isInstanceOf[Entity] =>
        // Check if the list contains usable items
        val entities = listSelect.list.asInstanceOf[Seq[Entity]]
        if (entities.exists(_.isUsableItem)) {
          Some(listSelect.index)
        } else None
      case _ => None
    }
    
    if (usableItems.isEmpty) {
      Batch.empty
    } else {
      val startX = uiXOffset
      val startY = uiYOffset + (spriteScale * 2) + defaultBorderSize // Position below experience bar
      val itemSize = spriteScale
      val itemSpacing = itemSize + defaultBorderSize // Increased spacing between items
      
      // Helper function to determine item type from UsableItem component
      def getItemType(entity: Entity): Option[String] = {
        entity.get[UsableItem] match {
          case Some(usableItem) =>
            usableItem.targeting match {
              case Targeting.Self => 
                // Check if it's a heal effect (potion)
                if (usableItem.effects.exists(_.isInstanceOf[HealEvent])) Some("potion") else None
              case Targeting.TileInRange(_) => Some("scroll") // Tile-targeted items are scrolls
              case Targeting.EnemyActor => Some("bow") // Enemy-targeted items are bows
            }
          case None => None
        }
      }
      
      // Group items by type and count them
      val potionCount = usableItems.count(getItemType(_) == Some("potion"))
      val scrollCount = usableItems.count(getItemType(_) == Some("scroll"))
      val bowCount = usableItems.count(getItemType(_) == Some("bow"))
      val arrowCount = allInventoryItems.count(_.isAmmo)
      
      // Create list of unique item types with counts
      val itemTypesWithCounts = Seq(
        if (potionCount > 0) Some((Sprites.potionSprite, potionCount)) else None,
        if (scrollCount > 0) Some((Sprites.scrollSprite, scrollCount)) else None,
        if (bowCount > 0) Some((Sprites.bowSprite, arrowCount)) else None // Show arrow count for bows
      ).flatten
      
      // Map usable items to their display type index for highlighting
      val itemToDisplayIndex = usableItems.zipWithIndex.map { case (item, _) =>
        getItemType(item) match {
          case Some("potion") => 0
          case Some("scroll") => if (potionCount > 0) 1 else 0
          case Some("bow") => 
            val offset = (if (potionCount > 0) 1 else 0) + (if (scrollCount > 0) 1 else 0)
            offset
          case _ => -1 // Should not happen with proper UsableItem components
        }
      }
      
      // Display each item type with count and optional highlighting
      val itemDisplays = itemTypesWithCounts.zipWithIndex.flatMap { case ((sprite, count), displayIndex) =>
        val itemX = startX + (displayIndex * itemSpacing)
        val itemY = startY
        
        // Check if this display item should be highlighted
        val isHighlighted = selectedItemIndex.exists { selectedIndex =>
          selectedIndex < itemToDisplayIndex.length && itemToDisplayIndex(selectedIndex) == displayIndex
        }
        
        val baseElements = Seq(
          spriteSheet.fromSprite(sprite).moveTo(itemX, itemY),
          text(count.toString, itemX + itemSize - (defaultBorderSize / 2), itemY + itemSize - (defaultBorderSize / 2))
        )
        
        // Add highlight background if selected
        if (isHighlighted) {
          val highlight = BlockBar.getBlockBar(
            Rectangle(Point(itemX - 2, itemY - 2), Size(itemSize + 4, itemSize + 4)),
            RGBA.Orange.withAlpha(0.7f)
          )
          highlight +: baseElements
        } else {
          baseElements
        }
      }
      
      itemDisplays.toBatch
    }
  }
  
  def keys(model: GameController, spriteSheet: Graphic[?]): Batch[SceneNode] = {
    import game.entity.Inventory.*
    import game.entity.KeyItem.*
    import game.entity.KeyColour
    import data.Sprites
    
    val player = model.gameState.playerEntity
    val playerKeys = player.keys(model.gameState)
    
    if (playerKeys.isEmpty) {
      Batch.empty
    } else {
      val startX = uiXOffset
      val startY = uiYOffset + (spriteScale * 3) + (defaultBorderSize * 2) // Position below usable items
      val itemSize = spriteScale
      val itemSpacing = itemSize + defaultBorderSize // Increased spacing between items
      
      // Group keys by color and count them
      val yellowKeyCount = playerKeys.count(_.keyItem.exists(_.keyColour == KeyColour.Yellow))
      val blueKeyCount = playerKeys.count(_.keyItem.exists(_.keyColour == KeyColour.Blue))
      val redKeyCount = playerKeys.count(_.keyItem.exists(_.keyColour == KeyColour.Red))
      
      // Create list of key types with counts
      val keyTypesWithCounts = Seq(
        if (yellowKeyCount > 0) Some((Sprites.yellowKeySprite, yellowKeyCount)) else None,
        if (blueKeyCount > 0) Some((Sprites.blueKeySprite, blueKeyCount)) else None,
        if (redKeyCount > 0) Some((Sprites.redKeySprite, redKeyCount)) else None
      ).flatten
      
      // Display each key type with count
      val keyDisplays = keyTypesWithCounts.zipWithIndex.flatMap { case ((sprite, count), index) =>
        val keyX = startX + (index * itemSpacing)
        val keyY = startY
        
        Seq(
          spriteSheet.fromSprite(sprite).moveTo(keyX, keyY),
          text(count.toString, keyX + itemSize - (defaultBorderSize / 2), keyY + itemSize - (defaultBorderSize / 2))
        )
      }
      
      keyDisplays.toBatch
    }
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

      val filledWidth = if (maxHealth > 0) (currentHealth * barWidth) / maxHealth else 0

      BlockBar.attributeBar(
        bounds = Rectangle(Point(xOffset, yOffset), Size(barWidth, barHeight)),
        filledWidth = filledWidth,
        fullColour = RGBA.Green,
        emptyColour = RGBA.Crimson,
        borderWidth = 1
      )
    } else Batch.empty
  }

  def versionInfo(model: GameController): Batch[SceneNode] = {
    import generated.Version
    
    // Position at bottom-left of screen
    val versionText = s"v${Version.gitCommit}"
    val xOffset = uiXOffset
    val yOffset = canvasHeight - (spriteScale / 2) - defaultBorderSize
    
    Batch(
      text(versionText, xOffset, yOffset)
    )
  }
}
