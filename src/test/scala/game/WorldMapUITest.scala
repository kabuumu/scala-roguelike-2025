package game

import org.scalatest.funsuite.AnyFunSuite
import ui.{InputHandler, UIState}
import game.Input

class WorldMapUITest extends AnyFunSuite {
  
  test("OpenMap input transitions from Move to WorldMap state with showOverworld=false") {
    val initialState = UIState.Move
    val gameState = StartingState.startingGameState
    
    val (newState, _) = InputHandler.handleInput(Input.OpenMap, initialState, gameState)
    
    assert(newState == UIState.WorldMap(showOverworld = false), "OpenMap input should transition to WorldMap state with showOverworld=false")
  }
  
  test("Tab toggles showOverworld between false and true") {
    val gameState = StartingState.startingGameState
    
    // Start with showOverworld=false, Tab should toggle to true
    val (state1, _) = InputHandler.handleInput(Input.Tab, UIState.WorldMap(showOverworld = false), gameState)
    assert(state1 == UIState.WorldMap(showOverworld = true), "Tab should toggle showOverworld to true")
    
    // Tab again should toggle back to false
    val (state2, _) = InputHandler.handleInput(Input.Tab, UIState.WorldMap(showOverworld = true), gameState)
    assert(state2 == UIState.WorldMap(showOverworld = false), "Tab should toggle showOverworld back to false")
  }
  
  test("OpenMap (M key) closes the map from WorldMap state") {
    val gameState = StartingState.startingGameState
    
    val (state1, _) = InputHandler.handleInput(Input.OpenMap, UIState.WorldMap(showOverworld = false), gameState)
    assert(state1 == UIState.Move, "M should close the local map")
    
    val (state2, _) = InputHandler.handleInput(Input.OpenMap, UIState.WorldMap(showOverworld = true), gameState)
    assert(state2 == UIState.Move, "M should close the overworld map")
  }
  
  test("Cancel closes the map from WorldMap state") {
    val gameState = StartingState.startingGameState
    
    val (state1, _) = InputHandler.handleInput(Input.Cancel, UIState.WorldMap(showOverworld = false), gameState)
    assert(state1 == UIState.Move, "Cancel should close the map")
    
    val (state2, _) = InputHandler.handleInput(Input.Cancel, UIState.WorldMap(showOverworld = true), gameState)
    assert(state2 == UIState.Move, "Cancel should close the overworld map")
  }
  
  test("WorldMap state is defined with showOverworld parameter") {
    val localMap = UIState.WorldMap(showOverworld = false)
    val overworldMap = UIState.WorldMap(showOverworld = true)
    
    val _: UIState.UIState = localMap
    val _: UIState.UIState = overworldMap
    
    assert(!localMap.showOverworld)
    assert(overworldMap.showOverworld)
  }
}
