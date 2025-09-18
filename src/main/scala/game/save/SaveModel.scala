package game.save

import upickle.default._
import ujson.Value
import game.*
import game.entity.*
import game.entity.Ammo.AmmoType
import game.entity.EquipmentSlot
import data.DeathEvents.DeathEventReference
import data.Entities.EntityReference
import data.Projectiles.ProjectileReference
import map.*

/**
 * DTOs for persisted game state using uPickle for serialization
 */

// We don't need PersistedGameState since we serialize manually
// case class PersistedGameState(
//   playerEntityId: String,
//   entities: Vector[PersistedEntity],
//   messages: Vector[String],
//   dungeon: Dungeon
// )

case class PersistedEntity(
  id: String,
  components: Vector[SavedComponent]
)

case class SavedComponent(
  tag: String,
  data: ujson.Value
)

/**
 * uPickle ReadWriter givens for all the types needed for save/load
 */
object SavePickle {
  // Basic types
  implicit val pointRW: ReadWriter[Point] = macroRW
  implicit val spriteRW: ReadWriter[Sprite] = macroRW
  
  // Manual ReadWriter for Direction enum
  implicit val directionRW: ReadWriter[Direction] = readwriter[String].bimap[Direction](
    direction => direction.toString,
    str => Direction.valueOf(str)
  )
  
  // Map types - simplify to avoid overloaded method issues
  implicit val roomConnectionRW: ReadWriter[RoomConnection] = macroRW
  // Skip Dungeon for now, will serialize it manually if needed
  
  // Manual ReadWriter for EntityType with proper enum parsing
  implicit val entityTypeRW: ReadWriter[EntityType] = readwriter[ujson.Value].bimap[EntityType](
    entityType => ujson.Str(entityType.toString),
    json => {
      val str = json.str
      str match {
        case "Player" => EntityType.Player
        case "Enemy" => EntityType.Enemy
        case "Wall" => EntityType.Wall
        case "Floor" => EntityType.Floor
        case "Projectile" => EntityType.Projectile
        case s if s.startsWith("LockedDoor(") =>
          val colorStr = s.substring(11, s.length - 1) // Extract "Red" from "LockedDoor(Red)"
          val keyColour = colorStr match {
            case "Red" => KeyColour.Red
            case "Blue" => KeyColour.Blue
            case "Yellow" => KeyColour.Yellow
          }
          EntityType.LockedDoor(keyColour)
        case s if s.startsWith("Key(") =>
          val colorStr = s.substring(4, s.length - 1) // Extract "Red" from "Key(Red)"
          val keyColour = colorStr match {
            case "Red" => KeyColour.Red
            case "Blue" => KeyColour.Blue
            case "Yellow" => KeyColour.Yellow
          }
          EntityType.Key(keyColour)
        case _ => EntityType.Player // fallback
      }
    }
  )
  
  // Manual ReadWriter for KeyColour enum
  implicit val keyColourRW: ReadWriter[KeyColour] = readwriter[String].bimap[KeyColour](
    colour => colour.toString,
    str => str match {
      case "Red" => KeyColour.Red
      case "Blue" => KeyColour.Blue
      case "Yellow" => KeyColour.Yellow
      case _ => KeyColour.Red // fallback
    }
  )
  
  implicit val itemDescriptorRW: ReadWriter[ItemDescriptor] = readwriter[ujson.Value].bimap[ItemDescriptor](
    item => item match {
      case ItemDescriptor.KeyDescriptor(keyColour) => 
        ujson.Obj("type" -> ujson.Str("KeyDescriptor"), "keyColour" -> ujson.Str(keyColour.toString))
      case ItemDescriptor.PotionDescriptor => ujson.Str("PotionDescriptor")
      case ItemDescriptor.ScrollDescriptor => ujson.Str("ScrollDescriptor")
      case ItemDescriptor.ArrowDescriptor => ujson.Str("ArrowDescriptor")
      case ItemDescriptor.LeatherHelmetDescriptor => ujson.Str("LeatherHelmetDescriptor")
      case ItemDescriptor.ChainmailArmorDescriptor => ujson.Str("ChainmailArmorDescriptor")
      case ItemDescriptor.IronHelmetDescriptor => ujson.Str("IronHelmetDescriptor")
      case ItemDescriptor.PlateArmorDescriptor => ujson.Str("PlateArmorDescriptor")
    },
    json => json match {
      case ujson.Obj(map) if map.get("type").contains(ujson.Str("KeyDescriptor")) =>
        val keyColour = map("keyColour").str match {
          case "Red" => KeyColour.Red
          case "Blue" => KeyColour.Blue
          case "Yellow" => KeyColour.Yellow
        }
        ItemDescriptor.KeyDescriptor(keyColour)
      case ujson.Str(str) => str match {
        case "PotionDescriptor" => ItemDescriptor.PotionDescriptor
        case "ScrollDescriptor" => ItemDescriptor.ScrollDescriptor
        case "ArrowDescriptor" => ItemDescriptor.ArrowDescriptor
        case "LeatherHelmetDescriptor" => ItemDescriptor.LeatherHelmetDescriptor
        case "ChainmailArmorDescriptor" => ItemDescriptor.ChainmailArmorDescriptor
        case "IronHelmetDescriptor" => ItemDescriptor.IronHelmetDescriptor
        case "PlateArmorDescriptor" => ItemDescriptor.PlateArmorDescriptor
        case _ => ItemDescriptor.PotionDescriptor // fallback
      }
    }
  )
  
  // Component enums and data
  implicit val ammoTypeRW: ReadWriter[AmmoType] = readwriter[String].bimap[AmmoType](
    ammoType => ammoType.toString,
    str => str match {
      case "Arrow" => AmmoType.Arrow
      case _ => AmmoType.Arrow // fallback
    }
  )
  
  implicit val targetingRW: ReadWriter[Targeting] = readwriter[ujson.Value].bimap[Targeting](
    targeting => targeting match {
      case Targeting.Self => ujson.Str("Self")
      case Targeting.EnemyActor(range) => ujson.Obj("type" -> ujson.Str("EnemyActor"), "range" -> ujson.Num(range))
      case Targeting.TileInRange(range) => ujson.Obj("type" -> ujson.Str("TileInRange"), "range" -> ujson.Num(range))
    },
    json => json match {
      case ujson.Str("Self") => Targeting.Self
      case ujson.Obj(map) =>
        map("type").str match {
          case "EnemyActor" => Targeting.EnemyActor(map("range").num.toInt)
          case "TileInRange" => Targeting.TileInRange(map("range").num.toInt)
        }
    }
  )
  
  implicit val chargeTypeRW: ReadWriter[ChargeType] = readwriter[ujson.Value].bimap[ChargeType](
    chargeType => chargeType match {
      case ChargeType.SingleUse => ujson.Str("SingleUse")
      case ChargeType.Ammo(ammoType) => ujson.Obj("type" -> ujson.Str("Ammo"), "ammoType" -> ujson.Str(ammoType.toString))
    },
    json => json match {
      case ujson.Str("SingleUse") => ChargeType.SingleUse
      case ujson.Obj(map) =>
        val ammoType = map("ammoType").str match {
          case "Arrow" => AmmoType.Arrow
          case _ => AmmoType.Arrow
        }
        ChargeType.Ammo(ammoType)
    }
  )
  
  implicit val weaponTypeRW: ReadWriter[WeaponType] = readwriter[ujson.Value].bimap[WeaponType](
    weaponType => weaponType match {
      case Melee => ujson.Str("Melee")
      case Ranged(range) => ujson.Obj("type" -> ujson.Str("Ranged"), "range" -> ujson.Num(range))
    },
    json => json match {
      case ujson.Str("Melee") => Melee
      case ujson.Obj(map) => Ranged(map("range").num.toInt)
    }
  )
  
  implicit val gameEffectRW: ReadWriter[game.entity.GameEffect] = readwriter[ujson.Value].bimap[game.entity.GameEffect](
    effect => effect match {
      case game.entity.GameEffect.Heal(amount) => ujson.Obj("type" -> ujson.Str("Heal"), "amount" -> ujson.Num(amount))
      case game.entity.GameEffect.CreateProjectile(ref) => ujson.Obj("type" -> ujson.Str("CreateProjectile"), "ref" -> ujson.Str(ref.toString))
    },
    json => {
      val map = json.obj
      map("type").str match {
        case "Heal" => game.entity.GameEffect.Heal(map("amount").num.toInt)
        case "CreateProjectile" => 
          // This is a placeholder - actual ProjectileReference parsing would be more complex
          game.entity.GameEffect.CreateProjectile(data.Projectiles.ProjectileReference.Fireball)
      }
    }
  )
  
  implicit val equipmentSlotRW: ReadWriter[EquipmentSlot] = readwriter[String].bimap[EquipmentSlot](
    slot => slot.toString,
    str => str match {
      case "Helmet" => EquipmentSlot.Helmet
      case "Armor" => EquipmentSlot.Armor
      case _ => EquipmentSlot.Helmet // fallback
    }
  )
  
  // For now, use simplified serializers for complex reference types
  implicit val deathEventReferenceRW: ReadWriter[DeathEventReference] = readwriter[String].bimap[DeathEventReference](
    ref => ref.toString,
    str => data.DeathEvents.DeathEventReference.GiveExperience(10) // Placeholder
  )
  
  // Memory events - simplified 
  implicit val memoryEventRW: ReadWriter[MemoryEvent] = readwriter[ujson.Value].bimap[MemoryEvent](
    event => ujson.Obj("type" -> ujson.Str(event.getClass.getSimpleName), "timestamp" -> ujson.Num(event.timestamp)),
    json => MemoryEvent.ItemUsed(json.obj("timestamp").num.toLong, "unknown") // Placeholder
  )
  
  // Persisted component case classes (for simple components that don't need entity context)
  case class PersistedMovement(position: Point)
  case class PersistedInitiative(maxInitiative: Int, currentInitiative: Int)
  case class PersistedExperience(currentExperience: Int, levelUp: Boolean)
  case class PersistedEntityTypeComponent(entityType: EntityType)
  case class PersistedDrawable(sprites: Set[(Point, Sprite)])
  case class PersistedHitbox() // Marker - will use default hitbox
  case class PersistedSightMemory(seenPoints: Set[Point])
  case class PersistedEquipment(helmet: Option[PersistedEquippable], armor: Option[PersistedEquippable])
  case class PersistedEquippable(slot: EquipmentSlot, damageReduction: Int, itemName: String)
  case class PersistedInventory(itemEntityIds: Seq[String], primaryWeaponId: Option[String], secondaryWeaponId: Option[String])
  case class PersistedWeaponItem(damage: Int, weaponType: WeaponType)
  case class PersistedCanPickUp() // Marker
  case class PersistedNameComponent(name: String, description: String)
  case class PersistedUsableItem(targeting: Targeting, chargeType: ChargeType, effect: game.entity.GameEffect)
  case class PersistedDeathEvents(deathEvents: Seq[DeathEventReference])
  case class PersistedAmmo(ammoType: AmmoType)
  case class PersistedKeyItem(keyColour: KeyColour)
  case class PersistedEventMemory(events: Seq[MemoryEvent])
  
  // Health is special - stored with computed current/max values
  case class PersistedHealth(current: Int, max: Int)
  
  // ReadWriters for persisted components
  implicit val persistedMovementRW: ReadWriter[PersistedMovement] = macroRW
  implicit val persistedHealthRW: ReadWriter[PersistedHealth] = macroRW
  implicit val persistedInitiativeRW: ReadWriter[PersistedInitiative] = macroRW
  implicit val persistedExperienceRW: ReadWriter[PersistedExperience] = macroRW
  implicit val persistedEntityTypeComponentRW: ReadWriter[PersistedEntityTypeComponent] = macroRW
  implicit val persistedDrawableRW: ReadWriter[PersistedDrawable] = macroRW
  implicit val persistedHitboxRW: ReadWriter[PersistedHitbox] = macroRW
  implicit val persistedSightMemoryRW: ReadWriter[PersistedSightMemory] = macroRW
  implicit val persistedEquipmentRW: ReadWriter[PersistedEquipment] = macroRW
  implicit val persistedEquippableRW: ReadWriter[PersistedEquippable] = macroRW
  implicit val persistedInventoryRW: ReadWriter[PersistedInventory] = macroRW
  implicit val persistedWeaponItemRW: ReadWriter[PersistedWeaponItem] = macroRW
  implicit val persistedCanPickUpRW: ReadWriter[PersistedCanPickUp] = macroRW
  implicit val persistedNameComponentRW: ReadWriter[PersistedNameComponent] = macroRW
  implicit val persistedUsableItemRW: ReadWriter[PersistedUsableItem] = macroRW
  implicit val persistedDeathEventsRW: ReadWriter[PersistedDeathEvents] = macroRW
  implicit val persistedAmmoRW: ReadWriter[PersistedAmmo] = macroRW
  implicit val persistedKeyItemRW: ReadWriter[PersistedKeyItem] = macroRW
  implicit val persistedEventMemoryRW: ReadWriter[PersistedEventMemory] = macroRW
  
  // DTOs
  implicit val savedComponentRW: ReadWriter[SavedComponent] = macroRW
  implicit val persistedEntityRW: ReadWriter[PersistedEntity] = macroRW
}

/**
 * JSON API for serializing/deserializing game states using uPickle
 */
object SaveGameJson {
  import SavePickle._
  
  def serialize(gameState: GameState): String = {
    val persistedEntities = gameState.entities.map(entity => 
      ComponentRegistry.toPersistedEntity(entity)
    ).toVector
    
    // Serialize dungeon manually to avoid macro issues
    val dungeonJson = ujson.Obj(
      "roomGrid" -> ujson.Arr(gameState.dungeon.roomGrid.toSeq.map(p => ujson.Arr(p.x, p.y))*),
      "seed" -> ujson.Num(gameState.dungeon.seed)
      // Add other essential dungeon fields as needed
    )
    
    val gameStateJson = ujson.Obj(
      "playerEntityId" -> ujson.Str(gameState.playerEntityId),
      "entities" -> writeJs(persistedEntities),
      "messages" -> ujson.Arr(gameState.messages.map(ujson.Str.apply)*),
      "dungeon" -> dungeonJson
    )
    
    ujson.write(gameStateJson)
  }
  
  def deserialize(jsonString: String): GameState = {
    val json = ujson.read(jsonString)
    val gameStateObj = json.obj
    
    val persistedEntities = read[Vector[PersistedEntity]](gameStateObj("entities"))
    val entities = persistedEntities.map(persistedEntity =>
      ComponentRegistry.fromPersistedEntity(persistedEntity)
    )
    
    // For now, create a simple dungeon - full dungeon restoration would be more complex
    val dungeonObj = gameStateObj("dungeon").obj
    val roomGrid = dungeonObj("roomGrid").arr.map(arr => Point(arr.arr(0).num.toInt, arr.arr(1).num.toInt)).toSet
    val seed = dungeonObj("seed").num.toLong
    val simpleDungeon = Dungeon(roomGrid = roomGrid, seed = seed)
    
    GameState(
      playerEntityId = gameStateObj("playerEntityId").str,
      entities = entities,
      messages = gameStateObj("messages").arr.map(_.str).toSeq,
      dungeon = simpleDungeon
    )
  }
}