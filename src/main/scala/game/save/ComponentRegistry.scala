package game.save

import upickle.default.*
import ujson.Value
import scala.reflect.ClassTag
import game.entity.*
import game.entity.Health._
import game.{Point, Sprite, Direction}

/** Self-contained component registry using automatic derivation where possible.
  * Uses uPickle macroRW for all components that support it.
  */
object ComponentRegistry {

  // Type alias for clarity
  type Codec[T <: Component] = ReadWriter[T]

  // One codec entry per component type you want to persist
  final case class CodecEntry[T <: Component](
      tag: String,
      toJson: T => Value,
      fromJson: Value => Either[String, T]
  )

  // Helper to create simple codecs using automatic derivation
  private def simpleCodec[T <: Component](
      tag: String
  )(using rw: ReadWriter[T]): CodecEntry[T] =
    CodecEntry[T](
      tag = tag,
      toJson = component => writeJs(component),
      fromJson = json =>
        try Right(read[T](json))
        catch case e => Left(e.getMessage)
    )

  // Automatic derivation for components - contained within save package
  // First provide ReadWriters for dependencies that can't be auto-derived
  implicit val pointRW: ReadWriter[Point] = macroRW
  implicit val spriteRW: ReadWriter[Sprite] = macroRW
  implicit val directionRW: ReadWriter[Direction] =
    readwriter[String].bimap[Direction](
      _.toString,
      str => Direction.valueOf(str)
    )
  implicit val entityTypeRW: ReadWriter[EntityType] =
    readwriter[String].bimap[EntityType](
      _.toString,
      str =>
        str match {
          case "Player"                         => EntityType.Player
          case "Enemy"                          => EntityType.Enemy
          case "Wall"                           => EntityType.Wall
          case "Floor"                          => EntityType.Floor
          case "Projectile"                     => EntityType.Projectile
          case s if s.startsWith("LockedDoor(") =>
            val colorStr = s.substring(11, s.length - 1)
            val keyColour = colorStr match {
              case "Red"    => KeyColour.Red
              case "Blue"   => KeyColour.Blue
              case "Yellow" => KeyColour.Yellow
              case _        => KeyColour.Red
            }
            EntityType.LockedDoor(keyColour)
          case s if s.startsWith("Key(") =>
            val colorStr = s.substring(4, s.length - 1)
            val keyColour = colorStr match {
              case "Red"    => KeyColour.Red
              case "Blue"   => KeyColour.Blue
              case "Yellow" => KeyColour.Yellow
              case _        => KeyColour.Red
            }
            EntityType.Key(keyColour)
          case _ => EntityType.Player
        }
    )
  implicit val keyColourRW: ReadWriter[KeyColour] =
    readwriter[String].bimap[KeyColour](
      _.toString,
      str =>
        str match {
          case "Red"    => KeyColour.Red
          case "Blue"   => KeyColour.Blue
          case "Yellow" => KeyColour.Yellow
          case _        => KeyColour.Red
        }
    )
  implicit val equipmentSlotRW: ReadWriter[EquipmentSlot] =
    readwriter[String].bimap[EquipmentSlot](
      _.toString,
      str =>
        str match {
          case "Helmet" => EquipmentSlot.Helmet
          case "Armor"  => EquipmentSlot.Armor
          case _        => EquipmentSlot.Helmet
        }
    )
  implicit val enemyReferenceRW: ReadWriter[data.Enemies.EnemyReference] =
    readwriter[String].bimap[data.Enemies.EnemyReference](
      _.toString,
      str =>
        str match {
          case "Rat"      => data.Enemies.EnemyReference.Rat
          case "Snake"    => data.Enemies.EnemyReference.Snake
          case "Slime"    => data.Enemies.EnemyReference.Slime
          case "Slimelet" => data.Enemies.EnemyReference.Slimelet
          case "Boss"     => data.Enemies.EnemyReference.Boss
          case _          => data.Enemies.EnemyReference.Rat
        }
    )

  // Now auto-derive the components that have all dependencies satisfied
  implicit val movementRW: ReadWriter[Movement] = macroRW
  implicit val initiativeRW: ReadWriter[Initiative] = macroRW
  implicit val experienceRW: ReadWriter[Experience] = macroRW
  implicit val entityTypeComponentRW: ReadWriter[EntityTypeComponent] = macroRW
  implicit val enemyTypeComponentRW: ReadWriter[EnemyTypeComponent] = macroRW
  implicit val drawableRW: ReadWriter[Drawable] = macroRW
  implicit val hitboxRW: ReadWriter[Hitbox] = macroRW
  implicit val sightMemoryRW: ReadWriter[SightMemory] = macroRW
  implicit val canPickUpRW: ReadWriter[CanPickUp] = macroRW
  implicit val nameComponentRW: ReadWriter[NameComponent] = macroRW
  implicit val inventoryRW: ReadWriter[Inventory] = macroRW
  implicit val equippableRW: ReadWriter[Equippable] = macroRW
  implicit val equippedItemRW: ReadWriter[EquippedItem] = macroRW
  implicit val equipmentRW: ReadWriter[Equipment] = macroRW

  // Registry of all component codecs using automatic derivation
  private val codecs: Map[Class[?], CodecEntry[? <: Component]] = Map(
    classOf[Movement] -> simpleCodec[Movement]("Movement")(using movementRW),
    classOf[Initiative] -> simpleCodec[Initiative]("Initiative")(using
      initiativeRW
    ),
    classOf[Experience] -> simpleCodec[Experience]("Experience")(using
      experienceRW
    ),
    classOf[EntityTypeComponent] -> simpleCodec[EntityTypeComponent](
      "EntityTypeComponent"
    )(using entityTypeComponentRW),
    classOf[EnemyTypeComponent] -> simpleCodec[EnemyTypeComponent](
      "EnemyTypeComponent"
    )(using enemyTypeComponentRW),
    classOf[Drawable] -> simpleCodec[Drawable]("Drawable")(using drawableRW),
    classOf[Hitbox] -> simpleCodec[Hitbox]("Hitbox")(using hitboxRW),
    classOf[SightMemory] -> simpleCodec[SightMemory]("SightMemory")(using
      sightMemoryRW
    ),
    classOf[CanPickUp] -> simpleCodec[CanPickUp]("CanPickUp")(using
      canPickUpRW
    ),
    classOf[NameComponent] -> simpleCodec[NameComponent]("NameComponent")(using
      nameComponentRW
    ),
    classOf[Inventory] -> simpleCodec[Inventory]("Inventory")(using
      inventoryRW
    ),
    classOf[Equipment] -> simpleCodec[Equipment]("Equipment")(using
      equipmentRW
    ),
    classOf[Equippable] -> simpleCodec[Equippable]("Equippable")(using
      equippableRW
    )

    // Adding new components is now even simpler:
    // 1. Add implicit val newComponentRW: ReadWriter[NewComponent] = macroRW
    // 2. Add classOf[NewComponent] -> simpleCodec[NewComponent]("NewComponent")(using newComponentRW) to this map
  )

  // Special entity-aware codec for Health - needs entity context for computed values
  private val healthCodec = CodecEntry[Health](
    tag = "Health",
    toJson = health => ujson.Obj(), // Not used - see toSavedWithEntity
    fromJson = json => {
      try {
        val obj = json.obj
        val current = obj("current").num.toInt
        val max = obj("max").num.toInt
        Right(new Health(current, max))
      } catch {
        case e: Exception =>
          Left(s"Failed to deserialize Health: ${e.getMessage}")
      }
    }
  )

  /** Convert a component to a SavedComponent. Standard method for most
    * components using automatic derivation.
    */
  def toSaved(component: Component): Option[SavedComponent] = {
    val componentClass = component.getClass
    codecs.get(componentClass) match {
      case Some(codec) =>
        val typedCodec = codec.asInstanceOf[CodecEntry[Component]]
        Some(SavedComponent(typedCodec.tag, typedCodec.toJson(component)))
      case None =>
        None // Component not registered for persistence
    }
  }

  /** Special method for components that need entity context (like Health).
    */
  def toSavedWithEntity(
      entity: Entity,
      component: Component
  ): Option[SavedComponent] = {
    component match {
      case h: Health =>
        // Use entity extension methods to compute current/max including status effects
        val healthData = ujson.Obj(
          "current" -> ujson.Num(entity.currentHealth),
          "max" -> ujson.Num(entity.maxHealth)
        )
        Some(SavedComponent(healthCodec.tag, healthData))
      case _ =>
        toSaved(component)
    }
  }

  /** Convert a SavedComponent back to a live Component.
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
