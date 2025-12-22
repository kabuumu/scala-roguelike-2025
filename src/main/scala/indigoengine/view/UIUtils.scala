package indigoengine.view

import indigo.*
import generated.PixelFont
import generated.Assets
import generated.PixelFontSmall
import _root_.ui.UIConfig.*

object UIUtils {
  def text(text: String, x: Int, y: Int): Text[?] = Text(
    text,
    x,
    y,
    PixelFont.fontKey,
    Assets.assets.generated.PixelFontMaterial
  )

  def smallText(text: String, x: Int, y: Int): Text[?] = Text(
    text,
    x,
    y,
    PixelFontSmall.fontKey,
    Assets.assets.generated.PixelFontSmallMaterial
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
}
