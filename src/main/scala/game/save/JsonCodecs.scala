package game.save

import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.scalajs.js.JSConverters.JSRichOption
import scala.util.{Try, Success, Failure}
import game.*
import game.entity.*
import game.entity.Health.{currentHealth, maxHealth}
import game.entity.Ammo.AmmoType
import game.entity.EquipmentSlot
import data.DeathEvents.DeathEventReference
import data.DeathEvents.DeathEventReference.*
import data.Entities.EntityReference
import data.Projectiles.ProjectileReference
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
        messages = data.messages.asInstanceOf[js.Array[String]].toList,
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
        case sm: SightMemory => 
          // Serialize sight memory to preserve fog of war
          serializeSightMemory(sm)
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
        case ui: UsableItem =>
          // Serialize the new unified usable item
          serializeUsableItem(ui)
        case de: DeathEvents =>
          // Serialize death events for enemy splitting/experience
          serializeDeathEvents(de)
        case ammo: Ammo =>
          // Serialize ammo type for arrows/bolts
          serializeAmmo(ammo)
        case equippable: Equippable =>
          // Serialize equippable item data
          serializeEquippable(equippable)
        case ki: KeyItem =>
          // Serialize key color for locked doors
          serializeKeyItem(ki)
        case _: EventMemory => js.Dynamic.literal("stored" -> true) // Will reset event memory
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
      val data = componentsData.SightMemory.asInstanceOf[js.Dynamic]
      result(classOf[SightMemory]) = deserializeSightMemory(data)
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
    if (js.Object.hasProperty(jsObj, "UsableItem")) {
      val data = componentsData.UsableItem.asInstanceOf[js.Dynamic]
      result(classOf[UsableItem]) = deserializeUsableItem(data)
    }
    if (js.Object.hasProperty(jsObj, "DeathEvents")) {
      val data = componentsData.DeathEvents.asInstanceOf[js.Dynamic]
      result(classOf[DeathEvents]) = deserializeDeathEvents(data)
    }
    if (js.Object.hasProperty(jsObj, "Ammo")) {
      val data = componentsData.Ammo.asInstanceOf[js.Dynamic]
      result(classOf[Ammo]) = deserializeAmmo(data)
    }
    if (js.Object.hasProperty(jsObj, "Equippable")) {
      val data = componentsData.Equippable.asInstanceOf[js.Dynamic]
      result(classOf[Equippable]) = deserializeEquippable(data)
    }
    if (js.Object.hasProperty(jsObj, "KeyItem")) {
      val data = componentsData.KeyItem.asInstanceOf[js.Dynamic]
      result(classOf[KeyItem]) = deserializeKeyItem(data)
    }
    if (js.Object.hasProperty(jsObj, "EventMemory")) {
      result(classOf[EventMemory]) = EventMemory()
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
  
  // Serialize the new simplified UsableItem component
  private def serializeUsableItem(item: UsableItem): js.Dynamic = {
    js.Dynamic.literal(
      "targeting" -> serializeTargeting(item.targeting),
      "chargeType" -> serializeChargeType(item.chargeType),
      "effect" -> serializeGameEffect(item.effect)
    )
  }
  
  private def deserializeUsableItem(data: js.Dynamic): UsableItem = {
    val targeting = deserializeTargeting(data.targeting.asInstanceOf[js.Dynamic])
    val chargeType = deserializeChargeType(data.chargeType.asInstanceOf[js.Dynamic])
    val effect = deserializeGameEffect(data.effect.asInstanceOf[js.Dynamic])
    UsableItem(targeting, chargeType, effect)
  }
  
  private def serializeTargeting(targeting: Targeting): js.Dynamic = {
    targeting match {
      case Targeting.Self => js.Dynamic.literal("type" -> "Self")
      case Targeting.EnemyActor(range) => js.Dynamic.literal("type" -> "EnemyActor", "range" -> range)
      case Targeting.TileInRange(range) => js.Dynamic.literal("type" -> "TileInRange", "range" -> range)
    }
  }
  
  private def deserializeTargeting(data: js.Dynamic): Targeting = {
    data.`type`.asInstanceOf[String] match {
      case "Self" => Targeting.Self
      case "EnemyActor" => Targeting.EnemyActor(data.range.asInstanceOf[Int])
      case "TileInRange" => Targeting.TileInRange(data.range.asInstanceOf[Int])
      case _ => Targeting.Self // Fallback
    }
  }
  
  private def serializeChargeType(chargeType: ChargeType): js.Dynamic = {
    chargeType match {
      case ChargeType.SingleUse => js.Dynamic.literal("type" -> "SingleUse")
      case ChargeType.Ammo(ammoType) => js.Dynamic.literal("type" -> "Ammo", "ammoType" -> ammoType.toString)
    }
  }
  
  private def deserializeChargeType(data: js.Dynamic): ChargeType = {
    data.`type`.asInstanceOf[String] match {
      case "SingleUse" => ChargeType.SingleUse
      case "Ammo" => ChargeType.Ammo(parseAmmoType(data.ammoType.asInstanceOf[String]))
      case _ => ChargeType.SingleUse // Fallback
    }
  }
  
  private def serializeGameEffect(effect: game.entity.GameEffect): js.Dynamic = {
    effect match {
      case game.entity.GameEffect.Heal(amount) => js.Dynamic.literal("type" -> "Heal", "amount" -> amount)
      case game.entity.GameEffect.CreateProjectile(projectileReference) => 
        js.Dynamic.literal("type" -> "CreateProjectile", "projectileReference" -> projectileReference.toString)
    }
  }
  
  private def deserializeGameEffect(data: js.Dynamic): game.entity.GameEffect = {
    data.`type`.asInstanceOf[String] match {
      case "Heal" => game.entity.GameEffect.Heal(data.amount.asInstanceOf[Int])
      case "CreateProjectile" => 
        val projectileRefString = data.projectileReference.asInstanceOf[String]
        val projectileRef = parseProjectileReference(projectileRefString)
        game.entity.GameEffect.CreateProjectile(projectileRef)
      case _ => game.entity.GameEffect.Heal(40) // Fallback to healing potion
    }
  }
  
  private def parseAmmoType(ammoString: String): AmmoType = {
    // Assuming AmmoType is an enum, parse accordingly
    ammoString match {
      case "Arrow" => AmmoType.Arrow
      case _ => AmmoType.Arrow // Fallback
    }
  }
  
  // Serialization methods for missing components
  
  private def serializeDeathEvents(deathEvents: DeathEvents): js.Dynamic = {
    js.Dynamic.literal(
      "events" -> js.Array(deathEvents.deathEvents.map(serializeDeathEventReference)*)
    )
  }
  
  private def deserializeDeathEvents(data: js.Dynamic): DeathEvents = {
    val events = data.events.asInstanceOf[js.Array[js.Dynamic]].map(deserializeDeathEventReference).toSeq
    DeathEvents(events)
  }
  
  private def serializeDeathEventReference(event: DeathEventReference): js.Dynamic = {
    event match {
      case GiveExperience(amount) => 
        js.Dynamic.literal("type" -> "GiveExperience", "amount" -> amount)
      case SpawnEntity(entityRef, forceSpawn) => 
        js.Dynamic.literal("type" -> "SpawnEntity", "entityReference" -> entityRef.toString, "forceSpawn" -> forceSpawn)
    }
  }
  
  private def deserializeDeathEventReference(data: js.Dynamic): DeathEventReference = {
    data.`type`.asInstanceOf[String] match {
      case "GiveExperience" => 
        GiveExperience(data.amount.asInstanceOf[Int])
      case "SpawnEntity" => 
        val entityRefString = data.entityReference.asInstanceOf[String]
        val entityRef = parseEntityReference(entityRefString)
        val forceSpawn = data.forceSpawn.asInstanceOf[Boolean]
        SpawnEntity(entityRef, forceSpawn)
      case _ => GiveExperience(10) // Fallback
    }
  }
  
  private def parseEntityReference(refString: String): EntityReference = {
    refString match {
      case "Slimelet" => EntityReference.Slimelet
      case _ => EntityReference.Slimelet // Fallback 
    }
  }
  
  private def parseProjectileReference(refString: String): ProjectileReference = {
    refString match {
      case "Arrow" => ProjectileReference.Arrow
      case "Fireball" => ProjectileReference.Fireball
      case _ => ProjectileReference.Arrow // Fallback
    }
  }
  
  private def serializeAmmo(ammo: Ammo): js.Dynamic = {
    js.Dynamic.literal("ammoType" -> ammo.ammoType.toString)
  }
  
  private def deserializeAmmo(data: js.Dynamic): Ammo = {
    val ammoType = parseAmmoType(data.ammoType.asInstanceOf[String])
    Ammo(ammoType)
  }
  
  private def serializeSightMemory(sightMemory: SightMemory): js.Dynamic = {
    js.Dynamic.literal(
      "seenPoints" -> js.Array(sightMemory.seenPoints.map(serializePoint).toSeq*)
    )
  }
  
  private def deserializeSightMemory(data: js.Dynamic): SightMemory = {
    val seenPoints = data.seenPoints.asInstanceOf[js.Array[js.Dynamic]].map(deserializePoint).toSet
    SightMemory(seenPoints)
  }
  
  private def serializeKeyItem(keyItem: KeyItem): js.Dynamic = {
    js.Dynamic.literal("keyColour" -> keyItem.keyColour.toString)
  }
  
  private def deserializeKeyItem(data: js.Dynamic): KeyItem = {
    val keyColour = parseKeyColour(data.keyColour.asInstanceOf[String])
    KeyItem(keyColour)
  }
}