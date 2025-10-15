package game.entity

import data.Items.ItemReference

/**
 * Component for entities that can trade items with the player.
 * @param tradeInventory Map of item references to their buy/sell prices (buyPrice, sellPrice)
 */
case class Trader(tradeInventory: Map[ItemReference, (Int, Int)] = Map.empty) extends Component {
  def buyPrice(item: ItemReference): Option[Int] = tradeInventory.get(item).map(_._1)
  def sellPrice(item: ItemReference): Option[Int] = tradeInventory.get(item).map(_._2)
  
  def canBuyFrom(item: ItemReference, playerCoins: Int): Boolean = {
    buyPrice(item).exists(price => playerCoins >= price)
  }
  
  def canSellTo(item: ItemReference): Boolean = {
    sellPrice(item).isDefined
  }
}

object Trader {
  extension (entity: Entity) {
    def isTrader: Boolean = entity.has[Trader]
    def trader: Option[Trader] = entity.get[Trader]
  }
  
  /**
   * Create a default trader with common items.
   */
  def defaultInventory: Map[ItemReference, (Int, Int)] = Map(
    ItemReference.HealingPotion -> (15, 8),
    ItemReference.FireballScroll -> (25, 12),
    ItemReference.Arrow -> (5, 2),
    ItemReference.Bow -> (40, 20),
    ItemReference.LeatherHelmet -> (20, 10),
    ItemReference.ChainmailArmor -> (30, 15),
    ItemReference.IronHelmet -> (35, 17),
    ItemReference.PlateArmor -> (50, 25),
    ItemReference.LeatherBoots -> (20, 10),
    ItemReference.IronBoots -> (35, 17),
    ItemReference.LeatherGloves -> (20, 10),
    ItemReference.IronGloves -> (35, 17),
    ItemReference.BasicSword -> (25, 12),
    ItemReference.IronSword -> (40, 20)
  )
}
