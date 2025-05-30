package game

import game.entity.Entity

case class DeathDetails(victim: Entity,
                        killerId: Option[String] = None)
