package game.save

import upickle.default._
import ujson.Value
import game.entity.*
import game.entity.Health._
import game.save.SavePickle._

/**
 * Registry that maps component types to stable tags and provides codecs.
 * Allows entity-aware serialization (particularly for Health which needs computed values).
 */
object ComponentRegistry {
  
  /**
   * Convert an entity to a persisted entity with all supported components
   */
  def toPersistedEntity(entity: Entity): PersistedEntity = {
    val components = entity.components.flatMap { case (clazz, component) =>
      toSaved(entity, component).map(savedComponent => savedComponent)
    }.toVector
    
    PersistedEntity(entity.id, components)
  }
  
  /**
   * Convert a persisted entity back to a live entity
   */
  def fromPersistedEntity(persistedEntity: PersistedEntity): Entity = {
    val components = persistedEntity.components.flatMap { savedComponent =>
      fromSaved(savedComponent)
    }.map(c => (c.getClass.asInstanceOf[Class[? <: Component]], c)).toMap
    
    Entity(persistedEntity.id, components)
  }
  
  /**
   * Convert a component to its saved representation, with entity context for special cases
   */
  def toSaved(entity: Entity, component: Component): Option[SavedComponent] = {
    component match {
      case m: Movement =>
        Some(SavedComponent("Movement", writeJs(PersistedMovement(m.position))))
      
      case h: Health =>
        // Use entity extension methods to get computed current/max values  
        Some(SavedComponent("Health", writeJs(PersistedHealth(entity.currentHealth, entity.maxHealth))))
      
      case i: Initiative =>
        Some(SavedComponent("Initiative", writeJs(PersistedInitiative(i.maxInitiative, i.currentInitiative))))
      
      case e: Experience =>
        Some(SavedComponent("Experience", writeJs(PersistedExperience(e.currentExperience, e.levelUp))))
      
      case et: EntityTypeComponent =>
        Some(SavedComponent("EntityTypeComponent", writeJs(PersistedEntityTypeComponent(et.entityType))))
      
      case d: Drawable =>
        Some(SavedComponent("Drawable", writeJs(PersistedDrawable(d.sprites))))
      
      case _: Hitbox =>
        Some(SavedComponent("Hitbox", writeJs(PersistedHitbox())))
      
      case sm: SightMemory =>
        Some(SavedComponent("SightMemory", writeJs(PersistedSightMemory(sm.seenPoints))))
      
      case eq: Equipment =>
        val helmetPersisted = eq.helmet.map(h => PersistedEquippable(h.slot, h.damageReduction, h.itemName))
        val armorPersisted = eq.armor.map(a => PersistedEquippable(a.slot, a.damageReduction, a.itemName))
        Some(SavedComponent("Equipment", writeJs(PersistedEquipment(helmetPersisted, armorPersisted))))
      
      case equ: Equippable =>
        Some(SavedComponent("Equippable", writeJs(PersistedEquippable(equ.slot, equ.damageReduction, equ.itemName))))
      
      case inv: Inventory =>
        Some(SavedComponent("Inventory", writeJs(PersistedInventory(inv.itemEntityIds, inv.primaryWeaponId, inv.secondaryWeaponId))))
      
      case w: WeaponItem =>
        Some(SavedComponent("WeaponItem", writeJs(PersistedWeaponItem(w.damage, w.weaponType))))
      
      case _: CanPickUp =>
        Some(SavedComponent("CanPickUp", writeJs(PersistedCanPickUp())))
      
      case nc: NameComponent =>
        Some(SavedComponent("NameComponent", writeJs(PersistedNameComponent(nc.name, nc.description))))
      
      case ui: UsableItem =>
        Some(SavedComponent("UsableItem", writeJs(PersistedUsableItem(ui.targeting, ui.chargeType, ui.effect))))
      
      case de: DeathEvents =>
        Some(SavedComponent("DeathEvents", writeJs(PersistedDeathEvents(de.deathEvents))))
      
      case ammo: Ammo =>
        Some(SavedComponent("Ammo", writeJs(PersistedAmmo(ammo.ammoType))))
      
      case ki: KeyItem =>
        Some(SavedComponent("KeyItem", writeJs(PersistedKeyItem(ki.keyColour))))
      
      case em: EventMemory =>
        Some(SavedComponent("EventMemory", writeJs(PersistedEventMemory(em.events))))
      
      case _ =>
        // Skip other components - only persist the same set as the old serializer
        None
    }
  }
  
  /**
   * Convert a saved component back to its live representation
   */
  def fromSaved(savedComponent: SavedComponent): Option[Component] = {
    savedComponent.tag match {
      case "Movement" =>
        val persisted = read[PersistedMovement](savedComponent.data)
        Some(Movement(persisted.position))
      
      case "Health" =>
        val persisted = read[PersistedHealth](savedComponent.data)
        // Use the private constructor to set different current and max values
        Some(new Health(persisted.current, persisted.max))
      
      case "Initiative" =>
        val persisted = read[PersistedInitiative](savedComponent.data)
        Some(Initiative(persisted.maxInitiative, persisted.currentInitiative))
      
      case "Experience" =>
        val persisted = read[PersistedExperience](savedComponent.data)
        Some(Experience(persisted.currentExperience, persisted.levelUp))
      
      case "EntityTypeComponent" =>
        val persisted = read[PersistedEntityTypeComponent](savedComponent.data)
        Some(EntityTypeComponent(persisted.entityType))
      
      case "Drawable" =>
        val persisted = read[PersistedDrawable](savedComponent.data)
        Some(Drawable(persisted.sprites))
      
      case "Hitbox" =>
        // Use default hitbox
        Some(Hitbox())
      
      case "SightMemory" =>
        val persisted = read[PersistedSightMemory](savedComponent.data)
        Some(SightMemory(persisted.seenPoints))
      
      case "Equipment" =>
        val persisted = read[PersistedEquipment](savedComponent.data)
        val helmet = persisted.helmet.map(p => Equippable(p.slot, p.damageReduction, p.itemName))
        val armor = persisted.armor.map(p => Equippable(p.slot, p.damageReduction, p.itemName))
        Some(Equipment(helmet, armor))
      
      case "Equippable" =>
        val persisted = read[PersistedEquippable](savedComponent.data)
        Some(Equippable(persisted.slot, persisted.damageReduction, persisted.itemName))
      
      case "Inventory" =>
        val persisted = read[PersistedInventory](savedComponent.data)
        Some(Inventory(persisted.itemEntityIds, persisted.primaryWeaponId, persisted.secondaryWeaponId))
      
      case "WeaponItem" =>
        val persisted = read[PersistedWeaponItem](savedComponent.data)
        Some(WeaponItem(persisted.damage, persisted.weaponType))
      
      case "CanPickUp" =>
        Some(CanPickUp())
      
      case "NameComponent" =>
        val persisted = read[PersistedNameComponent](savedComponent.data)
        Some(NameComponent(persisted.name, persisted.description))
      
      case "UsableItem" =>
        val persisted = read[PersistedUsableItem](savedComponent.data)
        Some(UsableItem(persisted.targeting, persisted.chargeType, persisted.effect))
      
      case "DeathEvents" =>
        val persisted = read[PersistedDeathEvents](savedComponent.data)
        Some(DeathEvents(persisted.deathEvents))
      
      case "Ammo" =>
        val persisted = read[PersistedAmmo](savedComponent.data)
        Some(Ammo(persisted.ammoType))
      
      case "KeyItem" =>
        val persisted = read[PersistedKeyItem](savedComponent.data)
        Some(KeyItem(persisted.keyColour))
      
      case "EventMemory" =>
        val persisted = read[PersistedEventMemory](savedComponent.data)
        Some(EventMemory(persisted.events))
      
      case _ =>
        // Unknown tags are safely ignored on load
        None
    }
  }
}