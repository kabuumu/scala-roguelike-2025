package game.entity

case class Initiative(maxInitiative: Int, currentInitiative: Int) extends Component {
  val isReady: Boolean = currentInitiative == 0

  def reset(): Initiative = {
    copy(currentInitiative = maxInitiative)
  }

  def decrement(): Initiative = {
    val newInitiative = math.max(currentInitiative - 1, 0)
    copy(currentInitiative = newInitiative)
  }
}

object Initiative {
  val isReady: Initiative => Boolean = _.isReady

  def apply(maxInitiative: Int): Initiative = {
    Initiative(maxInitiative, maxInitiative)
  }
}
