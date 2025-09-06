package game.save

import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.scalajs.js.JSConverters.JSRichOption
import scala.util.{Try, Success, Failure}
import game.*
import game.entity.*
import game.entity.Health.{currentHealth, maxHealth}
import game.entity.Ammo.AmmoType
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
      "components" -> serializeComponents(entity)
    )
  }
  
  private def serializeComponents(entity: Entity): js.Dynamic = {
    val result = js.Dynamic.literal()
    entity.components.foreach { case (clazz, component) =>
      val componentData = component match {
        case m: Movement => js.Dynamic.literal("x" -> m.position.x, "y" -> m.position.y)
        case h: Health => 
          // Use entity extension methods to get actual health values
          js.Dynamic.literal("current" -> entity.currentHealth, "max" -> entity.maxHealth)
        case i: Initiative => js.Dynamic.literal("max" -> i.maxInitiative, "current" -> i.currentInitiative)
        case e: Experience => js.Dynamic.literal("current" -> e.currentExperience, "levelUp" -> e.levelUp)
        case et: EntityTypeComponent => 
          // Serialize the actual entity type
          js.Dynamic.literal("entityType" -> et.entityType.toString)
        case d: Drawable => 
          // Serialize sprite information properly
          js.Dynamic.literal("sprites" -> serializeSprites(d.sprites))
        case _: Hitbox => js.Dynamic.literal("stored" -> true) // Will use default hitbox
        case _: SightMemory => js.Dynamic.literal("stored" -> true) // Will reset sight memory for now
        case eq: Equipment => 
          // Serialize equipment items
          serializeEquipment(eq)
        case inv: Inventory => 
          // Serialize inventory items
          serializeInventory(inv)
        case w: WeaponItem =>
          // Serialize weapon data
          serializeWeaponItem(w)
        case _: CanPickUp =>
          // Simple marker component
          js.Dynamic.literal("stored" -> true)
        case nc: NameComponent =>
          // Serialize name and description
          js.Dynamic.literal("name" -> nc.name, "description" -> nc.description)
        case sti: SelfTargetingItem =>
          // Serialize self-targeting items with type information for reconstruction
          serializeSelfTargetingItem(sti)
        case eti: EntityTargetingItem =>
          // Serialize entity-targeting items
          serializeEntityTargetingItem(eti)
        case tti: TileTargetingItem =>
          // Serialize tile-targeting items
          serializeTileTargetingItem(tti)
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
      val data = componentsData.Health.asInstanceOf[js.Dynamic]
      // Restore actual health values
      if (js.Object.hasProperty(data.asInstanceOf[js.Object], "current")) {
        val current = data.current.asInstanceOf[Int]
        val max = data.max.asInstanceOf[Int]
        // Use the private constructor directly to set different current and max values
        result(classOf[Health]) = new Health(current, max)
      } else {
        // Fallback for old format
        result(classOf[Health]) = Health(100)
      }
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
      val data = componentsData.Drawable.asInstanceOf[js.Dynamic]
      // Restore sprite information
      if (js.Object.hasProperty(data.asInstanceOf[js.Object], "sprites")) {
        val sprites = deserializeSprites(data.sprites.asInstanceOf[js.Array[js.Dynamic]])
        result(classOf[Drawable]) = Drawable(sprites)
      } else {
        // Fallback for old format
        result(classOf[Drawable]) = Drawable(Set())
      }
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
    if (js.Object.hasProperty(jsObj, "WeaponItem")) {
      val data = componentsData.WeaponItem.asInstanceOf[js.Dynamic]
      result(classOf[WeaponItem]) = deserializeWeaponItem(data)
    }
    if (js.Object.hasProperty(jsObj, "CanPickUp")) {
      result(classOf[CanPickUp]) = CanPickUp()
    }
    if (js.Object.hasProperty(jsObj, "NameComponent")) {
      val data = componentsData.NameComponent.asInstanceOf[js.Dynamic]
      result(classOf[NameComponent]) = deserializeNameComponent(data)
    }
    if (js.Object.hasProperty(jsObj, "SelfTargetingItem")) {
      val data = componentsData.SelfTargetingItem.asInstanceOf[js.Dynamic]
      result(classOf[SelfTargetingItem]) = deserializeSelfTargetingItem(data)
    }
    if (js.Object.hasProperty(jsObj, "EntityTargetingItem")) {
      val data = componentsData.EntityTargetingItem.asInstanceOf[js.Dynamic]
      result(classOf[EntityTargetingItem]) = deserializeEntityTargetingItem(data)
    }
    if (js.Object.hasProperty(jsObj, "TileTargetingItem")) {
      val data = componentsData.TileTargetingItem.asInstanceOf[js.Dynamic]
      result(classOf[TileTargetingItem]) = deserializeTileTargetingItem(data)
    }
    
    result.toMap
  }
  
  private def serializeDungeon(dungeon: Dungeon): js.Dynamic = {
    js.Dynamic.literal(
      "seed" -> dungeon.seed,
      "testMode" -> dungeon.testMode,
      // Store the actual dungeon structure to ensure exact reproduction
      "roomGrid" -> serializePointSet(dungeon.roomGrid),
      "roomConnections" -> serializeRoomConnections(dungeon.roomConnections),
      "blockedRooms" -> serializePointSet(dungeon.blockedRooms),
      "startPoint" -> serializePoint(dungeon.startPoint),
      "endpoint" -> dungeon.endpoint.map(serializePoint).orUndefined,
      "items" -> serializeItems(dungeon.items)
    )
  }
  
  private def deserializeDungeon(dungeonData: js.Dynamic): Dungeon = {
    val seed = dungeonData.seed.asInstanceOf[Double].toLong
    val testMode = dungeonData.testMode.asInstanceOf[Boolean]
    
    // Try to restore from stored structure if available, otherwise regenerate from seed
    if (js.Object.hasProperty(dungeonData.asInstanceOf[js.Object], "roomGrid")) {
      val roomGrid = deserializePointSet(dungeonData.roomGrid.asInstanceOf[js.Array[js.Dynamic]])
      val roomConnections = deserializeRoomConnections(dungeonData.roomConnections.asInstanceOf[js.Array[js.Dynamic]])
      val blockedRooms = deserializePointSet(dungeonData.blockedRooms.asInstanceOf[js.Array[js.Dynamic]])
      val startPoint = deserializePoint(dungeonData.startPoint.asInstanceOf[js.Dynamic])
      val endpoint = if (js.isUndefined(dungeonData.endpoint)) None else Some(deserializePoint(dungeonData.endpoint.asInstanceOf[js.Dynamic]))
      val items = deserializeItems(dungeonData.items.asInstanceOf[js.Array[js.Dynamic]])
      
      Dungeon(roomGrid, roomConnections, blockedRooms, startPoint, endpoint, items, testMode, seed)
    } else {
      // Fallback for old format - regenerate from seed
      if (testMode) {
        Dungeon(testMode = true, seed = seed)
      } else {
        map.MapGenerator.generateDungeon(dungeonSize = 20, lockedDoorCount = 3, itemCount = 6, seed = seed)
      }
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
  
  private def serializeSprites(sprites: Set[(Point, Sprite)]): js.Array[js.Dynamic] = {
    js.Array(sprites.toSeq.map { case (point, sprite) =>
      js.Dynamic.literal(
        "point" -> js.Dynamic.literal("x" -> point.x, "y" -> point.y),
        "sprite" -> js.Dynamic.literal("x" -> sprite.x, "y" -> sprite.y, "layer" -> sprite.layer)
      )
    }*)
  }
  
  private def deserializeSprites(spritesData: js.Array[js.Dynamic]): Set[(Point, Sprite)] = {
    spritesData.toSet.map { data =>
      val pointData = data.point.asInstanceOf[js.Dynamic]
      val spriteData = data.sprite.asInstanceOf[js.Dynamic]
      val point = Point(pointData.x.asInstanceOf[Int], pointData.y.asInstanceOf[Int])
      val sprite = Sprite(spriteData.x.asInstanceOf[Int], spriteData.y.asInstanceOf[Int], spriteData.layer.asInstanceOf[Int])
      (point, sprite)
    }
  }
  
  // Helper functions for dungeon serialization
  private def serializePointSet(points: Set[Point]): js.Array[js.Dynamic] = {
    js.Array(points.toSeq.map(serializePoint)*)
  }
  
  private def deserializePointSet(pointsData: js.Array[js.Dynamic]): Set[Point] = {
    pointsData.toSet.map(deserializePoint)
  }
  
  private def serializePoint(point: Point): js.Dynamic = {
    js.Dynamic.literal("x" -> point.x, "y" -> point.y)
  }
  
  private def deserializePoint(pointData: js.Dynamic): Point = {
    Point(pointData.x.asInstanceOf[Int], pointData.y.asInstanceOf[Int])
  }
  
  private def serializeRoomConnections(connections: Set[RoomConnection]): js.Array[js.Dynamic] = {
    js.Array(connections.toSeq.map { connection =>
      js.Dynamic.literal(
        "originRoom" -> serializePoint(connection.originRoom),
        "direction" -> connection.direction.toString,
        "destinationRoom" -> serializePoint(connection.destinationRoom),
        "optLock" -> connection.optLock.map(lock => js.Dynamic.literal("entityType" -> lock.toString)).orUndefined
      )
    }*)
  }
  
  private def deserializeRoomConnections(connectionsData: js.Array[js.Dynamic]): Set[RoomConnection] = {
    connectionsData.toSet.map { data =>
      val originRoom = deserializePoint(data.originRoom.asInstanceOf[js.Dynamic])
      val direction = parseDirection(data.direction.asInstanceOf[String])
      val destinationRoom = deserializePoint(data.destinationRoom.asInstanceOf[js.Dynamic])
      val optLock = if (js.isUndefined(data.optLock)) None else {
        val lockData = data.optLock.asInstanceOf[js.Dynamic]
        val lockString = lockData.entityType.asInstanceOf[String]
        parseEntityType(lockString) match {
          case lock: EntityType.LockedDoor => Some(lock)
          case _ => None
        }
      }
      RoomConnection(originRoom, direction, destinationRoom, optLock)
    }
  }
  
  private def serializeItems(items: Set[(Point, ItemDescriptor)]): js.Array[js.Dynamic] = {
    js.Array(items.toSeq.map { case (point, itemDesc) =>
      js.Dynamic.literal(
        "point" -> serializePoint(point),
        "item" -> serializeItemDescriptor(itemDesc)
      )
    }*)
  }
  
  private def deserializeItems(itemsData: js.Array[js.Dynamic]): Set[(Point, ItemDescriptor)] = {
    itemsData.toSet.map { data =>
      val point = deserializePoint(data.point.asInstanceOf[js.Dynamic])
      val itemDesc = deserializeItemDescriptor(data.item.asInstanceOf[js.Dynamic])
      (point, itemDesc)
    }
  }
  
  private def serializeItemDescriptor(item: ItemDescriptor): js.Dynamic = {
    item match {
      case ItemDescriptor.PotionDescriptor => js.Dynamic.literal("type" -> "Potion")
      case ItemDescriptor.ScrollDescriptor => js.Dynamic.literal("type" -> "Scroll")
      case ItemDescriptor.ArrowDescriptor => js.Dynamic.literal("type" -> "Arrow")
      case ItemDescriptor.LeatherHelmetDescriptor => js.Dynamic.literal("type" -> "LeatherHelmet")
      case ItemDescriptor.ChainmailArmorDescriptor => js.Dynamic.literal("type" -> "ChainmailArmor")
      case ItemDescriptor.IronHelmetDescriptor => js.Dynamic.literal("type" -> "IronHelmet")
      case ItemDescriptor.PlateArmorDescriptor => js.Dynamic.literal("type" -> "PlateArmor")
      case ItemDescriptor.KeyDescriptor(keyColour) => js.Dynamic.literal("type" -> "Key", "color" -> keyColour.toString)
    }
  }
  
  private def deserializeItemDescriptor(itemData: js.Dynamic): ItemDescriptor = {
    val itemType = itemData.`type`.asInstanceOf[String]
    itemType match {
      case "Potion" => ItemDescriptor.PotionDescriptor
      case "Scroll" => ItemDescriptor.ScrollDescriptor  
      case "Arrow" => ItemDescriptor.ArrowDescriptor
      case "LeatherHelmet" => ItemDescriptor.LeatherHelmetDescriptor
      case "ChainmailArmor" => ItemDescriptor.ChainmailArmorDescriptor
      case "IronHelmet" => ItemDescriptor.IronHelmetDescriptor
      case "PlateArmor" => ItemDescriptor.PlateArmorDescriptor
      case "Key" => 
        val colorString = itemData.color.asInstanceOf[String]
        val keyColour = parseKeyColour(colorString)
        ItemDescriptor.KeyDescriptor(keyColour)
      case _ => ItemDescriptor.PotionDescriptor // Fallback
    }
  }
  
  private def parseDirection(directionString: String): Direction = {
    directionString match {
      case "Up" => Direction.Up
      case "Down" => Direction.Down
      case "Left" => Direction.Left
      case "Right" => Direction.Right
      case _ => Direction.Up // Fallback
    }
  }
  
  // Serialization methods for new components
  private def serializeWeaponItem(weapon: WeaponItem): js.Dynamic = {
    js.Dynamic.literal(
      "damage" -> weapon.damage,
      "weaponType" -> serializeWeaponType(weapon.weaponType)
    )
  }
  
  private def deserializeWeaponItem(data: js.Dynamic): WeaponItem = {
    val damage = data.damage.asInstanceOf[Int]
    val weaponType = deserializeWeaponType(data.weaponType.asInstanceOf[js.Dynamic])
    WeaponItem(damage, weaponType)
  }
  
  private def serializeWeaponType(weaponType: WeaponType): js.Dynamic = {
    weaponType match {
      case Melee => js.Dynamic.literal("type" -> "Melee")
      case Ranged(range) => js.Dynamic.literal("type" -> "Ranged", "range" -> range)
    }
  }
  
  private def deserializeWeaponType(data: js.Dynamic): WeaponType = {
    val weaponTypeString = data.`type`.asInstanceOf[String]
    weaponTypeString match {
      case "Melee" => Melee
      case "Ranged" => 
        val range = data.range.asInstanceOf[Int]
        Ranged(range)
      case _ => Melee // Fallback
    }
  }
  
  private def deserializeNameComponent(data: js.Dynamic): NameComponent = {
    val name = data.name.asInstanceOf[String]
    val description = if (js.Object.hasProperty(data.asInstanceOf[js.Object], "description")) {
      data.description.asInstanceOf[String]
    } else ""
    NameComponent(name, description)
  }
  
  // For UsableItem components, we need to handle the function serialization carefully
  // We'll store the item type and reconstruct the functions using the builders
  private def serializeSelfTargetingItem(item: SelfTargetingItem): js.Dynamic = {
    js.Dynamic.literal(
      "itemType" -> "healing_potion", // Default type for self-targeting items
      "consumeOnUse" -> item.consumeOnUse,
      "ammo" -> item.ammo.map(_.toString).orUndefined
    )
  }
  
  private def deserializeSelfTargetingItem(data: js.Dynamic): SelfTargetingItem = {
    val itemType = if (js.Object.hasProperty(data.asInstanceOf[js.Object], "itemType")) {
      data.itemType.asInstanceOf[String]
    } else "healing_potion"
    val consumeOnUse = data.consumeOnUse.asInstanceOf[Boolean]
    val ammo = if (js.isUndefined(data.ammo)) None else Some(parseAmmoType(data.ammo.asInstanceOf[String]))
    
    // Reconstruct the appropriate item based on type
    itemType match {
      case "healing_potion" => UsableItem.builders.healingPotion()
      case _ => UsableItem.builders.healingPotion() // Fallback
    }
  }
  
  private def serializeEntityTargetingItem(item: EntityTargetingItem): js.Dynamic = {
    js.Dynamic.literal(
      "itemType" -> "bow", // Default type for entity-targeting items
      "consumeOnUse" -> item.consumeOnUse,
      "ammo" -> item.ammo.map(_.toString).orUndefined
    )
  }
  
  private def deserializeEntityTargetingItem(data: js.Dynamic): EntityTargetingItem = {
    val itemType = if (js.Object.hasProperty(data.asInstanceOf[js.Object], "itemType")) {
      data.itemType.asInstanceOf[String]
    } else "bow"
    val consumeOnUse = data.consumeOnUse.asInstanceOf[Boolean]
    val ammo = if (js.isUndefined(data.ammo)) None else Some(parseAmmoType(data.ammo.asInstanceOf[String]))
    
    // Reconstruct the appropriate item based on type
    itemType match {
      case "bow" => UsableItem.builders.bow()
      case _ => UsableItem.builders.bow() // Fallback
    }
  }
  
  private def serializeTileTargetingItem(item: TileTargetingItem): js.Dynamic = {
    js.Dynamic.literal(
      "itemType" -> "fireball_scroll", // Default type for tile-targeting items
      "range" -> item.range,
      "consumeOnUse" -> item.consumeOnUse,
      "ammo" -> item.ammo.map(_.toString).orUndefined
    )
  }
  
  private def deserializeTileTargetingItem(data: js.Dynamic): TileTargetingItem = {
    val itemType = if (js.Object.hasProperty(data.asInstanceOf[js.Object], "itemType")) {
      data.itemType.asInstanceOf[String]
    } else "fireball_scroll"
    val range = data.range.asInstanceOf[Int]
    val consumeOnUse = data.consumeOnUse.asInstanceOf[Boolean]
    val ammo = if (js.isUndefined(data.ammo)) None else Some(parseAmmoType(data.ammo.asInstanceOf[String]))
    
    // Reconstruct the appropriate item based on type
    itemType match {
      case "fireball_scroll" => UsableItem.builders.fireballScroll()
      case _ => UsableItem.builders.fireballScroll() // Fallback
    }
  }
  
  private def parseAmmoType(ammoString: String): AmmoType = {
    // Assuming AmmoType is an enum, parse accordingly
    // For now, assuming only Arrow type exists based on the UsableItem code
    AmmoType.Arrow // This might need adjustment based on actual AmmoType implementation
  }
}