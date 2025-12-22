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
import generated.Assets
import indigoengine.SpriteExtension.*
import game.entity.Inventory.inventoryItems
import game.entity.Entity
import game.entity.UsableItem
import game.entity.Equippable

object InventoryMenu {

  def render(
      model: GameController,
      spriteSheet: Graphic[?]
  ): Batch[SceneNode] = {
    // Determine if we are in Inventory state
    val (inventoryState, actionState) = model.uiState match {
      case inv: UIState.Inventory            => (Some(inv), None)
      case act: UIState.InventoryActionState =>
        // Assuming ActionState holds reference to the underlying grid state?
        // Actually InputHandler transitions FROM Inventory TO ActionState.
        // ActionState doesn't preserve the grid cursor implicitly?
        // We might need to persist the cursor in ActionState or just default it?
        // InputHandler:
        // case Input.Confirm => UIState.InventoryActionState(selectedItem, options)
        // It loses the cursor position!
        // We should probably modify InventoryActionState to keep the cursor or previous state,
        // OR just not render the grid cursor in action mode (but we want to see the item highlighted).
        // For now, let's assume we just render the item details for the selected item.
        (None, Some(act))
      case _ => (None, None)
    }

    if (inventoryState.isEmpty && actionState.isEmpty) return Batch.empty

    import data.Sprites

    val player = model.gameState.playerEntity
    val inventoryItems = player.inventoryItems(model.gameState)
    val equippedItemIds = player
      .get[game.entity.Equipment]
      .map(_.getAllEquipped.map(_.id).toSet)
      .getOrElse(Set.empty)

    // Filter unique items for the grid
    val uniqueItems = inventoryItems.distinctBy { item =>
      item.get[game.entity.NameComponent].map(_.name).getOrElse(item.id)
    }

    // Grid Configuration
    // Grid Configuration
    // Grid Configuration
    val columns = 5
    // 16px sprite + 12px padding (6px each side) = 28px
    val slotSize = spriteScale + 12
    val slotSpacing = 4

    val rows = (uniqueItems.size / columns) + 1
    val gridWidth =
      (columns * (slotSize + slotSpacing)) + (defaultBorderSize * 2)
    val gridHeight = (math.max(
      5,
      rows
    ) * (slotSize + slotSpacing)) + (defaultBorderSize * 2) // Min 5 rows

    val gridStartX = (canvasWidth - gridWidth) / 2
    val gridStartY = ((canvasHeight - gridHeight) / 2) - spriteScale

    val background = BlockBar.getBlockBar(
      Rectangle(
        0,
        0,
        canvasWidth,
        canvasHeight
      ),
      RGBA.Black.withAlpha(0.95f)
    )

    val title = UIUtils.text("Inventory", gridStartX, gridStartY - spriteScale)

    // Render Items
    val itemNodes = uniqueItems.zipWithIndex.flatMap { case (item, index) =>
      val col = index % columns
      val row = index / columns
      val x = gridStartX + (col * (slotSize + slotSpacing))
      val y = gridStartY + (row * (slotSize + slotSpacing))

      val sprite = item
        .get[Drawable]
        .flatMap(_.sprites.headOption.map(_._2))
        .getOrElse(Sprites.defaultItemSprite)

      val isEquipped = equippedItemIds.contains(item.id)
      val itemName = item.get[NameComponent].map(_.name).getOrElse("")
      val isStackEquipped = equippedItemIds.exists { id =>
        player
          .inventoryItems(model.gameState)
          .find(_.id == id)
          .exists(_.get[NameComponent].exists(_.name == itemName))
      }

      val count =
        inventoryItems.count(_.get[NameComponent] == item.get[NameComponent])

      val itemGraphic = spriteSheet
        .fromSprite(sprite)
        .moveTo(x + 6, y + 6) // Center in 28px slot (6px padding)

      val countText = if (count > 1) {
        // Small text in bottom right corner of the slot padding
        Some(UIUtils.smallText(s"$count", x + slotSize - 8, y + slotSize - 8))
      } else None

      val equippedIndicator = if (isStackEquipped) {
        // Small text in top left corner of the slot padding
        Some(UIUtils.smallText("E", x + 2, y + 2))
      } else None

      Seq(Some(itemGraphic), countText, equippedIndicator).flatten
    }

    // Helper to get selected item for details
    val selectedItem = if (inventoryState.isDefined) {
      val cursor = inventoryState.get.gridCursor
      val index = cursor.y * columns + cursor.x
      if (index >= 0 && index < uniqueItems.length) Some(uniqueItems(index))
      else None
    } else {
      actionState.map(_.selectedItem)
    }

    // Render Cursor
    val cursorNode = inventoryState.map { state =>
      val col = state.gridCursor.x
      val row = state.gridCursor.y
      val x = gridStartX + (col * (slotSize + slotSpacing))
      val y = gridStartY + (row * (slotSize + slotSpacing))

      BlockBar.getBlockBar(
        Rectangle(x, y, slotSize, slotSize),
        RGBA.White.withAlpha(0.3f)
      )
    }

    val cursorBatch = cursorNode match {
      case Some(node) => Batch(node)
      case None       => Batch.empty
    }

    // Render Details Panel
    val detailsNodes = selectedItem
      .map { item =>
        val detailsY = gridStartY + gridHeight + (spriteScale / 2)
        val name = item.get[NameComponent].map(_.name).getOrElse("Unknown Item")
        val desc = item.get[NameComponent].map(_.description).getOrElse("")

        Seq(
          UIUtils.text(name, gridStartX, detailsY),
          UIUtils
            .text(desc, gridStartX, detailsY + spriteScale)
        )
      }
      .getOrElse(Nil)

    // Render Action Menu (overlay)
    val actionMenuNodes = actionState
      .map { state =>
        val menuX = gridStartX + (spriteScale * 2)
        val menuY = gridStartY + (spriteScale * 2)
        val menuWidth = spriteScale * 6
        val menuHeight = (state.options.length * spriteScale) + (spriteScale)

        val bg = BlockBar.getBlockBar(
          Rectangle(menuX, menuY, menuWidth, menuHeight),
          RGBA.Blue.withAlpha(0.9f)
        )

        val options = state.options.zipWithIndex.map { case (opt, idx) =>
          val isSelected = idx == state.selectedOption
          val prefix = if (isSelected) "> " else "  "
          UIUtils.text(prefix + opt, menuX + 8, menuY + 8 + (idx * spriteScale))
        }

        Seq(bg) ++ options
      }
      .getOrElse(Nil)

    val hintText = UIUtils.text(
      "WASD/Arrows: Navigate | Space/Enter: Select | I/Esc: Close",
      uiXOffset,
      canvasHeight - spriteScale - 4
    )

    Batch(
      background,
      title,
      hintText
    ) ++ itemNodes.toBatch ++ cursorBatch ++ detailsNodes.toBatch ++ actionMenuNodes.toBatch
  }
}
