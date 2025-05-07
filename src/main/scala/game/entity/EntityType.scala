package game.entity

import game.Item.KeyColour
import game.Item.Item

case class EntityTypeComponent(entityType: EntityType) extends Component

object EntityType {
  extension (entity: Entity) {
    //TODO - remove default to player, potentially remove entity types entirely
    def entityType: EntityType = entity.get[EntityTypeComponent].map(_.entityType).getOrElse(EntityType.Player)
  }
}

enum EntityType(val isStatic: Boolean, val blocksMovement: Boolean):
  case Player extends EntityType(false, true)
  case Enemy extends EntityType(false, true)
  case Wall extends EntityType(true, true)
  case Floor extends EntityType(true, false)
  case LockedDoor(keyColour: KeyColour) extends EntityType(true, true)
  case Key(keyColour: KeyColour) extends EntityType(true, false)
  case ItemEntity(itemType: Item) extends EntityType(true, false)
  case Projectile extends EntityType(false, false)
