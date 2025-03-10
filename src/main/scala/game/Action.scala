package game

enum Action:
  case Move(direction: Direction)
  case Attack(cursorX: Int, cursorY: Int)
