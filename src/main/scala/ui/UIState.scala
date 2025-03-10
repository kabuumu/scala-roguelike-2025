package ui

enum UIState:
  case Move
  case Attack(cursorX: Int, cursorY: Int)