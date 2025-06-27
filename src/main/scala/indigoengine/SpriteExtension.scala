package indigoengine

import data.Sprites
import game.entity.Drawable.*
import game.entity.Entity
import map.TileType
import ui.UIConfig.*
import indigo.*


object SpriteExtension {
  extension (spriteSheet: Graphic[?]) {
    def fromSprite(sprite: game.Sprite): Graphic[?] = {
      spriteSheet
        .withCrop(
          sprite.x * spriteScale,
          sprite.y * spriteScale,
          spriteScale,
          spriteScale
        )
    }
    
    def moveTo(gamePoint: game.Point): Graphic[?] = {
      spriteSheet.moveTo(
        gamePoint.x * spriteScale,
        gamePoint.y * spriteScale
      )
    }
    
    def fromEntity(entity: Entity): Seq[Graphic[?]] = (for {
      (point, sprite) <- entity.sprites
    } yield {
      spriteSheet
        .fromSprite(sprite)
        .moveTo(point)
    }).toSeq

    def fromTile(point: game.Point, tileType: TileType): Graphic[?] = {
      val sprite = tileType match {
        case TileType.Floor => Sprites.floorSprite
        case TileType.Wall => Sprites.wallSprite
        case TileType.MaybeFloor => Sprites.maybeFloorSprite
        case TileType.Water => Sprites.waterSprite
        case TileType.Bridge => Sprites.bridgeSprite
        case TileType.Rock => Sprites.rockSprite
      }

      spriteSheet
        .fromSprite(sprite)
        .moveTo(point)
    }
  }

}
