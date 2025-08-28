package game.entity

import game.Item
import game.Item.ChargeType.{Ammo, SingleUse}
import game.Item.{Item, UnusableItem, UsableItem, Weapon}
import game.GameState

case class Inventory(itemEntityIds: Seq[String] = Nil, primaryWeapon: Option[Weapon] = None, secondaryWeapon: Option[Weapon] = None) extends Component {
  def contains(itemEntityId: String): Boolean = itemEntityIds.contains(itemEntityId)

  def -(itemEntityId: String): Inventory = {
    copy(itemEntityIds = itemEntityIds.filterNot(_ == itemEntityId))
  }

  def +(itemEntityId: String): Inventory = {
    copy(itemEntityIds = itemEntityIds :+ itemEntityId)
  }

  val isEmpty: Boolean = itemEntityIds.isEmpty
}

object Inventory {
  extension (entity: Entity) {
    def itemEntityIds: Seq[String] = entity.get[Inventory].toSeq.flatMap(_.itemEntityIds)
    
    def getItemEntities(gameState: GameState): Seq[Entity] = {
      val ids = itemEntityIds
      gameState.entities.filter(e => ids.contains(e.id))
    }
    
    def items(gameState: GameState): Seq[Item] = {
      import game.entity.EntityType.*
      getItemEntities(gameState).flatMap { itemEntity =>
        itemEntity.entityType match {
          case ItemEntity(item) => Some(item)
          case Key(keyColour) => Some(Item.Key(keyColour))
          case _ => None
        }
      }
    }

    def keys(gameState: GameState): Seq[Item.Key] = items(gameState).collect {
      case key: Item.Key => key
    }
    
    def groupedUsableItems(gameState: GameState): Map[UsableItem, Int] = {
      val allItems = items(gameState)
      allItems.collect {
        case usableItem: UsableItem => usableItem
      }.groupBy(identity).view.map {
        case (item, list) =>
          item.chargeType match {
            case SingleUse => item -> list.size
            case Ammo(ammoType) =>
              item -> allItems.count(_ == ammoType)
          }

      }.toMap
    }

    def groupedUnusableItems(gameState: GameState): Map[UnusableItem, Int] = {
      items(gameState).collect {
        case unusableItem: UnusableItem => unusableItem
      }.groupBy(identity).view.mapValues(_.size).toMap
    }

    def addItem(itemEntityId: String): Entity = entity.update[Inventory](_ + itemEntityId)

    def removeItem(itemEntityId: String): Entity = entity.update[Inventory](_ - itemEntityId)
    
    // Helper method to find item entity ID by Item type (for backward compatibility during transition)
    def findItemEntityId(gameState: GameState, item: Item): Option[String] = {
      import game.entity.EntityType.*
      getItemEntities(gameState).find { itemEntity =>
        itemEntity.entityType match {
          case ItemEntity(entityItem) => entityItem == item
          case Key(keyColour) => item == Item.Key(keyColour)
          case _ => false
        }
      }.map(_.id)
    }
    
    // Helper method to remove item by Item type (for backward compatibility)
    def removeItemByType(gameState: GameState, item: Item): Entity = {
      findItemEntityId(gameState, item) match {
        case Some(entityId) => entity.removeItem(entityId)
        case None => entity
      }
    }
  }
}
