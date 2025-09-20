# Balance & Damage Testing

## Unified Formula
`finalDamage = max(1, baseDamage + attackerBonus - defenderResistance)`

- `attackerBonus`: equipment damage bonus + IncreaseDamage status effects
- `defenderResistance`: equipment DR + ReduceIncomingDamage status effects

## Metrics
- Unarmoured TTK target band: 8–14
- Best Gear TTK cap: 30
- Gear Impact Ratio (best / unarmoured): ≤ 3.5
- Damage floor: always at least 1

## Key Files
- `DamageCalculator.scala` (authoritative computation)
- `BalanceConfig.scala` (tuning constants)
- `ArmorBalanceTest.scala` and `EventMemoryConsistencyTest.scala`

## Adjusting Balance
Change armour DR or player base HP or enemy weapon damages. Run `sbt test` to verify invariants. If the floor (1) triggers too often, consider:
- Reducing flat DR further
- Introducing percentage mitigation after flat DR (future)
- Increasing enemy base damage slightly

## Future Improvements
- Percentage mitigation layer
- Diminishing returns for DR
- Analytics for average TTK across dungeon floors