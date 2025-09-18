package game.entity

import upickle.default.ReadWriter

case class Initiative(maxInitiative: Int, currentInitiative: Int) extends Component derives ReadWriter {
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
  def apply(maxInitiative: Int): Initiative = {
    Initiative(maxInitiative, maxInitiative)
  }

  extension (entity: Entity) {
    def isReady: Boolean = entity.exists[Initiative](_.isReady)
    def notReady: Boolean = !isReady

    def resetInitiative(): Entity = {
      entity.update[Initiative](_.reset())
    }

    def decreaseInitiative(): Entity =
      entity.update[Initiative](_.decrement())
  }
}
