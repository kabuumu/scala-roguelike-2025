package map

import indigo.*
import game.Point
import data.Sprites
import map.TileType
import ui.UIConfig

object AutoTiler {

  val TileSize = 16
  // Tileset is assumed to be a single row of 16x16 sprites:
  // 0: Empty/Dirt
  // 1: Outer Corner
  // 2: Side
  // 3: Diagonal
  // 4: Inner Corner
  // 5: Solid Grass
  val AutoTileBaseX = 0
  val AutoTileBaseY = 0

  /** Generates a Graphic for the dual-grid tile at vertex (x, y).
    */
  def getTerrainSprite(
      world: WorldMap,
      x: Int,
      y: Int,
      spriteSheet: Graphic[?]
  ): Option[Graphic[?]] = {
    // Helper to get type.
    def isGrass(pt: Point): Boolean = {
      world.getTile(pt) match {
        case Some(TileType.Grass1) | Some(TileType.Grass2) |
            Some(TileType.Grass3) =>
          true
        case _ => false
      }
    }

    val isTLGrass = isGrass(Point(x - 1, y - 1))
    val isTRGrass = isGrass(Point(x, y - 1))
    val isBLGrass = isGrass(Point(x - 1, y))
    val isBRGrass = isGrass(Point(x, y))

    var grassCount = 0
    if (isTLGrass) grassCount += 1
    if (isTRGrass) grassCount += 1
    if (isBLGrass) grassCount += 1
    if (isBRGrass) grassCount += 1

    var spriteIndex = 0
    var rotation = Radians.zero

    if (grassCount == 0) {
      return None
    } else if (grassCount == 1) {
      spriteIndex = 1
      if (isTLGrass) rotation = Radians.fromDegrees(180)
      else if (isTRGrass) rotation = Radians.fromDegrees(-90)
      else if (isBLGrass) rotation = Radians.fromDegrees(90)
      else if (isBRGrass) rotation = Radians.fromDegrees(0)
    } else if (grassCount == 2) {
      if (isTLGrass && isTRGrass) {
        spriteIndex = 2
        rotation = Radians.fromDegrees(-90)
      } else if (isTLGrass && isBLGrass) {
        spriteIndex = 2
        rotation = Radians.fromDegrees(180)
      } else if (isTRGrass && isBRGrass) {
        spriteIndex = 2
        rotation = Radians.fromDegrees(0)
      } else if (isBLGrass && isBRGrass) {
        spriteIndex = 2
        rotation = Radians.fromDegrees(90)
      } else if (isTLGrass && isBRGrass) {
        spriteIndex = 3
        rotation = Radians.fromDegrees(90)
      } else if (isTRGrass && isBLGrass) {
        spriteIndex = 3
        rotation = Radians.fromDegrees(0)
      }
    } else if (grassCount == 3) {
      spriteIndex = 4
      if (!isTLGrass) rotation = Radians.fromDegrees(0)
      else if (!isTRGrass) rotation = Radians.fromDegrees(90)
      else if (!isBLGrass) rotation = Radians.fromDegrees(-90)
      else if (!isBRGrass) rotation = Radians.fromDegrees(180)
    } else if (grassCount == 4) {
      spriteIndex = 5
      rotation = Radians.zero
    }

    if (spriteIndex == 0) return None

    Some(
      Graphic(0, 0, TileSize, TileSize, Material.Bitmap(AssetName("tileset")))
        .withCrop(
          (AutoTileBaseX + spriteIndex) * TileSize,
          AutoTileBaseY * TileSize,
          TileSize,
          TileSize
        )
        .withRef(TileSize / 2, TileSize / 2) // Rotate around center
        .withRotation(rotation)
    )
  }

  def getDirtSprite(): Graphic[?] = {
    // Return the Dirt tile (Index 0)
    // No rotation needed, standard top-left anchor is fine for standard grid rendering.
    Graphic(0, 0, TileSize, TileSize, Material.Bitmap(AssetName("tileset")))
      .withCrop(
        AutoTileBaseX * TileSize,
        AutoTileBaseY * TileSize,
        TileSize,
        TileSize
      )
  }
}
