package dungeongenerator.pathfinder

import dungeongenerator.pathfinder.nodefinders.NodeFinder

import scala.annotation.tailrec

object PathFinder {
  type Path = Seq[Node]
  type TargetNodePredicate = Node => Boolean
  type PathFailurePredicate = Path => Boolean

  def findPath(startingNode: Node,
               targetNodePredicate: TargetNodePredicate,
               pathFailureTriggers: Set[PathFailurePredicate],
               nodeFinders: Iterable[NodeFinder]): Path = {
    findPath(Set(Seq(startingNode)), Set.empty, targetNodePredicate, pathFailureTriggers, nodeFinders)
  }

  @tailrec
  private def findPath(openPaths: Set[Path],
                       successfulPaths: Set[Path],
                       targetNodePredicate: TargetNodePredicate,
                       pathFailureTriggers: Set[PathFailurePredicate],
                       nodeFinders: Iterable[NodeFinder],
                       iteration: Int = 0): Path = {
    if (openPaths.exists(path => pathFailureTriggers.exists(_.apply(path)))) Nil
    else if (openPaths.isEmpty || iteration > 20000) successfulPaths.minByOption(_.size).getOrElse(Nil)
    else {
      val allPaths = openPaths ++ successfulPaths
      val newSuccessfulPaths = openPaths.filter(path => targetNodePredicate(path.last))

      val updatedPaths = openPaths.flatMap { path =>
        val possibleNodes = nodeFinders.flatMap(_.getPossibleNodes(path.last))
        possibleNodes
          .filterNot(node => allPaths.exists(_.contains(node)))
          .map(path :+ _)
      }

      findPath(
        updatedPaths,
        successfulPaths ++ newSuccessfulPaths,
        targetNodePredicate,
        pathFailureTriggers,
        nodeFinders,
        iteration + 1
      )
    }
  }
}