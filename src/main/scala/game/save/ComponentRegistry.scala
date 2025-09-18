package game.save

import upickle.default.*
import ujson.Value
import scala.reflect.ClassTag
import game.entity.*
import game.entity.Health._
import game.{Point, Sprite}

/**
 * Self-contained component registry that handles serialization without polluting game code.
 * Uses local implicit ReadWriters to avoid dependencies outside save package.
 */
object ComponentRegistry {
  import game.save.SavePickle.* // Import local ReadWriters

  // One codec entry per component type you want to persist
  final case class Codec[T <: Component](
    tag: String,
    toJson: T => Value,
    fromJson: Value => Either[String, T]
  )

  // Helper to create simple codecs using uPickle ReadWriters
  private def simpleCodec[T <: Component](tag: String)(implicit rw: ReadWriter[T]): Codec[T] = 
    Codec[T](
      tag = tag,
      toJson = component => writeJs(component),
      fromJson = json => try Right(read[T](json)) catch case e => Left(e.getMessage)
    )

  // Local ReadWriters for components - contained within save package
  implicit val movementRW: ReadWriter[Movement] = readwriter[ujson.Value].bimap[Movement](
    m => ujson.Obj("position" -> writeJs(m.position)),
    json => Movement(read[Point](json.obj("position")))
  )

  implicit val initiativeRW: ReadWriter[Initiative] = readwriter[ujson.Value].bimap[Initiative](
    i => ujson.Obj("maxInitiative" -> ujson.Num(i.maxInitiative), "currentInitiative" -> ujson.Num(i.currentInitiative)),
    json => {
      val obj = json.obj
      Initiative(obj("maxInitiative").num.toInt, obj("currentInitiative").num.toInt)
    }
  )

  implicit val experienceRW: ReadWriter[Experience] = readwriter[ujson.Value].bimap[Experience](
    e => ujson.Obj("currentExperience" -> ujson.Num(e.currentExperience), "levelUp" -> ujson.Bool(e.levelUp)),
    json => {
      val obj = json.obj
      Experience(obj("currentExperience").num.toInt, obj("levelUp").bool)
    }
  )

  implicit val entityTypeComponentRW: ReadWriter[EntityTypeComponent] = readwriter[ujson.Value].bimap[EntityTypeComponent](
    etc => ujson.Obj("entityType" -> writeJs(etc.entityType)),
    json => EntityTypeComponent(read[EntityType](json.obj("entityType")))
  )

  implicit val drawableRW: ReadWriter[Drawable] = readwriter[ujson.Value].bimap[Drawable](
    d => ujson.Obj("sprites" -> ujson.Arr(d.sprites.toSeq.map { case (point, sprite) =>
      ujson.Obj("point" -> writeJs(point), "sprite" -> writeJs(sprite))
    }*)),
    json => {
      val sprites = json.obj("sprites").arr.map { item =>
        val obj = item.obj
        (read[Point](obj("point")), read[Sprite](obj("sprite")))
      }.toSet
      Drawable(sprites)
    }
  )

  implicit val hitboxRW: ReadWriter[Hitbox] = readwriter[ujson.Value].bimap[Hitbox](
    h => ujson.Obj("points" -> ujson.Arr(h.points.toSeq.map(writeJs(_))*)),
    json => {
      val points = json.obj("points").arr.map(read[Point](_)).toSet
      Hitbox(points)
    }
  )

  implicit val sightMemoryRW: ReadWriter[SightMemory] = readwriter[ujson.Value].bimap[SightMemory](
    sm => ujson.Obj("seenPoints" -> ujson.Arr(sm.seenPoints.toSeq.map(writeJs(_))*)),
    json => {
      val points = json.obj("seenPoints").arr.map(read[Point](_)).toSet
      SightMemory(points)
    }
  )

  implicit val canPickUpRW: ReadWriter[CanPickUp] = readwriter[ujson.Value].bimap[CanPickUp](
    _ => ujson.Obj(),
    _ => CanPickUp()
  )

  implicit val nameComponentRW: ReadWriter[NameComponent] = readwriter[ujson.Value].bimap[NameComponent](
    nc => ujson.Obj("name" -> ujson.Str(nc.name), "description" -> ujson.Str(nc.description)),
    json => {
      val obj = json.obj
      NameComponent(obj("name").str, obj("description").str)
    }
  )

  implicit val inventoryRW: ReadWriter[Inventory] = readwriter[ujson.Value].bimap[Inventory](
    inv => ujson.Obj(
      "itemEntityIds" -> ujson.Arr(inv.itemEntityIds.map(ujson.Str(_))*),
      "primaryWeaponId" -> inv.primaryWeaponId.map(ujson.Str(_)).getOrElse(ujson.Null),
      "secondaryWeaponId" -> inv.secondaryWeaponId.map(ujson.Str(_)).getOrElse(ujson.Null)
    ),
    json => {
      val obj = json.obj
      val itemEntityIds = obj("itemEntityIds").arr.map(_.str).toSeq
      val primaryWeaponId = if (obj("primaryWeaponId") == ujson.Null) None else Some(obj("primaryWeaponId").str)
      val secondaryWeaponId = if (obj("secondaryWeaponId") == ujson.Null) None else Some(obj("secondaryWeaponId").str)
      Inventory(itemEntityIds, primaryWeaponId, secondaryWeaponId)
    }
  )

  implicit val equipmentSlotRW: ReadWriter[EquipmentSlot] = readwriter[String].bimap[EquipmentSlot](
    _.toString,
    str => str match {
      case "Helmet" => EquipmentSlot.Helmet
      case "Armor" => EquipmentSlot.Armor
      case _ => EquipmentSlot.Helmet
    }
  )

  implicit val equippableRW: ReadWriter[Equippable] = readwriter[ujson.Value].bimap[Equippable](
    eq => ujson.Obj(
      "slot" -> writeJs(eq.slot),
      "damageReduction" -> ujson.Num(eq.damageReduction),
      "itemName" -> ujson.Str(eq.itemName)
    ),
    json => {
      val obj = json.obj
      Equippable(
        read[EquipmentSlot](obj("slot")),
        obj("damageReduction").num.toInt,
        obj("itemName").str
      )
    }
  )

  implicit val equipmentRW: ReadWriter[Equipment] = readwriter[ujson.Value].bimap[Equipment](
    eq => ujson.Obj(
      "helmet" -> eq.helmet.map(writeJs(_)).getOrElse(ujson.Null),
      "armor" -> eq.armor.map(writeJs(_)).getOrElse(ujson.Null)
    ),
    json => {
      val obj = json.obj
      val helmet = if (obj("helmet") == ujson.Null) None else Some(read[Equippable](obj("helmet")))
      val armor = if (obj("armor") == ujson.Null) None else Some(read[Equippable](obj("armor")))
      Equipment(helmet, armor)
    }
  )

  // Registry of all component codecs
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
    classOf[Inventory] -> simpleCodec[Inventory]("Inventory"),
    classOf[Equipment] -> simpleCodec[Equipment]("Equipment"),
    classOf[Equippable] -> simpleCodec[Equippable]("Equippable")
    
    // TODO: Add more complex components (WeaponItem, UsableItem, etc.) as needed
    // For now, focusing on components that work without complex nested dependencies
  )

  // Special entity-aware codec for Health - needs entity context for computed values
  private val healthCodec = Codec[Health](
    tag = "Health",
    toJson = health => ujson.Obj(), // Not used - see toSavedWithEntity
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
   * Standard method for most components.
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
   * Special method for components that need entity context (like Health).
   */
  def toSavedWithEntity(entity: Entity, component: Component): Option[SavedComponent] = {
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