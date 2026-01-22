package indigoengine.view

import indigo.*
import indigoengine.view.ui.*
import indigoengine.view.components.MenuComponent
import _root_.ui.GameController
import game.entity.Entity

object Elements {
  def text(text: String, x: Int, y: Int): SceneNode = UIUtils.text(text, x, y)
  def wrapText(text: String, maxLineLength: Int): Seq[String] =
    UIUtils.wrapText(text, maxLineLength)

  def healthBar(model: GameController): Batch[SceneNode] =
    StatusUI.healthBar(model)
  def experienceBar(model: GameController): Batch[SceneNode] =
    StatusUI.experienceBar(model)
  def enemyHealthBar(enemyEntity: Entity): Batch[SceneNode] =
    StatusUI.enemyHealthBar(enemyEntity)

  def keys(model: GameController, spriteSheet: Graphic[?]): Batch[SceneNode] =
    InventoryUI.keys(model, spriteSheet)
  def coins(model: GameController, spriteSheet: Graphic[?]): Batch[SceneNode] =
    InventoryUI.coins(model, spriteSheet)
  def equipmentPaperdoll(
      model: GameController,
      spriteSheet: Graphic[?]
  ): Batch[SceneNode] = InventoryUI.equipmentPaperdoll(model, spriteSheet)

  def versionInfo(model: GameController): Batch[SceneNode] =
    HUD.versionInfo(model)
  def messageWindow(model: GameController): Batch[SceneNode] =
    HUD.messageWindow(model)
  def villageName(model: GameController): Batch[SceneNode] =
    HUD.villageName(model)

  def mainMenu(model: GameController): Batch[SceneNode] = Menus.mainMenu(model)
  def renderDebugMenu(model: GameController): Batch[SceneNode] =
    Menus.renderDebugMenu(model)
  def gameOverScreen(model: GameController, player: Entity): Batch[SceneNode] =
    Menus.gameOverScreen(model, player)

  def tradeItemDisplay(
      model: GameController,
      spriteSheet: Graphic[?]
  ): Batch[SceneNode] = TradeUI.tradeItemDisplay(model, spriteSheet)
  def debugItemSelection(
      model: GameController,
      spriteSheet: Graphic[?]
  ): Batch[SceneNode] = TradeUI.debugItemSelection(model, spriteSheet)

  def conversationWindow(
      model: GameController,
      spriteSheet: Graphic[?]
  ): Batch[SceneNode] = ConversationUI.conversationWindow(model, spriteSheet)

  def worldMapView(
      worldTiles: Map[game.Point, map.TileType],
      canvasWidth: Int,
      canvasHeight: Int,
      playerPosition: game.Point,
      seenPoints: Set[game.Point]
  ): SceneUpdateFragment =
    WorldMapUI.worldMapView(
      worldTiles,
      canvasWidth,
      canvasHeight,
      playerPosition,
      seenPoints
    )

  def worldMapView(model: GameController): SceneUpdateFragment =
    WorldMapUI.render(model)

  def perkSelection(model: GameController): Batch[SceneNode] =
    PerkUI.perkSelection(model)
  def debugPerkSelection(model: GameController): Batch[SceneNode] =
    PerkUI.debugPerkSelection(model)
}
