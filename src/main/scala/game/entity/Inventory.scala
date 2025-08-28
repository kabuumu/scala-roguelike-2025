package game.entity

import game.Item
import game.Item.ChargeType.{Ammo, SingleUse}
import game.Item.{Item, UnusableItem, UsableItem, Weapon}
import game.entity.ItemType.itemType

case class Inventory(
  itemEntityIds: Seq[String] = Nil, 
  primaryWeapon: Option[Weapon] = None, 
  secondaryWeapon: Option[Weapon] = None
) extends Component {
  
  def addItemEntityId(entityId: String): Inventory = {
    copy(itemEntityIds = itemEntityIds :+ entityId)
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
      entity.get[Inventory].toSeq.flatMap(_.itemEntityIds.flatMap(gameState.getEntity))
    
    // Get item objects for backward compatibility
    def items(gameState: game.GameState): Seq[Item] = 
      inventoryItems(gameState).flatMap(_.itemType)

    def keys(gameState: game.GameState): Seq[Item.Key] = items(gameState).collect {
      case key: Item.Key => key
    }
    
    def groupedUsableItems(gameState: game.GameState): Map[UsableItem, Int] = 
      items(gameState).collect {
        case usableItem: UsableItem => usableItem
      }.groupBy(identity).view.map {
        case (item, list) =>
          item.chargeType match {
            case SingleUse => item -> list.size
            case Ammo(ammoType) =>
              item -> items(gameState).count(_ == ammoType)
          }
      }.toMap

    def groupedUnusableItems(gameState: game.GameState): Map[UnusableItem, Int] = 
      items(gameState).collect {
        case unusableItem: UnusableItem => unusableItem
      }.groupBy(identity).view.mapValues(_.size).toMap

    def addItemEntity(itemEntityId: String): Entity = 
      entity.update[Inventory](_.addItemEntityId(itemEntityId))

    def removeItemEntity(itemEntityId: String): Entity = 
      entity.update[Inventory](_.removeItemEntityId(itemEntityId))
      
    // Backward compatibility methods - these will need game state
    def addItem(item: Item): Entity = {
      // This method can't work properly without creating an entity
      // It's kept for compilation but should be replaced
      entity
    }

    def removeItem(item: Item): Entity = {
      // This method can't work properly without knowing entity IDs
      // It's kept for compilation but should be replaced
      entity
    }
  }
}
