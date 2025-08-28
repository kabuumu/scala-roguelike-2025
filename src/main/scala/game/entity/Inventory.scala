package game.entity

import game.entity.KeyItem.{isKey, keyItem}

case class Inventory(
  itemEntityIds: Seq[String] = Nil, 
  primaryWeaponId: Option[String] = None, 
  secondaryWeaponId: Option[String] = None
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
    
    // Get primary weapon entity
    def primaryWeapon(gameState: game.GameState): Option[Entity] = 
      entity.get[Inventory].flatMap(_.primaryWeaponId.flatMap(gameState.getEntity))
    
    // Get secondary weapon entity  
    def secondaryWeapon(gameState: game.GameState): Option[Entity] = 
      entity.get[Inventory].flatMap(_.secondaryWeaponId.flatMap(gameState.getEntity))

    // Get keys from inventory
    def keys(gameState: game.GameState): Seq[Entity] = 
      inventoryItems(gameState).filter(_.isKey)
    
    // Check if inventory contains a specific key color
    def hasKey(gameState: game.GameState, keyColour: KeyColour): Boolean =
      keys(gameState).exists(_.keyItem.exists(_.keyColour == keyColour))

    def addItemEntity(itemEntityId: String): Entity = 
      entity.update[Inventory](_.addItemEntityId(itemEntityId))

    def removeItemEntity(itemEntityId: String): Entity = 
      entity.update[Inventory](_.removeItemEntityId(itemEntityId))
  }
}
