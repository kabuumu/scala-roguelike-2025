package game

import game.entity.*

case class DeathDetails(victim: Entity,
                        killerId: Option[String] = None)
