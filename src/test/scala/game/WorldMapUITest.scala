package game

import org.scalatest.funsuite.AnyFunSuite
import ui.{InputHandler, UIState}
import game.Input

class WorldMapUITest extends AnyFunSuite {
  
  test("OpenMap input transitions from Move to WorldMap state") {
    val initialState = UIState.Move
    val gameState = StartingState.startingGameState
    
    val (newState, _) = InputHandler.handleInput(Input.OpenMap, initialState, gameState)
    
    assert(newState == UIState.WorldMap, "OpenMap input should transition to WorldMap state")
  }
  
  test("WorldMap state exits to Move state on any input") {
    val worldMapState = UIState.WorldMap
    val gameState = StartingState.startingGameState
    
    // Test with various inputs - all should return to Move state
    val inputs = Seq(
      Input.Move(Direction.Up),
      Input.UseItem,
      Input.Cancel,
      Input.Action,
      Input.OpenMap
    )
    
    inputs.foreach { input =>
      val (newState, _) = InputHandler.handleInput(input, worldMapState, gameState)
      assert(newState == UIState.Move, s"WorldMap should exit to Move state on input: $input")
    }
  }
  
  test("WorldMap state is defined") {
    // Simple test to verify the WorldMap state exists and can be instantiated
    val mapState = UIState.WorldMap
    assert(mapState != null, "WorldMap state should be defined")
    assert(mapState.isInstanceOf[UIState.UIState], "WorldMap should be a UIState")
  }
}
