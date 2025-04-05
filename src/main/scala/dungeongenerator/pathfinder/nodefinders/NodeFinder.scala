package dungeongenerator.pathfinder.nodefinders

import dungeongenerator.pathfinder.Node

trait NodeFinder {
  def getPossibleNodes(currentNode: Node): Iterable[Node]
}
