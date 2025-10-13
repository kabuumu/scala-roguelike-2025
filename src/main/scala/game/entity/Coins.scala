package game.entity

case class Coins(current: Int = 0, totalCollected: Int = 0) extends Component {
  def addCoins(amount: Int): Coins = {
    copy(current = current + amount, totalCollected = totalCollected + amount)
  }
  
  def removeCoins(amount: Int): Coins = {
    copy(current = Math.max(0, current - amount))
  }
}

object Coins {
  extension (entity: Entity) {
    def coins: Int = entity.get[Coins].map(_.current).getOrElse(0)
    def totalCoinsCollected: Int = entity.get[Coins].map(_.totalCollected).getOrElse(0)
    def addCoins(amount: Int): Entity = {
      entity.get[Coins] match {
        case Some(coins) => entity.update[Coins](_.addCoins(amount))
        case None => entity.addComponent(Coins(amount, amount))
      }
    }
    def removeCoins(amount: Int): Entity = {
      entity.update[Coins](_.removeCoins(amount))
    }
  }
}
