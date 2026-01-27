package game.entity

import indigo.shared.constants.Key

case class Growth(
    currentStage: Int,
    maxStage: Int,
    growthTimer: Int,
    timePerStage: Int,
    stageSprites: Map[Int, game.Sprite]
) extends Component {
  def updateTimer(delta: Int): Growth = {
    copy(growthTimer = growthTimer - delta)
  }

  def grow: Growth = {
    if (currentStage < maxStage) {
      copy(currentStage = currentStage + 1, growthTimer = timePerStage)
    } else {
      this
    }
  }

  def currentSprite: Option[game.Sprite] = stageSprites.get(currentStage)
}
