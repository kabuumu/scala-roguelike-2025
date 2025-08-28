package ui

object UIConfig {
  val spriteScale = 16
  val uiScale = 3
  val xTiles = 32  // Increased from 24 to 32 for more room
  val yTiles = 16

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
