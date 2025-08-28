package game.entity

/**
 * Component that marks an item entity as being in someone's inventory.
 * Items with this component are not rendered on the world map.
 * @param ownerEntityId The ID of the entity that owns this item
 */
case class InInventory(ownerEntityId: String) extends Component

object InInventory {
  extension (entity: Entity) {
    def isInInventory: Boolean = entity.has[InInventory]
    
    def getInventoryOwner: Option[String] = entity.get[InInventory].map(_.ownerEntityId)
    
    def putInInventory(ownerEntityId: String): Entity = 
      entity.addComponent(InInventory(ownerEntityId))
    
    def removeFromInventory: Entity = 
      entity.removeComponent[InInventory]
  }
}