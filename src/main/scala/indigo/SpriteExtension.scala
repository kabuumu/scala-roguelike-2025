package indigo

import data.Sprites
import game.entity.Drawable.*
import game.entity.Entity
import map.TileType
import ui.UIConfig.*

object SpriteExtension {
  extension (spriteSheet: Graphic[?]) {
    def fromEntity(entity: Entity): Seq[Graphic[?]] = (for {
      (game.Point(x, y), game.Sprite(spriteX, spriteY, _)) <- entity.sprites
    } yield {
      spriteSheet
        .withCrop(
          spriteX * spriteScale,
          spriteY * spriteScale,
          spriteScale,
          spriteScale
        ).moveTo(
          x * spriteScale, y * spriteScale
        )
    }).toSeq
    def fromTile(point: game.Point, tileType: TileType): Graphic[?] = {
      val game.Sprite(spriteX, spriteY, _) = tileType match {
        case TileType.Floor => Sprites.floorSprite
        case TileType.Wall => Sprites.wallSprite
      }

      spriteSheet
        .withCrop(
          spriteX * spriteScale,
          spriteY * spriteScale,
          spriteScale,
          spriteScale
        ).moveTo(
          point.x * spriteScale, point.y * spriteScale
        )
    }
  }

}
