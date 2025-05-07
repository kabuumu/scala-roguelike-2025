package game.entity

case class Health(current: Int, max: Int) extends Component {
  lazy val toMax: Health = Health(max, max)
  def -(health: Int): Health = {
    val newCurrent = math.max(current - health, 0)
    Health(newCurrent, max)
  }

  def +(health: Int): Health = {
    val newCurrent = math.min(current + health, max)
    Health(newCurrent, max)
  }

  val isFull: Boolean = current == max

  val isAlive: Boolean = current > 0
  val isDead: Boolean = current <= 0
}

object Health {
  def apply(max: Int): Health = new Health(max, max)
}
