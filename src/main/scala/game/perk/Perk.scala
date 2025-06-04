package game.perk

import game.entity.{Component, Entity}
import game.status.StatusEffect

trait Perk extends StatusEffect {
  def name: String
  def description: String
}
