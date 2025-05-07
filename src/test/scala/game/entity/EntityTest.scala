package game.entity

import org.scalatest.funsuite.AnyFunSuite

class EntityTest extends AnyFunSuite {

  test("adding a new component should store it in the components map") {
    val entity = Entity()
    val component = new TestComponent
    val updatedEntity = entity.addComponent(component)

    assert(updatedEntity.components.contains(classOf[TestComponent]))
    assert(updatedEntity.components(classOf[TestComponent]) == component)
  }

  test("adding a component of the same type should overwrite the existing one") {
    val entity = Entity().addComponent(new TestComponent("old"))
    val newComponent = new TestComponent("new")
    val updatedEntity = entity.addComponent(newComponent)

    assert(updatedEntity.components.size == 1)
    assert(updatedEntity.components(classOf[TestComponent]) == newComponent)
  }

  test("adding a component should not modify the original entity") {
    val entity = Entity()
    val component = new TestComponent
    val updatedEntity = entity.addComponent(component)

    assert(!entity.components.contains(classOf[TestComponent]))
    assert(updatedEntity.components.contains(classOf[TestComponent]))
  }

  test("adding multiple components of different types should store all of them") {
    val entity = Entity()
    val component1 = new TestComponent
    val component2 = new AnotherTestComponent
    val updatedEntity = entity.addComponent(component1).addComponent(component2)

    assert(updatedEntity.components.size == 2)
    assert(updatedEntity.components.contains(classOf[TestComponent]))
    assert(updatedEntity.components.contains(classOf[AnotherTestComponent]))
  }

  test("removing a component should remove it from the components map") {
    val entity = Entity().addComponent(new TestComponent)
    val updatedEntity = entity.removeComponent[TestComponent]

    assert(!updatedEntity.components.contains(classOf[TestComponent]))
  }

  test("removing a component should not modify the original entity") {
    val entity = Entity().addComponent(new TestComponent)
    val updatedEntity = entity.removeComponent[TestComponent]

    assert(entity.components.contains(classOf[TestComponent]))
    assert(!updatedEntity.components.contains(classOf[TestComponent]))
  }

  test("removing a non-existent component should not affect the entity") {
    val entity = Entity()
    val updatedEntity = entity.removeComponent[TestComponent]

    assert(updatedEntity.components.isEmpty)
  }

  test("getting an existing component should return it") {
    val component = new TestComponent("data")
    val entity = Entity().addComponent(component)

    val retrievedComponent = entity.get[TestComponent]

    assert(retrievedComponent.isDefined)
    assert(retrievedComponent.get == component)
  }

  test("getting a non-existent component should return None") {
    val entity = Entity()

    val retrievedComponent = entity.get[TestComponent]

    assert(retrievedComponent.isEmpty)
  }

  test("updating an existing component should modify it") {
    val component = new TestComponent("old")
    val entity = Entity().addComponent(component)

    val updatedEntity = entity.update[TestComponent](c => new TestComponent("new"))
    val updatedComponent = updatedEntity.get[TestComponent]

    assert(updatedComponent.isDefined)
    assert(updatedComponent.get.data == "new")
  }

  test("updating a non-existent component should not affect the entity") {
    val entity = Entity()

    val updatedEntity = entity.update[TestComponent](c => new TestComponent("new"))

    assert(updatedEntity.components.isEmpty)
  }
}

class TestComponent(val data: String = "default") extends Component
class AnotherTestComponent extends Component
