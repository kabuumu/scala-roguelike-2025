package indigoengine.view.ui

import indigo.*
import indigo.Batch.toBatch
import indigoengine.view.UIUtils
import indigoengine.view.BlockBar
import _root_.ui.UIConfig.*
import _root_.ui.GameController
import _root_.ui.UIState
import game.entity.Entity
import game.entity.NameComponent
import game.entity.Drawable
import indigoengine.SpriteExtension.*
import game.entity.ChargeType.SingleUse
import indigoengine.view.components.MenuComponent

object TradeUI {

  def tradeItemDisplay(
      model: GameController,
      spriteSheet: Graphic[?]
  ): Batch[SceneNode] = {
    import game.entity.EntityType.entityType

    model.uiState match {
      case buyItemSelect: UIState.BuyItemSelect
          if buyItemSelect.list.nonEmpty =>
        val itemRef = buyItemSelect.currentItem
        renderBuyItemDisplay(model, spriteSheet, itemRef)

      case sellItemSelect: UIState.SellItemSelect
          if sellItemSelect.list.nonEmpty =>
        val itemEntity = sellItemSelect.currentItem
        renderSellItemDisplay(model, spriteSheet, itemEntity)

      case uiState: UIState.DebugGiveItemSelect =>
        val itemRef = uiState.currentItem
        val detailBatch = renderBuyItemDisplay(model, spriteSheet, itemRef)

        MenuComponent.renderScrollingMenu(
          "GIVE ITEM",
          uiState.list.map(_.toString),
          uiState.index,
          canvasWidth,
          canvasHeight,
          Some(detailBatch)
        )

      case _ => Batch.empty
    }
  }

  def debugItemSelection(
      model: GameController,
      spriteSheet: Graphic[?]
  ): Batch[SceneNode] = tradeItemDisplay(model, spriteSheet)

  private def renderBuyItemDisplay(
      model: GameController,
      spriteSheet: Graphic[?],
      itemRef: data.Items.ItemReference
  ): Batch[SceneNode] = {
    import game.entity.EntityType.entityType

    // Create the item entity to get its information
    val tempEntity = itemRef.createEntity("temp")
    val itemName = tempEntity
      .get[game.entity.NameComponent]
      .map(_.name)
      .getOrElse(itemRef.toString)
    val description =
      tempEntity.get[game.entity.NameComponent].map(_.description).getOrElse("")

    // Get the sprite for the item
    val sprite = tempEntity
      .get[Drawable]
      .flatMap(_.sprites.headOption.map(_._2))
      .getOrElse(data.Sprites.defaultItemSprite)

    // Get price information
    val priceOpt = model.gameState.entities.collectFirst {
      case e if e.entityType == game.entity.EntityType.Trader =>
        e.get[game.entity.Trader].flatMap(_.buyPrice(itemRef))
    }.flatten

    renderItemDisplayCommon(
      model,
      spriteSheet,
      itemName,
      description,
      sprite,
      priceOpt.map(p => s"Price: $p coins"),
      Left(tempEntity)
    )
  }

  private def renderSellItemDisplay(
      model: GameController,
      spriteSheet: Graphic[?],
      itemEntity: Entity
  ): Batch[SceneNode] = {
    import game.entity.EntityType.entityType

    // Display entity items (for selling) with same rich UI
    val itemName = itemEntity
      .get[game.entity.NameComponent]
      .map(_.name)
      .getOrElse("Unknown Item")
    val description =
      itemEntity.get[game.entity.NameComponent].map(_.description).getOrElse("")

    // Get the sprite for the item
    val sprite = itemEntity
      .get[Drawable]
      .flatMap(_.sprites.headOption.map(_._2))
      .getOrElse(data.Sprites.defaultItemSprite)

    // Get sell price information by finding matching ItemReference
    val priceOpt = {
      val itemRefOpt =
        itemEntity.get[game.entity.NameComponent].flatMap { nameComp =>
          data.Items.ItemReference.values.find { ref =>
            val refEntity = ref.createEntity("temp")
            refEntity.get[game.entity.NameComponent].map(_.name) == Some(
              nameComp.name
            )
          }
        }

      itemRefOpt.flatMap { itemRef =>
        model.gameState.entities.collectFirst {
          case e if e.entityType == game.entity.EntityType.Trader =>
            e.get[game.entity.Trader].flatMap(_.sellPrice(itemRef))
        }.flatten
      }
    }

    renderItemDisplayCommon(
      model,
      spriteSheet,
      itemName,
      description,
      sprite,
      priceOpt.map(p => s"Sell for: $p coins"),
      Right(itemEntity)
    )
  }

  private def renderItemDisplayCommon(
      model: GameController,
      spriteSheet: Graphic[?],
      itemName: String,
      description: String,
      sprite: game.Sprite,
      priceTextStr: Option[String],
      entityOrRef: Either[
        Entity,
        Entity
      ] // Left for temp entity, Right for actual entity
  ): Batch[SceneNode] = {
    // Position for the item display (center-left of screen)
    val displayX = canvasWidth / 4
    val displayY = canvasHeight / 3
    val displayWidth = canvasWidth / 2
    val displayHeight = spriteScale * 6

    // Background
    val background = BlockBar.getBlockBar(
      Rectangle(
        Point(displayX - defaultBorderSize, displayY - defaultBorderSize),
        Size(
          displayWidth + (defaultBorderSize * 2),
          displayHeight + (defaultBorderSize * 2)
        )
      ),
      RGBA.Black.withAlpha(0.9)
    )

    // Item sprite (large display)
    val spriteSize = spriteScale * 2
    val spriteX = displayX + defaultBorderSize
    val spriteY = displayY + defaultBorderSize
    val itemSprite = spriteSheet
      .fromSprite(sprite)
      .moveTo(spriteX, spriteY)
      .scaleBy(2.0, 2.0) // Make it larger

    // Item name
    val nameY = spriteY
    val nameX = spriteX + spriteSize + defaultBorderSize
    val nameText = UIUtils.text(itemName, nameX, nameY)

    // Price
    val priceY = nameY + spriteScale
    val priceText = priceTextStr.map { str =>
      UIUtils.text(str, nameX, priceY)
    }.toSeq

    // Description (wrapped)
    val descY = priceY + spriteScale
    val maxLineChars =
      (displayWidth - spriteSize - (defaultBorderSize * 3)) / (spriteScale / 3)
    val wrappedDesc = UIUtils.wrapText(description, maxLineChars)
    val descriptionLines = wrappedDesc.zipWithIndex.map { case (line, idx) =>
      UIUtils.text(line, nameX, descY + (idx * (spriteScale / 2)))
    }

    // Get UsableItem or Equippable info for effects
    val effectsY =
      descY + (wrappedDesc.length * (spriteScale / 2)) + spriteScale / 2

    val entityToInspect = entityOrRef.merge

    val effectsText = entityToInspect
      .get[game.entity.UsableItem]
      .map { usableItem =>
        val targetType = usableItem.targeting match {
          case game.entity.Targeting.Self              => "Self-targeted"
          case game.entity.Targeting.EnemyActor(range) =>
            s"Enemy (range: $range)"
          case game.entity.Targeting.TileInRange(range) =>
            s"Area (range: $range)"
        }
        val consumeText =
          if (usableItem.chargeType == SingleUse) "Single use" else "Reusable"
        Seq(
          UIUtils.text(s"Type: $targetType", nameX, effectsY),
          UIUtils.text(consumeText, nameX, effectsY + spriteScale / 2)
        )
      }
      .orElse {
        // Show equipment stats
        entityToInspect.get[game.entity.Equippable].map { equippable =>
          val slotText = s"Slot: ${equippable.slot}"
          val statsText = if (equippable.damageReduction > 0) {
            s"Defense: +${equippable.damageReduction}"
          } else if (equippable.damageBonus > 0) {
            s"Damage: +${equippable.damageBonus}"
          } else {
            "No special stats"
          }
          Seq(
            UIUtils.text(slotText, nameX, effectsY),
            UIUtils.text(statsText, nameX, effectsY + spriteScale / 2)
          )
        }
      }
      .getOrElse(Seq.empty)

    Batch(
      background,
      itemSprite,
      nameText
    ) ++ priceText.toBatch ++ descriptionLines.toBatch ++ effectsText.toBatch
  }
}
