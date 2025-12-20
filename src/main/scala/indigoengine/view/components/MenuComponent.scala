package indigoengine.view.components

import indigo.*
import indigo.Batch.toBatch
import indigoengine.view.UIUtils
import indigoengine.view.BlockBar
import ui.UIConfig.*
import generated.Assets
import generated.PixelFont
import indigo.shared.materials.Material

object MenuComponent {

  def renderScrollingMenu(
      title: String,
      options: Seq[String],
      selectedIndex: Int,
      canvasWidth: Int,
      canvasHeight: Int,
      renderDetail: Option[Batch[SceneNode]] = None
  ): Batch[SceneNode] = {
    // 1. Full screen semi-transparent black background
    val background = BlockBar.getBlockBar(
      Rectangle(Point(0, 0), Size(canvasWidth, canvasHeight)),
      RGBA.Black.withAlpha(0.9)
    )

    // 2. Title
    val titleX = (canvasWidth - (title.length * 8)) / 2
    val titleY = spriteScale * 2
    val titleNode = UIUtils.text(title, titleX, titleY)

    // 3. Center Selection Logic
    // We want the selected item to be EXACTLY in the middle of the vertical space (roughly)

    val menuWidth = canvasWidth / 3
    val menuX = spriteScale * 2 // Left aligned list

    val centerY = canvasHeight / 2
    val itemHeight = spriteScale * 2 // Spacing between items

    // We render a fixed number of items around the selected index
    // e.g. 5 above, 5 below
    val range = 6
    val minIndex = math.max(0, selectedIndex - range)
    val maxIndex = math.min(options.length - 1, selectedIndex + range)

    val listNodes = (minIndex to maxIndex).flatMap { index =>
      val option = options(index)
      val isSelected = index == selectedIndex

      // Calculate Y position relative to center
      // If index == selectedIndex, y = centerY
      // If index < selectedIndex, y < centerY
      val diff = index - selectedIndex
      val y = centerY + (diff * itemHeight)

      val prefix = if (isSelected) "> " else "  "
      val suffix = if (isSelected) " <" else "  "

      // Truncate
      val maxChars = 20
      val displayOption =
        if (option.length > maxChars) option.take(maxChars - 3) + "..."
        else option
      val textStr = s"$prefix$displayOption$suffix"

      // Fading logic
      val baseAlpha = if (isSelected) 1.0f else 0.5f
      // Optional: fade out more as it gets further away
      val distanceFade = math.max(0.2f, 1.0f - (math.abs(diff) * 0.15f))
      val finalAlpha = baseAlpha * distanceFade

      val optionText = UIUtils.text(textStr, menuX, y) match {
        case t: Text[?] =>
          t.asInstanceOf[Text[Material.Bitmap]]
            .modifyMaterial(_.toImageEffects.withAlpha(finalAlpha))
        case other => other
      }

      if (isSelected) {
        Seq(
          BlockBar.getBlockBar(
            Rectangle(
              Point(menuX - defaultBorderSize, y - defaultBorderSize),
              Size(menuWidth, spriteScale + defaultBorderSize * 2)
            ),
            RGBA.Orange.withAlpha(0.5)
          ),
          optionText
        )
      } else {
        Seq(optionText)
      }
    }

    // Scroll indicators if there are more items
    val indicatorX = menuX + (menuWidth / 2)
    val upArrow =
      if (minIndex > 0)
        Some(
          UIUtils
            .text("^", indicatorX, centerY - ((range + 1) * itemHeight))
            .asInstanceOf[Text[Material.Bitmap]]
            .modifyMaterial(_.toImageEffects.withAlpha(0.5))
        )
      else None
    val downArrow =
      if (maxIndex < options.length - 1)
        Some(
          UIUtils
            .text("v", indicatorX, centerY + ((range + 1) * itemHeight))
            .asInstanceOf[Text[Material.Bitmap]]
            .modifyMaterial(_.toImageEffects.withAlpha(0.5))
        )
      else None

    // 4. Detail View (Right side)
    val detailNodes = renderDetail.getOrElse(Batch.empty)

    Batch(background, titleNode) ++ listNodes.toBatch ++ Seq(
      upArrow,
      downArrow
    ).flatten.toBatch ++ detailNodes
  }
}
