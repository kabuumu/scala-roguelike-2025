package game

case class Health(current: Int, max: Int) {
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
}

object Health {
  def apply(max: Int): Health = new Health(max, max)
}
