package game.entity

import game.{Direction, Point}

case class Movement(position: Point) extends Component {
  def move(direction: Direction): Movement = {
    copy(position = position + direction)
  }
}
