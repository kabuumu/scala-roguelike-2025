package indigoengine.view

import game.entity
import game.entity.{Drawable, Entity, Equipment}
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
    import data.Sprites
    import game.entity.Ammo.isAmmo
    import game.entity.Inventory.*
    import game.entity.UsableItem.isUsableItem
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
      
      val itemTypesWithCounts = usableItems.map {
        item =>
          val sprite = item.get[Drawable].flatMap(_.sprites.headOption.map(_._2)).getOrElse(Sprites.defaultItemSprite) //Temp code, need explicit way to set item sprite
          val count = UsableItem.getUsableItem(item).flatMap(_.ammo) match {
            case Some(requiredAmmo) => allInventoryItems.count(_.exists[Ammo](_.ammoType == requiredAmmo))
            case None => usableItems.count(UsableItem.getUsableItem(_) == UsableItem.getUsableItem(item))
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
    import game.entity.Inventory.primaryWeapon
    import game.entity.WeaponItem.weaponItem
    import game.Direction
    import game.entity.Movement.*
    
    val messageContent = model.uiState match {
      case UIState.Move =>
        // Check for nearby equippable items
        val playerPosition = model.gameState.playerEntity.position
        val adjacentPositions = Direction.values.map(dir => playerPosition + Direction.asPoint(dir)).toSet
        
        val adjacentEquippableEntities = model.gameState.entities
          .filter(e => adjacentPositions.contains(e.position))
          .filter(_.isEquippable)
        
        adjacentEquippableEntities.headOption.flatMap(_.equippable) match {
          case Some(equippable) =>
            s"Press Q to equip ${equippable.itemName} (Damage reduction +${equippable.damageReduction})"
          case None =>
            // Check if enemies are within attack range to determine if attack is available
            val optWeaponEntity = model.gameState.playerEntity.primaryWeapon(model.gameState)
            val range = optWeaponEntity.flatMap(_.weaponItem.map(_.range)).getOrElse(1)
            val enemies = model.gameState.entities.filter { enemyEntity =>
              enemyEntity.entityType == EntityType.Enemy &&
              model.gameState.playerEntity.position.isWithinRangeOf(enemyEntity.position, range) &&
              model.gameState.getVisiblePointsFor(model.gameState.playerEntity).contains(enemyEntity.position) &&
              enemyEntity.isAlive
            }
            
            val moveKeys = "Arrow keys or WASD to move"
            val useItems = "U to use items"
            val attackKeys = if (enemies.nonEmpty) ", Z/X to attack" else ""
            val interactKey = ", E to interact"
            val equipKey = ", Q to equip"
            
            s"$moveKeys, $useItems$attackKeys$interactKey$equipKey"
        }
        
      case listSelect: UIState.ListSelect[_] =>
        if (listSelect.list.nonEmpty) {
          val selectedItem = listSelect.list(listSelect.index)
          selectedItem match {
            case entity: Entity if entity.isUsableItem =>
              // Show item name at top, then description
              val itemName = entity.name.getOrElse("Unknown Item")
              entity.usableItem match {
                case Some(usableItem) =>
                  val targetType = usableItem.targeting match {
                    case game.entity.Targeting.Self => "Self-targeted"
                    case game.entity.Targeting.EnemyActor => "Enemy-targeted"
                    case game.entity.Targeting.TileInRange(range) => s"Tile-targeted (range: $range)"
                  }
                  val consumeText = if (usableItem.consumeOnUse) "consumed on use" else "reusable"
                  val ammoText = usableItem.ammo.map(ammo => s", requires ${ammo.toString}").getOrElse("")
                  val description = entity.description.getOrElse("")
                  val descriptionText = if (description.nonEmpty) s" - $description" else ""
                  s"$itemName$descriptionText. $targetType item, $consumeText$ammoText. Press Enter to use."
                case None => s"$itemName. Press Enter to use."
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
              s"$entityTypeName$healthText. Press Enter to target."
            case statusEffect: game.status.StatusEffect =>
              // Show perk description
              s"${statusEffect.name}: ${statusEffect.description}. Press Enter to select."
            case _ =>
              "Press Enter to select, Escape to cancel."
          }
        } else {
          "No items available."
        }
        
      case scrollSelect: UIState.ScrollSelect =>
        val x = scrollSelect.cursor.x
        val y = scrollSelect.cursor.y
        s"Target: [$x,$y]. Press Enter to confirm action at this location."
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
    val wrappedLines = wrapText(messageContent, (messageWidth / 8)) // Approximate character width
    val textElements = wrappedLines.zipWithIndex.map { case (line, index) =>
      text(line, messageX, messageY + (index * (spriteScale / 2)))
    }
    
    Batch(background) ++ textElements.toBatch
  }
}
