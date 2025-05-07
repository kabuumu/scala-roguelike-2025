package scalafx

import scalafx.App.uiScale
import scalafx.scene.image.Image
import scalafx.scene.text.Font

//Store all loaded resources here for easy access
object Resources {
  lazy val spriteSheet: Image = Image("file:src/resources/sprites/sprites.png")
  lazy val pixelFont: Font = Font.loadFont("file:src/resources/fonts/Kenney Pixel.ttf", 11 * uiScale)
}
