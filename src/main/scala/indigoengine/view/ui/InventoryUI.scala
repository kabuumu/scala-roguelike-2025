package indigoengine.view.ui

import indigo.*
import indigo.Batch.toBatch
import indigoengine.view.UIUtils
import indigoengine.view.BlockBar
import ui.UIConfig.*
import ui.GameController
import ui.UIState
import game.entity.NameComponent
import game.entity.Drawable
import game.entity.ChargeType
import game.entity.Inventory.*
import game.entity.Ammo
import generated.Assets
import indigoengine.SpriteExtension.*

object InventoryUI {

  // UI Row system - each row takes up consistent vertical space
  private val uiRowHeight = spriteScale + (defaultBorderSize * 2)
  private def uiRowY(rowIndex: Int): Int = uiYOffset + (rowIndex * uiRowHeight)

  // Helper to position count text consistently next to sprites
  private def countTextOffset(spriteX: Int): (Int, Int) =
    (spriteX + spriteScale, (spriteScale / 4))

  def usableItems(
      model: GameController,
      spriteSheet: Graphic[?]
  ): Batch[SceneNode] = {
    import data.Sprites
    import game.entity.Ammo.isAmmo
    import game.entity.Inventory.*
    import game.entity.{Ammo, Targeting, UsableItem}
    import game.system.event.GameSystemEvent.HealEvent

    val player = model.gameState.playerEntity
    val usableItems = player.usableItems(model.gameState)
    val allInventoryItems = player.inventoryItems(model.gameState)

    // Check if we're in item selection mode (only for UseItemSelect)
    val selectedItemIndex = model.uiState match {
      case useItemSelect: UIState.UseItemSelect =>
        Some(useItemSelect.index)
      case _ => None
    }

    if (usableItems.isEmpty) {
      Batch.empty
    } else {
      val startX = uiXOffset
      val startY = uiRowY(2) // Row 2 - below experience bar
      val itemSize = spriteScale
      val itemSpacing = itemSize + uiXOffset

      val itemTypesWithCounts =
        usableItems.distinctBy(_.get[NameComponent]).map { itemEntity =>
          val sprite = itemEntity
            .get[Drawable]
            .flatMap(_.sprites.headOption.map(_._2))
            .getOrElse(Sprites.defaultItemSprite)
          val count = itemEntity.get[UsableItem].map(_.chargeType) match {
            case Some(ChargeType.Ammo(requiredAmmo)) =>
              allInventoryItems.count(
                _.exists[Ammo](_.ammoType == requiredAmmo)
              )
            case _ =>
              usableItems.count(
                _.get[NameComponent] == itemEntity.get[NameComponent]
              )
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
            UIUtils.text(count.toString, countX, itemY + countYOffset)
          )

          // Add highlight background if selected
          if (isHighlighted) {
            val highlight = BlockBar.getBlockBar(
              Rectangle(
                Point(itemX - 2, itemY - 2),
                Size(itemSize + 4, itemSize + 4)
              ),
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
      val yellowKeyCount =
        playerKeys.count(_.keyItem.exists(_.keyColour == KeyColour.Yellow))
      val blueKeyCount =
        playerKeys.count(_.keyItem.exists(_.keyColour == KeyColour.Blue))
      val redKeyCount =
        playerKeys.count(_.keyItem.exists(_.keyColour == KeyColour.Red))

      // Create list of key types with counts
      val keyTypesWithCounts = Seq(
        if (yellowKeyCount > 0) Some((Sprites.yellowKeySprite, yellowKeyCount))
        else None,
        if (blueKeyCount > 0) Some((Sprites.blueKeySprite, blueKeyCount))
        else None,
        if (redKeyCount > 0) Some((Sprites.redKeySprite, redKeyCount)) else None
      ).flatten

      // Display each key type with count
      val keyDisplays = keyTypesWithCounts.zipWithIndex.flatMap {
        case ((sprite, count), index) =>
          val keyX = startX + (index * itemSpacing)
          val keyY = startY

          val (countX, countYOffset) = countTextOffset(keyX)
          Seq(
            spriteSheet.fromSprite(sprite).moveTo(keyX, keyY),
            UIUtils.text(count.toString, countX, keyY + countYOffset)
          )
      }

      keyDisplays.toBatch
    }
  }

  def coins(
      model: GameController,
      spriteSheet: Graphic[?]
  ): Batch[SceneNode] = {
    import data.Sprites
    import game.entity.Coins.*

    val player = model.gameState.playerEntity
    val coinCount = player.coins

    val startX = uiXOffset
    val startY = uiRowY(4) // Row 4 - below keys

    val (countX, countYOffset) = countTextOffset(startX)
    Seq(
      spriteSheet.fromSprite(Sprites.coinSprite).moveTo(startX, startY),
      UIUtils.text(s"$coinCount", countX, startY + countYOffset)
    ).toBatch
  }

  def equipmentPaperdoll(
      model: GameController,
      spriteSheet: Graphic[?]
  ): Batch[SceneNode] = {
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
      Rectangle(
        Point(paperdollX - defaultBorderSize, paperdollY - defaultBorderSize),
        Size(
          paperdollWidth + (defaultBorderSize * 2),
          paperdollHeight + (defaultBorderSize * 2)
        )
      ),
      RGBA.SlateGray.withAlpha(0.3f)
    )

    // Title
    val title =
      UIUtils.text(
        "Equipment",
        paperdollX,
        paperdollY - (defaultBorderSize * 3)
      )

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
        case "Iron Helmet"    => data.Sprites.ironHelmetSprite
        case _                => data.Sprites.defaultItemSprite
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
        case "Iron Sword"  => data.Sprites.ironSwordSprite
        case _             => data.Sprites.defaultItemSprite
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
        case "Plate Armor"     => data.Sprites.plateArmorSprite
        case _                 => data.Sprites.defaultItemSprite
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
        case "Iron Gloves"    => data.Sprites.ironGlovesSprite
        case _                => data.Sprites.defaultItemSprite
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
        case "Iron Boots"    => data.Sprites.ironBootsSprite
        case _               => data.Sprites.defaultItemSprite
      }
      spriteSheet.fromSprite(sprite).moveTo(bootsSlotX, bootsY)
    }.toSeq

    // Equipment stats with better positioning to avoid overlap
    val totalDamageReduction = equipment.getTotalDamageReduction
    val totalDamageBonus = equipment.getTotalDamageBonus
    val statsY = paperdollY + (spriteScale * 5) + (spriteScale / 2)
    val drText =
      UIUtils.text(
        s"DR: $totalDamageReduction",
        paperdollX + defaultBorderSize,
        statsY
      )
    val dmgText = UIUtils.text(
      s"DMG: +$totalDamageBonus",
      paperdollX + defaultBorderSize + (spriteScale * 2),
      statsY
    )

    Seq(
      background,
      title,
      helmetSlot,
      weaponSlot,
      armorSlot,
      glovesSlot,
      bootsSlot,
      drText,
      dmgText
    ).toBatch ++
      helmetItem.toBatch ++ weaponItem.toBatch ++ armorItem.toBatch ++ glovesItem.toBatch ++ bootsItem.toBatch
  }
}
