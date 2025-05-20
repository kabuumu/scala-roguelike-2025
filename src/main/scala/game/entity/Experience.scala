package game.entity

case class Experience(currentExperience: Int) extends Component {
  val levelConstant = 10

  val currentLevel: Int = {
    val level = math.sqrt(currentExperience / levelConstant).toInt
    if (level < 1) 1 else level
  }

  val nextLevel: Int = currentLevel + 1

  val nextLevelExperience: Int = {
    if (currentLevel < 1) levelConstant else nextLevel * nextLevel * levelConstant
  }

  def addExperience(amount: Int): Experience = copy(currentExperience + amount)
}

object Experience {
  extension (entity: Entity) {
    def experience: Int = entity.get[Experience].map(_.currentExperience).getOrElse(0)

    def level: Int = entity.get[Experience].map(_.currentLevel).getOrElse(1)

    def addExperience(amount: Int): Entity = entity.update[Experience](_.addExperience(amount))
  }
}
