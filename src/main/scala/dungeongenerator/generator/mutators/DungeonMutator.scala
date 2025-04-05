package dungeongenerator.generator.mutators

import dungeongenerator.generator.{Dungeon, DungeonGeneratorConfig}

trait DungeonMutator {
  def getPossibleMutations(dungeon: Dungeon, config: DungeonGeneratorConfig): Iterable[Dungeon]
}
