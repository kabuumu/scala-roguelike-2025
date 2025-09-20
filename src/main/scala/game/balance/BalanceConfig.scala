package game.balance

object BalanceConfig {
  val MaxGearImpactRatio: Double = 3.5  // Kept for reference but not actively enforced
  val TargetUnarmouredTTKMin: Int = 8
  val TargetUnarmouredTTKMax: Int = 14
  val TargetBestGearTTKMax: Int = 50  // Increased to allow meaningful equipment progression
}