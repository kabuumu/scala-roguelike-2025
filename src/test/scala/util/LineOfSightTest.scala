package scala.util

import game.Point
import org.scalatest.funsuite.AnyFunSuite

class LineOfSightTest extends AnyFunSuite {

  test("getVisiblePoints: clear area") {
    val start = Point(5, 5)
    val sightRange = 2
    val isBlocked: Point => Boolean = _ => false
    
    val visible = LineOfSight.getVisiblePoints(start, isBlocked, sightRange)
    
    // Range calculation: x from 3 to 7, y from 3 to 7. (5x5 = 25 points)
    assert(visible.size == 25)
    assert(visible.contains(start))
    assert(visible.contains(Point(3, 3)))
    assert(visible.contains(Point(7, 7)))
  }

  test("getVisiblePoints: blocked by wall") {
    val start = Point(5, 5)
    val sightRange = 5
    // Wall at x=6
    val isBlocked: Point => Boolean = p => p.x == 6
    
    val visible = LineOfSight.getVisiblePoints(start, isBlocked, sightRange)
    
    // Should see points where x <= 6 (but not x=6 as a blocker if it's intermediate?)
    // Looking at implementation: if ((x != end.x || y != end.y) && isBlocked(Point(x, y))) return false;
    // If the line hits (6,5), it stops. 
    // So points with x < 6 should be visible. Points with x >= 6 might not be if they are beyond the wall.
    assert(visible.contains(Point(5, 5)))
    // Point (7, 5) is behind the wall at (6, 5). Should NOT be visible.
    assert(!visible.contains(Point(7, 5)))
  }

  test("getBresenhamLine: simple line") {
    val start = Point(0, 0)
    val end = Point(2, 2)
    val line = LineOfSight.getBresenhamLine(start, end)
    
    assert(line.contains(Point(0, 0)))
    assert(line.contains(Point(1, 1)))
    assert(line.contains(Point(2, 2)))
  }
}
