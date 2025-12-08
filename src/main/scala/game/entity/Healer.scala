package game.entity

/** Component for entities that can heal the player for a price.
  */
case class Healer(healAmount: Int = 20, cost: Int = 10) extends Component

object Healer {
  extension (entity: Entity) {
    def isHealer: Boolean = entity.has[Healer]
    def healer: Option[Healer] = entity.get[Healer]
  }
}
