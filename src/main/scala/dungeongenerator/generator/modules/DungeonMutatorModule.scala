package dungeongenerator.generator.modules

import dungeongenerator.generator.mutators.DungeonMutator
import dungeongenerator.pathfinder.nodefinders.NodeFinder

case class DungeonMutatorModule(mutator: DungeonMutator, nodeFinder: NodeFinder)
