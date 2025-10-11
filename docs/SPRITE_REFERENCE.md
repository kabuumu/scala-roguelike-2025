# Sprite Reference Guide

This document provides a comprehensive reference for all 1,078 sprites in the `assets/sprites/sprites.png` sprite sheet.

## Overview

- **Sprite Sheet Dimensions**: 784×352 pixels (49 columns × 22 rows)
- **Individual Sprite Size**: 16×16 pixels
- **Total Sprites**: 1,078 (1,059 with visible content, 19 empty/transparent)
- **File**: `src/main/scala/data/Sprites.scala`

## Sprite Layers

Sprites are organized into four rendering layers:

- **floorLayer (0)**: Ground tiles, terrain, basic environment
- **backgroundLayer (1)**: Background effects like corpses, blood stains
- **entityLayer (2)**: Characters, monsters, walls, objects
- **uiLayer (3)**: UI elements, items, hearts, keys, text

## Sprite Organization by Row

### Row 0: Floor Tiles and NPCs (47 sprites)
Floor tiles, terrain variants, and character sprites including:
- Basic floor types: `maybeFloorSprite`, `floorSprite`, `crackedFloorSprite`, `wetFloorSprite`
- Natural terrain: `grassSprite`, `tallGrassSprite`, `flowersSprite`
- Brick variants: `brickRedSprite`, `brickDarkSprite`, `brickGreySprite`
- Stone types: `darkStoneSprite`, `cobblestoneSprite`, `stoneTileSprite`, `marbleSprite`
- Environmental: `sandSprite`, `dirtSprite`, `mudSprite`, `iceSprite`, `snowSprite`, `lavaSprite`, `ashSprite`
- Characters: `playerSprite`, `enemySprite`, `npcSprite`, `merchantSprite`, `guardSprite`, `mageSprite`, `warriorSprite`, `rogueSprite`
- Special NPCs: `nobleSprite`, `kingSprite`, `priestSprite`, `blacksmithSprite`, `farmerSprite`, `fishermanSprite`, `hunterSprite`, `traderSprite`, `alchemistSprite`, `wizardSprite`, `witchSprite`, `sageSprite`

### Row 1: Objects and Equipment (47 sprites)
Environmental objects and equipment items:
- Plants: `shrubSprite`, `bushSprite`, `treeSprite`, `mushroomSprite`, `fungusSprite`, `vinesSprite`, `mossSprite`, `lichenSprite`, `fernSprite`
- Rocks: `rocksSprite`, `boulderSprite`, `pebblesSprite`
- Containers: `barrelSprite`, `crateSprite`, `sackSprite`, `urnSprite`, `vaseSprite`, `potSprite`, `jarSprite`, `basketSprite`, `bucketSprite`
- Furniture: `anvilSprite`, `forgeSprite`, `workbenchSprite`, `tableSprite`, `chairSprite`, `bedSprite`, `bookshelfSprite`
- Armor: `armorSprite`, `chainmailArmorSprite`, `plateArmorSprite`, `helmSprite`, `shieldSprite`, `bucklerSprite`
- Equipment slots: `leatherHelmetSprite`, `ironHelmetSprite`, `leatherBootsSprite`, `ironBootsSprite`, `leatherGlovesSprite`, `ironGlovesSprite`

### Row 2: Dungeon Elements and Boss (45 sprites)
Structural elements and boss sprites:
- Rocks and materials: `smallStoneSprite`, `mediumStoneSprite`, `largeStoneSprite`, `roundStoneSprite`, `boulderRockSprite`, `rubbleSprite`, `debrisSprite`, `gravelSprite`, `brickSprite`
- Wood: `plankSprite`, `beamSprite`, `logSprite`, `stumpSprite`
- Light sources: `torchSprite`, `lanternSprite`, `candleSprite`, `brazierSprite`, `firepitSprite`, `campfireSprite`
- Structures: `altarSprite`, `statueSprite`, `monumentSprite`, `pillarSprite`
- Boss sprites: `bossSpriteTL`, `bossSpriteTR`, `bossSpriteBL`, `bossSpriteBR`
- Interactive: `gateSprite`, `fenceSprite`, `wall_2_30Sprite`, `barrierSprite`, `ladderSprite`, `ropeSprite`, `chainSprite`, `leverSprite`, `buttonSprite`, `wheelSprite`, `gearSprite`, `pipeSprite`, `valveSprite`, `machineSprite`, `deviceSprite`, `panelSprite`

### Row 3: Monsters and Creatures (44 sprites)
Undead, demons, and mythical creatures:
- Undead: `skeletonSprite`, `zombieSprite`, `ghostSprite`, `wraithSprite`, `demonSprite`, `impSprite`, `devilSprite`, `fiendSprite`, `hellspawnSprite`
- Humanoid monsters: `goblinSprite`, `orcSprite`, `trollSprite`, `ogreSprite`, `giantSprite`, `cyclopsSprite`
- Mythical: `centaurSprite`, `satyrSprite`, `harpySprite`, `dragonSprite`, `wyvernSprite`, `drakeSprite`, `serpentSprite`, `basiliskSprite`, `cockatriceSprite`, `griffinSprite`, `hippogriffSprite`, `pegasusSprite`, `unicornSprite`, `chimeraSprite`, `hydraSprite`, `manticoreSprite`, `sphinxSprite`, `phoenixSprite`, `rocSprite`, `krakenSprite`, `leviathanSprite`, `behemothSprite`, `golemSprite`, `elementalSprite`, `spiritSprite`, `shadeSprite`, `wraith_3_47Sprite`, `bansheeSprite`

### Row 4: Walls and Water (47 sprites)
Wall types and liquid terrain:
- Walls: `woodWallSprite`, `stoneWallSprite`, `brickWallSprite`, `rockWallSprite`, `caveWallSprite`, `metalWallSprite`, `crystalWallSprite`, `iceWallSprite`
- Water: `waterDeepSprite`, `waterMidSprite`, `waterShallowSprite`, `lavaSprite`
- Hazards: `pitSprite`, `chasmSprite`, `abyssSprite`, `portalSprite`, `vortexSprite`, `riftSprite`
- Navigation: `stairs_4_21Sprite`, `ladder_4_22Sprite`, `trapdoorSprite`, `hatchSprite`, `grateSprite`, `drainSprite`
- Water features: `fountainSprite`, `wellSprite`, `poolSprite`, `pondSprite`, `stream_4_31Sprite`, `river_4_32Sprite`, `waterfallSprite`, `rapidsSprite`, `whirlpoolSprite`, `geyserSprite`, `springSprite`, `bogSprite`, `swampSprite`, `marshSprite`, `mireSprite`, `quicksandSprite`, `tarSprite`, `slime_4_44Sprite`, `ooze_4_45Sprite`, `acid_4_46Sprite`, `poisonSprite`

### Row 5: Environment and Resources (49 sprites)
Natural environments and gemstones:
- Terrain: `forestSprite`, `grasslandSprite`, `desertSprite`, `tundraSprite`, `mountainSprite`, `hillSprite`, `cliffSprite`, `canyonSprite`
- Water edges: `waterEdgeSprite`, `waterShallow_5_9Sprite`, `waterDeep_5_10Sprite`, `bridgeSprite`, `pathSprite`, `roadSprite`, `cobblestone_5_14Sprite`, `pavementSprite`
- Crystals: `crystalBlueSprite`, `crystalRedSprite`, `crystalGreenSprite`, `crystalYellowSprite`, `crystalPurpleSprite`, `crystalWhiteSprite`, `crystalBlackSprite`
- Gems: `gemSmallSprite`, `gemMediumSprite`, `diamondSprite`, `rubySprite`, `emeraldSprite`, `sapphireSprite`, `amethystSprite`, `topazSprite`, `opalSprite`, `pearlSprite`, `amberSprite`, `jadeSprite`, `onyxSprite`, `obsidianSprite`, `quartzSprite`
- Ores: `coalSprite`, `ironSprite`, `copperSprite`, `silverSprite`, `goldSprite`, `platinumSprite`, `mithrilSprite`, `adamantSprite`, `oreSprite`, `ingotSprite`

### Row 6: Weapons (49 sprites)
Comprehensive weapon arsenal:
- Swords: `daggerSprite`, `knifeSprite`, `shortswordSprite`, `swordSprite`, `longswordSprite`, `greatswordSprite`, `scimitarSprite`, `katanaSprite`, `rapierSprite`, `saberSprite`, `cutlassSprite`, `falchionSprite`, `claymoreSprite`, `bladeSprite`
- Axes: `axeSprite`, `handaxeSprite`, `battleaxeSprite`, `waraxeSprite`
- Polearms: `halberdSprite`, `poleaxeSprite`, `pikeSprite`, `spearSprite`, `javelinSprite`, `lanceSprite`, `tridentSprite`, `quarterstaffSprite`
- Blunt: `maceSprite`, `morningstarSprite`, `flailSprite`, `clubSprite`, `hammerSprite`, `warhammerSprite`, `maulSprite`
- Ranged: `bow_6_33Sprite`, `longbowSprite`, `shortbowSprite`, `crossbowSprite`, `bowSprite`, `arbalestSprite`, `slingSprite`, `blowgunSprite`
- Exotic: `dartSprite`, `shurikenSprite`, `chakramSprite`, `boomerangSprite`, `whipSprite`, `nunchakuSprite`, `flail_6_46Sprite`, `chainSprite`

### Row 7: Armor and Clothing (48 sprites)
Complete armor and clothing sets:
- Armor types: `clothArmorSprite`, `leatherArmorSprite`, `studdedSprite`, `hideSprite`, `chainmailSprite`, `scalemailSprite`, `ringmailSprite`, `splintSprite`, `plateSprite`, `fullPlateSprite`
- Shields: `shield_7_10Sprite`, `buckler_7_11Sprite`, `towerSprite`, `kiteSprite`, `roundSprite`
- Headgear: `helmet_7_15Sprite`, `capSprite`, `coifSprite`, `cowlSprite`, `hoodSprite`, `maskSprite`, `visorSprite`, `faceplateSprite`
- Hand protection: `gauntletsSprite`, `glovesSprite`, `bracersSprite`, `vambracesSprite`
- Leg protection: `greavesSprite`, `sabatonsSprite`, `cuissesSprite`
- Body armor: `pauldronsSprite`, `cuirassSprite`, `breastplateSprite`, `chestplateSprite`, `gorgetSprite`
- Clothing: `capeSprite`, `cloakSprite`, `robeSprite`, `tunicSprite`, `surcoatSprite`, `tabardSprite`, `vestSprite`, `jerkinSprite`, `doubletSprite`, `hauberkSprite`

### Row 8: Small Creatures (49 sprites)
Insects, rodents, and small monsters:
- Insects: `batSprite`, `spiderSprite`, `scorpionSprite`, `beetleSprite`, `antSprite`, `waspSprite`, `beeSprite`, `hornetSprite`, `mothSprite`, `butterflySprite`, `dragonflySprite`, `centipedeSprite`, `millipedeSprite`
- Worms: `wormSprite`, `slugSprite`, `snailSprite`, `leechSprite`, `tickSprite`, `fleaSprite`, `miteSprite`, `maggotSprite`, `larvaSprite`, `cocoonSprite`, `eggSprite`, `pupaSprite`, `chrysalisSprite`, `hiveSprite`
- Reptiles: `snakeSprite`, `slimeSprite`, `slimeletSprite`, `ooze_8_30Sprite`
- Rodents: `ratSprite`, `mouseSprite`, `voleSprite`, `shrewSprite`, `moleSprite`
- Small mammals: `badgerSprite`, `weaselSprite`, `ferretSprite`, `otterSprite`, `beaverSprite`, `rabbitSprite`, `hareSprite`, `squirrelSprite`, `chipmunkSprite`, `hedgehogSprite`, `porcupineSprite`, `armadilloSprite`, `critterSprite`

### Row 9: Medium Creatures (49 sprites)
Wild animals and beasts:
- Canines: `wolfSprite`, `dogSprite`, `foxSprite`, `coyoteSprite`, `jackalSprite`, `hyenaSprite`
- Large predators: `bearSprite`, `lionSprite`, `tigerSprite`, `leopardSprite`, `pantherSprite`, `cheetahSprite`
- Felines: `lynxSprite`, `bobcatSprite`, `wildcatSprite`, `jaguarSprite`, `pumaSprite`, `caracalSprite`, `servalSprite`, `mongooseSprite`
- Boars: `boarSprite`, `pigSprite`, `warthogSprite`
- Deer: `deerSprite`, `elkSprite`, `mooseSprite`, `caribouSprite`, `reindeerSprite`, `antelopeSprite`, `gazelleSprite`, `impalaSprite`
- Bovines: `buffaloSprite`, `bisonSprite`, `oxSprite`, `bullSprite`, `cowSprite`, `yakSprite`
- Domesticated: `goatSprite`, `sheepSprite`, `ramSprite`, `llamaSprite`, `alpacaSprite`, `camelSprite`, `horseSprite`, `ponySprite`, `donkeySprite`, `muleSprite`, `zebraSprite`

### Row 10: Food and Currency (49 sprites)
Consumables and treasure:
- Meats: `appleSprite`, `breadSprite`, `cheeseSprite`, `meatSprite`, `fishSprite`, `chickenSprite`, `turkeySprite`, `hamSprite`, `baconSprite`, `sausageSprite`
- Dairy: `eggSprite`, `milkSprite`, `butterSprite`, `honeySprite`, `jamSprite`, `jellySprite`
- Baked goods: `pieSprite`, `cakeSprite`, `cookieSprite`, `biscuitSprite`, `crackerSprite`, `pastrySprite`, `tartSprite`, `muffinSprite`, `sconeSprite`, `bagelSprite`, `donutSprite`, `croissantSprite`, `pretzelSprite`, `waferSprite`, `waffleSprite`, `pancakeSprite`
- Raw foods: `fruitSprite`, `vegetableSprite`, `grainSprite`, `riceSprite`, `wheatSprite`, `cornSprite`, `barleySprite`
- Health: `emptyHeartSprite`, `halfHeartSprite`, `fullHeartSprite`
- Currency: `coinSprite`, `bronzeSprite`, `treasureSprite`

### Row 11: Books, Keys, and Tools (49 sprites)
Knowledge items and keys:
- Books: `scroll_11_1Sprite`, `tomeSprite`, `grimoireSprite`, `codexSprite`, `manuscriptSprite`, `parchmentSprite`, `paperSprite`, `letterSprite`
- Doors: `yellowDoorSprite`, `blueDoor_11_10Sprite`, `redDoor_11_11Sprite`
- Navigation: `mapSprite`, `chartSprite`, `atlasSprite`, `compassSprite`, `sextantSprite`, `astrolabeSprite`, `telescopeSprite`, `spyglassSprite`, `magnifierSprite`, `lensSprite`, `prismSprite`, `mirrorSprite`
- Time: `hourglassSprite`, `sundialSprite`, `clockSprite`, `watchSprite`
- Projectiles: `projectileSprite`, `arrow_11_29Sprite`, `boltSprite`
- Keys: `yellowKeySprite`, `blueKeySprite`, `redKeySprite`, `purpleKeySprite`, `whiteKeySprite`, `blackKeySprite`, `keySprite`
- Tools: `lockpickSprite`, `crowbarSprite`, `pickSprite`, `shovelSprite`, `hoeSprite`, `rakeSprite`, `sickleSprite`, `scytheSprite`, `pitchforkSprite`, `toolSprite`

### Row 12: Potions and Alchemy (48 sprites)
Bottles, potions, and reagents:
- Containers: `vialSprite`, `flaskSprite`, `bottleSprite`, `phialSprite`, `elixirSprite`, `tonicSprite`, `draughtSprite`, `brewSprite`, `mixtureSprite`, `concoctionSprite`
- Potions: `potionSprite`, `antidoteSprite`, `remedySprite`, `cureSprite`, `salveSprite`, `ointmentSprite`, `balmSprite`
- Liquids: `oilSprite`, `perfumeSprite`, `essenceSprite`, `extractSprite`, `tinctureSprite`, `serumSprite`, `toxinSprite`, `poison_12_27Sprite`, `venomSprite`, `acid_12_29Sprite`
- Materials: `reagentSprite`, `catalystSprite`, `solventSprite`, `solutionSprite`, `gelSprite`, `pasteSprite`, `powderSprite`, `crystal_12_37Sprite`, `shardSprite`, `fragmentSprite`, `dust_12_40Sprite`, `ash_12_41Sprite`, `saltSprite`, `spiceSprite`, `herbSprite`, `rootSprite`, `seedSprite`, `flowerSprite`, `leafSprite`

### Row 13: Magic Items (49 sprites)
Magical implements and artifacts:
- Wands: `wandSprite`, `rodSprite`, `staffSprite`, `scepterSprite`
- Orbs: `orbSprite`, `crystalSprite`, `sphereSprite`, `globeSprite`, `ballSprite`
- Jewelry: `ringSprite`, `amuletSprite`, `talismanSprite`, `charmSprite`
- Totems: `totemSprite`, `fetishSprite`, `idolSprite`, `figurineSprite`, `statuetteSprite`
- Artifacts: `trophySprite`, `relicSprite`, `artifactSprite`, `heirloomSprite`, `antiqueSprite`
- Scrolls: `scroll_13_24Sprite`, `spellbookSprite`, `runestoneSprite`, `tabletSprite`, `obeliskSprite`, `monolithSprite`, `pillar_13_30Sprite`, `column_13_31Sprite`, `totem_13_32Sprite`
- Symbols: `ankhSprite`, `crossSprite`, `pentacleSprite`, `hexagramSprite`, `runeSprite`, `glyphSprite`, `sigilSprite`, `symbolSprite`, `markSprite`, `emblemSprite`, `insigniaSprite`, `badgeSprite`, `crestSprite`, `sealSprite`, `stampSprite`, `tokenSprite`, `medallionSprite`

### Row 14: UI Symbols (49 sprites)
User interface elements:
- Math: `checkSprite`, `crossSprite`, `plusSprite`, `minusSprite`, `multiplySprite`, `divideSprite`, `equalsSprite`, `percentSprite`
- Currency: `dollarSprite`, `poundSprite`, `euroSprite`, `yenSprite`
- Punctuation: `questionSprite`, `exclamationSprite`, `periodSprite`, `commaSprite`, `colonSprite`, `semicolonSprite`, `apostropheSprite`, `quoteSprite`
- Pointers: `cursorSprite`, `pointerSprite`, `handSprite`, `fingerSprite`, `arrow_14_32Sprite`, `chevronSprite`, `caretSprite`, `angleSprite`
- Brackets: `bracketSprite`, `parenSprite`, `braceSprite`
- Shapes: `squareSprite`, `circleSprite`, `triangleSprite`, `diamondSprite`, `starSprite`, `asteriskSprite`, `hashSprite`, `atSprite`, `ampersandSprite`, `tildeSprite`

### Row 15: Effects and Deaths (49 sprites)
Visual effects and status indicators:
- Death: `deadSprite`, `corpseSprite`, `remainsSprite`, `bonesSprite`, `skullSprite`
- Blood: `bloodSprite`, `goreSprite`, `splatterSprite`, `stainSprite`, `puddleSprite`, `poolSprite`
- Damage: `slashSprite`, `cutSprite`, `woundSprite`, `scarSprite`, `bruiseSprite`, `burnSprite`, `scorchSprite`, `charSprite`
- Particles: `ash_15_21Sprite`, `smoke_15_22Sprite`, `fireSprite`, `flameSprite`, `spark_15_25Sprite`, `emberSprite`, `glow_15_27Sprite`, `shimmerSprite`, `glitterSprite`, `sparkleSprite`, `twinkleSprite`, `flashSprite`, `burstSprite`, `explosionSprite`, `impactSprite`
- Waves: `shockwaveSprite`, `rippleSprite`, `waveSprite`, `pulseSprite`, `auraSprite`, `haloSprite`, `ring_15_42Sprite`, `circle_15_43Sprite`, `spiralSprite`, `vortex_15_45Sprite`, `swirlSprite`, `tornadoSprite`, `whirlwindSprite`

### Row 16: Dungeon Walls (49 sprites)
Wall and room connections:
- Corners: `cornerTLSprite`, `cornerTRSprite`, `cornerBLSprite`, `cornerBRSprite`
- Edges: `edgeTopSprite`, `edgeBottomSprite`, `edgeLeftSprite`, `edgeRightSprite`
- Junctions: `tJuncUpSprite`, `tJuncDownSprite`, `tJuncLeftSprite`, `tJuncRightSprite`, `crossSprite`
- Ends: `endNorthSprite`, `endSouthSprite`, `endEastSprite`, `endWestSprite`
- Passages: `corridorSprite`, `passageSprite`, `hallwaySprite`, `tunnelSprite`, `alcoveSprite`, `nicheSprite`, `recessSprite`
- Rooms: `chamberSprite`, `roomSprite`, `vault_16_26Sprite`, `cryptSprite`, `tombSprite`, `graveSprite`, `sarcophagusSprite`, `coffinSprite`, `casketSprite`, `urn_16_33Sprite`, `reliquarySprite`, `shrineSprite`
- Buildings: `sanctumSprite`, `templeSprite`, `chapelSprite`, `cathedralSprite`, `monasterySprite`, `abbeySprite`, `cloisterSprite`, `refectorySprite`, `library_16_44Sprite`, `archiveSprite`, `treasurySprite`, `armorySprite`, `vault_16_48Sprite`

### Row 17: Doors and Gates (47 sprites)
Entrances and barriers:
- Walls: `wallTopSprite`, `wallBottomSprite`, `wallLeftSprite`, `wallRightSprite`, `wallCornerTLSprite`, `stairsSprite`, `wallCornerSprite`, `wallJuncSprite`, `wallCross_17_8Sprite`, `wallDiagSprite`, `wallFrontSprite`, `wallSideSprite`, `wallBackSprite`, `wallInnerSprite`, `wallOuterSprite`
- Doors: `doorFrameSprite`, `doorwaySprite`, `entranceSprite`, `exitSprite`, `portal_17_19Sprite`, `archSprite`, `gate_17_21Sprite`, `window_17_22Sprite`, `openingSprite`, `passageSprite`, `doorOpenSprite`, `doorClosedSprite`, `doorLockedSprite`, `doorBrokenSprite`, `doorSecretSprite`
- Windows: `windowOpenSprite`, `windowClosedSprite`, `windowBrokenSprite`, `windowBarredSprite`, `portcullisSprite`
- Bridges: `drawbridge_17_37Sprite`, `rampSprite`, `platformSprite`, `bridge_17_40Sprite`, `walkwaySprite`, `plank_17_42Sprite`, `beam_17_43Sprite`, `supportSprite`, `column_17_45Sprite`, `pillar_17_46Sprite`, `postSprite`, `pierSprite`

### Row 18: Large Sprites (49 sprites)
Multi-tile sprites and large objects:
- Boss: `bossTLSprite`, `bossTRSprite`, `bossBLSprite`, `bossBRSprite`
- Large entities: `largeCreatureSprite`, `largeMonsterSprite`, `largeObjectSprite`, `largeStructureSprite`, `largeBuildingSprite`, `largeDecoSprite`, `largeTreeSprite`, `largeBoulderSprite`, `largeRockSprite`, `largeStatueSprite`, `largeMonumentSprite`
- Structures: `largeFountainSprite`, `largeTowerSprite`, `largeCastleSprite`, `largeGateSprite`, `largeDoorSprite`, `largeWindowSprite`, `largeArchSprite`, `largePillarSprite`, `largeColumnSprite`, `largeWallSprite`
- Building parts: `largeTileSprite`, `largeFloorSprite`, `largeRoofSprite`, `largeChimneySprite`, `largeBannerSprite`, `largeflagSprite`, `largeSignSprite`, `largePlaqueSprite`, `largeCrestSprite`, `largeShieldSprite`
- Equipment: `largeSwordSprite`, `largeAxeSprite`, `largeHammerSprite`, `largeBowSprite`, `largeCrossbowSprite`, `largeStaffSprite`, `largeCrownSprite`
- Items: `largeTreasureSprite`, `largeChestSprite`, `largeCrateSprite`, `largeBarrelSprite`, `largeUrnSprite`, `largeVaseSprite`

### Row 19: Elemental Effects (49 sprites)
Magical elements and animations:
- Fire: `fire_19_0Sprite`, `fireAnimSprite`, `fireBurstSprite`, `fireExplosionSprite`
- Water: `water_19_4Sprite`, `waterRippleSprite`, `waterSplashSprite`, `waterWaveSprite`
- Earth: `earth_19_8Sprite`, `earthShakeSprite`, `earthCrackSprite`, `earthSpikeSprite`
- Air: `air_19_12Sprite`, `airGustSprite`, `airTornadoSprite`, `airWhirlwindSprite`
- Ice: `ice_19_16Sprite`, `iceShatterSprite`, `iceSpikeSprite`, `iceBlastSprite`
- Lightning: `lightningSprite`, `lightningBoltSprite`, `lightningArcSprite`, `lightningStrikeSprite`
- Light: `light_19_24Sprite`, `lightBeamSprite`, `lightFlashSprite`, `lightGlowSprite`
- Dark: `dark_19_28Sprite`, `darkShadowSprite`, `darkVoidSprite`, `darkAuraSprite`
- Nature: `nature_19_32Sprite`, `natureGrowthSprite`, `natureVineSprite`, `natureThornSprite`
- Holy: `holySprite`, `holyLightSprite`, `holyAuraSprite`, `holyBlessingSprite`
- Shadow: `shadow_19_40Sprite`, `shadowStepSprite`, `shadowCloakSprite`, `shadowBladeSprite`
- Arcane: `arcaneSprite`, `arcaneBoltSprite`, `arcaneMissileSprite`, `arcaneShieldSprite`, `chaosSprite`

### Row 20: Particle Effects (49 sprites)
Visual effects and particles:
- Particles: `particleSprite`, `particleSmallSprite`, `particleMediumSprite`, `particleLargeSprite`, `particleTinySprite`, `particleDotSprite`, `particleCircleSprite`, `particleSquareSprite`, `particleTriSprite`, `particleStarSprite`, `particleCrossSprite`, `particlePlusSprite`, `particleDiamondSprite`, `particleHexSprite`, `particleOctSprite`, `particleGlowSprite`
- Dust: `dust_20_17Sprite`, `dustCloudSprite`
- Smoke: `smoke_20_20Sprite`, `smokeCloudSprite`, `smokePuffSprite`
- Mist: `mistSprite`, `mistCloudSprite`, `fogSprite`, `fogBankSprite`
- Clouds: `cloud_20_27Sprite`, `cloudWhiteSprite`, `steamSprite`, `vaporSprite`, `hazeSprite`, `smogSprite`
- Bubbles: `bubbleSprite`, `bubblePopSprite`
- Sparkles: `spark_20_41Sprite`, `sparkGlitterSprite`
- Glow: `glow_20_47Sprite`, `glowBrightSprite`

### Row 21: Numbers and Debug (49 sprites)
Text, numbers, and debug symbols:
- Numbers: `num0Sprite`, `num1Sprite`, `num2Sprite`, `num3Sprite`, `num4Sprite`, `num5Sprite`, `num6Sprite`, `num7Sprite`, `num8Sprite`, `num9Sprite`
- Letters: `letterASprite`, `letterBSprite`, `letterCSprite`, `letterDSprite`, `letterESprite`, `letterFSprite`, `letterGSprite`, `letterHSprite`, `letterISprite`, `letterJSprite`, `letterKSprite`, `letterLSprite`, `letterMSprite`, `letterNSprite`, `letterOSprite`, `letterPSprite`, `letterQSprite`, `letterRSprite`, `letterSSprite`, `letterTSprite`, `letterUSprite`, `letterVSprite`, `letterWSprite`, `letterXSprite`, `letterYSprite`
- Debug: `errorSprite`, `warningSprite`, `infoSprite`, `debugSprite`, `traceSprite`, `logSprite`, `consoleSprite`, `terminalSprite`, `shellSprite`, `promptSprite`, `commandSprite`, `outputSprite`, `inputSprite`, `bufferSprite`

## Usage Examples

```scala
// Floor tiles
val tile = Sprites.floorSprite
val wall = Sprites.wallSprite

// Characters
val player = Sprites.playerSprite
val enemy = Sprites.ratSprite

// Items
val key = Sprites.yellowKeySprite
val potion = Sprites.potionSprite
val sword = Sprites.basicSwordSprite

// UI
val heart = Sprites.fullHeartSprite
val cursor = Sprites.cursorSprite
```

## Layer Guidelines

When adding new sprite usages:

1. **floorLayer**: Use for ground tiles, terrain that should be rendered first
2. **backgroundLayer**: Use for effects that should appear behind entities (corpses, blood)
3. **entityLayer**: Use for characters, monsters, walls, interactive objects
4. **uiLayer**: Use for UI elements, inventory items, status indicators

## Naming Conventions

- All sprite names end with `Sprite`
- Descriptive names based on visual appearance
- Duplicates resolved with position suffix (e.g., `wall_17_2Sprite`)
- Empty/transparent sprites marked as `empty{x}_{y}Sprite`
- Multi-word names use camelCase (e.g., `leatherHelmetSprite`)

## Adding New Sprites

When adding new sprites to the sheet:

1. Update the sprite coordinate in `Sprites.scala`
2. Choose appropriate layer (floor/background/entity/ui)
3. Use descriptive, meaningful names
4. Document the sprite in this reference guide
5. Test rendering to ensure correct position and layer

## Related Files

- **Sprites.scala**: `/src/main/scala/data/Sprites.scala` - Main sprite definitions
- **Sprite Sheet**: `/assets/sprites/sprites.png` - The actual image file
- **Sprite.scala**: `/src/main/scala/game/Sprite.scala` - Sprite case class definition
- **SpriteExtension.scala**: `/src/main/scala/indigoengine/SpriteExtension.scala` - Rendering extensions
