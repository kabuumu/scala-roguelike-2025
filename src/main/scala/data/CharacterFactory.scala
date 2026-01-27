package data

import game.Point
import game.entity._
import game.Sprite

object CharacterFactory {

  /** Creates a standard character entity with common components.
    *
    * @param id
    *   Unique identifier
    * @param position
    *   Starting position
    * @param sprite
    *   Visual sprite
    * @param entityType
    *   Type of entity (Enemy, Player, Villager, etc.)
    * @param name
    *   Optional name (NameComponent)
    * @param hp
    *   Optional max health (Health component)
    * @param initiative
    *   Optional initiative (Initiative component). Required for movement. If
    *   None, entity cannot move/act in turn system.
    * @param inventory
    *   Optional starting inventory.
    * @param isActive
    *   If true, adds Active() component (for AI processing).
    * @param blocksMovement
    *   If true, adds Hitbox() component.
    * @param extraComponents
    *   Any additional components (e.g. AI behavior, specific stats)
    */
  def create(
      id: String,
      position: Point,
      sprite: Sprite,
      entityType: EntityType,
      name: Option[NameComponent] = None,
      hp: Option[Int] = None,
      initiative: Option[Int] = None,
      inventory: Option[Inventory] = None,
      isActive: Boolean = false,
      blocksMovement: Boolean = true,
      extraComponents: Seq[Component] = Seq.empty
  ): Entity = {
    val baseComponents = scala.collection.mutable.ListBuffer[Component]()

    // Required/Core
    baseComponents += Movement(position)
    baseComponents += EntityTypeComponent(entityType)
    baseComponents += Drawable(sprite)

    if (blocksMovement) {
      baseComponents += Hitbox()
    }

    // Optional
    name.foreach(n => baseComponents += n)
    hp.foreach(h => baseComponents += Health(h, h))
    initiative.foreach(i => baseComponents += Initiative(i))
    inventory.foreach(inv => baseComponents += inv)

    if (isActive) {
      baseComponents += Active()
    }

    Entity(id, (baseComponents.toSeq ++ extraComponents)*)
  }
}
