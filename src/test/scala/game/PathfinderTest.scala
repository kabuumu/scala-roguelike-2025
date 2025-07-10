package game

import game.Point
import org.scalatest.funsuite.AnyFunSuite
import util.Pathfinder

class PathfinderTest extends AnyFunSuite {

  val start = Point(0, 0)
  val end = Point(3, 3)

  def isValidPath(path: Seq[Point], start: Point, end: Point, blockers: Seq[Point]): Boolean = {
    (path == Seq(start) && start == end) || // Handle start == end case
      (path.headOption.contains(start) &&
        path.lastOption.contains(end) &&
        path.sliding(2).forall {
          case Seq(a, b) =>
            (Math.abs(a.x - b.x) <= 1 && Math.abs(a.y - b.y) <= 1) && // adjacent
              !blockers.contains(b)
          case _ => true
        })
  }

  test("findPath returns a valid path when no blockers are present") {
    val blockers = Seq.empty[Point]
    val path = Pathfinder.findPath(start, end, blockers)
    assert(isValidPath(path, start, end, blockers))
  }

  test("findPath returns a path of only the start when start equals end") {
    val blockers = Seq.empty[Point]
    val path = Pathfinder.findPath(start, start, blockers)
    assert(path == Seq(start) || path.isEmpty)
  }

  test("findPath returns no valid path when no path is possible") {
    val blockers = Seq(Point(1, 0), Point(0, 1), Point(0, -1), Point(-1, 0))
    val path = Pathfinder.findPath(start, end, blockers)
    assert(path.isEmpty || !isValidPath(path, start, end, blockers))
  }

  test("findPath avoids blockers and finds a valid alternative path") {
    val blockers = Seq(Point(1, 1), Point(2, 2))
    val path = Pathfinder.findPath(start, end, blockers)
    assert(path.nonEmpty)
    assert(isValidPath(path, start, end, blockers))
    assert(!path.exists(blockers.contains))
  }
}