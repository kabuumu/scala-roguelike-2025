package indigoengine

import data.Sprites
import game.entity.Drawable.*
import game.entity.Entity
import map.TileType
import ui.UIConfig.*
import indigo.*
import indigo.Batch.toBatch

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

    def fromEntity(entity: Entity): Batch[Graphic[?]] = (for {
      (point, sprite) <- entity.sprites
    } yield {
      spriteSheet
        .fromSprite(sprite)
        .moveTo(point)
    }).toSeq.toBatch

    def fromTile(point: game.Point, tileType: TileType): Graphic[?] = {
      val sprite = tileType match {
        case TileType.Floor      => Sprites.floorSprite
        case TileType.Wall       => Sprites.wallSprite
        case TileType.MaybeFloor => Sprites.maybeFloorSprite
        case TileType.Water      => Sprites.waterSprite
        case TileType.Bridge     => Sprites.bridgeSprite
        case TileType.Rock       => Sprites.rockSprite
        case TileType.Tree       => Sprites.treeSprite
        case TileType.Grass1     => Sprites.grass1Sprite
        case TileType.Grass2     => Sprites.grass2Sprite
        case TileType.Grass3     => Sprites.grass3Sprite
        case TileType.Dirt       => Sprites.dirtSprite
        case TileType.Farmland   => Sprites.farmlandSprite
      }

      spriteSheet
        .fromSprite(sprite)
        .moveTo(point)
    }
  }

}
