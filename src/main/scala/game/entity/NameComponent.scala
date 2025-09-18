package game.entity

case class NameComponent(name: String, description: String = "") extends Component

object NameComponent {
  extension (entity: Entity) {
    def name: Option[String] = entity.get[NameComponent].map(_.name)
    def description: Option[String] = entity.get[NameComponent].map(_.description)
    def hasName: Boolean = entity.has[NameComponent]
  }
}