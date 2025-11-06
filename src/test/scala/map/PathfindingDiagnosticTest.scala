package map

import org.scalatest.funsuite.AnyFunSuite
import game.Point

class PathfindingDiagnosticTest extends AnyFunSuite {
  
  test("Diagnostic: Start point is an obstacle") {
    val bounds = MapBounds(0, 100, 0, 100)
    val start = Point(50, 50)
    val target = Point(60, 60)
    
    // Start point is an obstacle
    val obstacles = Set(start)
    
    val path = PathGenerator.generatePathAroundObstacles(start, target, obstacles, width = 0, bounds)
    
    println("=== Test: Start point is obstacle ===")
    assert(path.nonEmpty, "Should fall back to direct line")
  }
  
  test("Diagnostic: Target point is an obstacle") {
    val bounds = MapBounds(0, 100, 0, 100)
    val start = Point(50, 50)
    val target = Point(60, 60)
    
    // Target point is an obstacle
    val obstacles = Set(target)
    
    val path = PathGenerator.generatePathAroundObstacles(start, target, obstacles, width = 0, bounds)
    
    println("=== Test: Target point is obstacle ===")
    assert(path.nonEmpty, "Should fall back to direct line")
  }
  
  test("Diagnostic: Start point outside bounds") {
    val bounds = MapBounds(0, 50, 0, 50)
    val start = Point(100, 100) // Way outside bounds
    val target = Point(25, 25)
    
    val obstacles = Set.empty[Point]
    
    val path = PathGenerator.generatePathAroundObstacles(start, target, obstacles, width = 0, bounds)
    
    println("=== Test: Start outside bounds ===")
    assert(path.nonEmpty, "Should fall back to direct line")
  }
  
  test("Diagnostic: Completely blocked path") {
    val bounds = MapBounds(0, 100, 0, 100)
    val start = Point(10, 50)
    val target = Point(90, 50)
    
    // Create a complete vertical wall blocking the path
    val obstacles = (0 to 100).map(y => Point(50, y)).toSet
    
    val path = PathGenerator.generatePathAroundObstacles(start, target, obstacles, width = 0, bounds)
    
    println("=== Test: Completely blocked ===")
    assert(path.nonEmpty, "Should fall back to direct line")
  }
}

