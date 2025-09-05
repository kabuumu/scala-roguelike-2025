package game.save

import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.scalajs.js.JSConverters.JSRichOption
import scala.util.{Try, Success, Failure}
import game.*
import game.entity.*
import map.*

/**
 * Simple JSON serialization using native JS JSON for browser compatibility.
 * Uses a simpler approach than Circe to avoid complex macro dependencies.
 */
object SaveGameSerializer {

  /**
   * Serializes a GameState to JSON string for storage
   */
  def serialize(gameState: GameState): String = {
    val data = js.Dynamic.literal(
      "playerEntityId" -> gameState.playerEntityId,
      "entities" -> serializeEntities(gameState.entities),
      "messages" -> js.Array(gameState.messages*),
      "dungeon" -> serializeDungeon(gameState.dungeon)
    )
    JSON.stringify(data)
  }
  
  /**
   * Deserializes a JSON string back to GameState
   */
  def deserialize(jsonString: String): Try[GameState] = {
    Try {
      val data = JSON.parse(jsonString).asInstanceOf[js.Dynamic]
      GameState(
        playerEntityId = data.playerEntityId.asInstanceOf[String],
        entities = deserializeEntities(data.entities.asInstanceOf[js.Array[js.Dynamic]]),
        messages = data.messages.asInstanceOf[js.Array[String]].toSeq,
        dungeon = deserializeDungeon(data.dungeon.asInstanceOf[js.Dynamic])
      )
    }
  }
  
  private def serializeEntities(entities: Seq[Entity]): js.Array[js.Dynamic] = {
    js.Array(entities.map(serializeEntity)*)
  }
  
  private def serializeEntity(entity: Entity): js.Dynamic = {
    js.Dynamic.literal(
      "id" -> entity.id,
      "components" -> serializeComponents(entity.components)
    )
  }
  
  private def serializeComponents(components: Map[Class[? <: Component], Component]): js.Dynamic = {
    val result = js.Dynamic.literal()
    components.foreach { case (clazz, component) =>
      val componentData = component match {
        case m: Movement => js.Dynamic.literal("x" -> m.position.x, "y" -> m.position.y)
        case h: Health => 
          // We need to access the health values through extension methods
          // For now, we'll just store default values since baseCurrent/baseMax are private
          js.Dynamic.literal("stored" -> true)
        case i: Initiative => js.Dynamic.literal("max" -> i.maxInitiative, "current" -> i.currentInitiative)
        case e: Experience => js.Dynamic.literal("current" -> e.currentExperience, "levelUp" -> e.levelUp)
        case et: EntityTypeComponent => 
          // Serialize the actual entity type
          js.Dynamic.literal("entityType" -> et.entityType.toString)
        case d: Drawable => 
          // For now, just store a flag since we need to handle sprite serialization differently
          js.Dynamic.literal("stored" -> true)
        case _: Hitbox => js.Dynamic.literal("stored" -> true) // Will use default hitbox
        case _: SightMemory => js.Dynamic.literal("stored" -> true) // Will reset sight memory for now
        case eq: Equipment => 
          // Serialize equipment items
          serializeEquipment(eq)
        case inv: Inventory => 
          // Serialize inventory items
          serializeInventory(inv)
        case _ => js.Dynamic.literal() // Skip other components for now
      }
      result.updateDynamic(clazz.getSimpleName)(componentData)
    }
    result
  }
  
  private def deserializeEntities(entitiesData: js.Array[js.Dynamic]): Seq[Entity] = {
    entitiesData.map(deserializeEntity).toSeq
  }
  
  private def deserializeEntity(entityData: js.Dynamic): Entity = {
    val id = entityData.id.asInstanceOf[String]
    val componentsData = entityData.components.asInstanceOf[js.Dynamic]
    val components = deserializeComponents(componentsData)
    Entity(id, components)
  }
  
  private def deserializeComponents(componentsData: js.Dynamic): Map[Class[? <: Component], Component] = {
    val result = scala.collection.mutable.Map[Class[? <: Component], Component]()
    val jsObj = componentsData.asInstanceOf[js.Object]
    
    // Check for each component type we support
    if (js.Object.hasProperty(jsObj, "Movement")) {
      val data = componentsData.Movement.asInstanceOf[js.Dynamic]
      result(classOf[Movement]) = Movement(Point(data.x.asInstanceOf[Int], data.y.asInstanceOf[Int]))
    }
    if (js.Object.hasProperty(jsObj, "Health")) {
      // For now, use default health values since we simplified serialization
      result(classOf[Health]) = Health(100)
    }
    if (js.Object.hasProperty(jsObj, "Initiative")) {
      val data = componentsData.Initiative.asInstanceOf[js.Dynamic]
      result(classOf[Initiative]) = Initiative(data.max.asInstanceOf[Int], data.current.asInstanceOf[Int])
    }
    if (js.Object.hasProperty(jsObj, "Experience")) {
      val data = componentsData.Experience.asInstanceOf[js.Dynamic]
      result(classOf[Experience]) = Experience(data.current.asInstanceOf[Int], data.levelUp.asInstanceOf[Boolean])
    }
    if (js.Object.hasProperty(jsObj, "EntityTypeComponent")) {
      val data = componentsData.EntityTypeComponent.asInstanceOf[js.Dynamic]
      // Restore actual entity type instead of defaulting to Player
      if (js.Object.hasProperty(data.asInstanceOf[js.Object], "entityType")) {
        val entityTypeString = data.entityType.asInstanceOf[String]
        val entityType = parseEntityType(entityTypeString)
        result(classOf[EntityTypeComponent]) = EntityTypeComponent(entityType)
      } else {
        // Fallback for old format
        result(classOf[EntityTypeComponent]) = EntityTypeComponent(EntityType.Player)
      }
    }
    if (js.Object.hasProperty(jsObj, "Drawable")) {
      // For now, use default drawable since we simplified serialization
      result(classOf[Drawable]) = Drawable(Set())
    }
    if (js.Object.hasProperty(jsObj, "Hitbox")) {
      result(classOf[Hitbox]) = Hitbox()
    }
    if (js.Object.hasProperty(jsObj, "SightMemory")) {
      result(classOf[SightMemory]) = SightMemory()
    }
    if (js.Object.hasProperty(jsObj, "Equipment")) {
      val data = componentsData.Equipment.asInstanceOf[js.Dynamic]
      result(classOf[Equipment]) = deserializeEquipment(data)
    }
    if (js.Object.hasProperty(jsObj, "Inventory")) {
      val data = componentsData.Inventory.asInstanceOf[js.Dynamic]
      result(classOf[Inventory]) = deserializeInventory(data)
    }
    
    result.toMap
  }
  
  private def serializeDungeon(dungeon: Dungeon): js.Dynamic = {
    js.Dynamic.literal(
      "seed" -> dungeon.seed,
      "testMode" -> dungeon.testMode
      // We'll reconstruct the dungeon from seed rather than serialize all data
    )
  }
  
  private def deserializeDungeon(dungeonData: js.Dynamic): Dungeon = {
    val seed = dungeonData.seed.asInstanceOf[Double].toLong
    val testMode = dungeonData.testMode.asInstanceOf[Boolean]
    
    // For now, create a simple dungeon - we may need to store more dungeon state later
    if (testMode) {
      Dungeon(testMode = true, seed = seed)
    } else {
      // Regenerate the dungeon from the seed
      map.MapGenerator.generateDungeon(dungeonSize = 20, lockedDoorCount = 3, itemCount = 6, seed = seed)
    }
  }
  
  private def serializeEquipment(equipment: Equipment): js.Dynamic = {
    js.Dynamic.literal(
      "helmet" -> equipment.helmet.map(serializeEquippable).orUndefined,
      "armor" -> equipment.armor.map(serializeEquippable).orUndefined
    )
  }
  
  private def deserializeEquipment(data: js.Dynamic): Equipment = {
    if (js.Object.hasProperty(data.asInstanceOf[js.Object], "helmet")) {
      val helmet = if (js.isUndefined(data.helmet)) None else Some(deserializeEquippable(data.helmet.asInstanceOf[js.Dynamic]))
      val armor = if (js.isUndefined(data.armor)) None else Some(deserializeEquippable(data.armor.asInstanceOf[js.Dynamic]))
      Equipment(helmet, armor)
    } else {
      // Fallback for old format
      Equipment()
    }
  }
  
  private def serializeEquippable(equippable: Equippable): js.Dynamic = {
    js.Dynamic.literal(
      "slot" -> equippable.slot.toString,
      "damageReduction" -> equippable.damageReduction,
      "itemName" -> equippable.itemName
    )
  }
  
  private def deserializeEquippable(data: js.Dynamic): Equippable = {
    val slot = parseEquipmentSlot(data.slot.asInstanceOf[String])
    val damageReduction = data.damageReduction.asInstanceOf[Int] 
    val itemName = data.itemName.asInstanceOf[String]
    Equippable(slot, damageReduction, itemName)
  }
  
  private def serializeInventory(inventory: Inventory): js.Dynamic = {
    js.Dynamic.literal(
      "itemEntityIds" -> js.Array(inventory.itemEntityIds*),
      "primaryWeaponId" -> inventory.primaryWeaponId.orUndefined,
      "secondaryWeaponId" -> inventory.secondaryWeaponId.orUndefined
    )
  }
  
  private def deserializeInventory(data: js.Dynamic): Inventory = {
    if (js.Object.hasProperty(data.asInstanceOf[js.Object], "itemEntityIds")) {
      val itemEntityIds = data.itemEntityIds.asInstanceOf[js.Array[String]].toSeq
      val primaryWeaponId = if (js.isUndefined(data.primaryWeaponId)) None else Some(data.primaryWeaponId.asInstanceOf[String])
      val secondaryWeaponId = if (js.isUndefined(data.secondaryWeaponId)) None else Some(data.secondaryWeaponId.asInstanceOf[String])
      Inventory(itemEntityIds, primaryWeaponId, secondaryWeaponId)
    } else {
      // Fallback for old format
      Inventory()
    }
  }
  
  private def parseEntityType(entityTypeString: String): EntityType = {
    entityTypeString match {
      case "Player" => EntityType.Player
      case "Enemy" => EntityType.Enemy
      case "Wall" => EntityType.Wall
      case "Floor" => EntityType.Floor
      case s if s.startsWith("LockedDoor(") => 
        // Parse LockedDoor(Red) format
        val colorPart = s.substring(11, s.length - 1) // Extract "Red" from "LockedDoor(Red)"
        val keyColour = parseKeyColour(colorPart)
        EntityType.LockedDoor(keyColour)
      case s if s.startsWith("Key(") =>
        // Parse Key(Red) format
        val colorPart = s.substring(4, s.length - 1) // Extract "Red" from "Key(Red)"
        val keyColour = parseKeyColour(colorPart)
        EntityType.Key(keyColour)
      case "Projectile" => EntityType.Projectile
      case _ => EntityType.Player // Fallback to Player if unknown
    }
  }
  
  private def parseKeyColour(colorString: String): KeyColour = {
    colorString match {
      case "Yellow" => KeyColour.Yellow
      case "Red" => KeyColour.Red
      case "Blue" => KeyColour.Blue
      case _ => KeyColour.Yellow // Fallback
    }
  }
  
  private def parseEquipmentSlot(slotString: String): EquipmentSlot = {
    slotString match {
      case "Helmet" => EquipmentSlot.Helmet
      case "Armor" => EquipmentSlot.Armor
      case _ => EquipmentSlot.Helmet // Fallback
    }
  }
}