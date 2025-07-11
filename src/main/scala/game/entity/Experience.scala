package game.entity

import game.entity.Experience.*
import game.perk.Perks.*
import game.status.StatusEffect

case class Experience(currentExperience: Int = 0, levelUp: Boolean = false) extends Component {
  val currentLevel: Int = {
    val level = math.sqrt(currentExperience / LEVEL_CONSTANT).toInt
    if (level < 1) 1 else level
  }
  


  val nextLevel: Int = currentLevel + 1

  val nextLevelExperience: Int = nextLevel * nextLevel * LEVEL_CONSTANT

  def addExperience(amount: Int): Experience = {
    val newExperience = currentExperience + amount
    val newLevelUp = levelUp || newExperience >= nextLevelExperience
    
    copy(currentExperience = newExperience, levelUp = newLevelUp)
  }
}

object Experience {
  final val LEVEL_CONSTANT: Int = 1000

  // Calculate the experience required for a given level 
  def experienceForLevel(level: Int): Int =
    if (level <= 1) 0
    else level * level * LEVEL_CONSTANT

  extension (entity: Entity) {
    def experience: Int = entity.get[Experience].map(_.currentExperience).getOrElse(0)

    def level: Int = entity.get[Experience].map(_.currentLevel).getOrElse(1)

    def addExperience(amount: Int): Entity = entity.update[Experience](_.addExperience(amount))
    
    def canLevelUp: Boolean = entity.get[Experience].exists(_.levelUp)
    
    def levelUp: Entity = entity.update[Experience](_.copy(levelUp = false))
    
    def previousLevelExperience: Int = entity.get[Experience].map(_ => experienceForLevel(entity.level)).getOrElse(0)
    
    def nextLevelExperience: Int = entity.get[Experience].map(_.nextLevelExperience).getOrElse(0)
    
    def getPossiblePerks: Seq[StatusEffect] = if(canLevelUp) Seq(
      IncreaseMaxHealthPerk,
      IncreaseDamagePerk,
      ReduceIncomingDamagePerk
    ) else Nil
  }
}
