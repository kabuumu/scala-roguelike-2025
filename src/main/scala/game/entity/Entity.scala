package game.entity

import game.status.StatusEffect

import scala.reflect.ClassTag
import scala.util.Random

case class Entity(id: String = Random.nextString(12),
                  components: Map[Class[? <: Component], Component] = Map.empty,
                  entityStatuses: Seq[StatusEffect] = Nil
                 ) {
  def addStatusEffect(statusEffect: StatusEffect): Entity = {
    copy(entityStatuses = entityStatuses :+ statusEffect)
  }

  def removeStatusEffect(statusEffect: StatusEffect): Entity = {
    copy(entityStatuses = entityStatuses.filterNot(_ == statusEffect))
  }
  
  def addComponent(component: Component): Entity = {
    val updatedComponents = components.get(component.getClass) match {
      case Some(existingComponent) =>
        // If the component already exists, we can keep the existing one
        components
      case None =>
        // If it doesn't exist, we add the new component
        components + (component.getClass -> component)
    }

    copy(components = updatedComponents)
  }

  def removeComponent[ComponentType <: Component](implicit classTag: ClassTag[ComponentType]): Entity = {
    copy(components = components - classTag.runtimeClass.asInstanceOf[Class[? <: Component]])
  }

  def get[ComponentType <: Component](implicit classTag: ClassTag[ComponentType]): Option[ComponentType] = {
    entityStatuses.foldLeft(this) { (entity, effect) =>
        effect(entity)
      }.getBase[ComponentType]
  }
  
  def getBase[ComponentType <: Component](implicit classTag: ClassTag[ComponentType]): Option[ComponentType] = {
    components
      .get(classTag.runtimeClass.asInstanceOf[Class[ComponentType]])
      .map(_.asInstanceOf[ComponentType])
  }

  def update[ComponentType <: Component](updateFunction: ComponentType => ComponentType)(implicit classTag: ClassTag[ComponentType]): Entity = {
    getBase[ComponentType] match {
      case Some(component) =>
        val updatedComponent = updateFunction(component)
        copy(components = components + (classTag.runtimeClass.asInstanceOf[Class[? <: Component]] -> updatedComponent))
      case None => this
    }
  }

  def exists[ComponentType <: Component](predicate: ComponentType => Boolean)(implicit classTag: ClassTag[ComponentType]): Boolean = {
    get[ComponentType].exists(predicate)
  }

  def has[ComponentType <: Component](implicit classTag: ClassTag[ComponentType]): Boolean = {
    components.contains(classTag.runtimeClass.asInstanceOf[Class[? <: Component]])
  }
}

object Entity {
  def unapply[ComponentType <: Component](entity: Entity)(implicit classTag: ClassTag[ComponentType]): Option[ComponentType] = {
    entity.get[ComponentType]
  }

  def apply(id: String, components: Component*): Entity = {
    Entity(id = id, components = components.map(c => c.getClass -> c).toMap)
  }

  def apply(components: Component*): Entity = {
    Entity(components = components.map(c => c.getClass -> c).toMap)
  }
}