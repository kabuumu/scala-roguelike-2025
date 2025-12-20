package indigoengine.view.ui

import indigo.*
import indigo.Batch.toBatch
import indigoengine.view.UIUtils
import indigoengine.view.BlockBar
import indigoengine.view.components.MenuComponent
import _root_.ui.UIConfig.*
import _root_.ui.GameController
import _root_.ui.UIState
import generated.Assets
import generated.PixelFont
import generated.PixelFontSmall

object PerkUI {

  def perkSelection(model: GameController): Batch[SceneNode] = {
    model.uiState match {
      case uiState: UIState.StatusEffectSelect =>
        val perkCardWidth = spriteScale * 4 // Width of the perk card
        val perkCardHeight = spriteScale * 6 // Height of the perk card

        // Get the possible perks for the player
        val perks = uiState.list

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
              Rectangle(
                Point(itemX.toInt, itemY.toInt),
                Size(perkCardWidth, perkCardHeight)
              ),
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
              // Wrap the description text if full words are longer than 14 characters on a line
              UIUtils.wrapText(perk.description, 13).mkString("\n"),
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

  def debugPerkSelection(model: GameController): Batch[SceneNode] = {
    model.uiState match {
      case uiState: UIState.DebugGivePerkSelect =>
        val perk = uiState.currentItem

        // Custom detail view for perks
        val detailX = canvasWidth / 2
        val detailY = canvasHeight / 3
        val detailWidth = canvasWidth / 2 - spriteScale * 2

        val detailNodes = Seq(
          UIUtils.text(perk.name, detailX, detailY),
          UIUtils.text(
            "- " + perk.description,
            detailX,
            detailY + spriteScale * 2
          ),
          UIUtils.text(
            s"Index: ${uiState.index + 1}/${uiState.listLength}",
            detailX,
            detailY + spriteScale * 4
          )
        ).toBatch

        MenuComponent.renderScrollingMenu(
          "GIVE PERK",
          uiState.list.map(_.name),
          uiState.index,
          canvasWidth,
          canvasHeight,
          Some(detailNodes)
        )

      case _ => Batch.empty
    }
  }
}
