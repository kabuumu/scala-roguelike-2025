package indigoengine

import data.Sprites
import game.entity.Movement.position
import game.entity.{Entity, SightMemory}
import game.StartingState
import generated.{PixelFont, PixelFontSmall}
import indigo.*
import indigo.Batch.toBatch
import indigoengine.SpriteExtension.*
import indigoengine.shaders.CustomShader
import indigoengine.view.Elements.*
import ui.UIConfig.*
import ui.{GameController, UIConfig, UIState}
import util.LineOfSight
import indigoengine.view.ui.InventoryMenu
import indigoengine.view.ui.CharacterScreen
import map.TileType

import scala.scalajs.js.annotation.JSExportTopLevel

@JSExportTopLevel("IndigoGame")
object Game extends IndigoSandbox[Unit, GameController] {

  override def config: GameConfig = GameConfig.default
    .withViewport(xPixels, yPixels)
    .withMagnification(uiScale)
    .withFrameRateLimit(30)
    .noResize

  override def assets: Set[AssetType] = Set(
    AssetType.Image(
      AssetName("sprites"),
      AssetPath("assets/sprites/sprites.png")
    ),
    AssetType.Image(
      AssetName("portraits"),
      AssetPath("assets/sprites/portraits.png")
    ),
    AssetType.Image(
      AssetName("tileset"),
      AssetPath("assets/sprites/tileset.png")
    ),
    AssetType.Image(
      AssetName("tree"),
      AssetPath("assets/sprites/tree.png")
    ),
    AssetType.Image(
      AssetName("tree2"),
      AssetPath("assets/sprites/tree2.png")
    ),
    AssetType.Image(
      AssetName("tree3"),
      AssetPath("assets/sprites/tree3.png")
    ),
    AssetType.Image(
      AssetName("tree4"),
      AssetPath("assets/sprites/tree4.png")
    )
  ) ++ generated.Assets.assets.generated.assetSet

  override def fonts: Set[FontInfo] = Set(
    PixelFont.fontInfo,
    PixelFontSmall.fontInfo
  )

  override def animations: Set[Animation] = Set.empty

  override def shaders: Set[ShaderProgram] = Set(
    CustomShader.shader
  )

  override def setup(
      assetCollection: AssetCollection,
      dice: Dice
  ): Outcome[Startup[Unit]] = Outcome(
    Startup.Success(())
  )

  override def initialModel(startupData: Unit): Outcome[GameController] = {
    // Create a minimal dummy game state for the main menu (it won't be used until New Game is selected)
    val dummyDungeon = map.Dungeon(
      roomGrid = Set(game.Point(0, 0)),
      roomConnections = Set.empty,
      blockedRooms = Set.empty,
      startPoint = game.Point(0, 0),
      endpoint = None,
      items = Set.empty,
      testMode = true,
      seed = 0L
    )

    // Create a dummy world map with the dummy dungeon
    val dummyWorldMap = map.WorldMap(
      tiles = dummyDungeon.tiles,
      dungeons = Seq(dummyDungeon),
      paths = Set.empty,
      bridges = Set.empty,
      bounds = map.MapBounds(-1, 1, -1, 1)
    )

    val dummyPlayer = game.entity.Entity(
      id = "dummy",
      game.entity.Movement(position = game.Point(0, 0)),
      game.entity.EntityTypeComponent(game.entity.EntityType.Player),
      game.entity.Health(100),
      game.entity.Initiative(5),
      game.entity.Inventory(Nil),
      game.entity.Drawable(data.Sprites.playerSprite),
      game.entity.Hitbox(),
      game.entity.DeathEvents()
    )

    val dummyGameState = game.GameState(
      playerEntityId = "dummy",
      entities = Vector(dummyPlayer),
      worldMap = dummyWorldMap
    )

    Outcome(
      GameController(
        UIState.MainMenu(),
        dummyGameState
      )
    )
  }

  override def updateModel(
      context: Context[Unit],
      model: GameController
  ): GlobalEvent => Outcome[GameController] =
    _ =>
      val optInput =
        context.frame.input.mapInputsOption(InputMappings.inputMapping)
      val time = context.frame.time.running.toMillis.toLong * 1000000L

      try {
        Outcome(model.update(optInput, time))
      } catch {
        case e: Exception =>
          println(s"Error during model update: ${e.getMessage}")
          e.printStackTrace()
          Outcome(model)
      }

  override def present(
      context: Context[Unit],
      model: GameController
  ): Outcome[SceneUpdateFragment] = {
    model.uiState match {
      case _: UIState.MainMenu =>
        // Render main menu screen
        Outcome(
          SceneUpdateFragment(
            Layer.Content(mainMenu(model))
          )
        )
      case gameOver: UIState.GameOver =>
        // Render game over screen
        Outcome(
          SceneUpdateFragment(
            Layer.Content(gameOverScreen(model, gameOver.player))
          )
        )
      case UIState.WorldMap =>
        Outcome(
          worldMapView(model)
        )
      case preview: UIState.WorldMapPreview =>
        Outcome(
          indigoengine.view.ui.OverworldMapUI
            .render(preview.overworldMap, preview.seed)
        )
      case _: UIState.Inventory | _: UIState.InventoryActionState =>
        Outcome(
          presentGame(context, model) |+| SceneUpdateFragment(
            Layer.Content(
              InventoryMenu.render(
                model,
                Graphic(0, 0, 784, 352, Material.Bitmap(AssetName("sprites")))
              )
            )
          )
        )
      case UIState.Character =>
        Outcome(
          presentGame(context, model) |+| SceneUpdateFragment(
            Layer.Content(
              CharacterScreen.render(
                model,
                Graphic(0, 0, 784, 352, Material.Bitmap(AssetName("sprites")))
              )
            )
          )
        )

      case _ =>
        Outcome(presentGame(context, model))
    }
  }

  def presentGame(
      context: Context[Unit],
      model: GameController
  ): SceneUpdateFragment = {
    // Render normal game
    val spriteSheet =
      Graphic(0, 0, 784, 352, Material.Bitmap(AssetName("sprites")))
    val player = model.gameState.playerEntity
    val game.Point(playerX, playerY) = player.position
    val visiblePoints = model.gameState.getVisiblePointsFor(player)
    val sightMemory =
      player.get[SightMemory].map(_.seenPoints).getOrElse(Set.empty)

    // Combine filtering and mapping for tileSprites
    // Optimize: Only iterate over visible viewport instead of entire world map
    val rangeX =
      (UIConfig.xTiles / 2) + 2 // +2 buffer to ensure no edges visible
    val rangeY = (UIConfig.yTiles / 2) + 2

    val minX = playerX - rangeX
    val maxX = playerX + rangeX
    val minY = playerY - rangeY
    val maxY = playerY + rangeY

    // Sepia tint for Fog of War
    val sepiaTint = RGBA.fromHexString("#8b7e66")

    // 0. Base Dirt Layer Pass
    // The dual grid system assumes a base layer of soil/dirt (likely sprite index 0).
    // We render this on the standard logical grid for the entire viewport.
    val dirtSprites = (for {
      x <- minX to maxX
      y <- minY to maxY
    } yield {
      val tilePosition = game.Point(x, y)
      if (sightMemory.contains(tilePosition) || UIConfig.ignoreLineOfSight) {
        val pixelX = x * map.AutoTiler.TileSize
        val pixelY = y * map.AutoTiler.TileSize
        val sprite = map.AutoTiler.getDirtSprite().moveTo(pixelX, pixelY)

        if (visiblePoints.contains(tilePosition)) Some(sprite)
        else
          Some(
            sprite
              .asInstanceOf[Graphic[Material.Bitmap]]
              .modifyMaterial(_.toImageEffects.withTint(sepiaTint))
          )
      } else {
        None
      }
    }).flatten

    // 1. Dual Grid Terrain Pass (Grass/Dirt)
    // Iterate over vertices. Vertex (x,y) is at the corner of logical tiles.
    // We render a tile centered at this vertex.
    val terrainSprites = (for {
      x <- minX to (maxX + 1)
      y <- minY to (maxY + 1)
    } yield {
      // Visibility check for vertex:
      // Check the 4 surrounding tiles.
      val neighbors = List(
        game.Point(x - 1, y - 1),
        game.Point(x, y - 1),
        game.Point(x - 1, y),
        game.Point(x, y)
      )

      val anyVisible = neighbors.exists(visiblePoints.contains)
      val anySeen = neighbors.exists(sightMemory.contains)

      if ((anySeen || UIConfig.ignoreLineOfSight)) {
        map.AutoTiler
          .getTerrainSprite(model.gameState.worldMap, x, y, spriteSheet)
          .map { graphic =>
            val pixelX = x * map.AutoTiler.TileSize
            val pixelY = y * map.AutoTiler.TileSize
            val sprite = graphic.moveTo(pixelX, pixelY)

            if (anyVisible || UIConfig.ignoreLineOfSight) sprite
            else
              sprite
                .asInstanceOf[Graphic[Material.Bitmap]]
                .modifyMaterial(_.toImageEffects.withTint(sepiaTint))
          }
      } else {
        None
      }
    }).flatten

    // 2. Foreground Pass (Objects + Entities) sorted by Y
    // We mix Objects (Trees, Walls) and Entities (Player, Enemies) and sort them by Y-coordinate.

    val objectRenderables = (for {
      y <- minY to maxY
    } yield {
      // Helper to check if a position contains a visible tree
      def isVisibleTree(pos: game.Point): Boolean = {
        model.gameState.worldMap.getTile(pos).exists { t =>
          t == TileType.Tree && (sightMemory.contains(
            pos
          ) || UIConfig.ignoreLineOfSight)
        }
      }

      // Helper to check if a position contains a tree (Map Truth, ignores visibility)
      def isTree(pos: game.Point): Boolean = {
        model.gameState.worldMap.getTile(pos).exists(_ == TileType.Tree)
      }

      // Helper: Identify the tree group starting at (startX, y)
      // Returns: (horizontalTilesConsumed, isVerticalGroup, matchingSprite)
      // horizontalTilesConsumed: how many tiles this group occupies horizontally on its starting row.
      // isVerticalGroup: true if this group also occupies tiles on the row below.
      def identifyTreeGroup(
          startX: Int,
          startY: Int
      ): (Int, Boolean, Option[Graphic[Material.Bitmap]]) = {
        // Check horizontal availability using Map Truth (isTree)
        // We allows hidden anchors to form groups, so they can render if they extend into view
        val h1 = isTree(game.Point(startX, startY))
        if (!h1) return (0, false, None)

        // Check neighbors using Map Truth (isTree) to ensure consistent grouping
        val h2 = isTree(game.Point(startX + 1, startY))
        val h3 = isTree(game.Point(startX + 2, startY))

        // Check 3x2 (Tree4)
        if (h1 && h2 && h3) {
          // Check vertical for 3x2
          val v3_1 = isTree(game.Point(startX, startY + 1))
          val v3_2 = isTree(game.Point(startX + 1, startY + 1))
          val v3_3 = isTree(game.Point(startX + 2, startY + 1))

          if (v3_1 && v3_2 && v3_3) {
            return (
              3,
              true,
              Some(
                Graphic(0, 0, 54, 103, Material.Bitmap(AssetName("tree4")))
                  .withRef(27, 103)
                  .moveTo(startX * 16 + 24, startY * 16 + 24)
              )
            )
          }
        }

        // Check 2x2 (Tree3)
        if (h1 && h2) {
          // Check vertical for 2x2
          val v2_1 = isTree(game.Point(startX, startY + 1))
          val v2_2 = isTree(game.Point(startX + 1, startY + 1))

          if (v2_1 && v2_2) {
            return (
              2,
              true,
              Some(
                Graphic(0, 0, 37, 76, Material.Bitmap(AssetName("tree3")))
                  .withRef(18, 76)
                  .moveTo(startX * 16 + 16, startY * 16 + 24)
              )
            )
          }
        }

        // Check 2x1 (Tree2) - Greedy horizontal fallback
        if (h1 && h2) {
          return (
            2,
            false,
            Some(
              Graphic(0, 0, 29, 47, Material.Bitmap(AssetName("tree2")))
                .withRef(14, 47)
                .moveTo(startX * 16 + 16, startY * 16 + 8)
            )
          )
        }

        // Fallback 1x1 (Tree)
        (
          1,
          false,
          Some(
            Graphic(0, 0, 16, 31, Material.Bitmap(AssetName("tree")))
              .withRef(8, 31)
              .moveTo(startX * 16 + 8, startY * 16 + 8)
          )
        )
      }

      // Pre-calculate occlusion from the row above (y-1)
      val prevY = y - 1
      val coveredIndices = scala.collection.mutable.HashSet.empty[Int]
      val renderMinX = minX - 3

      // Broad phase: Check if there are ANY trees in the scan range on prevY before doing the expensive scan
      // This is a micro-optimization but useful if the map is sparse
      var prevScanX = renderMinX
      while (isTree(game.Point(prevScanX - 1, prevY))) {
        prevScanX -= 1
      }

      // Forward scan on prevY to populate coveredIndices
      var simX = prevScanX
      while (simX <= maxX) {
        val p = game.Point(simX, prevY)
        if (isTree(p)) {
          val (width, isVertical, _) = identifyTreeGroup(simX, prevY)
          if (isVertical) {
            var k = 0
            while (k < width) {
              coveredIndices.add(simX + k)
              k += 1
            }
          }
          simX += (if (width > 0) width else 1)
        } else {
          simX += 1
        }
      }

      // Backscan local row to find start of streak
      // This ensures that the greedy grouping relies on the absolute start of a tree row
      var scanX = minX - 1
      while (isTree(game.Point(scanX, y))) {
        scanX -= 1
      }
      val streakStart = scanX + 1

      // Simulate forward from streak start to determine state at minX
      var currentSimX = streakStart
      var skipNext = 0
      if (currentSimX < renderMinX) {
        // Run simulation up to renderMinX to correctly set skipNext for the first processed tile
        while (currentSimX < renderMinX) {
          val (width, _, _) = identifyTreeGroup(currentSimX, y)
          currentSimX += (if (width > 0) width else 1)
        }
        skipNext = currentSimX - renderMinX
      }

      (renderMinX to maxX).flatMap { x =>
        if (skipNext > 0) {
          skipNext -= 1
          None
        } else {
          // Check if covered by a vertical group from above
          if (coveredIndices.contains(x)) {
            None
          } else {
            val tilePosition = game.Point(x, y)
            val isMem =
              sightMemory.contains(tilePosition) || UIConfig.ignoreLineOfSight
            val isTreeTile = isTree(tilePosition)

            // We process if it's in memory OR if it's a tree (Hidden Anchor Logic)
            if (isMem || isTreeTile) {

              model.gameState.worldMap.getTile(tilePosition).flatMap {
                tileType =>
                  val isGrassOrDirt = tileType match {
                    case TileType.Grass1 | TileType.Grass2 | TileType.Grass3 |
                        TileType.Dirt =>
                      true
                    case _ => false
                  }

                  if (!isGrassOrDirt) {
                    val isVisible = visiblePoints.contains(tilePosition)

                    // Special handling for Tree
                    val maybeSprite = if (tileType == TileType.Tree) {
                      val (width, _, sprite) = identifyTreeGroup(x, y)
                      skipNext = if (width > 0) width - 1 else 0

                      // If this is a Hidden Anchor (not in memory), we only render if the group extends into memory
                      if (!isMem) {
                        // Check if any tile in this group is in memory
                        val groupVisible = (0 until width).exists(offset =>
                          sightMemory.contains(game.Point(x + offset, y))
                        )
                        if (groupVisible) sprite else None
                      } else {
                        sprite
                      }
                    } else if (isMem) { // Non-tree tiles must be in memory
                      Some(spriteSheet.fromTile(tilePosition, tileType))
                    } else {
                      None
                    }

                    maybeSprite.map { sprite =>
                      val finalSprite =
                        if (isVisible) sprite // Anchor itself is visible
                        else
                          sprite
                            .asInstanceOf[Graphic[Material.Bitmap]]
                            .modifyMaterial(
                              _.toImageEffects.withTint(sepiaTint)
                            )

                      (y, finalSprite)
                    }
                  } else None
              }
            } else {
              None
            }
          }
        }
      }
    }).flatten

    // 2b. Entity Renderables
    val entityRenderables = model.gameState.entities
      .filter(e => visiblePoints.contains(e.position))
      .flatMap { entity =>
        val y = entity.position.y
        (spriteSheet.fromEntity(entity) ++ enemyHealthBar(entity)).map { node =>
          (y, node)
        }.toList
      }

    // 2c. Combined Sort and Batch
    val sortedForeground = (objectRenderables ++ entityRenderables)
      .sortBy(_._1) // Sort by Y
      .map(_._2) // Extract Node
      .toBatch

    val tileSprites = Batch.fromSeq(dirtSprites ++ terrainSprites)
    val cursorBatch = Batch.fromSeq(drawUIElements(spriteSheet, model))

    SceneUpdateFragment(
      Layer
        .Content(tileSprites ++ sortedForeground ++ cursorBatch)
        .withCamera(
          Camera
            .LookAt(Point(playerX * spriteScale, playerY * spriteScale))
        ),
      Layer.Content(
        healthBar(model)
          ++ experienceBar(model)
          ++ coins(model, spriteSheet)
          ++ perkSelection(model)
          ++ keys(model, spriteSheet)
          ++ equipmentPaperdoll(model, spriteSheet)
          ++ tradeItemDisplay(model, spriteSheet)
          ++ conversationWindow(model, spriteSheet)
          ++ messageWindow(model)
          ++ villageName(model)
          ++ versionInfo(model)
          ++ renderDebugMenu(model)
          ++ debugItemSelection(model, spriteSheet)
          ++ debugPerkSelection(model)
      )
    )
  }

  private def drawUIElements(
      spriteSheet: Graphic[?],
      model: GameController
  ): Seq[SceneNode] = {
    // Don't show targeting for trade-related lists (buying/selling)
    val optCursorTargetInfo = model.uiState match {
      case scrollSelect: UIState.ScrollSelectState =>
        Some((scrollSelect.cursor, None)) // Position only, no entity

      // Skip targeting for buying/selling - these are inventory/shop UIs
      case _: UIState.BuyItemSelect | _: UIState.SellItemSelect =>
        None

      // Handle action target selection (for attacks, equipping, trading, etc.)
      case actionTargetSelect: UIState.ActionTargetSelect =>
        if (actionTargetSelect.list.nonEmpty) {
          val actionTarget = actionTargetSelect.currentItem
          Some((actionTarget.entity.position, Some(actionTarget.entity)))
        } else {
          None
        }

      // Handle enemy targeting (for ranged attacks, spells, etc.)
      case enemyTargetSelect: UIState.EnemyTargetSelect =>
        if (enemyTargetSelect.list.nonEmpty) {
          val targetEntity = enemyTargetSelect.currentItem
          Some((targetEntity.position, Some(targetEntity)))
        } else {
          None
        }

      case _ =>
        None
    }

    val playerPosition = model.gameState.playerEntity.position

    optCursorTargetInfo.toSeq.flatMap {
      case (game.Point(cursorX, cursorY), optTargetEntity) =>
        val line = LineOfSight
          .getBresenhamLine(playerPosition, game.Point(cursorX, cursorY))
          .dropRight(1)
          .map { point =>
            Shape.Box(
              Rectangle(
                Point(point.x * spriteScale, point.y * spriteScale),
                Size(spriteScale)
              ),
              Fill.Color(RGBA.Red.withAlpha(0.5f))
            )
          }

        // Get cursor positions based on entity hitbox or single position
        val cursorPositions = optTargetEntity match {
          case Some(entity) =>
            // For entities with hitboxes, highlight all hitbox points
            import game.entity.Hitbox.*
            entity.get[game.entity.Hitbox] match {
              case Some(hitbox) =>
                hitbox.points.map(hitboxPoint =>
                  game.Point(cursorX + hitboxPoint.x, cursorY + hitboxPoint.y)
                )
              case None =>
                Set(game.Point(cursorX, cursorY))
            }
          case None =>
            // For scroll select or non-entity targets, just use the position
            Set(game.Point(cursorX, cursorY))
        }

        val cursorSprite = Sprites.cursorSprite

        // Draw cursor sprite at each position
        val cursorSprites = cursorPositions.map { pos =>
          spriteSheet
            .fromSprite(cursorSprite)
            .moveTo(pos.x * spriteScale, pos.y * spriteScale)
        }

        // Draw red highlight boxes for multi-tile entities
        val redHighlights = optTargetEntity match {
          case Some(entity) =>
            import game.entity.Hitbox.*
            entity.get[game.entity.Hitbox] match {
              case Some(hitbox) if hitbox.points.size > 1 =>
                // Multi-tile entity: draw red box for each hitbox point
                hitbox.points.map { hitboxPoint =>
                  val highlightPos =
                    game.Point(cursorX + hitboxPoint.x, cursorY + hitboxPoint.y)
                  Shape.Box(
                    Rectangle(
                      Point(
                        highlightPos.x * spriteScale,
                        highlightPos.y * spriteScale
                      ),
                      Size(spriteScale)
                    ),
                    Fill.Color(RGBA.Red.withAlpha(0.5f))
                  )
                }
              case _ =>
                Set.empty
            }
          case None =>
            Set.empty
        }

        line ++ cursorSprites ++ redHighlights
    }
  }
}
