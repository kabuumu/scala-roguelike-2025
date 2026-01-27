package game.entity

case class EntityTypeComponent(entityType: EntityType) extends Component

object EntityType {
  extension (entity: Entity) {
    // TODO - remove default to player, potentially remove entity types entirely
    def entityType: EntityType = entity
      .get[EntityTypeComponent]
      .map(_.entityType)
      .getOrElse(EntityType.Player)
  }

  def unapply(entity: Entity): Option[EntityType] = {
    entity.get[EntityTypeComponent].map(_.entityType)
  }
}

enum EntityType(val isStatic: Boolean, val blocksMovement: Boolean):
  case Player extends EntityType(false, true)
  case Enemy extends EntityType(false, true)
  case Trader extends EntityType(false, true)
  case Wall extends EntityType(true, true)
  case Floor extends EntityType(true, false)
  case Animal extends EntityType(false, true)
  case LockedDoor(keyColour: KeyColour) extends EntityType(true, true)
  case Key(keyColour: KeyColour) extends EntityType(true, false)
  case Projectile extends EntityType(false, false)
  case Stairs extends EntityType(true, false)
  case Plant extends EntityType(true, false)
  case Villager extends EntityType(false, true)
  case PackAnimal extends EntityType(false, true)
