package map

/** Tile types for the zoomed-out overworld map. These represent large-scale
  * terrain features visible at 10x zoom out.
  */
enum OverworldTileType:
  case Ocean // Deep water
  case Water // Shallow water/lakes
  case Beach // Coastal areas
  case Plains // Grassland
  case Forest // Forested areas
  case Desert // Arid regions
  case Mountain // Elevated terrain
  case Village // Small settlement (white pixel)
  case Town // Medium settlement (yellow pixel)
  case City // Large settlement (red pixel)
  case Road // Main road between cities
  case Bridge // Bridge over water (main road)
  case Path // Secondary path from towns to roads
  case PathBridge // Bridge over water (secondary path)
  case Trail // Tertiary trail from villages
  case TrailBridge // Bridge over water (village trail)
