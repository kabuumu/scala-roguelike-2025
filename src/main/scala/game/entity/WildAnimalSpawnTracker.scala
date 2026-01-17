package game.entity

case class WildAnimalSpawnTracker(ticksSinceLastSpawn: Int = 0)
    extends Component {
  def tick: WildAnimalSpawnTracker =
    copy(ticksSinceLastSpawn = ticksSinceLastSpawn + 1)
  def reset: WildAnimalSpawnTracker = copy(ticksSinceLastSpawn = 0)
}
