package game.entity

import game.status.StatusEffect

import java.util.UUID
import scala.reflect.ClassTag

case class Entity(id: String = UUID.randomUUID().toString,
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
    copy(components = components + (component.getClass -> component))
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