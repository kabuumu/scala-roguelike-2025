package game

import upickle.default.ReadWriter

case class Sprite(x: Int, y: Int, layer: Int) derives ReadWriter
