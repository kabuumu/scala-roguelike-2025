package testsupport

import game.{GameState, Point}
import game.Input
import game.Input.*
import game.Direction
import game.entity.*
import game.entity.EntityType
import game.entity.Health.*
import game.entity.EntityType.*
import org.scalatest.Assertions.*
import ui.GameController
import ui.UIState
import ui.UIState.UIState
import ui.GameController.frameTime

import scala.reflect.ClassTag

// Immutable scenario object
final case class GameStory(controller: GameController, tick: Int) {
  def gs: GameState = controller.gameState
  def ui: UIState = controller.uiState
  def time: Long = tick.toLong * frameTime

  // Advance one frame with optional input, returning a NEW GameStory
  def step(input: Option[Input]): GameStory =
    copy(controller = controller.update(input, time), tick = tick + 1)

  // Entry points for DSL sections
  def when: When = When(this)
  def `then`: Then = Then(this)
  def and: When = when
}

object GameStory {
  def begin(initialUI: UIState, initialGame: GameState, startTick: Int = 0): GameStory =
    GameStory(GameController(initialUI, initialGame), startTick)
}

// WHEN section: returns new GameStory values (pure, no mutation)
final case class When(s: GameStory) {

  def timePasses(ticks: Int): GameStory =
    (0 until ticks).foldLeft(s)((acc, _) => acc.step(None))

  object thePlayer {
    def moves(dir: Direction, steps: Int = 1): GameStory =
      (0 until steps).foldLeft(s)((acc, _) => acc.step(Some(Input.Move(dir))))

    // Enter the item usage menu (use-item state)
    def opensItems(): GameStory =
      s.step(Some(Input.UseItem))

    // Confirm the current selection or current target (context-sensitive)
    def confirmsSelection(): GameStory =
      s.step(Some(Input.UseItem))
  }

  object cursor {
    def moves(dir: Direction, times: Int = 1): GameStory =
      (0 until times).foldLeft(s)((acc, _) => acc.step(Some(Input.Move(dir))))

    def confirm(): GameStory =
      s.step(Some(Input.UseItem))
  }

  // Allow access to Then methods through and
  def and: StoryAndChain = StoryAndChain(s)
}

// THEN section: assertions return the same GameStory so chains can continue
final case class Then(s: GameStory) {

  // Generic component expectation for an entity
  final class ComponentExpect[C](private val where: String, private val opt: Option[C])(implicit ct: ClassTag[C]) {
    private def missing(): Nothing =
      fail(s"Expected $where to have component ${ct.runtimeClass.getSimpleName}, but it was missing")

    // Assert the component exists
    def exists(): GameStory = {
      assert(opt.nonEmpty, s"Expected $where to have component but it was missing")
      s
    }

    // Assert equals
    def is(expected: C): GameStory = {
      val actual = opt.getOrElse(missing())
      assert(actual == expected, s"Expected $where component to equal $expected but was $actual")
      s
    }

    // Assert predicate holds; optional clue for better failure messages
    def satisfies(pred: C => Boolean, clue: String = ""): GameStory = {
      val actual = opt.getOrElse(missing())
      assert(pred(actual), {
        val msg = if (clue.nonEmpty) s" ($clue)" else ""
        s"Expected $where component to satisfy predicate$msg, but was $actual"
      })
      s
    }

    // Extract the component value (failing if missing) to do arbitrary checks with ScalaTest matchers
    def value: C = opt.getOrElse(missing())
  }

  // Subject for entity by id
  final class EntityExpect(private val id: String) {
    private def targetOpt: Option[Entity] = s.gs.entities.find(_.id == id)
    private def target: Entity = targetOpt.getOrElse(fail(s"Entity '$id' not found"))

    def hasHealth(hp: Int): GameStory = {
      val actual = target.currentHealth
      assert(actual == hp, s"Expected entity '$id' health $hp but was $actual")
      s
    }

    def component[C](implicit ct: ClassTag[C]): ComponentExpect[C] =
      new ComponentExpect[C](s"entity '$id'", target.get[C])
  }

  // Subject for the player entity
  object thePlayer {
    private def player: Entity = s.gs.playerEntity

    // Optional convenience assertion using Movement
    def isAt(x: Int, y: Int): GameStory = {
      val pos = player.get[Movement].map(_.position)
      assert(pos.contains(Point(x, y)), s"Expected player at ($x,$y) but was $pos")
      s
    }

    def hasHealth(hp: Int): GameStory = {
      val actual = player.currentHealth
      assert(actual == hp, s"Expected player health $hp but was $actual")
      s
    }

    def component[C](implicit ct: ClassTag[C]): ComponentExpect[C] =
      new ComponentExpect[C]("player", player.get[C])
  }

  def enemy(id: String): EntityExpect = new EntityExpect(id)
  def entity(id: String): EntityExpect = new EntityExpect(id)

  def projectilesAre(count: Int): GameStory = {
    val actual = s.gs.entities.count(_.entityType == EntityType.Projectile)
    assert(actual == count, s"Expected $count projectiles but found $actual")
    s
  }

  def entityMissing(id: String): GameStory = {
    val missing = s.gs.entities.forall(_.id != id)
    assert(missing, s"Expected entity '$id' to be missing")
    s
  }

  def uiIsScrollTargetAt(x: Int, y: Int): GameStory = {
    s.ui match {
      case ss: ui.UIState.ScrollSelect =>
        assert(ss.cursor == Point(x, y), s"Expected scroll target at ($x,$y) but was ${ss.cursor}")
      case other =>
        fail(s"Expected UI state ScrollSelect but was $other")
    }
    s
  }

  // Allow chaining from Then back to When
  def and: StoryAndChain = StoryAndChain(s)
}

// Universal "and" chain that provides access to both When and Then methods
final case class StoryAndChain(s: GameStory) {
  // When methods
  def timePasses(ticks: Int): StoryAndChain = {
    val newStory = (0 until ticks).foldLeft(s)((acc, _) => acc.step(None))
    StoryAndChain(newStory)
  }

  object thePlayer {
    def moves(dir: Direction, steps: Int = 1): StoryAndChain = {
      val newStory = (0 until steps).foldLeft(s)((acc, _) => acc.step(Some(Input.Move(dir))))
      StoryAndChain(newStory)
    }

    def opensItems(): StoryAndChain = {
      val newStory = s.step(Some(Input.UseItem))
      StoryAndChain(newStory)
    }

    def confirmsSelection(): StoryAndChain = {
      val newStory = s.step(Some(Input.UseItem))
      StoryAndChain(newStory)
    }
  }

  object cursor {
    def moves(dir: Direction, times: Int = 1): StoryAndChain = {
      val newStory = (0 until times).foldLeft(s)((acc, _) => acc.step(Some(Input.Move(dir))))
      StoryAndChain(newStory)
    }

    def confirm(): StoryAndChain = {
      val newStory = s.step(Some(Input.UseItem))
      StoryAndChain(newStory)
    }
  }

  // Then methods (assertions)
  def projectilesAre(count: Int): StoryAndChain = {
    val actual = s.gs.entities.count(_.entityType == EntityType.Projectile)
    assert(actual == count, s"Expected $count projectiles but found $actual")
    StoryAndChain(s)
  }

  def entityMissing(id: String): StoryAndChain = {
    val missing = s.gs.entities.forall(_.id != id)
    assert(missing, s"Expected entity '$id' to be missing")
    StoryAndChain(s)
  }

  def uiIsScrollTargetAt(x: Int, y: Int): StoryAndChain = {
    s.ui match {
      case ss: ui.UIState.ScrollSelect =>
        assert(ss.cursor == Point(x, y), s"Expected scroll target at ($x,$y) but was ${ss.cursor}")
      case other =>
        fail(s"Expected UI state ScrollSelect but was $other")
    }
    StoryAndChain(s)
  }

  // Entry to Then for more complex assertions
  def `then`: Then = Then(s)

  // Allow continued chaining
  def and: StoryAndChain = this
}