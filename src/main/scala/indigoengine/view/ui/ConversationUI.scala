package indigoengine.view.ui

import indigo.*
import indigo.Batch.toBatch
import indigoengine.view.UIUtils
import indigoengine.view.BlockBar
import _root_.ui.UIConfig.*
import _root_.ui.GameController
import _root_.ui.UIState
import game.entity.NameComponent
import game.entity.Portrait
import game.entity.Drawable
import indigoengine.SpriteExtension.*
import generated.Assets

object ConversationUI {

  def conversationWindow(
      model: GameController,
      spriteSheet: Graphic[?]
  ): Batch[SceneNode] = {
    model.uiState match {
      case interactionState: UIState.InteractionState =>
        val entity = interactionState.entity
        val entityName =
          entity.get[NameComponent].map(_.name).getOrElse("Speaker")
        val entityDescription = entity
          .get[NameComponent]
          .map(_.description)
          .getOrElse("")

        val iconSize = spriteScale * 3 // 96px
        val padding = defaultBorderSize * 2 // 20px
        val windowWidth = spriteScale * 15 // 480px width for clean text wrapping

        // Calculate text height & wrapping first to determine required window height
        val textWidthAvailable = windowWidth - iconSize - (padding * 2)
        val maxLineChars = math.max(20, textWidthAvailable / 9) // ~9px font width

        val wrappedText = UIUtils.wrapText(interactionState.message, maxLineChars)
        val textLineHeight = 18
        val textTotalHeight = wrappedText.length * textLineHeight

        // Vertical spacing calculations relative to windowY offset
        val nameToTextOffset = 24
        val iconHeight = iconSize
        val headerAreaHeight = math.max(iconHeight, nameToTextOffset + textTotalHeight)

        val optionsGap = 16
        val optionHeight = 26
        val totalOptionsHeight = interactionState.options.length * optionHeight

        val contentHeight = padding + headerAreaHeight + optionsGap + totalOptionsHeight + padding
        val windowHeight = math.max(spriteScale * 8, contentHeight)

        // Center window on canvas
        val windowX = (canvasWidth - windowWidth) / 2
        val windowY = (canvasHeight - windowHeight) / 2

        // Background panel
        val background = BlockBar.getBlockBar(
          Rectangle(
            Point(windowX - defaultBorderSize, windowY - defaultBorderSize),
            Size(
              windowWidth + (defaultBorderSize * 2),
              windowHeight + (defaultBorderSize * 2)
            )
          ),
          RGBA.Black.withAlpha(0.9)
        )

        // Portrait Icon on left side
        val iconX = windowX + padding
        val iconY = windowY + padding

        val icon = entity.get[Portrait] match {
          case Some(portrait) =>
            val portraitSheet =
              Graphic(0, 0, 192, 192, Material.Bitmap(AssetName("portraits")))

            val cellSize = 96
            val pX = portrait.sprite.x * cellSize
            val pY = portrait.sprite.y * cellSize

            portraitSheet
              .withCrop(pX, pY, cellSize, cellSize)
              .moveTo(iconX, iconY)
              .scaleBy(
                iconSize.toDouble / cellSize.toDouble,
                iconSize.toDouble / cellSize.toDouble
              )

          case None =>
            val entitySprite = entity
              .get[Drawable]
              .flatMap(_.sprites.headOption.map(_._2))
              .getOrElse(data.Sprites.playerSprite)

            spriteSheet
              .fromSprite(entitySprite)
              .moveTo(iconX, iconY)
              .scaleBy(3.0, 3.0)
        }

        // Speaker Name & Wrapped NPC Text (right of icon)
        val messageX = iconX + iconSize + defaultBorderSize
        val nameY = windowY + padding
        val nameText = UIUtils.text(entityName, messageX, nameY)

        val textY = nameY + nameToTextOffset
        val textLines = wrappedText.zipWithIndex.map { case (line, idx) =>
          UIUtils.text(line, messageX, textY + (idx * textLineHeight))
        }

        // Options Menu - starts strictly below both text and icon
        val textBottomY = textY + textTotalHeight
        val iconBottomY = iconY + iconSize
        val optionsStartY = math.max(textBottomY, iconBottomY) + optionsGap

        val optionElements = interactionState.options.zipWithIndex.flatMap {
          case ((optionText, action), index) =>
            val optionY = optionsStartY + (index * optionHeight)
            val isSelected = index == interactionState.selectedOption

            val highlight = if (isSelected) {
              Some(
                BlockBar.getBlockBar(
                  Rectangle(
                    Point(windowX + defaultBorderSize, optionY - 2),
                    Size(windowWidth - (defaultBorderSize * 2), optionHeight - 2)
                  ),
                  RGBA.Orange.withAlpha(0.5)
                )
              )
            } else None

            val displayText =
              if (isSelected) s"> $optionText <" else s"  $optionText  "
            val textX = windowX + (windowWidth - (displayText.length * 8)) / 2

            highlight.toSeq :+ UIUtils.text(displayText, textX, optionY + 2)
        }

        Batch(
          background,
          icon,
          nameText
        ) ++ textLines.toBatch ++ optionElements.toBatch

      case _ => Batch.empty
    }
  }
}
