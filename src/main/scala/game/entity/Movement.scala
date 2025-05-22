package game.entity

import game.{Direction, Point}

case class Movement(position: Point) extends Component {
  def move(direction: Direction): Movement = {
    copy(position = position + direction)
  }
}

object Movement {
  extension (entity: Entity) {
    def position: Point = entity.get[Movement].map(_.position).getOrElse(Point(0, 0))
  }
}
