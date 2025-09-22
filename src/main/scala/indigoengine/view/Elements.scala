package indigoengine.view

import game.entity
import game.entity.ChargeType.SingleUse
import game.entity.EquipmentSlot.Weapon
import game.entity.{ChargeType, Drawable, Entity, Equipment, NameComponent}
import game.status.StatusEffect
import indigo.*
import indigo.Batch.toBatch
import indigoengine.SpriteExtension.*
import ui.UIConfig.*
import ui.{GameController, UIState}
import generated.PixelFont
import generated.Assets
import generated.PixelFontSmall

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
    import data.Sprites
    import game.entity.Ammo.isAmmo
    import game.entity.Inventory.*
    import game.entity.{Ammo, Targeting, UsableItem}
    import game.system.event.GameSystemEvent.HealEvent
    
    val player = model.gameState.playerEntity
    val usableItems = player.usableItems(model.gameState)
    val allInventoryItems = player.inventoryItems(model.gameState)
    
    // Check if we're in item selection mode
    val selectedItemIndex = model.uiState match {
      case listSelect: UIState.ListSelect[?] if listSelect.list.nonEmpty && listSelect.list.head.isInstanceOf[Entity] =>
        // Check if the list contains usable items
        val entities = listSelect.list.asInstanceOf[Seq[Entity]]
        if (entities.exists(_.has[UsableItem])) {
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
      
      val itemTypesWithCounts = usableItems.distinctBy(_.get[NameComponent]).map {
        itemEntity =>
          val sprite = itemEntity.get[Drawable].flatMap(_.sprites.headOption.map(_._2)).getOrElse(Sprites.defaultItemSprite) //Temp code, need explicit way to set item sprite
          val count = itemEntity.get[UsableItem].map(_.chargeType) match {
            case Some(ChargeType.Ammo(requiredAmmo)) => allInventoryItems.count(_.exists[Ammo](_.ammoType == requiredAmmo))
            case _ => usableItems.count(_.get[NameComponent] == itemEntity.get[NameComponent])
          }

          sprite -> count
      }
      
      // Display each item type with count and optional highlighting
      val itemDisplays = itemTypesWithCounts.zipWithIndex.flatMap {
        case ((sprite, count), displayIndex) =>
          val itemX = startX + (displayIndex * itemSpacing)
          val itemY = startY
          
          // Check if this display item should be highlighted
          val isHighlighted = selectedItemIndex.contains(displayIndex)
          
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
    import data.Sprites
    import game.entity.Inventory.*
    import game.entity.KeyColour
    import game.entity.KeyItem.*
    
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
    val paperdollWidth = spriteScale * 5
    val paperdollHeight = spriteScale * 6
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
    
    // Helmet slot (top center)
    val helmetY = paperdollY + spriteScale / 2
    val helmetSlotX = paperdollX + spriteScale * 2
    val helmetSlot = BlockBar.getBlockBar(
      Rectangle(Point(helmetSlotX, helmetY), Size(spriteScale, spriteScale)),
      RGBA.SlateGray.withAlpha(0.5f)
    )
    
    val helmetItem = equipment.helmet.map { helmet =>
      val sprite = helmet.itemName match {
        case "Leather Helmet" => data.Sprites.leatherHelmetSprite
        case "Iron Helmet" => data.Sprites.ironHelmetSprite
        case _ => data.Sprites.defaultItemSprite
      }
      spriteSheet.fromSprite(sprite).moveTo(helmetSlotX, helmetY)
    }.toSeq
    
    // Weapon slot (left middle)
    val weaponY = paperdollY + (spriteScale * 2)
    val weaponSlotX = paperdollX + spriteScale / 2
    val weaponSlot = BlockBar.getBlockBar(
      Rectangle(Point(weaponSlotX, weaponY), Size(spriteScale, spriteScale)),
      RGBA.SlateGray.withAlpha(0.5f)
    )
    
    val weaponItem = equipment.weapon.map { weapon =>
      val sprite = weapon.itemName match {
        case "Basic Sword" => data.Sprites.basicSwordSprite
        case "Iron Sword" => data.Sprites.ironSwordSprite
        case _ => data.Sprites.defaultItemSprite
      }
      spriteSheet.fromSprite(sprite).moveTo(weaponSlotX, weaponY)
    }.toSeq
    
    // Armor slot (center middle)
    val armorY = paperdollY + (spriteScale * 2)
    val armorSlotX = paperdollX + spriteScale * 2
    val armorSlot = BlockBar.getBlockBar(
      Rectangle(Point(armorSlotX, armorY), Size(spriteScale, spriteScale)),
      RGBA.SlateGray.withAlpha(0.5f)
    )
    
    val armorItem = equipment.armor.map { armor =>
      val sprite = armor.itemName match {
        case "Chainmail Armor" => data.Sprites.chainmailArmorSprite
        case "Plate Armor" => data.Sprites.plateArmorSprite
        case _ => data.Sprites.defaultItemSprite
      }
      spriteSheet.fromSprite(sprite).moveTo(armorSlotX, armorY)
    }.toSeq
    
    // Gloves slot (right middle)
    val glovesY = paperdollY + (spriteScale * 2)
    val glovesSlotX = paperdollX + spriteScale * 7 / 2
    val glovesSlot = BlockBar.getBlockBar(
      Rectangle(Point(glovesSlotX, glovesY), Size(spriteScale, spriteScale)),
      RGBA.SlateGray.withAlpha(0.5f)
    )
    
    val glovesItem = equipment.gloves.map { gloves =>
      val sprite = gloves.itemName match {
        case "Leather Gloves" => data.Sprites.leatherGlovesSprite
        case "Iron Gloves" => data.Sprites.ironGlovesSprite
        case _ => data.Sprites.defaultItemSprite
      }
      spriteSheet.fromSprite(sprite).moveTo(glovesSlotX, glovesY)
    }.toSeq
    
    // Boots slot (bottom center)
    val bootsY = paperdollY + (spriteScale * 7 / 2)
    val bootsSlotX = paperdollX + spriteScale * 2
    val bootsSlot = BlockBar.getBlockBar(
      Rectangle(Point(bootsSlotX, bootsY), Size(spriteScale, spriteScale)),
      RGBA.SlateGray.withAlpha(0.5f)
    )
    
    val bootsItem = equipment.boots.map { boots =>
      val sprite = boots.itemName match {
        case "Leather Boots" => data.Sprites.leatherBootsSprite
        case "Iron Boots" => data.Sprites.ironBootsSprite
        case _ => data.Sprites.defaultItemSprite
      }
      spriteSheet.fromSprite(sprite).moveTo(bootsSlotX, bootsY)
    }.toSeq
    
    // Equipment stats with better positioning to avoid overlap
    val totalDamageReduction = equipment.getTotalDamageReduction
    val totalDamageBonus = equipment.getTotalDamageBonus
    val statsY = paperdollY + (spriteScale * 5) + (spriteScale / 2)
    val drText = text(s"DR: $totalDamageReduction", paperdollX + defaultBorderSize, statsY)
    val dmgText = text(s"DMG: +$totalDamageBonus", paperdollX + defaultBorderSize + (spriteScale * 2), statsY)
    
    Seq(background, title, 
        helmetSlot,
        weaponSlot,
        armorSlot,
        glovesSlot,
        bootsSlot,
        drText, dmgText).toBatch ++ 
    helmetItem.toBatch ++ weaponItem.toBatch ++ armorItem.toBatch ++ glovesItem.toBatch ++ bootsItem.toBatch
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
    import game.entity.Hitbox.*

    if (enemyEntity.entityType == game.entity.EntityType.Enemy) {
      val hitboxCount = enemyEntity.get[game.entity.Hitbox].map(_.points.size).getOrElse(1)
      val entitySize = Math.max(1, hitboxCount / 2)
      
      val game.Point(xPosition, yPosition) = enemyEntity.position
      
      val currentHealth = enemyEntity.currentHealth
      val maxHealth = enemyEntity.maxHealth

      val barWidth = entitySize * spriteScale // Total width of the health bar
      val barHeight = spriteScale / 5 // Height of the health bar

      // Check if this is a multi-tile entity (more than just the default (0,0) hitbox point)
      val hitboxPoints = enemyEntity.get[game.entity.Hitbox].map(_.points).getOrElse(Set(game.Point(0, 0)))
      val isMultiTile = hitboxPoints.size > 1

      val xOffset = (xPosition * spriteScale) + ((entitySize * spriteScale - barWidth) / 2)
      val yOffset = (yPosition * spriteScale) + (entitySize * spriteScale)

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
    
    // Position version info above the message window
    val versionText = s"v${Version.gitCommit}"
    val xOffset = uiXOffset
    val yOffset = canvasHeight - (spriteScale * 3) - defaultBorderSize // Above message window
    
    Batch(
      text(versionText, xOffset, yOffset)
    )
  }

  def messageWindow(model: GameController): Batch[SceneNode] = {
    import game.entity.{UsableItem, Equippable, EntityType, Health, NameComponent}
    import game.entity.EntityType.*
    import game.entity.Health.*
    import game.entity.Equippable.*
    import game.entity.UsableItem.*
    import game.entity.NameComponent.*
    import game.Direction
    import game.entity.Movement.*
    
    val messageContent = model.uiState match {
      case UIState.Move =>
        // Check for nearby action targets
        val actionTargets = model.nearbyActionTargets()
        
        if (actionTargets.nonEmpty) {
          val firstTarget = actionTargets.head
          if (actionTargets.length == 1) {
            s"${firstTarget.description}. Press Space/E/Enter for action."
          } else {
            s"${actionTargets.length} actions available. Press Space/E/Enter to choose."
          }
        } else {
          // No actions available
          val moveKeys = "Arrow keys or WASD to move"
          val useItems = "U to use items"
          
          s"$moveKeys, $useItems"
        }
        
      case listSelect: UIState.ListSelect[_] =>
        if (listSelect.list.nonEmpty) {
          val selectedItem = listSelect.list(listSelect.index)
          selectedItem match {
            case actionTarget: ui.GameController.ActionTarget =>
              // Show action description for unified action targets
              s"${actionTarget.description}. Press Space/E/Enter to confirm."
            case entity: Entity if entity.has[UsableItem] =>
              // Show item name at top, then description
              val itemName = entity.name.getOrElse("Unknown Item")
              entity.get[UsableItem] match {
                case Some(usableItem) =>
                  val targetType = usableItem.targeting match {
                    case game.entity.Targeting.Self => "Self-targeted"
                    case game.entity.Targeting.EnemyActor(range) => s"Enemy-targeted (range: $range)"
                    case game.entity.Targeting.TileInRange(range) => s"Tile-targeted (range: $range)"
                  }
                  val consumeText = if (usableItem.chargeType == SingleUse) "consumed on use" else "reusable"
                  val ammoText = usableItem.chargeType match {
                    case ChargeType.Ammo(ammo) => s", requires ${ammo.toString}"
                    case _ => ""
                  }
                  val description = entity.description.getOrElse("")
                  val descriptionText = if (description.nonEmpty) s" - $description" else ""
                  s"$itemName$descriptionText. $targetType item, $consumeText$ammoText. Press Space/E/Enter to use."
                case None => s"$itemName. Press Space/E/Enter to use."
              }
            case entity: Entity =>
              // Show entity information
              val entityTypeName = entity.entityType match {
                case Enemy => "Enemy"
                case Player => "Player"
                case _ => entity.entityType.toString
              }
              val healthText = if (entity.has[game.entity.Health]) {
                s" (${entity.currentHealth}/${entity.maxHealth} HP)"
              } else ""
              s"$entityTypeName$healthText. Press Space/E/Enter to target."
            case statusEffect: game.status.StatusEffect =>
              // Show perk description
              s"${statusEffect.name}: ${statusEffect.description}. Press Space/E/Enter to select."
            case _ =>
              "Press Space/E/Enter to select, Escape to cancel."
          }
        } else {
          "No items available."
        }
        
      case scrollSelect: UIState.ScrollSelect =>
        val x = scrollSelect.cursor.x
        val y = scrollSelect.cursor.y
        s"Target: [$x,$y]. Press Enter to confirm action at this location."
      case _: UIState.MainMenu =>
        "" // No message window content for main menu
    }
    
    // Position message window at the very bottom of the visible canvas area
    val messageWindowHeight = spriteScale * 2
    val messageY = canvasHeight - messageWindowHeight // Position at bottom of game canvas, visible
    val messageX = uiXOffset
    val messageWidth = canvasWidth - (uiXOffset * 2)
    
    // Create background
    val background = BlockBar.getBlockBar(
      Rectangle(Point(messageX - defaultBorderSize, messageY - defaultBorderSize), 
               Size(messageWidth + (defaultBorderSize * 2), messageWindowHeight + (defaultBorderSize * 2))),
      RGBA.Black.withAlpha(0.8)
    )
    
    // Wrap and display text
    val wrappedLines = wrapText(messageContent, messageWidth) // Approximate character width
    val textElements = wrappedLines.zipWithIndex.map { case (line, index) =>
      text(line, messageX, messageY + (index * (spriteScale / 2)))
    }
    
    Batch(background) ++ textElements.toBatch
  }

  def mainMenu(model: GameController): Batch[SceneNode] = {
    model.uiState match {
      case mainMenu: UIState.MainMenu =>
        val titleText = "Scala Roguelike 2025"
        val titleX = (canvasWidth - (titleText.length * 8)) / 2 // Center title roughly
        val titleY = canvasHeight / 3
        
        val title = text(titleText, titleX, titleY)
        
        val menuOptions = mainMenu.options.zipWithIndex.map { case (option, index) =>
          val optionY = titleY + (spriteScale * 3) + (index * spriteScale * 2)
          val optionX = (canvasWidth - (option.length * 8)) / 2 // Center options roughly
          val isSelected = index == mainMenu.selectedOption
          
          val optionText = if (isSelected) s"> $option <" else s"  $option  "
          text(optionText, optionX - 16, optionY)
        }
        
        val instructions = text("Use Arrow Keys and Enter", (canvasWidth - 160) / 2, canvasHeight - spriteScale * 2)
        
        Batch(title) ++ menuOptions.toBatch ++ Batch(instructions)
      case _ =>
        Batch.empty
    }
  }
}
