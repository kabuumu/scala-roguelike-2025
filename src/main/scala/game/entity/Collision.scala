package game.entity

import game.entity.EntityType.*
import game.entity.Health.*
import game.entity.Hitbox.*
import game.event.*
import game.{DeathDetails, GameState}
import game.entity.Movement.*

import scala.language.postfixOps

case class Collision(damage: Int, persistent: Boolean, target: EntityType, creatorId: String) extends Component


