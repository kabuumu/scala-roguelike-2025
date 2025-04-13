package dungeongenerator.output

import dungeongenerator._
import dungeongenerator.generator.Entity._
import dungeongenerator.generator.{DefaultDungeonGeneratorConfig, Dungeon, DungeonGenerator, Point}
import dungeongenerator.pathfinder.PathFinder.locationPredicate
import dungeongenerator.pathfinder.nodefinders._
import dungeongenerator.pathfinder.{DungeonCrawler, Node, PathFinder}
import scalafx.Includes._
import scalafx.application.JFXApp3
import scalafx.scene.Scene
import scalafx.scene.canvas.{Canvas, GraphicsContext}
import scalafx.scene.input.{KeyCode, KeyEvent}
import scalafx.scene.paint.Color
import scalafx.scene.paint.Color._

import scala.util.Try

object Main extends JFXApp3 {
  val Size = 10
  val CanvasSize = 1000

  override def start(): Unit = {
    val nodeFinders: Set[NodeFinder] = Set(
//      AdjacentFloorNodeFinder,
//      DoorNodeFinder,
//      KeyNodeFinder,
//      SwitchNodeFinder,
//      TeleporterNodeFinder,
//      BossKeyNodeFinder
    )

    val startTime = System.currentTimeMillis()
    val possibleDungeons = DungeonGenerator.generatePossibleDungeonsLinear(
      config = DefaultDungeonGeneratorConfig
    )
    val endTime = System.currentTimeMillis()
    println(s"Generated ${possibleDungeons.size} dungeons in ${endTime - startTime} milliseconds.")

    val floorCanvas = new Canvas(CanvasSize, CanvasSize)
    val entityCanvas = new Canvas(CanvasSize, CanvasSize)

    val floorGc = floorCanvas.graphicsContext2D
    val entityGc = entityCanvas.graphicsContext2D

    var dungeonIndex = 0
    var pathIndex = 0
    var path: Seq[Node] = Nil

    def dungeon = possibleDungeons.toSet.toSeq(dungeonIndex)

    floorGc.clearRect(0, 0, CanvasSize, CanvasSize)

    drawDungeon(floorGc, entityGc, dungeon)

    def generatePath(): Seq[Node] = {
      val startPathTime = System.currentTimeMillis()
      val path = PathFinder.findPath(
        startingNode = dungeon.longestRoomPath.head,
        targetNodePredicate = locationPredicate(dungeon.longestRoomPath.last.currentCrawler.location),
        pathFailureTriggers = Set.empty,
        nodeFinders = nodeFinders
      )

      val endPathTime = System.currentTimeMillis()

      println(s"${endPathTime - startPathTime} millis to generate path of length ${path.size}.")

      path
    }

    path = generatePath()

    def dungeonState = Try(path(pathIndex).dungeonState).getOrElse(dungeon)

    Try(drawPathStartAndEnd(floorGc, Seq(path.head, path.last)))
    Try(drawPath(floorGc, path(pathIndex)))
    Try(drawPathOutline(floorGc, path))


    stage = new JFXApp3.PrimaryStage {
      title.value = "Hello Stage"
      width = CanvasSize
      height = CanvasSize
      scene = new Scene {
        content = Seq(floorCanvas, entityCanvas)
        onKeyPressed = { (event: KeyEvent) =>
          event.code match {
            case KeyCode.Right =>
              if (dungeonIndex < possibleDungeons.size - 1) {
                dungeonIndex += 1
                pathIndex = 0
                path = generatePath()
              }
            case KeyCode.Left =>
              if (dungeonIndex > 0) {
                dungeonIndex -= 1
                pathIndex = 0
                path = generatePath()
              }
            case KeyCode.Up =>
              if (pathIndex < path.size - 1) pathIndex += 1
            case KeyCode.Down =>
              if (pathIndex > 0) pathIndex -= 1
            case _ =>
          }

          //          println(s"Dungeon path length = ${dungeon.longestRoomPath.size}")
          floorGc.clearRect(0, 0, CanvasSize, CanvasSize)
          entityGc.clearRect(0, 0, CanvasSize, CanvasSize)
          drawDungeon(floorGc, entityGc, dungeonState)
          drawPathStartAndEnd(entityGc, Seq(path.head, path.last))
          drawPath(entityGc, path(pathIndex))

          val currentCrawler = path(pathIndex).currentCrawler
          val currentTiles = dungeonState.entities.collect {
            case (point, entity) if point == currentCrawler.location => entity
          }

          //          println(s"$currentCrawler -> $currentTiles")
        }
      }
    }

  }


  def drawDungeon(floorGc: GraphicsContext, entityGc: GraphicsContext, dungeon: Dungeon): Unit = {
    dungeon.entities.foreach {
      case (Point(x, y), entity) =>
        val (layer, colour) = entity match {
          case Wall => (floorGc, Color.Grey.darker)
          case Floor => (floorGc, Color.Grey)
          case Door(Some(KeyLock)) => (entityGc, Color.DarkRed)
          case Door(Some(BossKeyLock)) => (entityGc, Color.DarkRed.darker)
          case Door(Some(SwitchLock)) => (entityGc, Color.DarkRed.brighter)
          case Door(None) => (entityGc, Color.DarkGrey)
          case Key => (entityGc, Color.Gold)
          case BossKey => (entityGc, Color.Gold.brighter)
          case Switch(_) => (entityGc, Color.Purple)
          case Teleporter(_) => (entityGc, Color.Teal)
          case _ => (floorGc, Color.Transparent)
        }

        layer.setFill(colour)
        layer.fillRect(x * Size, (y * Size), Size, Size)
    }
  }

  def drawPathOutline(g2d: GraphicsContext, path: Seq[Node]): Unit = {
    path.reduce[Node]{
      case(Node(DungeonCrawler(Point(x1, y1), _, _), _), nextNode @ Node(DungeonCrawler(Point(x2, y2), _, _), _)) =>
        g2d.strokeLine(x1 * Size + 8, y1 * Size + 8, x2 * Size + 8, y2 * Size + 8)
        nextNode
    }
  }

  def drawPath(g2d: GraphicsContext, node: Node): Unit =
    node match {
      case Node(DungeonCrawler(Point(x, y), _, _), _) =>
        g2d.setFill(Color.Blue)
        g2d.fillRect(x * Size, y * Size, Size, Size)
    }

  def drawPathStartAndEnd(g2d: GraphicsContext, path: Seq[Node]): Unit = {
    path.foreach {
      case Node(DungeonCrawler(Point(x, y), _, _), _) =>
        g2d.setFill(Color.LightBlue)
        g2d.fillRect(x * Size, y * Size, Size, Size)
    }
  }
}
