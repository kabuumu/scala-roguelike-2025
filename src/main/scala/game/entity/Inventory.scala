package game.entity

import game.entity
import game.entity.KeyItem.{isKey, keyItem}

case class Inventory(
    itemEntityIds: Seq[String] = Nil,
    capacity: Int = 20
) extends Component {

  def isFull: Boolean = itemEntityIds.size >= capacity

  def addItemEntityId(entityId: String): Inventory = {
    if (itemEntityIds.contains(entityId) || isFull) this
    else copy(itemEntityIds = itemEntityIds :+ entityId)
  }

  def removeItemEntityId(entityId: String): Inventory = {
    copy(itemEntityIds = itemEntityIds.filterNot(_ == entityId))
  }

  val isEmpty: Boolean = itemEntityIds.isEmpty
}

object Inventory {
  extension (entity: Entity) {
    // Get actual item entities from the game state
    def inventoryItems(gameState: game.GameState): Seq[Entity] =
      entity
        .get[Inventory]
        .toSeq
        .flatMap(_.itemEntityIds.flatMap(gameState.getEntity))

    // Get keys from inventory
    def keys(gameState: game.GameState): Seq[Entity] =
      inventoryItems(gameState).filter(_.isKey)

    // Check if inventory contains a specific key color
    def hasKey(gameState: game.GameState, keyColour: KeyColour): Boolean =
      keys(gameState).exists(_.keyItem.exists(_.keyColour == keyColour))

    // Get usable items from inventory (items with UsableItem component)
    def usableItems(gameState: game.GameState): Seq[Entity] =
      inventoryItems(gameState).filter(_.has[UsableItem])

    def addItemEntity(itemEntityId: String): Entity =
      entity.update[Inventory](_.addItemEntityId(itemEntityId))

    def removeItemEntity(itemEntityId: String): Entity =
      entity.update[Inventory](_.removeItemEntityId(itemEntityId))
  }
}
