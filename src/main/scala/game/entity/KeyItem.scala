package game.entity

import upickle.default.ReadWriter

// Key colors
enum KeyColour derives ReadWriter {
  case Yellow, Red, Blue
}

// Component marking that an entity is a key with a specific color
case class KeyItem(keyColour: KeyColour) extends Component

object KeyItem {
  extension (entity: Entity) {
    def keyItem: Option[KeyItem] = entity.get[KeyItem]
    def isKey: Boolean = entity.has[KeyItem]
  }
}