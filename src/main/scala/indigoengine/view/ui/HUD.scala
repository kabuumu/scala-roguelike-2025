package indigoengine.view.ui

import indigo.*
import indigo.Batch.toBatch
import indigoengine.view.UIUtils
import indigoengine.view.BlockBar
import _root_.ui.UIConfig.* // explicit root to avoid ambiguity with indigoengine.view.ui
import _root_.ui.GameController
import _root_.ui.UIState
import game.entity.Entity
import generated.Assets
import generated.PixelFont
import generated.PixelFontSmall

object HUD {

  // UI Row system - each row takes up consistent vertical space
  private val uiRowHeight = spriteScale + (defaultBorderSize * 2)
  private def uiRowY(rowIndex: Int): Int = uiYOffset + (rowIndex * uiRowHeight)

  def versionInfo(model: GameController): Batch[SceneNode] = {
    import generated.Version

    // Position version info above the message window
    val versionText = s"v${Version.gitCommit}"
    val xOffset = uiXOffset
    val yOffset =
      canvasHeight - (spriteScale * 3) - defaultBorderSize // Above message window

    Batch(
      UIUtils.text(versionText, xOffset, yOffset)
    )
  }

  def messageWindow(model: GameController): Batch[SceneNode] = {
    import game.entity.{
      UsableItem,
      Equippable,
      EntityType,
      Health,
      NameComponent
    }
    import game.entity.EntityType.*
    import game.entity.Health.*
    import game.entity.Equippable.*
    import game.entity.UsableItem.*
    import game.entity.NameComponent.*
    import game.Direction
    import game.entity.Movement.*
    import game.entity.ChargeType
    import game.entity.ChargeType.SingleUse

    val messageContent = model.uiState match {
      case UIState.Move =>
        // Check for nearby action targets
        val actionTargets =
          _root_.ui.GameTargeting.nearbyActionTargets(model.gameState)

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

          s"$moveKeys"
        }

      // Handle specific list select types

      case buyItemSelect: UIState.BuyItemSelect =>
        if (buyItemSelect.list.nonEmpty) {
          val itemRef = buyItemSelect.currentItem
          val tempEntity = itemRef.createEntity("temp")
          val itemName = tempEntity
            .get[game.entity.NameComponent]
            .map(_.name)
            .getOrElse(itemRef.toString)
          val description = tempEntity
            .get[game.entity.NameComponent]
            .map(_.description)
            .getOrElse("")
          val descriptionText =
            if (description.nonEmpty) s" - $description" else ""

          // Get price from trader
          val priceText = model.gameState.entities
            .collectFirst {
              case e if e.entityType == game.entity.EntityType.Trader =>
                e.get[game.entity.Trader]
                  .flatMap(_.buyPrice(itemRef))
                  .map(price => s" (${price} coins)")
            }
            .flatten
            .getOrElse("")

          s"$itemName$descriptionText$priceText. Press Space/E/Enter to buy."
        } else {
          "No items available."
        }

      case sellItemSelect: UIState.SellItemSelect =>
        if (sellItemSelect.list.nonEmpty) {
          val entity = sellItemSelect.currentItem
          val itemName =
            entity.get[game.entity.NameComponent].map(_.name).getOrElse("Item")
          val description = entity
            .get[game.entity.NameComponent]
            .map(_.description)
            .getOrElse("")
          val descriptionText =
            if (description.nonEmpty) s" - $description" else ""

          // Try to get sell price from trader
          val priceText = model.gameState.entities
            .collectFirst {
              case e if e.entityType == game.entity.EntityType.Trader =>
                e.get[game.entity.Trader].flatMap { traderComp =>
                  entity.get[game.entity.NameComponent].flatMap { nameComp =>
                    data.Items.ItemReference.values
                      .find { ref =>
                        val refEntity = ref.createEntity("temp")
                        refEntity
                          .get[game.entity.NameComponent]
                          .map(_.name) == Some(nameComp.name)
                      }
                      .flatMap(traderComp.sellPrice)
                      .map(price => s" (${price} coins)")
                  }
                }
            }
            .flatten
            .getOrElse("")

          s"$itemName$descriptionText$priceText. Press Space/E/Enter to sell."
        } else {
          "No items available."
        }

      case statusEffectSelect: UIState.StatusEffectSelect =>
        if (statusEffectSelect.list.nonEmpty) {
          val statusEffect = statusEffectSelect.currentItem
          s"${statusEffect.name}: ${statusEffect.description}. Press Space/E/Enter to select."
        } else {
          "No items available."
        }

      case actionTargetSelect: UIState.ActionTargetSelect =>
        if (actionTargetSelect.list.nonEmpty) {
          val target = actionTargetSelect.currentItem
          s"${target.description}. Press Space/E/Enter to select, Escape to cancel."
        } else {
          "No actions available."
        }

      case enemyTargetSelect: UIState.EnemyTargetSelect =>
        if (enemyTargetSelect.list.nonEmpty) {
          val enemy = enemyTargetSelect.currentItem
          val healthText = if (enemy.has[game.entity.Health]) {
            s" (${enemy.currentHealth}/${enemy.maxHealth} HP)"
          } else ""
          s"Target: ${enemy.name.getOrElse("Enemy")}$healthText. Press Space/E/Enter to confirm."
        } else {
          "No targets available."
        }

      case scrollSelect: UIState.ScrollSelect =>
        val x = scrollSelect.cursor.x
        val y = scrollSelect.cursor.y
        s"Target: [$x,$y]. Press Enter to confirm action at this location."
      case interactionState: UIState.InteractionState =>
        val (optionText, _) = interactionState.getSelectedOption
        s"Interaction - Selected: $optionText. Use arrows to navigate, Space/E/Enter to select."
      case _: UIState.MainMenu =>
        "" // No message window content for main menu
      case _: UIState.GameOver =>
        "" // Game over screen handles its own messaging
      case _: UIState.DebugMenu =>
        "Use Arrows to navigate, Enter/Space/E to select, ` (backtick) to close."
      case _: UIState.DebugGiveItemSelect =>
        "Select an item to give to the player."
      case _: UIState.DebugGivePerkSelect =>
        "Select a perk to give to the player."
      case UIState.WorldMap =>
        "" // World map handles its own messaging
      case _: UIState.Inventory =>
        "Inventory Mode. WASD/Arrows to Navigate, E/Space/Enter to Select, I/Esc to Close."
      case _: UIState.InventoryActionState =>
        "Item Actions. WASD/Arrows to Navigate, E/Space/Enter to Select, Esc to Cancel."
      case UIState.Character =>
        "" // Character screen handles its own hints
    }

    // Position message window at the very bottom of the visible canvas area
    val messageWindowHeight = spriteScale * 2
    val messageY =
      canvasHeight - messageWindowHeight // Position at bottom of game canvas, visible
    val messageX = uiXOffset
    val messageWidth = canvasWidth - (uiXOffset * 2)

    // Create background
    val background = BlockBar.getBlockBar(
      Rectangle(
        Point(messageX - defaultBorderSize, messageY - defaultBorderSize),
        Size(
          messageWidth + (defaultBorderSize * 2),
          messageWindowHeight + (defaultBorderSize * 2)
        )
      ),
      RGBA.Black.withAlpha(0.8)
    )

    // Wrap and display text
    val wrappedLines =
      UIUtils.wrapText(
        messageContent,
        messageWidth
      ) // Approximate character width
    val textElements = wrappedLines.zipWithIndex.map { case (line, index) =>
      UIUtils.text(line, messageX, messageY + (index * (spriteScale / 2)))
    }

    Batch(background) ++ textElements.toBatch
  }
}
