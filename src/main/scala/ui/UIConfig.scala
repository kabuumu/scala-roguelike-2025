package ui

object UIConfig {
  val spriteScale = 16
  val uiScale = 3
  val xTiles = 96/uiScale  
  val yTiles = 48/uiScale  
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
