package ui

object UIConfig {
  val spriteScale = 16
  val uiScale = 3

  // Viewport dimensions in tiles (at uiScale=1)
  private val baseViewportWidthTiles = 96
  private val baseViewportHeightTiles = 48

  val xTiles = baseViewportWidthTiles / uiScale
  val yTiles = baseViewportHeightTiles / uiScale
  val ignoreLineOfSight = false

  val canvasWidth: Int = xTiles * spriteScale
  val canvasHeight: Int = yTiles * spriteScale

  val xPixels: Int = canvasWidth * uiScale
  val yPixels: Int = canvasHeight * uiScale

  val defaultBorderSize: Int = 2

  val uiXOffset: Int = spriteScale / 2
  val uiYOffset: Int = spriteScale / 2

  val itemBorder: Int = spriteScale / 2
  val uiItemScale: Int = spriteScale + itemBorder
}
