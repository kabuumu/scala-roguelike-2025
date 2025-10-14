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

  // UI Row system - each row takes up consistent vertical space
  private val uiRowHeight = spriteScale + (defaultBorderSize * 2)
  private def uiRowY(rowIndex: Int): Int = uiYOffset + (rowIndex * uiRowHeight)
  
  // Helper to position count text consistently next to sprites
  private def countTextOffset(spriteX: Int): (Int, Int) = 
    (spriteX + spriteScale, (spriteScale / 4))

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

    val barWidth = spriteScale * 6 // Total width of the experience bar
    val barHeight = spriteScale / 2 // Height of the experience bar
    val xOffset = uiXOffset // X position of the bar
    val yOffset = uiRowY(1) // Row 1

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
      val startY = uiRowY(2) // Row 2 - below experience bar
      val itemSize = spriteScale
      val itemSpacing = itemSize + uiXOffset
      
      val itemTypesWithCounts = usableItems.distinctBy(_.get[NameComponent]).map {
        itemEntity =>
          val sprite = itemEntity.get[Drawable].flatMap(_.sprites.headOption.map(_._2)).getOrElse(Sprites.defaultItemSprite)
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
          
          val (countX, countYOffset) = countTextOffset(itemX)
          val baseElements = Seq(
            spriteSheet.fromSprite(sprite).moveTo(itemX, itemY),
            text(count.toString, countX, itemY + countYOffset)
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
      val startY = uiRowY(3) // Row 3 - below usable items
      val itemSize = spriteScale
      val itemSpacing = itemSize + defaultBorderSize
      
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
        
        val (countX, countYOffset) = countTextOffset(keyX)
        Seq(
          spriteSheet.fromSprite(sprite).moveTo(keyX, keyY),
          text(count.toString, countX, keyY + countYOffset)
        )
      }
      
      keyDisplays.toBatch
    }
  }

  def coins(model: GameController, spriteSheet: Graphic[?]): Batch[SceneNode] = {
    import data.Sprites
    import game.entity.Coins.*
    
    val player = model.gameState.playerEntity
    val coinCount = player.coins
    
    val startX = uiXOffset
    val startY = uiRowY(4) // Row 4 - below keys
    
    val (countX, countYOffset) = countTextOffset(startX)
    Seq(
      spriteSheet.fromSprite(Sprites.coinSprite).moveTo(startX, startY),
      text(s"$coinCount", countX, startY + countYOffset)
    ).toBatch
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
        val actionTargets = ui.GameTargeting.nearbyActionTargets(model.gameState)
        
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
            case actionTarget: ui.ActionTargets.ActionTarget =>
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
            case itemRef: data.Items.ItemReference =>
              // Show item reference for trading
              val tempEntity = itemRef.createEntity("temp")
              val itemName = tempEntity.get[game.entity.NameComponent].map(_.name).getOrElse(itemRef.toString)
              val description = tempEntity.get[game.entity.NameComponent].map(_.description).getOrElse("")
              val descriptionText = if (description.nonEmpty) s" - $description" else ""
              
              // Get price from trader if we're in trade menu
              val priceText = model.uiState match {
                case tradeMenu: UIState.TradeMenu =>
                  tradeMenu.trader.get[game.entity.Trader].flatMap(_.buyPrice(itemRef)) match {
                    case Some(price) => s" (${price} coins)"
                    case None => ""
                  }
                case _ => ""
              }
              
              s"$itemName$descriptionText$priceText. Press Space/E/Enter to buy."
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
      case tradeMenu: UIState.TradeMenu =>
        val selectedOption = tradeMenu.getSelectedOption
        s"Trading Menu - Selected: $selectedOption. Use arrows to navigate, Space/E/Enter to select."
      case _: UIState.MainMenu =>
        "" // No message window content for main menu
      case _: UIState.GameOver =>
        "" // Game over screen handles its own messaging
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

  def tradeItemDisplay(model: GameController, spriteSheet: Graphic[?]): Batch[SceneNode] = {
    import game.entity.EntityType.entityType
    
    model.uiState match {
      case list: UIState.ListSelect[_] if list.list.nonEmpty =>
        list.list(list.index) match {
          case itemRef: data.Items.ItemReference =>
            // Create the item entity to get its information
            val tempEntity = itemRef.createEntity("temp")
            val itemName = tempEntity.get[game.entity.NameComponent].map(_.name).getOrElse(itemRef.toString)
            val description = tempEntity.get[game.entity.NameComponent].map(_.description).getOrElse("")
            
            // Get the sprite for the item
            val sprite = tempEntity.get[Drawable].flatMap(_.sprites.headOption.map(_._2)).getOrElse(data.Sprites.defaultItemSprite)
            
            // Get price information
            val priceOpt = model.gameState.entities.collectFirst {
              case e if e.entityType == game.entity.EntityType.Trader =>
                e.get[game.entity.Trader].flatMap(_.buyPrice(itemRef))
            }.flatten
            
            // Position for the item display (center-left of screen)
            val displayX = canvasWidth / 4
            val displayY = canvasHeight / 3
            val displayWidth = canvasWidth / 2
            val displayHeight = spriteScale * 6
            
            // Background
            val background = BlockBar.getBlockBar(
              Rectangle(Point(displayX - defaultBorderSize, displayY - defaultBorderSize), 
                       Size(displayWidth + (defaultBorderSize * 2), displayHeight + (defaultBorderSize * 2))),
              RGBA.Black.withAlpha(0.9)
            )
            
            // Item sprite (large display)
            val spriteSize = spriteScale * 2
            val spriteX = displayX + defaultBorderSize
            val spriteY = displayY + defaultBorderSize
            val itemSprite = spriteSheet.fromSprite(sprite)
              .moveTo(spriteX, spriteY)
              .scaleBy(2.0, 2.0) // Make it larger
            
            // Item name
            val nameY = spriteY
            val nameX = spriteX + spriteSize + defaultBorderSize
            val nameText = text(itemName, nameX, nameY)
            
            // Price
            val priceY = nameY + spriteScale
            val priceText = priceOpt.map { price =>
              text(s"Price: $price coins", nameX, priceY)
            }.toSeq
            
            // Description (wrapped)
            val descY = priceY + spriteScale
            val maxLineChars = (displayWidth - spriteSize - (defaultBorderSize * 3)) / (spriteScale / 3)
            val wrappedDesc = wrapText(description, maxLineChars)
            val descriptionLines = wrappedDesc.zipWithIndex.map { case (line, idx) =>
              text(line, nameX, descY + (idx * (spriteScale / 2)))
            }
            
            // Get UsableItem info for effects
            val effectsY = descY + (wrappedDesc.length * (spriteScale / 2)) + spriteScale / 2
            val effectsText = tempEntity.get[game.entity.UsableItem].map { usableItem =>
              val targetType = usableItem.targeting match {
                case game.entity.Targeting.Self => "Self-targeted"
                case game.entity.Targeting.EnemyActor(range) => s"Enemy (range: $range)"
                case game.entity.Targeting.TileInRange(range) => s"Area (range: $range)"
              }
              val consumeText = if (usableItem.chargeType == SingleUse) "Single use" else "Reusable"
              Seq(
                text(s"Type: $targetType", nameX, effectsY),
                text(consumeText, nameX, effectsY + spriteScale / 2)
              )
            }.getOrElse(Seq.empty)
            
            Batch(background, itemSprite, nameText) ++ priceText.toBatch ++ descriptionLines.toBatch ++ effectsText.toBatch
          case itemEntity: Entity =>
            // Display entity items (for selling) with same rich UI
            val itemName = itemEntity.get[game.entity.NameComponent].map(_.name).getOrElse("Unknown Item")
            val description = itemEntity.get[game.entity.NameComponent].map(_.description).getOrElse("")
            
            // Get the sprite for the item
            val sprite = itemEntity.get[Drawable].flatMap(_.sprites.headOption.map(_._2)).getOrElse(data.Sprites.defaultItemSprite)
            
            // Get sell price information by finding matching ItemReference
            val priceOpt = {
              val itemRefOpt = itemEntity.get[game.entity.NameComponent].flatMap { nameComp =>
                data.Items.ItemReference.values.find { ref =>
                  val refEntity = ref.createEntity("temp")
                  refEntity.get[game.entity.NameComponent].map(_.name) == Some(nameComp.name)
                }
              }
              
              itemRefOpt.flatMap { itemRef =>
                model.gameState.entities.collectFirst {
                  case e if e.entityType == game.entity.EntityType.Trader =>
                    e.get[game.entity.Trader].flatMap(_.sellPrice(itemRef))
                }.flatten
              }
            }
            
            // Position for the item display (center-left of screen)
            val displayX = canvasWidth / 4
            val displayY = canvasHeight / 3
            val displayWidth = canvasWidth / 2
            val displayHeight = spriteScale * 6
            
            // Background
            val background = BlockBar.getBlockBar(
              Rectangle(Point(displayX - defaultBorderSize, displayY - defaultBorderSize), 
                       Size(displayWidth + (defaultBorderSize * 2), displayHeight + (defaultBorderSize * 2))),
              RGBA.Black.withAlpha(0.9)
            )
            
            // Item sprite (large display)
            val spriteSize = spriteScale * 2
            val spriteX = displayX + defaultBorderSize
            val spriteY = displayY + defaultBorderSize
            val itemSprite = spriteSheet.fromSprite(sprite)
              .moveTo(spriteX, spriteY)
              .scaleBy(2.0, 2.0) // Make it larger
            
            // Item name
            val nameY = spriteY
            val nameX = spriteX + spriteSize + defaultBorderSize
            val nameText = text(itemName, nameX, nameY)
            
            // Sell Price
            val priceY = nameY + spriteScale
            val priceText = priceOpt.map { price =>
              text(s"Sell for: $price coins", nameX, priceY)
            }.toSeq
            
            // Description (wrapped)
            val descY = priceY + spriteScale
            val maxLineChars = (displayWidth - spriteSize - (defaultBorderSize * 3)) / (spriteScale / 3)
            val wrappedDesc = wrapText(description, maxLineChars)
            val descriptionLines = wrappedDesc.zipWithIndex.map { case (line, idx) =>
              text(line, nameX, descY + (idx * (spriteScale / 2)))
            }
            
            // Get UsableItem or Equippable info for effects
            val effectsY = descY + (wrappedDesc.length * (spriteScale / 2)) + spriteScale / 2
            val effectsText = itemEntity.get[game.entity.UsableItem].map { usableItem =>
              val targetType = usableItem.targeting match {
                case game.entity.Targeting.Self => "Self-targeted"
                case game.entity.Targeting.EnemyActor(range) => s"Enemy (range: $range)"
                case game.entity.Targeting.TileInRange(range) => s"Area (range: $range)"
              }
              val consumeText = if (usableItem.chargeType == SingleUse) "Single use" else "Reusable"
              Seq(
                text(s"Type: $targetType", nameX, effectsY),
                text(consumeText, nameX, effectsY + spriteScale / 2)
              )
            }.orElse {
              // Show equipment stats
              itemEntity.get[game.entity.Equippable].map { equippable =>
                val slotText = s"Slot: ${equippable.slot}"
                val statsText = if (equippable.damageReduction > 0) {
                  s"Defense: +${equippable.damageReduction}"
                } else if (equippable.damageBonus > 0) {
                  s"Damage: +${equippable.damageBonus}"
                } else {
                  "No special stats"
                }
                Seq(
                  text(slotText, nameX, effectsY),
                  text(statsText, nameX, effectsY + spriteScale / 2)
                )
              }
            }.getOrElse(Seq.empty)
            
            Batch(background, itemSprite, nameText) ++ priceText.toBatch ++ descriptionLines.toBatch ++ effectsText.toBatch
          case _ => Batch.empty
        }
      case _ => Batch.empty
    }
  }

  def gameOverScreen(model: GameController, player: Entity): Batch[SceneNode] = {
    import game.entity.EventMemory.*
    import game.entity.MemoryEvent
    import game.entity.Equipment.*
    import game.entity.EntityType
    import data.Sprites
    
    val spriteSheet = Graphic(0, 0, 784, 352, Material.Bitmap(AssetName("sprites")))
    
    // Game Over title - large text centered at top
    val titleText = "GAME OVER"
    val titleX = (canvasWidth - (titleText.length * 16)) / 2
    val titleY = spriteScale * 2
    val title = text(titleText, titleX, titleY)
    
    // Statistics section 
    val statsStartY = titleY + spriteScale * 4
    
    // Get events from player memory
    val memoryEvents = player.getMemoryEvents
    val enemiesDefeated = player.getMemoryEventsByType[MemoryEvent.EnemyDefeated]
    val stepsTaken = player.getStepCount
    val itemsUsed = player.getMemoryEventsByType[MemoryEvent.ItemUsed].length
    
    // Group enemies by type and count them
    val enemyStats = enemiesDefeated.groupBy(_.enemyType).view.mapValues(_.length).toSeq.sortBy(_._1)
    
    // Enemies defeated section
    val enemiesHeaderY = statsStartY
    val enemiesHeader = text("Enemies defeated:", uiXOffset, enemiesHeaderY)
    
    val enemyDisplayElements = if (enemyStats.nonEmpty) {
      enemyStats.zipWithIndex.flatMap { case ((enemyType, count), index) =>
        val spriteY = enemiesHeaderY + spriteScale + (index * spriteScale)
        val sprite = enemyType match {
          case "Rat" => Sprites.ratSprite  
          case "Slimelet" => Sprites.slimeletSprite
          case "Slime" => Sprites.slimeSprite
          case "Snake" => Sprites.snakeSprite
          case "Boss" => Sprites.bossSpriteTL  // Use top-left boss sprite
          case "Enemy" => Sprites.enemySprite  // Fallback for generic enemies
          case _ => Sprites.enemySprite  // Default fallback
        }
        
        Seq(
          spriteSheet.fromSprite(sprite).moveTo(uiXOffset, spriteY),
          text(s"x$count", uiXOffset + spriteScale, spriteY)
        )
      }
    } else {
      Seq(text("None", uiXOffset + spriteScale, enemiesHeaderY + spriteScale))
    }
    
    // Other statistics
    val statsY = enemiesHeaderY + spriteScale + (enemyStats.length * spriteScale) + spriteScale
    val stepsText = text(s"Steps travelled: $stepsTaken", uiXOffset, statsY)
    val itemsText = text(s"Items used: $itemsUsed", uiXOffset, statsY + spriteScale)
    
    // Coins collected
    import game.entity.Coins.*
    val totalCoins = player.totalCoinsCollected
    val coinsText = text(s"Coins collected: $totalCoins", uiXOffset, statsY + (spriteScale * 2))
    
    // Final equipment section
    val equipmentY = statsY + spriteScale * 4
    val equipmentHeader = text("Final equipment:", uiXOffset, equipmentY)
    
    val equipment = player.equipment
    val equipmentElements = Seq(
      // Helmet
      equipment.helmet.map { helmet =>
        val sprite = helmet.itemName match {
          case "Leather Helmet" => Sprites.leatherHelmetSprite
          case "Iron Helmet" => Sprites.ironHelmetSprite
          case _ => Sprites.defaultItemSprite
        }
        spriteSheet.fromSprite(sprite).moveTo(uiXOffset, equipmentY + spriteScale)
      },
      // Armor  
      equipment.armor.map { armor =>
        val sprite = armor.itemName match {
          case "Leather Armor" => Sprites.defaultItemSprite  // Use default since leatherArmorSprite doesn't exist
          case "Chainmail Armor" => Sprites.chainmailArmorSprite
          case "Plate Armor" => Sprites.plateArmorSprite
          case _ => Sprites.defaultItemSprite
        }
        spriteSheet.fromSprite(sprite).moveTo(uiXOffset + spriteScale, equipmentY + spriteScale)
      },
      // Weapon
      equipment.weapon.map { weapon =>
        val sprite = weapon.itemName match {
          case "Basic Sword" => Sprites.basicSwordSprite
          case "Iron Sword" => Sprites.ironSwordSprite
          case _ => Sprites.defaultItemSprite
        }
        spriteSheet.fromSprite(sprite).moveTo(uiXOffset + spriteScale * 2, equipmentY + spriteScale)
      },
      // Gloves
      equipment.gloves.map { gloves =>
        val sprite = gloves.itemName match {
          case "Leather Gloves" => Sprites.leatherGlovesSprite
          case "Iron Gloves" => Sprites.ironGlovesSprite
          case _ => Sprites.defaultItemSprite
        }
        spriteSheet.fromSprite(sprite).moveTo(uiXOffset + spriteScale * 3, equipmentY + spriteScale)
      },
      // Boots
      equipment.boots.map { boots =>
        val sprite = boots.itemName match {
          case "Leather Boots" => Sprites.leatherBootsSprite
          case "Iron Boots" => Sprites.ironBootsSprite
          case _ => Sprites.defaultItemSprite
        }
        spriteSheet.fromSprite(sprite).moveTo(uiXOffset + spriteScale * 4, equipmentY + spriteScale)
      }
    ).flatten
    
    // Instructions
    val instructionsY = canvasHeight - spriteScale * 2
    val instructions = text("Press Space/Enter/E to return to main menu", uiXOffset, instructionsY)
    
    Batch(title, enemiesHeader, stepsText, itemsText, coinsText, equipmentHeader, instructions) ++ 
    enemyDisplayElements.toBatch ++ equipmentElements.toBatch
  }
}
