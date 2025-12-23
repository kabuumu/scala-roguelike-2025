package indigoengine.view.ui

import indigo.*
import indigo.Batch.toBatch
import indigoengine.view.UIUtils
import indigoengine.view.BlockBar
import ui.UIConfig.*
import ui.GameController
import ui.UIState
import game.entity.Experience
import game.entity.Experience.level
import game.entity.Experience.experience
import game.entity.Experience.nextLevelExperience
import game.entity.Health
import game.entity.Health.currentHealth
import game.entity.Health.maxHealth
import game.status.StatusEffect
import game.status.StatusEffect.statusEffects

object CharacterScreen {

  def render(
      model: GameController,
      spriteSheet: Graphic[?]
  ): Batch[SceneNode] = {
    // Determine if we are in Character state
    val isCharacterScreen = model.uiState match {
      case UIState.Character => true
      case _                 => false
    }

    if (!isCharacterScreen) return Batch.empty

    val player = model.gameState.playerEntity

    // Window Configuration
    // Centered window, slightly smaller than full screen
    val windowWidth = 400
    val windowHeight = 300
    val startX = (canvasWidth - windowWidth) / 2
    val startY = (canvasHeight - windowHeight) / 2

    val background = BlockBar.getBlockBar(
      Rectangle(
        0,
        0,
        canvasWidth,
        canvasHeight
      ),
      RGBA.Black.withAlpha(0.95f)
    )

    val title = UIUtils.text("Character", startX, startY)

    // Player Stats
    val statsY = startY + (spriteScale * 2)
    val leftColX = startX + spriteScale

    val healthText = UIUtils.text(
      s"Health: ${player.currentHealth} / ${player.maxHealth}",
      leftColX,
      statsY
    )

    val levelText = UIUtils.text(
      s"Level: ${player.level}",
      leftColX,
      statsY + spriteScale
    )

    val xpText = UIUtils.text(
      s"XP: ${player.experience} / ${player.nextLevelExperience}",
      leftColX,
      statsY + (spriteScale * 2)
    )

    // Perks List
    val perksTitleY = statsY + (spriteScale * 4)
    val perksTitle = UIUtils.text("Perks:", leftColX, perksTitleY)

    val perksListY = perksTitleY + spriteScale
    val perks = player.statusEffects

    val perkNodes = if (perks.isEmpty) {
      Seq(
        UIUtils
          .text("None", leftColX, perksListY)
          .asInstanceOf[Text[Material.Bitmap]]
          .modifyMaterial(_.toImageEffects.withAlpha(0.5))
      )
    } else {
      perks.zipWithIndex.flatMap { case (perk, index) =>
        val y =
          perksListY + (index * (spriteScale * 2)) // Double spacing for name + desc
        Seq(
          UIUtils.text(s"- ${perk.name}", leftColX, y),
          UIUtils
            .text(perk.description, leftColX + spriteScale, y + spriteScale)
            .asInstanceOf[Text[Material.Bitmap]]
            .modifyMaterial(_.toImageEffects.withAlpha(0.7))
        )
      }
    }

    val hintText = UIUtils.text(
      "C/Esc: Close",
      uiXOffset,
      canvasHeight - spriteScale - 4
    )

    Batch(
      background,
      title,
      healthText,
      levelText,
      xpText,
      perksTitle,
      hintText
    ) ++ perkNodes.toBatch
  }
}
