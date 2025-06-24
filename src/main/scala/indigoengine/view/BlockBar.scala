package indigoengine.view

import indigo.*
import indigoengine.shaders.{CustomShader, RGBAData}
import ui.UIConfig
import ui.UIConfig.borderSize
import ultraviolet.syntax.*

object BlockBar {
  def getBlockBar(bounds: Rectangle, colour: RGBA): BlankEntity =
    BlankEntity(
      bounds = bounds,
      shaderData = ShaderData(
        CustomShader.customShaderId, RGBAData(
          vec4((colour.r * colour.a).toFloat, (colour.g * colour.a).toFloat, (colour.b * colour.a).toFloat, colour.a.toFloat)
        )
      )
    )

  def attributeBar(bounds: Rectangle, filledWidth: Int, fullColour: RGBA, emptyColour: RGBA): Batch[BlankEntity] = {
    Batch(
      getBlockBar(
        bounds + Rectangle(
          Point(-borderSize, -borderSize),
          Size(borderSize * 2, borderSize * 2)
        ),
        RGBA.Black
      ),
      getBlockBar(
        bounds,
        emptyColour
      ),
      getBlockBar(
        bounds.withSize(
          if(filledWidth == 0) 0 else filledWidth + borderSize,
          bounds.height
        ),
        RGBA.Black
      ),
      getBlockBar(
        bounds.withSize(
          filledWidth,
          bounds.height
        ),
        fullColour
      ),
    )
  }
}
