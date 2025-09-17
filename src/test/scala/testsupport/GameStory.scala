package testsupport

import game.{GameState, Point}
import game.Input
import game.Input.*
import game.Direction
import game.entity.*
import game.entity.EntityType
import game.entity.Health.*
import game.entity.Initiative.*
import game.entity.EntityType.*
import org.scalatest.Assertions.*
import ui.GameController
import ui.UIState
import ui.UIState.UIState
import ui.GameController.frameTime

import scala.reflect.ClassTag

// Immutable scenario object that supports both When and Then operations
final case class GameStory(controller: GameController, tick: Int) {
  def gameState: GameState = controller.gameState
  def uiState: UIState = controller.uiState
  def time: Long = tick.toLong * frameTime

  // Advance one frame with optional input, returning a NEW GameStory
  def step(input: Option[Input]): GameStory = {
    val newTick = tick + 1
    val newTime = newTick.toLong * frameTime
    copy(controller = controller.update(input, newTime), tick = newTick)
  }

  // WHEN: Actions that modify the game state
  def timePasses(ticks: Int): GameStory =
    (0 until ticks).foldLeft(this)((acc, _) => acc.step(None))

  object thePlayer {
    def moves(dir: Direction, steps: Int = 1): GameStory =
      (0 until steps).foldLeft(GameStory.this)((acc, _) => acc.step(Some(Input.Move(dir))))

    def opensItems(): GameStory =
      GameStory.this.step(Some(Input.UseItem))

    def confirmsSelection(): GameStory =
      GameStory.this.step(Some(Input.UseItem))

    // THEN: Player assertions
    def isAt(x: Int, y: Int): GameStory = {
      val pos = gameState.playerEntity.get[Movement].map(_.position)
      assert(pos.contains(Point(x, y)), s"Expected player at ($x,$y) but was $pos")
      GameStory.this
    }

    def hasHealth(hp: Int): GameStory = {
      val actual = gameState.playerEntity.currentHealth
      assert(actual == hp, s"Expected player health $hp but was $actual")
      GameStory.this
    }

    def component[C <: Component](implicit ct: ClassTag[C]): ComponentExpect[C] =
      new ComponentExpect[C]("player", gameState.playerEntity.get[C])
  }

  object cursor {
    def moves(dir: Direction, times: Int = 1): GameStory =
      (0 until times).foldLeft(GameStory.this)((acc, _) => acc.step(Some(Input.Move(dir))))

    def confirm(): GameStory =
      GameStory.this.step(Some(Input.UseItem))
  }

  // THEN: General assertions
  def projectilesAre(count: Int): GameStory = {
    val actual = gameState.entities.count(_.entityType == EntityType.Projectile)
    assert(actual == count, s"Expected $count projectiles but found $actual")
    this
  }

  def entityMissing(id: String): GameStory = {
    val missing = gameState.entities.forall(_.id != id)
    assert(missing, s"Expected entity '$id' to be missing")
    this
  }

  def debug(msg: String): GameStory = {
    val playerReady = gameState.playerEntity.isReady
    val playerInit = gameState.playerEntity.get[Initiative]
    val entityCount = gameState.entities.length
    val enemyCount = gameState.entities.count(_.entityType == EntityType.Enemy)
    val enemiesWithDeath = gameState.entities.filter(_.entityType == EntityType.Enemy).map(e => s"${e.id}:${e.has[DeathEvents]}")
    println(s"DEBUG $msg: UI=${uiState}, PlayerHealth=${gameState.playerEntity.currentHealth}, Tick=$tick, Time=$time, Ready=$playerReady, Init=$playerInit, Entities=$entityCount, Enemies=$enemyCount, EnemyDeath=$enemiesWithDeath")
    this
  }

  def uiIsListSelect(): GameStory = {
    uiState match {
      case _: UIState.ListSelect[_] => this
      case other => fail(s"Expected UI state ListSelect but was $other")
    }
  }

  def uiIsScrollTargetAt(x: Int, y: Int): GameStory = {
    uiState match {
      case ss: UIState.ScrollSelect =>
        assert(ss.cursor == Point(x, y), s"Expected scroll target at ($x,$y) but was ${ss.cursor}")
      case other =>
        fail(s"Expected UI state ScrollSelect but was $other")
    }
    this
  }

  def enemy(id: String): EntityExpect = new EntityExpect(id)
  def entity(id: String): EntityExpect = new EntityExpect(id)

  // Generic component expectation for an entity
  final class ComponentExpect[C <: Component](private val where: String, private val opt: Option[C])(implicit ct: ClassTag[C]) {
    private def missing(): Nothing =
      fail(s"Expected $where to have component ${ct.runtimeClass.getSimpleName}, but it was missing")

    def exists(): GameStory = {
      assert(opt.nonEmpty, s"Expected $where to have component but it was missing")
      GameStory.this
    }

    def is(expected: C): GameStory = {
      val actual = opt.getOrElse(missing())
      assert(actual == expected, s"Expected $where component to equal $expected but was $actual")
      GameStory.this
    }

    def satisfies(pred: C => Boolean, clue: String = ""): GameStory = {
      val actual = opt.getOrElse(missing())
      assert(pred(actual), {
        val msg = if (clue.nonEmpty) s" ($clue)" else ""
        s"Expected $where component to satisfy predicate$msg, but was $actual"
      })
      GameStory.this
    }

    def value: C = opt.getOrElse(missing())
  }

  // Subject for entity by id
  final class EntityExpect(private val id: String) {
    private def targetOpt: Option[Entity] = gameState.entities.find(_.id == id)
    private def target: Entity = targetOpt.getOrElse(fail(s"Entity '$id' not found"))

    def hasHealth(hp: Int): GameStory = {
      val actual = target.currentHealth
      assert(actual == hp, s"Expected entity '$id' health $hp but was $actual")
      GameStory.this
    }

    def component[C <: Component](implicit ct: ClassTag[C]): ComponentExpect[C] =
      new ComponentExpect[C](s"entity '$id'", target.get[C])
  }
}

object GameStory {
  import game.save.TestSaveGameSystem
  
  def begin(initialUI: UIState, initialGame: GameState, startTick: Int = 0): GameStory =
    GameStory(GameController(initialUI, initialGame, saveService = TestSaveGameSystem), startTick)
}