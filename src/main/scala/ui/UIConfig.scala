package ui

object UIConfig {
  val spriteScale = 16
  val uiScale = 3
  val xTiles = 24
  val yTiles = 16

  val xPixels: Int = xTiles * spriteScale * uiScale
  val yPixels: Int = yTiles * spriteScale * uiScale
  
  val borderSize: Int = 2
}
