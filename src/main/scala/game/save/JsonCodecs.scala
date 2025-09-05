package game.save

import scala.scalajs.js
import scala.scalajs.js.JSON
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
          // Use reflection to access private fields or use a simplified approach for now
          // For MVP, we'll just restore with default health values
          js.Dynamic.literal("stored" -> true)
        case i: Initiative => js.Dynamic.literal("max" -> i.maxInitiative, "current" -> i.currentInitiative)
        case e: Experience => js.Dynamic.literal("current" -> e.currentExperience, "levelUp" -> e.levelUp)
        case _: EntityTypeComponent => js.Dynamic.literal("stored" -> true) // Will need entity type restoration
        case _: Drawable => js.Dynamic.literal("stored" -> true) // Will use default sprites
        case _: Hitbox => js.Dynamic.literal("stored" -> true) // Will use default hitbox
        case _: SightMemory => js.Dynamic.literal("stored" -> true) // Will reset sight memory
        case _: Equipment => js.Dynamic.literal("stored" -> true) // Will need equipment restoration
        case _: Inventory => js.Dynamic.literal("stored" -> true) // Will need inventory restoration  
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
      // For MVP, use default health values - will improve this later
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
    // Add other components with default values for MVP
    if (js.Object.hasProperty(jsObj, "EntityTypeComponent")) {
      result(classOf[EntityTypeComponent]) = EntityTypeComponent(EntityType.Player) // Will improve this
    }
    if (js.Object.hasProperty(jsObj, "Drawable")) {
      result(classOf[Drawable]) = Drawable(Set()) // Will use default sprites
    }
    if (js.Object.hasProperty(jsObj, "Hitbox")) {
      result(classOf[Hitbox]) = Hitbox()
    }
    if (js.Object.hasProperty(jsObj, "SightMemory")) {
      result(classOf[SightMemory]) = SightMemory()
    }
    if (js.Object.hasProperty(jsObj, "Equipment")) {
      result(classOf[Equipment]) = Equipment()
    }
    if (js.Object.hasProperty(jsObj, "Inventory")) {
      result(classOf[Inventory]) = Inventory()
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
}