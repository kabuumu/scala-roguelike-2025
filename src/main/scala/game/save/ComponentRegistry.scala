package game.save

import upickle.default.*
import ujson.Value
import scala.reflect.ClassTag
import game.entity.*
import game.entity.Health._

/**
 * Minimal runtime registry that tells the save system how to encode/decode each component type.
 * - No need to seal Component.
 * - Add one line per component you want persisted.
 * - You can intentionally omit transient components.
 */
object ComponentRegistry {

  // One codec entry per component type you want to persist
  final case class Codec[T <: Component](
    tag: String,
    toJson: T => Value,
    fromJson: Value => Either[String, T]
  )

  // Special codec for Health that needs entity context for computed values
  final case class EntityAwareCodec[T <: Component](
    tag: String,
    toJson: (Entity, T) => Value,
    fromJson: Value => Either[String, T]
  )

  // Simple helper to create codecs for basic components
  private def simpleCodec[T <: Component : ReadWriter](tag: String): Codec[T] = 
    Codec[T](
      tag = tag,
      toJson = component => writeJs(component),
      fromJson = json => try Right(read[T](json)) catch case e => Left(e.getMessage)
    )

  // Registry of all component codecs - only register simple ones for now
  private val codecs: Map[Class[?], Codec[? <: Component]] = Map(
    classOf[Movement] -> simpleCodec[Movement]("Movement"),
    classOf[Initiative] -> simpleCodec[Initiative]("Initiative"),
    classOf[Experience] -> simpleCodec[Experience]("Experience"),
    classOf[EntityTypeComponent] -> simpleCodec[EntityTypeComponent]("EntityTypeComponent"),
    classOf[Drawable] -> simpleCodec[Drawable]("Drawable"),
    classOf[Hitbox] -> simpleCodec[Hitbox]("Hitbox"),
    classOf[SightMemory] -> simpleCodec[SightMemory]("SightMemory"),
    classOf[CanPickUp] -> simpleCodec[CanPickUp]("CanPickUp"),
    classOf[NameComponent] -> simpleCodec[NameComponent]("NameComponent"),
    
    // For now skip complex components that need special handling
    // classOf[Equipment] -> ...,
    // classOf[Equippable] -> ...,
    // classOf[Inventory] -> ...,
    // classOf[WeaponItem] -> ...,
    // classOf[UsableItem] -> ...,
    // classOf[DeathEvents] -> ...,
    // classOf[Ammo] -> ...,
    // classOf[KeyItem] -> ...,
    // classOf[EventMemory] -> ...,
  )

  // Special entity-aware codec for Health
  private val healthCodec = EntityAwareCodec[Health](
    tag = "Health",
    toJson = (entity, health) => ujson.Obj(
      "current" -> ujson.Num(entity.currentHealth),
      "max" -> ujson.Num(entity.maxHealth)
    ),
    fromJson = json => {
      try {
        val obj = json.obj
        val current = obj("current").num.toInt
        val max = obj("max").num.toInt
        Right(new Health(current, max))
      } catch {
        case e: Exception => Left(s"Failed to deserialize Health: ${e.getMessage}")
      }
    }
  )

  /**
   * Convert a component to a SavedComponent.
   * For Health, we need the entity context to compute current/max values.
   */
  def toSaved(component: Component): Option[SavedComponent] = {
    val componentClass = component.getClass
    codecs.get(componentClass) match {
      case Some(codec) =>
        val typedCodec = codec.asInstanceOf[Codec[Component]]
        Some(SavedComponent(typedCodec.tag, typedCodec.toJson(component)))
      case None =>
        None // Component not registered for persistence
    }
  }

  /**
   * Special method for Health which needs entity context
   */
  def toSavedWithEntity(entity: Entity, component: Component): Option[SavedComponent] = {
    component match {
      case h: Health =>
        Some(SavedComponent(healthCodec.tag, healthCodec.toJson(entity, h)))
      case _ =>
        toSaved(component)
    }
  }

  /**
   * Convert a SavedComponent back to a live Component.
   */
  def fromSaved(savedComponent: SavedComponent): Either[String, Component] = {
    if (savedComponent.tag == healthCodec.tag) {
      healthCodec.fromJson(savedComponent.data)
    } else {
      codecs.values.find(_.tag == savedComponent.tag) match {
        case Some(codec) =>
          codec.fromJson(savedComponent.data)
        case None =>
          Left(s"Unknown component tag: ${savedComponent.tag}")
      }
    }
  }
}