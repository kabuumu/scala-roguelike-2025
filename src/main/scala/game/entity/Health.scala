package game.entity

import game.Constants.DEFAULT_EXP
import game.DeathDetails
import game.event.AddExperienceEvent

case class Health(current: Int, max: Int) extends Component {
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
  
  def setToFull(): Health = Health(max, max)
}

object Health {
  def apply(max: Int): Health = new Health(max, max)

  extension (entity: Entity) {
    def isAlive: Boolean = entity.exists[Health](_.isAlive)
    def isDead: Boolean = entity.exists[Health](_.isDead)

    def hasFullHealth: Boolean = entity.exists[Health](_.isFull)

    def damage(amount: Int, attackerId: String): Entity =
      entity.update[Health](_ - amount) match {
        case entity if entity.isDead =>
          entity.addComponent(MarkedForDeath(DeathDetails(entity, Some(attackerId))))
        case entity =>
          entity
      }
    
    def heal(amount: Int): Entity = entity.update[Health](_ + amount)
    
    def currentHealth: Int = entity.get[Health].map(_.current).getOrElse(0)
    def maxHealth: Int = entity.get[Health].map(_.max).getOrElse(0)
  }
}
