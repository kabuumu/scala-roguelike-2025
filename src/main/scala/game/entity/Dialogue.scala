package game.entity

case class Dialogue(message: String) extends Component

object Dialogue {
  extension (entity: Entity) {
    def dialogue: Option[String] = entity.get[Dialogue].map(_.message)
    def hasDialogue: Boolean = entity.has[Dialogue]
  }
}
