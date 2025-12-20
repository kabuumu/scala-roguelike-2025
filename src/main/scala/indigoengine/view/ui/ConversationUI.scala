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

        // Window dimensions - centered on screen
        val windowWidth = spriteScale * 12
        val windowHeight = spriteScale * 8
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

        // Icon on left side (large)
        val iconSize = spriteScale * 3
        val iconX = windowX + defaultBorderSize
        val iconY = windowY + defaultBorderSize

        val icon = entity.get[Portrait] match {
          case Some(portrait) =>
            val portraitSheet =
              Graphic(0, 0, 192, 192, Material.Bitmap(AssetName("portraits")))

            // Image is 192x192, grid is 2x2, so each cell is 96x96
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
              .getOrElse(data.Sprites.playerSprite) // Fallback

            spriteSheet
              .fromSprite(entitySprite)
              .moveTo(iconX, iconY)
              .scaleBy(3.0, 3.0)
        }

        // Name at top (right of icon)
        val messageX = iconX + iconSize + defaultBorderSize
        val messageY = windowY + defaultBorderSize
        val nameText = UIUtils.text(entityName, messageX, messageY)

        // Message Text (below name, wrapped)
        val textY = messageY + spriteScale
        val maxLineChars =
          (windowWidth - iconSize - (defaultBorderSize * 3)) / (spriteScale / 3)
        // Use the message from state
        val wrappedText =
          UIUtils.wrapText(interactionState.message, maxLineChars)
        val textLines = wrappedText.zipWithIndex.map { case (line, idx) =>
          UIUtils.text(line, messageX, textY + (idx * (spriteScale / 2)))
        }

        // Options menu - centered below icon and message area
        // Determine start Y based on icon size to ensure no overlap
        val optionsStartY =
          windowY + iconSize + (defaultBorderSize * 2) + (spriteScale / 2)
        val optionHeight = spriteScale + defaultBorderSize

        val optionElements = interactionState.options.zipWithIndex.flatMap {
          case ((optionText, action), index) =>
            val optionY = optionsStartY + (index * optionHeight)
            val isSelected = index == interactionState.selectedOption

            // Highlight background for selected option
            val highlight = if (isSelected) {
              Some(
                BlockBar.getBlockBar(
                  Rectangle(
                    Point(windowX, optionY - (defaultBorderSize / 2)),
                    Size(windowWidth, optionHeight)
                  ),
                  RGBA.Orange.withAlpha(0.5)
                )
              )
            } else None

            // Option text centered
            val displayText =
              if (isSelected) s"> $optionText <" else s"  $optionText  "
            val textX = windowX + (windowWidth - (displayText.length * 8)) / 2

            highlight.toSeq :+ UIUtils.text(displayText, textX, optionY)
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
