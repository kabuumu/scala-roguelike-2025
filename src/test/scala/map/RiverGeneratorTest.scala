package map

import game.Point
import org.scalatest.funsuite.AnyFunSuite

class RiverGeneratorTest extends AnyFunSuite {
  
  test("generateRiver creates river tiles") {
    val bounds = MapBounds(0, 10, 0, 10)
    val config = RiverConfig(
      startPoint = Point(50, 0), // Start at top center (tile coords)
      flowDirection = (0, 1),    // Flow downward
      length = 30,
      width = 0,                 // Single tile wide
      curviness = 0.0,           // Straight line
      bounds = bounds,
      seed = 12345
    )
    
    val river = RiverGenerator.generateRiver(config)
    
    assert(river.nonEmpty, "River should have tiles")
    assert(river.contains(config.startPoint), "River should contain start point")
    
    println(s"Generated river with ${river.size} tiles")
  }
  
  test("generateRiver respects bounds") {
    val bounds = MapBounds(0, 5, 0, 5)
    val config = RiverConfig(
      startPoint = Point(25, 25),
      flowDirection = (1, 1),
      length = 100,
      width = 0,
      curviness = 0.0,
      bounds = bounds,
      seed = 12345
    )
    
    val river = RiverGenerator.generateRiver(config)
    val (tileMinX, tileMaxX, tileMinY, tileMaxY) = bounds.toTileBounds()
    
    river.foreach { point =>
      assert(point.x >= tileMinX && point.x <= tileMaxX, s"X coordinate ${point.x} out of bounds")
      assert(point.y >= tileMinY && point.y <= tileMaxY, s"Y coordinate ${point.y} out of bounds")
    }
    
    println(s"All ${river.size} river tiles within bounds")
  }
  
  test("generateRiver creates curves with non-zero curviness") {
    val bounds = MapBounds(0, 10, 0, 10)
    
    // Generate straight river
    val straightConfig = RiverConfig(
      startPoint = Point(50, 10),
      flowDirection = (0, 1),
      length = 50,
      width = 0,
      curviness = 0.0,
      bounds = bounds,
      seed = 12345
    )
    val straightRiver = RiverGenerator.generateRiver(straightConfig)
    
    // Generate curved river
    val curvedConfig = straightConfig.copy(curviness = 0.3, seed = 67890)
    val curvedRiver = RiverGenerator.generateRiver(curvedConfig)
    
    // Curved river should explore more X coordinates (not perfectly vertical)
    val straightXRange = straightRiver.map(_.x).toSet.size
    val curvedXRange = curvedRiver.map(_.x).toSet.size
    
    assert(curvedXRange >= straightXRange, 
           s"Curved river should explore more X coordinates: curved=$curvedXRange, straight=$straightXRange")
    
    println(s"Straight river X range: $straightXRange tiles, Curved river X range: $curvedXRange tiles")
  }
  
  test("generateRiver with width creates wider rivers") {
    val bounds = MapBounds(0, 10, 0, 10)
    
    val narrowConfig = RiverConfig(
      startPoint = Point(50, 10),
      flowDirection = (0, 1),
      length = 20,
      width = 0,
      curviness = 0.0,
      bounds = bounds,
      seed = 12345
    )
    val narrowRiver = RiverGenerator.generateRiver(narrowConfig)
    
    val wideConfig = narrowConfig.copy(width = 2)
    val wideRiver = RiverGenerator.generateRiver(wideConfig)
    
    assert(wideRiver.size > narrowRiver.size, 
           s"Wide river should have more tiles: wide=${wideRiver.size}, narrow=${narrowRiver.size}")
    
    println(s"Narrow river: ${narrowRiver.size} tiles, Wide river: ${wideRiver.size} tiles")
  }
  
  test("generateRivers creates multiple rivers") {
    val bounds = MapBounds(0, 10, 0, 10)
    
    val configs = Seq(
      RiverConfig(Point(10, 10), (1, 1), 20, 0, 0.1, bounds, 1),
      RiverConfig(Point(100, 10), (0, 1), 20, 0, 0.1, bounds, 2),
      RiverConfig(Point(50, 100), (-1, 0), 20, 0, 0.1, bounds, 3)
    )
    
    val rivers = RiverGenerator.generateRivers(configs)
    
    assert(rivers.nonEmpty, "Should generate river tiles")
    println(s"Generated ${rivers.size} total river tiles from ${configs.size} rivers")
  }
  
  test("describeRivers provides AI-readable output") {
    val bounds = MapBounds(0, 5, 0, 5)
    val config = RiverConfig(
      startPoint = Point(25, 0),
      flowDirection = (0, 1),
      length = 30,
      width = 1,
      curviness = 0.2,
      bounds = bounds,
      seed = 12345
    )
    
    val river = RiverGenerator.generateRiver(config)
    val description = RiverGenerator.describeRivers(river, bounds)
    
    println("\n" + description + "\n")
    
    assert(description.contains("River Generation Summary"))
    assert(description.contains("Total river tiles"))
    assert(description.contains("Coverage"))
  }
  
  test("rivers are deterministic with same seed") {
    val bounds = MapBounds(0, 5, 0, 5)
    val config1 = RiverConfig(Point(25, 10), (0, 1), 30, 1, 0.3, bounds, 99999)
    val config2 = RiverConfig(Point(25, 10), (0, 1), 30, 1, 0.3, bounds, 99999)
    
    val river1 = RiverGenerator.generateRiver(config1)
    val river2 = RiverGenerator.generateRiver(config2)
    
    assert(river1 == river2, "Same seed should produce identical rivers")
    println(s"Deterministic generation verified: ${river1.size} tiles")
  }
  
  test("rivers produce different results with different seeds") {
    val bounds = MapBounds(0, 5, 0, 5)
    val config1 = RiverConfig(Point(25, 10), (0, 1), 30, 1, 0.3, bounds, 11111)
    val config2 = RiverConfig(Point(25, 10), (0, 1), 30, 1, 0.3, bounds, 22222)
    
    val river1 = RiverGenerator.generateRiver(config1)
    val river2 = RiverGenerator.generateRiver(config2)
    
    // With curviness, different seeds should produce different rivers
    assert(river1 != river2, "Different seeds should produce different rivers")
    println(s"Different seeds: river1=${river1.size} tiles, river2=${river2.size} tiles")
  }
  
  test("river stops when leaving bounds") {
    val bounds = MapBounds(0, 3, 0, 3)
    val config = RiverConfig(
      startPoint = Point(15, 15), // Center of bounds
      flowDirection = (1, 1),     // Diagonal - will leave bounds
      length = 100,               // Request long river
      width = 0,
      curviness = 0.0,
      bounds = bounds,
      seed = 12345
    )
    
    val river = RiverGenerator.generateRiver(config)
    val (tileMinX, tileMaxX, tileMinY, tileMaxY) = bounds.toTileBounds()
    
    // River should stop before reaching full length
    assert(river.size < config.length, "River should stop when leaving bounds")
    
    // All tiles should be within bounds
    river.foreach { point =>
      assert(point.x >= tileMinX && point.x <= tileMaxX)
      assert(point.y >= tileMinY && point.y <= tileMaxY)
    }
    
    println(s"River stopped at ${river.size} tiles (requested ${config.length})")
  }
}
