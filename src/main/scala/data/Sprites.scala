package data

import game.entity.KeyColour.*
import game.entity.EntityType
import game.entity.EntityType.*
import game.Sprite

object Sprites {
  val floorLayer = 0
  val backgroundLayer = 1
  val entityLayer = 2
  val uiLayer = 3
  
  /**
   * Get sprite name by coordinates for debug purposes
   * Returns the actual variable name for all 1,059 defined sprites
   */
  def getSpriteNameByCoordinates(x: Int, y: Int): String = {
    // Check each map separately to avoid combining them into one large method
    spriteMap0.getOrElse((x, y),
      spriteMap1.getOrElse((x, y),
        spriteMap2.getOrElse((x, y),
          spriteMap3.getOrElse((x, y),
            spriteMap4.getOrElse((x, y),
              spriteMap5.getOrElse((x, y),
                spriteMap6.getOrElse((x, y),
                  spriteMap7.getOrElse((x, y),
                    spriteMap8.getOrElse((x, y),
                      spriteMap9.getOrElse((x, y), s"<empty at ($x, $y)>")
                    )
                  )
                )
              )
            )
          )
        )
      )
    )
  }
  
  private def spriteMap0 = Map(
      (1, 0) -> "maybeFloorSprite",
      (2, 0) -> "floorSprite",
      (3, 0) -> "crackedFloorSprite",
      (4, 0) -> "wetFloorSprite",
      (5, 0) -> "grassSprite",
      (6, 0) -> "tallGrassSprite",
      (7, 0) -> "flowersSprite",
      (8, 0) -> "brickRedSprite",
      (9, 0) -> "brickDarkSprite",
      (10, 0) -> "brickGreySprite",
      (11, 0) -> "woodFloorSprite",
      (12, 0) -> "empty12_0Sprite",
      (14, 0) -> "darkStoneSprite",
      (15, 0) -> "cobblestoneSprite",
      (16, 0) -> "stoneTileSprite",
      (17, 0) -> "marbleSprite",
      (18, 0) -> "sandSprite",
      (19, 0) -> "dirtSprite",
      (20, 0) -> "mudSprite",
      (21, 0) -> "iceSprite",
      (22, 0) -> "snowSprite",
      (23, 0) -> "lavaSprite",
      (24, 0) -> "ashSprite",
      (25, 0) -> "playerSprite",
      (26, 0) -> "enemySprite",
      (27, 0) -> "npcSprite",
      (28, 0) -> "merchantSprite",
      (29, 0) -> "guardSprite",
      (30, 0) -> "mageSprite",
      (31, 0) -> "warriorSprite",
      (32, 0) -> "rogueSprite",
      (33, 0) -> "leatherHelmetSprite",
      (34, 0) -> "nobleSprite",
      (35, 0) -> "kingSprite",
      (36, 0) -> "ironHelmetSprite",
      (37, 0) -> "priestSprite",
      (38, 0) -> "blacksmithSprite",
      (39, 0) -> "ironBootsSprite",
      (40, 0) -> "farmerSprite",
      (41, 0) -> "ironGlovesSprite",
      (42, 0) -> "fishermanSprite",
      (43, 0) -> "hunterSprite",
      (44, 0) -> "traderSprite",
      (45, 0) -> "alchemistSprite",
      (46, 0) -> "wizardSprite",
      (47, 0) -> "witchSprite",
      (48, 0) -> "sageSprite",
      (0, 1) -> "shrubSprite",
      (1, 1) -> "bushSprite",
      (2, 1) -> "treeSprite",
      (3, 1) -> "rocksSprite",
      (4, 1) -> "boulderSprite",
      (5, 1) -> "pebblesSprite",
      (6, 1) -> "mushroomSprite",
      (7, 1) -> "fungusSprite",
      (8, 1) -> "vinesSprite",
      (9, 1) -> "mossSprite",
      (10, 1) -> "lichenSprite",
      (11, 1) -> "fernSprite",
      (12, 1) -> "empty12_1Sprite",
      (15, 1) -> "barrelSprite",
      (16, 1) -> "crateSprite",
      (17, 1) -> "sackSprite",
      (18, 1) -> "urnSprite",
      (19, 1) -> "vaseSprite",
      (20, 1) -> "potSprite",
      (21, 1) -> "jarSprite",
      (22, 1) -> "basketSprite",
      (23, 1) -> "bucketSprite",
      (24, 1) -> "anvilSprite",
      (25, 1) -> "forgeSprite",
      (26, 1) -> "workbenchSprite",
      (27, 1) -> "tableSprite",
      (28, 1) -> "chairSprite",
      (29, 1) -> "bedSprite",
      (30, 1) -> "bookshelfSprite",
      (31, 1) -> "armorSprite",
      (32, 1) -> "chainmailArmorSprite",
      (33, 1) -> "plateArmorSprite",
      (34, 1) -> "helmSprite",
      (35, 1) -> "shieldSprite",
      (36, 1) -> "bucklerSprite",
      (37, 1) -> "bootsSprite",
      (38, 1) -> "ironBoots_1_38Sprite",
      (39, 1) -> "leatherBootsSprite",
      (40, 1) -> "ironGloves_1_40Sprite",
      (41, 1) -> "leatherGlovesSprite",
      (42, 1) -> "amuletSprite",
      (43, 1) -> "crownSprite",
      (44, 1) -> "circletSprite",
      (45, 1) -> "tiaraSprite",
      (46, 1) -> "braceletSprite",
      (47, 1) -> "pendantSprite",
      (48, 1) -> "gemSprite",
      (0, 2) -> "smallStoneSprite",
      (1, 2) -> "mediumStoneSprite",
      (2, 2) -> "largeStoneSprite",
      (3, 2) -> "roundStoneSprite",
      (4, 2) -> "boulderRockSprite",
      (5, 2) -> "rockSprite",
      (6, 2) -> "debrisSprite",
      (7, 2) -> "gravelSprite",
      (8, 2) -> "brickSprite",
      (9, 2) -> "stoneTile_2_9Sprite",
      (10, 2) -> "plankSprite",
      (11, 2) -> "beamSprite",
  )

  private def spriteMap1 = Map(
      (12, 2) -> "logSprite",
      (13, 2) -> "stumpSprite",
      (14, 2) -> "empty14_2Sprite",
      (15, 2) -> "empty15_2Sprite",
      (16, 2) -> "torchSprite",
      (17, 2) -> "lanternSprite",
      (18, 2) -> "candleSprite",
      (19, 2) -> "brazierSprite",
      (20, 2) -> "firepitSprite",
      (21, 2) -> "campfireSprite",
      (22, 2) -> "altarSprite",
      (23, 2) -> "statueSprite",
      (24, 2) -> "monumentSprite",
      (27, 2) -> "bossSprite",
      (28, 2) -> "gateSprite",
      (29, 2) -> "fenceSprite",
      (30, 2) -> "wall_2_30Sprite",
      (31, 2) -> "barrierSprite",
      (32, 2) -> "ladderSprite",
      (33, 2) -> "ropeSprite",
      (34, 2) -> "chainSprite",
      (35, 2) -> "leverSprite",
      (36, 2) -> "buttonSprite",
      (37, 2) -> "wheelSprite",
      (38, 2) -> "gearSprite",
      (39, 2) -> "pipeSprite",
      (40, 2) -> "valveSprite",
      (41, 2) -> "machineSprite",
      (42, 2) -> "deviceSprite",
      (43, 2) -> "panelSprite",
      (44, 2) -> "terminalSprite",
      (45, 2) -> "consoleSprite",
      (46, 2) -> "controlSprite",
      (0, 3) -> "skeletonSprite",
      (1, 3) -> "zombieSprite",
      (2, 3) -> "ghostSprite",
      (3, 3) -> "wraithSprite",
      (4, 3) -> "demonSprite",
      (5, 3) -> "impSprite",
      (6, 3) -> "devilSprite",
      (7, 3) -> "fiendSprite",
      (8, 3) -> "hellspawnSprite",
      (10, 3) -> "goblinSprite",
      (11, 3) -> "orcSprite",
      (12, 3) -> "trollSprite",
      (13, 3) -> "ogreSprite",
      (14, 3) -> "giantSprite",
      (15, 3) -> "cyclopsSprite",
      (16, 3) -> "boss_3_16Sprite",
      (17, 3) -> "boss_3_17Sprite",
      (19, 3) -> "satyrSprite",
      (20, 3) -> "harpySprite",
      (21, 3) -> "empty21_3Sprite",
      (22, 3) -> "empty22_3Sprite",
      (23, 3) -> "empty23_3Sprite",
      (24, 3) -> "empty24_3Sprite",
      (27, 3) -> "drakeSprite",
      (28, 3) -> "serpentSprite",
      (29, 3) -> "basiliskSprite",
      (30, 3) -> "cockatriceSprite",
      (31, 3) -> "griffinSprite",
      (32, 3) -> "hippogriffSprite",
      (33, 3) -> "pegasusSprite",
      (35, 3) -> "chimeraSprite",
      (36, 3) -> "hydraSprite",
      (37, 3) -> "manticoreSprite",
      (38, 3) -> "sphinxSprite",
      (39, 3) -> "phoenixSprite",
      (40, 3) -> "rocSprite",
      (41, 3) -> "krakenSprite",
      (42, 3) -> "leviathanSprite",
      (43, 3) -> "behemothSprite",
      (44, 3) -> "golemSprite",
      (45, 3) -> "elementalSprite",
      (46, 3) -> "spiritSprite",
      (47, 3) -> "shadeSprite",
      (48, 3) -> "wraith_3_48Sprite",
      (0, 4) -> "woodWallSprite",
      (1, 4) -> "stoneWallSprite",
      (2, 4) -> "brickWallSprite",
      (3, 4) -> "rockWallSprite",
      (4, 4) -> "caveWallSprite",
      (5, 4) -> "metalWallSprite",
      (6, 4) -> "crystalWallSprite",
      (7, 4) -> "iceWallSprite",
      (8, 4) -> "waterDeepSprite",
      (9, 4) -> "waterMidSprite",
      (10, 4) -> "waterShallowSprite",
      (11, 4) -> "lava_4_11Sprite",
      (12, 4) -> "empty12_4Sprite",
      (13, 4) -> "empty13_4Sprite",
      (14, 4) -> "empty14_4Sprite",
      (15, 4) -> "pitSprite",
      (16, 4) -> "chasmSprite",
      (17, 4) -> "abyssSprite",
      (19, 4) -> "vortexSprite",
      (20, 4) -> "riftSprite",
      (21, 4) -> "stairs_4_21Sprite",
      (22, 4) -> "ladder_4_22Sprite",
      (23, 4) -> "trapdoorSprite",
      (24, 4) -> "hatchSprite",
      (25, 4) -> "grateSprite",
      (26, 4) -> "drainSprite",
      (27, 4) -> "fountainSprite",
      (28, 4) -> "wellSprite",
      (29, 4) -> "poolSprite",
  )

  private def spriteMap2 = Map(
      (30, 4) -> "pondSprite",
      (31, 4) -> "streamSprite",
      (32, 4) -> "riverSprite",
      (33, 4) -> "waterfallSprite",
      (34, 4) -> "rapidsSprite",
      (35, 4) -> "whirlpoolSprite",
      (36, 4) -> "geyserSprite",
      (37, 4) -> "springSprite",
      (38, 4) -> "bogSprite",
      (39, 4) -> "swampSprite",
      (40, 4) -> "marshSprite",
      (41, 4) -> "mireSprite",
      (42, 4) -> "quicksandSprite",
      (43, 4) -> "tarSprite",
      (44, 4) -> "slime_4_44Sprite",
      (45, 4) -> "oozeSprite",
      (46, 4) -> "acidSprite",
      (47, 4) -> "poisonSprite",
      (0, 5) -> "forestSprite",
      (1, 5) -> "grasslandSprite",
      (2, 5) -> "desertSprite",
      (3, 5) -> "tundraSprite",
      (4, 5) -> "mountainSprite",
      (5, 5) -> "hillSprite",
      (6, 5) -> "cliffSprite",
      (7, 5) -> "canyonSprite",
      (8, 5) -> "waterSprite",
      (9, 5) -> "waterShallow_5_9Sprite",
      (10, 5) -> "waterDeep_5_10Sprite",
      (11, 5) -> "bridge_5_11Sprite",
      (12, 5) -> "pathSprite",
      (13, 5) -> "roadSprite",
      (14, 5) -> "cobblestone_5_14Sprite",
      (15, 5) -> "pavementSprite",
      (16, 5) -> "bridgeSprite",
      (17, 5) -> "crystalBlueSprite",
      (18, 5) -> "crystalRedSprite",
      (19, 5) -> "crystalGreenSprite",
      (20, 5) -> "crystalYellowSprite",
      (21, 5) -> "crystalPurpleSprite",
      (22, 5) -> "crystalWhiteSprite",
      (23, 5) -> "crystalBlackSprite",
      (24, 5) -> "gemSmallSprite",
      (25, 5) -> "gemMediumSprite",
      (26, 5) -> "diamondSprite",
      (27, 5) -> "rubySprite",
      (28, 5) -> "emeraldSprite",
      (29, 5) -> "sapphireSprite",
      (30, 5) -> "amethystSprite",
      (31, 5) -> "topazSprite",
      (32, 5) -> "opalSprite",
      (33, 5) -> "pearlSprite",
      (34, 5) -> "amberSprite",
      (35, 5) -> "jadeSprite",
      (36, 5) -> "onyxSprite",
      (37, 5) -> "obsidianSprite",
      (38, 5) -> "quartzSprite",
      (39, 5) -> "empty39_5Sprite",
      (40, 5) -> "arrowSprite",
      (41, 5) -> "coalSprite",
      (42, 5) -> "ironSprite",
      (43, 5) -> "copperSprite",
      (44, 5) -> "silverSprite",
      (45, 5) -> "goldSprite",
      (46, 5) -> "platinumSprite",
      (47, 5) -> "mithrilSprite",
      (48, 5) -> "adamantSprite",
      (0, 6) -> "daggerSprite",
      (1, 6) -> "knifeSprite",
      (2, 6) -> "shortswordSprite",
      (3, 6) -> "swordSprite",
      (4, 6) -> "longswordSprite",
      (5, 6) -> "greatswordSprite",
      (6, 6) -> "scimitarSprite",
      (7, 6) -> "katanaSprite",
      (8, 6) -> "rapierSprite",
      (9, 6) -> "saberSprite",
      (10, 6) -> "cutlassSprite",
      (11, 6) -> "falchionSprite",
      (12, 6) -> "claymoreSprite",
      (13, 6) -> "bladeSprite",
      (14, 6) -> "axeSprite",
      (15, 6) -> "handaxeSprite",
      (16, 6) -> "battleaxeSprite",
      (17, 6) -> "waraxeSprite",
      (18, 6) -> "halberdSprite",
      (19, 6) -> "poleaxeSprite",
      (20, 6) -> "pikeSprite",
      (21, 6) -> "spearSprite",
      (22, 6) -> "javelinSprite",
      (23, 6) -> "lanceSprite",
      (24, 6) -> "tridentSprite",
      (25, 6) -> "quarterstaffSprite",
      (26, 6) -> "maceSprite",
      (27, 6) -> "morningstarSprite",
      (28, 6) -> "flailSprite",
      (29, 6) -> "clubSprite",
      (30, 6) -> "hammerSprite",
      (31, 6) -> "warhammerSprite",
      (32, 6) -> "basicSwordSprite",
      (33, 6) -> "bow_6_33Sprite",
      (34, 6) -> "longbowSprite",
      (35, 6) -> "shortbowSprite",
      (36, 6) -> "crossbowSprite",
      (37, 6) -> "bowSprite",
      (38, 6) -> "arbalestSprite",
  )

  private def spriteMap3 = Map(
      (39, 6) -> "slingSprite",
      (40, 6) -> "blowgunSprite",
      (41, 6) -> "dartSprite",
      (42, 6) -> "shurikenSprite",
      (43, 6) -> "chakramSprite",
      (44, 6) -> "boomerangSprite",
      (45, 6) -> "whipSprite",
      (46, 6) -> "nunchakuSprite",
      (47, 6) -> "flail_6_47Sprite",
      (48, 6) -> "chain_6_48Sprite",
      (0, 7) -> "clothArmorSprite",
      (1, 7) -> "leatherArmorSprite",
      (2, 7) -> "studdedSprite",
      (3, 7) -> "hideSprite",
      (4, 7) -> "chainmailSprite",
      (5, 7) -> "scalemailSprite",
      (6, 7) -> "ringmailSprite",
      (7, 7) -> "splintSprite",
      (8, 7) -> "plateSprite",
      (9, 7) -> "fullPlateSprite",
      (10, 7) -> "shield_7_10Sprite",
      (11, 7) -> "buckler_7_11Sprite",
      (12, 7) -> "towerSprite",
      (13, 7) -> "kiteSprite",
      (14, 7) -> "roundSprite",
      (15, 7) -> "helmetSprite",
      (16, 7) -> "helm_7_16Sprite",
      (17, 7) -> "capSprite",
      (18, 7) -> "coifSprite",
      (19, 7) -> "cowlSprite",
      (20, 7) -> "hoodSprite",
      (21, 7) -> "maskSprite",
      (22, 7) -> "visorSprite",
      (23, 7) -> "faceplateSprite",
      (24, 7) -> "gauntletsSprite",
      (25, 7) -> "glovesSprite",
      (26, 7) -> "bracersSprite",
      (27, 7) -> "vambracesSprite",
      (28, 7) -> "greavesSprite",
      (29, 7) -> "boots_7_29Sprite",
      (30, 7) -> "sabatonsSprite",
      (31, 7) -> "cuissesSprite",
      (32, 7) -> "ironSwordSprite",
      (33, 7) -> "pauldronsSprite",
      (34, 7) -> "cuirassSprite",
      (35, 7) -> "breastplateSprite",
      (36, 7) -> "chestplateSprite",
      (37, 7) -> "gorgetSprite",
      (38, 7) -> "capeSprite",
      (39, 7) -> "cloakSprite",
      (40, 7) -> "robeSprite",
      (41, 7) -> "tunicSprite",
      (42, 7) -> "surcoatSprite",
      (43, 7) -> "tabardSprite",
      (44, 7) -> "vestSprite",
      (45, 7) -> "jerkinSprite",
      (46, 7) -> "doubletSprite",
      (47, 7) -> "hauberkSprite",
      (0, 8) -> "batSprite",
      (1, 8) -> "spiderSprite",
      (2, 8) -> "scorpionSprite",
      (3, 8) -> "beetleSprite",
      (4, 8) -> "antSprite",
      (5, 8) -> "waspSprite",
      (6, 8) -> "beeSprite",
      (7, 8) -> "hornetSprite",
      (8, 8) -> "mothSprite",
      (9, 8) -> "butterflySprite",
      (10, 8) -> "dragonflySprite",
      (11, 8) -> "centipedeSprite",
      (12, 8) -> "millipedeSprite",
      (13, 8) -> "wormSprite",
      (14, 8) -> "slugSprite",
      (15, 8) -> "snailSprite",
      (16, 8) -> "leechSprite",
      (17, 8) -> "tickSprite",
      (18, 8) -> "fleaSprite",
      (19, 8) -> "miteSprite",
      (20, 8) -> "maggotSprite",
      (21, 8) -> "larvaSprite",
      (22, 8) -> "cocoonSprite",
      (23, 8) -> "eggSprite",
      (24, 8) -> "pupaSprite",
      (25, 8) -> "chrysalisSprite",
      (26, 8) -> "hiveSprite",
      (27, 8) -> "snake_8_27Sprite",
      (28, 8) -> "snakeSprite",
      (29, 8) -> "slimeSprite",
      (30, 8) -> "slimeletSprite",
      (31, 8) -> "ratSprite",
      (32, 8) -> "mouseSprite",
      (33, 8) -> "voleSprite",
      (34, 8) -> "shrewSprite",
      (35, 8) -> "moleSprite",
      (36, 8) -> "badgerSprite",
      (37, 8) -> "weaselSprite",
      (38, 8) -> "ferretSprite",
      (39, 8) -> "otterSprite",
      (40, 8) -> "beaverSprite",
      (41, 8) -> "rabbitSprite",
      (42, 8) -> "hareSprite",
      (43, 8) -> "squirrelSprite",
      (44, 8) -> "chipmunkSprite",
      (45, 8) -> "hedgehogSprite",
      (46, 8) -> "porcupineSprite",
      (47, 8) -> "armadilloSprite",
  )

  private def spriteMap4 = Map(
      (48, 8) -> "critterSprite",
      (0, 9) -> "blueDoorSprite",
      (1, 9) -> "dogSprite",
      (2, 9) -> "foxSprite",
      (3, 9) -> "coyoteSprite",
      (4, 9) -> "jackalSprite",
      (5, 9) -> "hyenaSprite",
      (6, 9) -> "bearSprite",
      (7, 9) -> "lionSprite",
      (8, 9) -> "tigerSprite",
      (9, 9) -> "leopardSprite",
      (10, 9) -> "pantherSprite",
      (11, 9) -> "cheetahSprite",
      (12, 9) -> "empty12_9Sprite",
      (13, 9) -> "lynxSprite",
      (14, 9) -> "bobcatSprite",
      (15, 9) -> "wildcatSprite",
      (16, 9) -> "jaguarSprite",
      (17, 9) -> "pumaSprite",
      (18, 9) -> "caracalSprite",
      (19, 9) -> "servalSprite",
      (20, 9) -> "mongooseSprite",
      (21, 9) -> "boarSprite",
      (22, 9) -> "pigSprite",
      (23, 9) -> "warthogSprite",
      (24, 9) -> "deerSprite",
      (25, 9) -> "elkSprite",
      (26, 9) -> "mooseSprite",
      (27, 9) -> "caribouSprite",
      (28, 9) -> "reindeerSprite",
      (29, 9) -> "antelopeSprite",
      (30, 9) -> "gazelleSprite",
      (31, 9) -> "impalaSprite",
      (32, 9) -> "buffaloSprite",
      (33, 9) -> "bisonSprite",
      (34, 9) -> "oxSprite",
      (35, 9) -> "bullSprite",
      (36, 9) -> "cowSprite",
      (37, 9) -> "yakSprite",
      (38, 9) -> "goatSprite",
      (39, 9) -> "sheepSprite",
      (40, 9) -> "ramSprite",
      (41, 9) -> "llamaSprite",
      (42, 9) -> "alpacaSprite",
      (43, 9) -> "camelSprite",
      (44, 9) -> "horseSprite",
      (45, 9) -> "ponySprite",
      (46, 9) -> "donkeySprite",
      (47, 9) -> "muleSprite",
      (48, 9) -> "zebraSprite",
      (0, 10) -> "redDoorSprite",
      (1, 10) -> "breadSprite",
      (2, 10) -> "cheeseSprite",
      (3, 10) -> "meatSprite",
      (4, 10) -> "fishSprite",
      (5, 10) -> "chickenSprite",
      (6, 10) -> "turkeySprite",
      (7, 10) -> "hamSprite",
      (8, 10) -> "baconSprite",
      (9, 10) -> "sausageSprite",
      (10, 10) -> "egg_10_10Sprite",
      (11, 10) -> "milkSprite",
      (12, 10) -> "butterSprite",
      (13, 10) -> "honeySprite",
      (14, 10) -> "jamSprite",
      (15, 10) -> "jellySprite",
      (16, 10) -> "empty16_10Sprite",
      (17, 10) -> "pieSprite",
      (18, 10) -> "cakeSprite",
      (19, 10) -> "cookieSprite",
      (20, 10) -> "biscuitSprite",
      (21, 10) -> "crackerSprite",
      (22, 10) -> "pastrySprite",
      (23, 10) -> "tartSprite",
      (24, 10) -> "muffinSprite",
      (25, 10) -> "sconeSprite",
      (26, 10) -> "bagelSprite",
      (27, 10) -> "donutSprite",
      (28, 10) -> "croissantSprite",
      (29, 10) -> "pretzelSprite",
      (30, 10) -> "waferSprite",
      (31, 10) -> "waffleSprite",
      (32, 10) -> "pancakeSprite",
      (33, 10) -> "fruitSprite",
      (34, 10) -> "vegetableSprite",
      (35, 10) -> "grainSprite",
      (36, 10) -> "riceSprite",
      (37, 10) -> "wheatSprite",
      (38, 10) -> "cornSprite",
      (39, 10) -> "barleySprite",
      (40, 10) -> "emptyHeartSprite",
      (41, 10) -> "halfHeartSprite",
      (42, 10) -> "fullHeartSprite",
      (43, 10) -> "coinSprite",
      (44, 10) -> "gold_10_44Sprite",
      (45, 10) -> "silver_10_45Sprite",
      (46, 10) -> "copper_10_46Sprite",
      (47, 10) -> "bronzeSprite",
      (48, 10) -> "treasureSprite",
      (0, 11) -> "yellowDoorSprite",
      (1, 11) -> "scroll_11_1Sprite",
      (2, 11) -> "tomeSprite",
      (3, 11) -> "grimoireSprite",
      (4, 11) -> "codexSprite",
      (5, 11) -> "manuscriptSprite",
      (6, 11) -> "parchmentSprite",
  )

  private def spriteMap5 = Map(
      (7, 11) -> "paperSprite",
      (8, 11) -> "letterSprite",
      (9, 11) -> "yellowDoor_11_9Sprite",
      (10, 11) -> "blueDoor_11_10Sprite",
      (11, 11) -> "redDoor_11_11Sprite",
      (12, 11) -> "mapSprite",
      (13, 11) -> "chartSprite",
      (14, 11) -> "atlasSprite",
      (15, 11) -> "compassSprite",
      (16, 11) -> "sextantSprite",
      (17, 11) -> "astrolabeSprite",
      (18, 11) -> "telescopeSprite",
      (19, 11) -> "spyglassSprite",
      (20, 11) -> "magnifierSprite",
      (21, 11) -> "lensSprite",
      (22, 11) -> "prismSprite",
      (23, 11) -> "mirrorSprite",
      (24, 11) -> "hourglassSprite",
      (25, 11) -> "sundialSprite",
      (26, 11) -> "clockSprite",
      (27, 11) -> "watchSprite",
      (28, 11) -> "projectileSprite",
      (29, 11) -> "arrow_11_29Sprite",
      (30, 11) -> "boltSprite",
      (31, 11) -> "yellowKey_11_31Sprite",
      (32, 11) -> "yellowKeySprite",
      (33, 11) -> "blueKeySprite",
      (34, 11) -> "redKeySprite",
      (35, 11) -> "purpleKeySprite",
      (36, 11) -> "whiteKeySprite",
      (37, 11) -> "blackKeySprite",
      (38, 11) -> "keySprite",
      (39, 11) -> "lockpickSprite",
      (40, 11) -> "crowbarSprite",
      (41, 11) -> "pickSprite",
      (42, 11) -> "shovelSprite",
      (43, 11) -> "hoeSprite",
      (44, 11) -> "rakeSprite",
      (45, 11) -> "sickleSprite",
      (46, 11) -> "scytheSprite",
      (47, 11) -> "pitchforkSprite",
      (48, 11) -> "toolSprite",
      (0, 12) -> "vialSprite",
      (1, 12) -> "flaskSprite",
      (2, 12) -> "bottleSprite",
      (3, 12) -> "phialSprite",
      (4, 12) -> "elixirSprite",
      (5, 12) -> "tonicSprite",
      (6, 12) -> "draughtSprite",
      (7, 12) -> "brewSprite",
      (8, 12) -> "mixtureSprite",
      (9, 12) -> "concoctionSprite",
      (10, 12) -> "empty10_12Sprite",
      (11, 12) -> "empty11_12Sprite",
      (13, 12) -> "empty13_12Sprite",
      (14, 12) -> "antidoteSprite",
      (15, 12) -> "remedySprite",
      (16, 12) -> "cureSprite",
      (17, 12) -> "salveSprite",
      (18, 12) -> "ointmentSprite",
      (19, 12) -> "balmSprite",
      (20, 12) -> "oilSprite",
      (21, 12) -> "perfumeSprite",
      (22, 12) -> "essenceSprite",
      (23, 12) -> "extractSprite",
      (24, 12) -> "tinctureSprite",
      (25, 12) -> "serumSprite",
      (26, 12) -> "toxinSprite",
      (27, 12) -> "poison_12_27Sprite",
      (28, 12) -> "venomSprite",
      (29, 12) -> "acid_12_29Sprite",
      (30, 12) -> "reagentSprite",
      (31, 12) -> "catalystSprite",
      (32, 12) -> "solventSprite",
      (33, 12) -> "solutionSprite",
      (34, 12) -> "gelSprite",
      (35, 12) -> "pasteSprite",
      (36, 12) -> "powderSprite",
      (37, 12) -> "crystalSprite",
      (38, 12) -> "shardSprite",
      (39, 12) -> "fragmentSprite",
      (40, 12) -> "dustSprite",
      (41, 12) -> "ash_12_41Sprite",
      (42, 12) -> "saltSprite",
      (43, 12) -> "spiceSprite",
      (44, 12) -> "herbSprite",
      (45, 12) -> "rootSprite",
      (46, 12) -> "seedSprite",
      (47, 12) -> "flowerSprite",
      (48, 12) -> "leafSprite",
      (0, 13) -> "wandSprite",
      (1, 13) -> "rodSprite",
      (2, 13) -> "staffSprite",
      (3, 13) -> "scepterSprite",
      (4, 13) -> "orbSprite",
      (5, 13) -> "crystal_13_5Sprite",
      (6, 13) -> "sphereSprite",
      (7, 13) -> "globeSprite",
      (8, 13) -> "ballSprite",
      (9, 13) -> "ringSprite",
      (10, 13) -> "amulet_13_10Sprite",
      (11, 13) -> "talismanSprite",
      (12, 13) -> "charmSprite",
      (13, 13) -> "totemSprite",
      (14, 13) -> "fetishSprite",
      (15, 13) -> "idolSprite",
  )

  private def spriteMap6 = Map(
      (16, 13) -> "figurineSprite",
      (17, 13) -> "statuetteSprite",
      (18, 13) -> "trophySprite",
      (19, 13) -> "relicSprite",
      (20, 13) -> "artifactSprite",
      (21, 13) -> "heirloomSprite",
      (22, 13) -> "antiqueSprite",
      (23, 13) -> "scroll_13_23Sprite",
      (24, 13) -> "spellbookSprite",
      (25, 13) -> "runestoneSprite",
      (26, 13) -> "tabletSprite",
      (27, 13) -> "obeliskSprite",
      (28, 13) -> "monolithSprite",
      (29, 13) -> "pillarSprite",
      (30, 13) -> "columnSprite",
      (31, 13) -> "totem_13_31Sprite",
      (32, 13) -> "ankhSprite",
      (33, 13) -> "potionSprite",
      (34, 13) -> "pentacleSprite",
      (35, 13) -> "hexagramSprite",
      (36, 13) -> "runeSprite",
      (37, 13) -> "defaultItemSprite",
      (38, 13) -> "sigilSprite",
      (39, 13) -> "symbolSprite",
      (40, 13) -> "markSprite",
      (41, 13) -> "emblemSprite",
      (42, 13) -> "insigniaSprite",
      (43, 13) -> "badgeSprite",
      (44, 13) -> "crestSprite",
      (45, 13) -> "sealSprite",
      (46, 13) -> "stampSprite",
      (47, 13) -> "tokenSprite",
      (48, 13) -> "medallionSprite",
      (0, 14) -> "checkSprite",
      (1, 14) -> "crossSprite",
      (2, 14) -> "plusSprite",
      (3, 14) -> "minusSprite",
      (4, 14) -> "multiplySprite",
      (5, 14) -> "divideSprite",
      (6, 14) -> "equalsSprite",
      (7, 14) -> "percentSprite",
      (8, 14) -> "dollarSprite",
      (9, 14) -> "poundSprite",
      (10, 14) -> "euroSprite",
      (11, 14) -> "yenSprite",
      (12, 14) -> "questionSprite",
      (13, 14) -> "exclamationSprite",
      (14, 14) -> "periodSprite",
      (15, 14) -> "commaSprite",
      (16, 14) -> "colonSprite",
      (17, 14) -> "semicolonSprite",
      (18, 14) -> "apostropheSprite",
      (19, 14) -> "quoteSprite",
      (20, 14) -> "empty20_14Sprite",
      (21, 14) -> "empty21_14Sprite",
      (22, 14) -> "empty22_14Sprite",
      (23, 14) -> "empty23_14Sprite",
      (24, 14) -> "empty24_14Sprite",
      (25, 14) -> "empty25_14Sprite",
      (26, 14) -> "empty26_14Sprite",
      (27, 14) -> "empty27_14Sprite",
      (28, 14) -> "cursor_14_28Sprite",
      (29, 14) -> "cursorSprite",
      (30, 14) -> "handSprite",
      (31, 14) -> "fingerSprite",
      (32, 14) -> "arrow_14_32Sprite",
      (33, 14) -> "chevronSprite",
      (34, 14) -> "caretSprite",
      (35, 14) -> "angleSprite",
      (36, 14) -> "bracketSprite",
      (37, 14) -> "parenSprite",
      (38, 14) -> "braceSprite",
      (39, 14) -> "squareSprite",
      (40, 14) -> "circleSprite",
      (41, 14) -> "triangleSprite",
      (42, 14) -> "diamond_14_42Sprite",
      (43, 14) -> "starSprite",
      (44, 14) -> "asteriskSprite",
      (45, 14) -> "hashSprite",
      (46, 14) -> "atSprite",
      (47, 14) -> "ampersandSprite",
      (48, 14) -> "tildeSprite",
      (0, 15) -> "deadSprite",
      (1, 15) -> "corpseSprite",
      (2, 15) -> "remainsSprite",
      (3, 15) -> "bonesSprite",
      (4, 15) -> "skullSprite",
      (5, 15) -> "empty5_15Sprite",
      (6, 15) -> "empty6_15Sprite",
      (7, 15) -> "bloodSprite",
      (8, 15) -> "goreSprite",
      (9, 15) -> "splatterSprite",
      (10, 15) -> "stainSprite",
      (11, 15) -> "puddleSprite",
      (12, 15) -> "pool_15_12Sprite",
      (13, 15) -> "slashSprite",
      (14, 15) -> "cutSprite",
      (15, 15) -> "woundSprite",
      (16, 15) -> "scarSprite",
      (17, 15) -> "bruiseSprite",
      (18, 15) -> "burnSprite",
      (19, 15) -> "scorchSprite",
      (20, 15) -> "charSprite",
      (21, 15) -> "ash_15_21Sprite",
      (22, 15) -> "smokeSprite",
      (23, 15) -> "fireSprite",
  )

  private def spriteMap7 = Map(
      (24, 15) -> "flameSprite",
      (25, 15) -> "sparkSprite",
      (26, 15) -> "emberSprite",
      (27, 15) -> "glowSprite",
      (28, 15) -> "shimmerSprite",
      (29, 15) -> "glitterSprite",
      (30, 15) -> "sparkleSprite",
      (31, 15) -> "twinkleSprite",
      (32, 15) -> "flashSprite",
      (33, 15) -> "scrollSprite",
      (34, 15) -> "explosionSprite",
      (35, 15) -> "impactSprite",
      (36, 15) -> "shockwaveSprite",
      (37, 15) -> "rippleSprite",
      (38, 15) -> "waveSprite",
      (39, 15) -> "pulseSprite",
      (40, 15) -> "auraSprite",
      (41, 15) -> "haloSprite",
      (42, 15) -> "ring_15_42Sprite",
      (43, 15) -> "circle_15_43Sprite",
      (44, 15) -> "spiralSprite",
      (45, 15) -> "vortex_15_45Sprite",
      (46, 15) -> "swirlSprite",
      (47, 15) -> "tornadoSprite",
      (48, 15) -> "whirlwindSprite",
      (0, 16) -> "cornerTLSprite",
      (1, 16) -> "cornerTRSprite",
      (2, 16) -> "cornerBLSprite",
      (3, 16) -> "cornerBRSprite",
      (4, 16) -> "edgeTopSprite",
      (5, 16) -> "edgeBottomSprite",
      (6, 16) -> "edgeLeftSprite",
      (7, 16) -> "edgeRightSprite",
      (8, 16) -> "tJuncUpSprite",
      (9, 16) -> "tJuncDownSprite",
      (10, 16) -> "tJuncLeftSprite",
      (11, 16) -> "tJuncRightSprite",
      (12, 16) -> "cross_16_12Sprite",
      (13, 16) -> "endNorthSprite",
      (14, 16) -> "endSouthSprite",
      (15, 16) -> "endEastSprite",
      (16, 16) -> "endWestSprite",
      (17, 16) -> "corridorSprite",
      (18, 16) -> "passageSprite",
      (19, 16) -> "hallwaySprite",
      (20, 16) -> "tunnelSprite",
      (21, 16) -> "alcoveSprite",
      (22, 16) -> "nicheSprite",
      (23, 16) -> "recessSprite",
      (24, 16) -> "chamberSprite",
      (25, 16) -> "roomSprite",
      (26, 16) -> "vaultSprite",
      (27, 16) -> "cryptSprite",
      (28, 16) -> "tombSprite",
      (29, 16) -> "graveSprite",
      (30, 16) -> "sarcophagusSprite",
      (31, 16) -> "coffinSprite",
      (32, 16) -> "casketSprite",
      (33, 16) -> "urn_16_33Sprite",
      (34, 16) -> "reliquarySprite",
      (35, 16) -> "shrineSprite",
      (36, 16) -> "sanctumSprite",
      (37, 16) -> "templeSprite",
      (38, 16) -> "chapelSprite",
      (39, 16) -> "cathedralSprite",
      (40, 16) -> "monasterySprite",
      (41, 16) -> "abbeySprite",
      (42, 16) -> "cloisterSprite",
      (43, 16) -> "refectorySprite",
      (44, 16) -> "librarySprite",
      (45, 16) -> "archiveSprite",
      (46, 16) -> "treasurySprite",
      (47, 16) -> "armorySprite",
      (48, 16) -> "vault_16_48Sprite",
      (0, 17) -> "wallTopSprite",
      (1, 17) -> "wallBottomSprite",
      (2, 17) -> "wallLeftSprite",
      (3, 17) -> "wallRightSprite",
      (4, 17) -> "wallCornerTLSprite",
      (5, 17) -> "stairsSprite",
      (6, 17) -> "wallCornerSprite",
      (7, 17) -> "wallJuncSprite",
      (8, 17) -> "wallCrossSprite",
      (9, 17) -> "wallDiagSprite",
      (10, 17) -> "wallSprite",
      (11, 17) -> "wallSideSprite",
      (12, 17) -> "wallBackSprite",
      (13, 17) -> "wallInnerSprite",
      (14, 17) -> "wallOuterSprite",
      (15, 17) -> "doorFrameSprite",
      (16, 17) -> "doorwaySprite",
      (17, 17) -> "entranceSprite",
      (18, 17) -> "exitSprite",
      (19, 17) -> "portalSprite",
      (20, 17) -> "archSprite",
      (21, 17) -> "gate_17_21Sprite",
      (22, 17) -> "windowSprite",
      (23, 17) -> "openingSprite",
      (24, 17) -> "passage_17_24Sprite",
      (25, 17) -> "doorOpenSprite",
      (26, 17) -> "doorClosedSprite",
      (27, 17) -> "doorLockedSprite",
      (28, 17) -> "doorBrokenSprite",
      (29, 17) -> "doorSecretSprite",
      (30, 17) -> "windowOpenSprite",
      (31, 17) -> "windowClosedSprite",
  )

  private def spriteMap8 = Map(
      (32, 17) -> "windowBrokenSprite",
      (33, 17) -> "windowBarredSprite",
      (34, 17) -> "portcullisSprite",
      (35, 17) -> "empty35_17Sprite",
      (36, 17) -> "empty36_17Sprite",
      (37, 17) -> "drawbridgeSprite",
      (38, 17) -> "rampSprite",
      (39, 17) -> "platformSprite",
      (40, 17) -> "bridge_17_40Sprite",
      (41, 17) -> "walkwaySprite",
      (42, 17) -> "plank_17_42Sprite",
      (43, 17) -> "beam_17_43Sprite",
      (44, 17) -> "supportSprite",
      (47, 17) -> "postSprite",
      (48, 17) -> "pierSprite",
      (0, 18) -> "bossTLSprite",
      (1, 18) -> "bossTRSprite",
      (2, 18) -> "bossBLSprite",
      (3, 18) -> "bossBRSprite",
      (4, 18) -> "largeCreatureSprite",
      (5, 18) -> "largeMonsterSprite",
      (6, 18) -> "largeObjectSprite",
      (7, 18) -> "largeStructureSprite",
      (8, 18) -> "largeBuildingSprite",
      (9, 18) -> "largeDecoSprite",
      (10, 18) -> "largeTreeSprite",
      (11, 18) -> "largeBoulderSprite",
      (12, 18) -> "largeRockSprite",
      (13, 18) -> "largeStatueSprite",
      (14, 18) -> "largeMonumentSprite",
      (15, 18) -> "largeFountainSprite",
      (16, 18) -> "largeTowerSprite",
      (17, 18) -> "largeCastleSprite",
      (18, 18) -> "largeGateSprite",
      (19, 18) -> "largeDoorSprite",
      (20, 18) -> "largeWindowSprite",
      (21, 18) -> "largeArchSprite",
      (22, 18) -> "largePillarSprite",
      (23, 18) -> "largeColumnSprite",
      (24, 18) -> "largeWallSprite",
      (25, 18) -> "largeTileSprite",
      (26, 18) -> "largeFloorSprite",
      (27, 18) -> "largeRoofSprite",
      (28, 18) -> "largeChimneySprite",
      (29, 18) -> "largeBannerSprite",
      (30, 18) -> "largeflagSprite",
      (31, 18) -> "largeSignSprite",
      (32, 18) -> "largePlaqueSprite",
      (33, 18) -> "largeCrestSprite",
      (34, 18) -> "largeShieldSprite",
      (35, 18) -> "largeSwordSprite",
      (36, 18) -> "largeAxeSprite",
      (37, 18) -> "largeHammerSprite",
      (38, 18) -> "empty38_18Sprite",
      (39, 18) -> "largeBowSprite",
      (40, 18) -> "largeCrossbowSprite",
      (41, 18) -> "largeStaffSprite",
      (42, 18) -> "largeCrownSprite",
      (43, 18) -> "largeTreasureSprite",
      (44, 18) -> "largeChestSprite",
      (45, 18) -> "largeCrateSprite",
      (46, 18) -> "largeBarrelSprite",
      (47, 18) -> "largeUrnSprite",
      (48, 18) -> "largeVaseSprite",
      (0, 19) -> "fire_19_0Sprite",
      (1, 19) -> "fireAnimSprite",
      (2, 19) -> "fireBurstSprite",
      (3, 19) -> "fireExplosionSprite",
      (4, 19) -> "water_19_4Sprite",
      (5, 19) -> "waterRippleSprite",
      (6, 19) -> "waterSplashSprite",
      (7, 19) -> "waterWaveSprite",
      (8, 19) -> "earthSprite",
      (9, 19) -> "earthShakeSprite",
      (10, 19) -> "earthCrackSprite",
      (11, 19) -> "earthSpikeSprite",
      (12, 19) -> "airSprite",
      (13, 19) -> "airGustSprite",
      (14, 19) -> "airTornadoSprite",
      (15, 19) -> "airWhirlwindSprite",
      (16, 19) -> "ice_19_16Sprite",
      (17, 19) -> "iceShatterSprite",
      (18, 19) -> "iceSpikeSprite",
      (19, 19) -> "iceBlastSprite",
      (20, 19) -> "lightningSprite",
      (21, 19) -> "lightningBoltSprite",
      (22, 19) -> "lightningArcSprite",
      (23, 19) -> "lightningStrikeSprite",
      (24, 19) -> "lightSprite",
      (25, 19) -> "lightBeamSprite",
      (26, 19) -> "lightFlashSprite",
      (27, 19) -> "lightGlowSprite",
      (28, 19) -> "darkSprite",
      (29, 19) -> "darkShadowSprite",
      (30, 19) -> "darkVoidSprite",
      (31, 19) -> "darkAuraSprite",
      (32, 19) -> "natureSprite",
      (33, 19) -> "natureGrowthSprite",
      (34, 19) -> "natureVineSprite",
      (35, 19) -> "natureThornSprite",
      (36, 19) -> "holySprite",
      (37, 19) -> "holyLightSprite",
      (38, 19) -> "holyAuraSprite",
      (39, 19) -> "holyBlessingSprite",
      (40, 19) -> "shadowSprite",
      (41, 19) -> "shadowStepSprite",
  )

  private def spriteMap9 = Map(
      (42, 19) -> "shadowCloakSprite",
      (43, 19) -> "shadowBladeSprite",
      (44, 19) -> "arcaneSprite",
      (45, 19) -> "arcaneBoltSprite",
      (46, 19) -> "arcaneMissileSprite",
      (47, 19) -> "arcaneShieldSprite",
      (48, 19) -> "chaosSprite",
      (0, 20) -> "particleSprite",
      (1, 20) -> "particleSmallSprite",
      (2, 20) -> "particleMediumSprite",
      (3, 20) -> "particleLargeSprite",
      (4, 20) -> "particleTinySprite",
      (5, 20) -> "particleDotSprite",
      (6, 20) -> "particleCircleSprite",
      (7, 20) -> "particleSquareSprite",
      (8, 20) -> "particleTriSprite",
      (9, 20) -> "particleStarSprite",
      (10, 20) -> "particleCrossSprite",
      (11, 20) -> "particlePlusSprite",
      (12, 20) -> "particleDiamondSprite",
      (13, 20) -> "particleHexSprite",
      (14, 20) -> "particleOctSprite",
      (15, 20) -> "particleGlowSprite",
      (16, 20) -> "empty16_20Sprite",
      (17, 20) -> "dust_20_17Sprite",
      (18, 20) -> "dustCloudSprite",
      (19, 20) -> "empty19_20Sprite",
      (20, 20) -> "smoke_20_20Sprite",
      (21, 20) -> "smokeCloudSprite",
      (22, 20) -> "smokePuffSprite",
      (23, 20) -> "mistSprite",
      (24, 20) -> "mistCloudSprite",
      (25, 20) -> "fogSprite",
      (26, 20) -> "fogBankSprite",
      (27, 20) -> "cloudSprite",
      (28, 20) -> "cloudWhiteSprite",
      (29, 20) -> "steamSprite",
      (30, 20) -> "vaporSprite",
      (31, 20) -> "hazeSprite",
      (32, 20) -> "smogSprite",
      (33, 20) -> "empty33_20Sprite",
      (34, 20) -> "empty34_20Sprite",
      (35, 20) -> "empty35_20Sprite",
      (36, 20) -> "bubbleSprite",
      (37, 20) -> "bubblePopSprite",
      (38, 20) -> "empty38_20Sprite",
      (39, 20) -> "empty39_20Sprite",
      (40, 20) -> "empty40_20Sprite",
      (41, 20) -> "spark_20_41Sprite",
      (42, 20) -> "sparkGlitterSprite",
      (43, 20) -> "empty43_20Sprite",
      (44, 20) -> "empty44_20Sprite",
      (45, 20) -> "empty45_20Sprite",
      (46, 20) -> "empty46_20Sprite",
      (47, 20) -> "glow_20_47Sprite",
      (48, 20) -> "glowBrightSprite",
      (0, 21) -> "num0Sprite",
      (1, 21) -> "num1Sprite",
      (2, 21) -> "num2Sprite",
      (3, 21) -> "num3Sprite",
      (4, 21) -> "num4Sprite",
      (5, 21) -> "num5Sprite",
      (6, 21) -> "num6Sprite",
      (7, 21) -> "num7Sprite",
      (8, 21) -> "num8Sprite",
      (9, 21) -> "num9Sprite",
      (10, 21) -> "letterASprite",
      (11, 21) -> "letterBSprite",
      (12, 21) -> "letterCSprite",
      (13, 21) -> "letterDSprite",
      (14, 21) -> "letterESprite",
      (15, 21) -> "letterFSprite",
      (16, 21) -> "letterGSprite",
      (17, 21) -> "letterHSprite",
      (18, 21) -> "letterISprite",
      (19, 21) -> "letterJSprite",
      (20, 21) -> "letterKSprite",
      (21, 21) -> "letterLSprite",
      (22, 21) -> "letterMSprite",
      (23, 21) -> "letterNSprite",
      (24, 21) -> "letterOSprite",
      (25, 21) -> "letterPSprite",
      (26, 21) -> "letterQSprite",
      (27, 21) -> "letterRSprite",
      (28, 21) -> "letterSSprite",
      (29, 21) -> "letterTSprite",
      (30, 21) -> "letterUSprite",
      (31, 21) -> "letterVSprite",
      (32, 21) -> "letterWSprite",
      (33, 21) -> "letterXSprite",
      (34, 21) -> "letterYSprite",
      (35, 21) -> "errorSprite",
      (36, 21) -> "warningSprite",
      (37, 21) -> "infoSprite",
      (38, 21) -> "debugSprite",
      (39, 21) -> "traceSprite",
      (40, 21) -> "log_21_40Sprite",
      (41, 21) -> "console_21_41Sprite",
      (42, 21) -> "terminal_21_42Sprite",
      (43, 21) -> "shellSprite",
      (44, 21) -> "promptSprite",
      (45, 21) -> "commandSprite",
      (46, 21) -> "outputSprite",
      (47, 21) -> "inputSprite",
      (48, 21) -> "bufferSprite",
  )

  // Row 0 sprites
  val maybeFloorSprite: Sprite = Sprite(1, 0, floorLayer)
  val floorSprite: Sprite = Sprite(2, 0, floorLayer)
  val crackedFloorSprite: Sprite = Sprite(3, 0, floorLayer)
  val wetFloorSprite: Sprite = Sprite(4, 0, floorLayer)
  val grassSprite: Sprite = Sprite(5, 0, floorLayer)
  val tallGrassSprite: Sprite = Sprite(6, 0, floorLayer)
  val flowersSprite: Sprite = Sprite(7, 0, floorLayer)
  val brickRedSprite: Sprite = Sprite(8, 0, floorLayer)
  val brickDarkSprite: Sprite = Sprite(9, 0, floorLayer)
  val brickGreySprite: Sprite = Sprite(10, 0, floorLayer)
  val woodFloorSprite: Sprite = Sprite(11, 0, floorLayer)
  val empty12_0Sprite: Sprite = Sprite(12, 0, floorLayer)
  val darkStoneSprite: Sprite = Sprite(14, 0, floorLayer)
  val cobblestoneSprite: Sprite = Sprite(15, 0, floorLayer)
  val stoneTileSprite: Sprite = Sprite(16, 0, floorLayer)
  val marbleSprite: Sprite = Sprite(17, 0, floorLayer)
  val sandSprite: Sprite = Sprite(18, 0, floorLayer)
  val dirtSprite: Sprite = Sprite(19, 0, floorLayer)
  val mudSprite: Sprite = Sprite(20, 0, floorLayer)
  val iceSprite: Sprite = Sprite(21, 0, floorLayer)
  val snowSprite: Sprite = Sprite(22, 0, floorLayer)
  val lavaSprite: Sprite = Sprite(23, 0, floorLayer)
  val ashSprite: Sprite = Sprite(24, 0, floorLayer)
  val playerSprite: Sprite = Sprite(25, 0, entityLayer)
  val enemySprite: Sprite = Sprite(26, 0, entityLayer)
  val npcSprite: Sprite = Sprite(27, 0, floorLayer)
  val merchantSprite: Sprite = Sprite(28, 0, floorLayer)
  val guardSprite: Sprite = Sprite(29, 0, floorLayer)
  val mageSprite: Sprite = Sprite(30, 0, floorLayer)
  val warriorSprite: Sprite = Sprite(31, 0, floorLayer)
  val rogueSprite: Sprite = Sprite(32, 0, floorLayer)
  val leatherHelmetSprite: Sprite = Sprite(33, 0, uiLayer)
  val nobleSprite: Sprite = Sprite(34, 0, floorLayer)
  val kingSprite: Sprite = Sprite(35, 0, floorLayer)
  val ironHelmetSprite: Sprite = Sprite(36, 0, uiLayer)
  val priestSprite: Sprite = Sprite(37, 0, floorLayer)
  val blacksmithSprite: Sprite = Sprite(38, 0, floorLayer)
  val ironBootsSprite: Sprite = Sprite(39, 0, uiLayer)
  val farmerSprite: Sprite = Sprite(40, 0, floorLayer)
  val ironGlovesSprite: Sprite = Sprite(41, 0, uiLayer)
  val fishermanSprite: Sprite = Sprite(42, 0, floorLayer)
  val hunterSprite: Sprite = Sprite(43, 0, floorLayer)
  val traderSprite: Sprite = Sprite(44, 0, floorLayer)
  val alchemistSprite: Sprite = Sprite(45, 0, floorLayer)
  val wizardSprite: Sprite = Sprite(46, 0, floorLayer)
  val witchSprite: Sprite = Sprite(47, 0, floorLayer)
  val sageSprite: Sprite = Sprite(48, 0, floorLayer)

  // Row 1 sprites
  val shrubSprite: Sprite = Sprite(0, 1, floorLayer)
  val bushSprite: Sprite = Sprite(1, 1, floorLayer)
  val treeSprite: Sprite = Sprite(2, 1, floorLayer)
  val rocksSprite: Sprite = Sprite(3, 1, floorLayer)
  val boulderSprite: Sprite = Sprite(4, 1, floorLayer)
  val pebblesSprite: Sprite = Sprite(5, 1, floorLayer)
  val mushroomSprite: Sprite = Sprite(6, 1, floorLayer)
  val fungusSprite: Sprite = Sprite(7, 1, floorLayer)
  val vinesSprite: Sprite = Sprite(8, 1, floorLayer)
  val mossSprite: Sprite = Sprite(9, 1, floorLayer)
  val lichenSprite: Sprite = Sprite(10, 1, floorLayer)
  val fernSprite: Sprite = Sprite(11, 1, floorLayer)
  val empty12_1Sprite: Sprite = Sprite(12, 1, floorLayer)
  val barrelSprite: Sprite = Sprite(15, 1, floorLayer)
  val crateSprite: Sprite = Sprite(16, 1, floorLayer)
  val sackSprite: Sprite = Sprite(17, 1, floorLayer)
  val urnSprite: Sprite = Sprite(18, 1, floorLayer)
  val vaseSprite: Sprite = Sprite(19, 1, floorLayer)
  val potSprite: Sprite = Sprite(20, 1, floorLayer)
  val jarSprite: Sprite = Sprite(21, 1, floorLayer)
  val basketSprite: Sprite = Sprite(22, 1, floorLayer)
  val bucketSprite: Sprite = Sprite(23, 1, floorLayer)
  val anvilSprite: Sprite = Sprite(24, 1, floorLayer)
  val forgeSprite: Sprite = Sprite(25, 1, floorLayer)
  val workbenchSprite: Sprite = Sprite(26, 1, floorLayer)
  val tableSprite: Sprite = Sprite(27, 1, floorLayer)
  val chairSprite: Sprite = Sprite(28, 1, floorLayer)
  val bedSprite: Sprite = Sprite(29, 1, floorLayer)
  val bookshelfSprite: Sprite = Sprite(30, 1, floorLayer)
  val armorSprite: Sprite = Sprite(31, 1, floorLayer)
  val chainmailArmorSprite: Sprite = Sprite(32, 1, uiLayer)
  val plateArmorSprite: Sprite = Sprite(33, 1, uiLayer)
  val helmSprite: Sprite = Sprite(34, 1, floorLayer)
  val shieldSprite: Sprite = Sprite(35, 1, floorLayer)
  val bucklerSprite: Sprite = Sprite(36, 1, floorLayer)
  val bootsSprite: Sprite = Sprite(37, 1, floorLayer)
  val ironBoots_1_38Sprite: Sprite = Sprite(38, 1, floorLayer)
  val leatherBootsSprite: Sprite = Sprite(39, 1, uiLayer)
  val ironGloves_1_40Sprite: Sprite = Sprite(40, 1, floorLayer)
  val leatherGlovesSprite: Sprite = Sprite(41, 1, uiLayer)
  val amuletSprite: Sprite = Sprite(42, 1, floorLayer)
  val crownSprite: Sprite = Sprite(43, 1, floorLayer)
  val circletSprite: Sprite = Sprite(44, 1, floorLayer)
  val tiaraSprite: Sprite = Sprite(45, 1, floorLayer)
  val braceletSprite: Sprite = Sprite(46, 1, floorLayer)
  val pendantSprite: Sprite = Sprite(47, 1, floorLayer)
  val gemSprite: Sprite = Sprite(48, 1, floorLayer)

  // Row 2 sprites
  val smallStoneSprite: Sprite = Sprite(0, 2, entityLayer)
  val mediumStoneSprite: Sprite = Sprite(1, 2, entityLayer)
  val largeStoneSprite: Sprite = Sprite(2, 2, entityLayer)
  val roundStoneSprite: Sprite = Sprite(3, 2, entityLayer)
  val boulderRockSprite: Sprite = Sprite(4, 2, entityLayer)
  val rockSprite: Sprite = Sprite(5, 2, floorLayer)
  val debrisSprite: Sprite = Sprite(6, 2, entityLayer)
  val gravelSprite: Sprite = Sprite(7, 2, entityLayer)
  val brickSprite: Sprite = Sprite(8, 2, entityLayer)
  val stoneTile_2_9Sprite: Sprite = Sprite(9, 2, entityLayer)
  val plankSprite: Sprite = Sprite(10, 2, entityLayer)
  val beamSprite: Sprite = Sprite(11, 2, entityLayer)
  val logSprite: Sprite = Sprite(12, 2, entityLayer)
  val stumpSprite: Sprite = Sprite(13, 2, entityLayer)
  val empty14_2Sprite: Sprite = Sprite(14, 2, entityLayer)
  val empty15_2Sprite: Sprite = Sprite(15, 2, entityLayer)
  val torchSprite: Sprite = Sprite(16, 2, entityLayer)
  val lanternSprite: Sprite = Sprite(17, 2, entityLayer)
  val candleSprite: Sprite = Sprite(18, 2, entityLayer)
  val brazierSprite: Sprite = Sprite(19, 2, entityLayer)
  val firepitSprite: Sprite = Sprite(20, 2, entityLayer)
  val campfireSprite: Sprite = Sprite(21, 2, entityLayer)
  val altarSprite: Sprite = Sprite(22, 2, entityLayer)
  val statueSprite: Sprite = Sprite(23, 2, entityLayer)
  val monumentSprite: Sprite = Sprite(24, 2, entityLayer)
  val bossSpriteTL: Sprite = Sprite(25, 2, entityLayer)
  val bossSpriteTR: Sprite = Sprite(26, 2, entityLayer)
  val bossSprite: Sprite = Sprite(27, 2, entityLayer)
  val gateSprite: Sprite = Sprite(28, 2, entityLayer)
  val fenceSprite: Sprite = Sprite(29, 2, entityLayer)
  val wall_2_30Sprite: Sprite = Sprite(30, 2, entityLayer)
  val barrierSprite: Sprite = Sprite(31, 2, entityLayer)
  val ladderSprite: Sprite = Sprite(32, 2, entityLayer)
  val ropeSprite: Sprite = Sprite(33, 2, entityLayer)
  val chainSprite: Sprite = Sprite(34, 2, entityLayer)
  val leverSprite: Sprite = Sprite(35, 2, entityLayer)
  val buttonSprite: Sprite = Sprite(36, 2, entityLayer)
  val wheelSprite: Sprite = Sprite(37, 2, entityLayer)
  val gearSprite: Sprite = Sprite(38, 2, entityLayer)
  val pipeSprite: Sprite = Sprite(39, 2, entityLayer)
  val valveSprite: Sprite = Sprite(40, 2, entityLayer)
  val machineSprite: Sprite = Sprite(41, 2, entityLayer)
  val deviceSprite: Sprite = Sprite(42, 2, entityLayer)
  val panelSprite: Sprite = Sprite(43, 2, entityLayer)
  val terminalSprite: Sprite = Sprite(44, 2, entityLayer)
  val consoleSprite: Sprite = Sprite(45, 2, entityLayer)
  val controlSprite: Sprite = Sprite(46, 2, entityLayer)
  val sprite47_2: Sprite = Sprite(47, 2, entityLayer)
  val sprite48_2: Sprite = Sprite(48, 2, entityLayer)

  // Row 3 sprites
  val skeletonSprite: Sprite = Sprite(0, 3, entityLayer)
  val zombieSprite: Sprite = Sprite(1, 3, entityLayer)
  val ghostSprite: Sprite = Sprite(2, 3, entityLayer)
  val wraithSprite: Sprite = Sprite(3, 3, entityLayer)
  val demonSprite: Sprite = Sprite(4, 3, entityLayer)
  val impSprite: Sprite = Sprite(5, 3, entityLayer)
  val devilSprite: Sprite = Sprite(6, 3, entityLayer)
  val fiendSprite: Sprite = Sprite(7, 3, entityLayer)
  val hellspawnSprite: Sprite = Sprite(8, 3, entityLayer)
  val goblinSprite: Sprite = Sprite(10, 3, entityLayer)
  val orcSprite: Sprite = Sprite(11, 3, entityLayer)
  val trollSprite: Sprite = Sprite(12, 3, entityLayer)
  val ogreSprite: Sprite = Sprite(13, 3, entityLayer)
  val giantSprite: Sprite = Sprite(14, 3, entityLayer)
  val cyclopsSprite: Sprite = Sprite(15, 3, entityLayer)
  val boss_3_16Sprite: Sprite = Sprite(16, 3, entityLayer)
  val boss_3_17Sprite: Sprite = Sprite(17, 3, entityLayer)
  val satyrSprite: Sprite = Sprite(19, 3, entityLayer)
  val harpySprite: Sprite = Sprite(20, 3, entityLayer)
  val empty21_3Sprite: Sprite = Sprite(21, 3, entityLayer)
  val empty22_3Sprite: Sprite = Sprite(22, 3, entityLayer)
  val empty23_3Sprite: Sprite = Sprite(23, 3, entityLayer)
  val empty24_3Sprite: Sprite = Sprite(24, 3, entityLayer)
  val bossSpriteBL: Sprite = Sprite(25, 3, entityLayer)
  val bossSpriteBR: Sprite = Sprite(26, 3, entityLayer)
  val drakeSprite: Sprite = Sprite(27, 3, entityLayer)
  val serpentSprite: Sprite = Sprite(28, 3, entityLayer)
  val basiliskSprite: Sprite = Sprite(29, 3, entityLayer)
  val cockatriceSprite: Sprite = Sprite(30, 3, entityLayer)
  val griffinSprite: Sprite = Sprite(31, 3, entityLayer)
  val hippogriffSprite: Sprite = Sprite(32, 3, entityLayer)
  val pegasusSprite: Sprite = Sprite(33, 3, entityLayer)
  val chimeraSprite: Sprite = Sprite(35, 3, entityLayer)
  val hydraSprite: Sprite = Sprite(36, 3, entityLayer)
  val manticoreSprite: Sprite = Sprite(37, 3, entityLayer)
  val sphinxSprite: Sprite = Sprite(38, 3, entityLayer)
  val phoenixSprite: Sprite = Sprite(39, 3, entityLayer)
  val rocSprite: Sprite = Sprite(40, 3, entityLayer)
  val krakenSprite: Sprite = Sprite(41, 3, entityLayer)
  val leviathanSprite: Sprite = Sprite(42, 3, entityLayer)
  val behemothSprite: Sprite = Sprite(43, 3, entityLayer)
  val golemSprite: Sprite = Sprite(44, 3, entityLayer)
  val elementalSprite: Sprite = Sprite(45, 3, entityLayer)
  val spiritSprite: Sprite = Sprite(46, 3, entityLayer)
  val shadeSprite: Sprite = Sprite(47, 3, entityLayer)
  val wraith_3_48Sprite: Sprite = Sprite(48, 3, entityLayer)

  // Row 4 sprites
  val woodWallSprite: Sprite = Sprite(0, 4, entityLayer)
  val stoneWallSprite: Sprite = Sprite(1, 4, entityLayer)
  val brickWallSprite: Sprite = Sprite(2, 4, entityLayer)
  val rockWallSprite: Sprite = Sprite(3, 4, entityLayer)
  val caveWallSprite: Sprite = Sprite(4, 4, entityLayer)
  val metalWallSprite: Sprite = Sprite(5, 4, entityLayer)
  val crystalWallSprite: Sprite = Sprite(6, 4, entityLayer)
  val iceWallSprite: Sprite = Sprite(7, 4, entityLayer)
  val waterDeepSprite: Sprite = Sprite(8, 4, entityLayer)
  val waterMidSprite: Sprite = Sprite(9, 4, entityLayer)
  val waterShallowSprite: Sprite = Sprite(10, 4, entityLayer)
  val lava_4_11Sprite: Sprite = Sprite(11, 4, entityLayer)
  val empty12_4Sprite: Sprite = Sprite(12, 4, entityLayer)
  val empty13_4Sprite: Sprite = Sprite(13, 4, entityLayer)
  val empty14_4Sprite: Sprite = Sprite(14, 4, entityLayer)
  val pitSprite: Sprite = Sprite(15, 4, entityLayer)
  val chasmSprite: Sprite = Sprite(16, 4, entityLayer)
  val abyssSprite: Sprite = Sprite(17, 4, entityLayer)
  val vortexSprite: Sprite = Sprite(19, 4, entityLayer)
  val riftSprite: Sprite = Sprite(20, 4, entityLayer)
  val stairs_4_21Sprite: Sprite = Sprite(21, 4, entityLayer)
  val ladder_4_22Sprite: Sprite = Sprite(22, 4, entityLayer)
  val trapdoorSprite: Sprite = Sprite(23, 4, entityLayer)
  val hatchSprite: Sprite = Sprite(24, 4, entityLayer)
  val grateSprite: Sprite = Sprite(25, 4, entityLayer)
  val drainSprite: Sprite = Sprite(26, 4, entityLayer)
  val fountainSprite: Sprite = Sprite(27, 4, entityLayer)
  val wellSprite: Sprite = Sprite(28, 4, entityLayer)
  val poolSprite: Sprite = Sprite(29, 4, entityLayer)
  val pondSprite: Sprite = Sprite(30, 4, entityLayer)
  val streamSprite: Sprite = Sprite(31, 4, entityLayer)
  val riverSprite: Sprite = Sprite(32, 4, entityLayer)
  val waterfallSprite: Sprite = Sprite(33, 4, entityLayer)
  val rapidsSprite: Sprite = Sprite(34, 4, entityLayer)
  val whirlpoolSprite: Sprite = Sprite(35, 4, entityLayer)
  val geyserSprite: Sprite = Sprite(36, 4, entityLayer)
  val springSprite: Sprite = Sprite(37, 4, entityLayer)
  val bogSprite: Sprite = Sprite(38, 4, entityLayer)
  val swampSprite: Sprite = Sprite(39, 4, entityLayer)
  val marshSprite: Sprite = Sprite(40, 4, entityLayer)
  val mireSprite: Sprite = Sprite(41, 4, entityLayer)
  val quicksandSprite: Sprite = Sprite(42, 4, entityLayer)
  val tarSprite: Sprite = Sprite(43, 4, entityLayer)
  val slime_4_44Sprite: Sprite = Sprite(44, 4, entityLayer)
  val oozeSprite: Sprite = Sprite(45, 4, entityLayer)
  val acidSprite: Sprite = Sprite(46, 4, entityLayer)
  val poisonSprite: Sprite = Sprite(47, 4, entityLayer)
  val sprite48_4: Sprite = Sprite(48, 4, entityLayer)

  // Row 5 sprites
  val forestSprite: Sprite = Sprite(0, 5, entityLayer)
  val grasslandSprite: Sprite = Sprite(1, 5, entityLayer)
  val desertSprite: Sprite = Sprite(2, 5, entityLayer)
  val tundraSprite: Sprite = Sprite(3, 5, entityLayer)
  val mountainSprite: Sprite = Sprite(4, 5, entityLayer)
  val hillSprite: Sprite = Sprite(5, 5, entityLayer)
  val cliffSprite: Sprite = Sprite(6, 5, entityLayer)
  val canyonSprite: Sprite = Sprite(7, 5, entityLayer)
  val waterSprite: Sprite = Sprite(8, 5, floorLayer)
  val waterShallow_5_9Sprite: Sprite = Sprite(9, 5, entityLayer)
  val waterDeep_5_10Sprite: Sprite = Sprite(10, 5, entityLayer)
  val bridge_5_11Sprite: Sprite = Sprite(11, 5, entityLayer)
  val pathSprite: Sprite = Sprite(12, 5, entityLayer)
  val roadSprite: Sprite = Sprite(13, 5, entityLayer)
  val cobblestone_5_14Sprite: Sprite = Sprite(14, 5, entityLayer)
  val pavementSprite: Sprite = Sprite(15, 5, entityLayer)
  val bridgeSprite: Sprite = Sprite(16, 5, floorLayer)
  val crystalBlueSprite: Sprite = Sprite(17, 5, entityLayer)
  val crystalRedSprite: Sprite = Sprite(18, 5, entityLayer)
  val crystalGreenSprite: Sprite = Sprite(19, 5, entityLayer)
  val crystalYellowSprite: Sprite = Sprite(20, 5, entityLayer)
  val crystalPurpleSprite: Sprite = Sprite(21, 5, entityLayer)
  val crystalWhiteSprite: Sprite = Sprite(22, 5, entityLayer)
  val crystalBlackSprite: Sprite = Sprite(23, 5, entityLayer)
  val gemSmallSprite: Sprite = Sprite(24, 5, entityLayer)
  val gemMediumSprite: Sprite = Sprite(25, 5, entityLayer)
  val diamondSprite: Sprite = Sprite(26, 5, entityLayer)
  val rubySprite: Sprite = Sprite(27, 5, entityLayer)
  val emeraldSprite: Sprite = Sprite(28, 5, entityLayer)
  val sapphireSprite: Sprite = Sprite(29, 5, entityLayer)
  val amethystSprite: Sprite = Sprite(30, 5, entityLayer)
  val topazSprite: Sprite = Sprite(31, 5, entityLayer)
  val opalSprite: Sprite = Sprite(32, 5, entityLayer)
  val pearlSprite: Sprite = Sprite(33, 5, entityLayer)
  val amberSprite: Sprite = Sprite(34, 5, entityLayer)
  val jadeSprite: Sprite = Sprite(35, 5, entityLayer)
  val onyxSprite: Sprite = Sprite(36, 5, entityLayer)
  val obsidianSprite: Sprite = Sprite(37, 5, entityLayer)
  val quartzSprite: Sprite = Sprite(38, 5, entityLayer)
  val empty39_5Sprite: Sprite = Sprite(39, 5, entityLayer)
  val arrowSprite: Sprite = Sprite(40, 5, entityLayer)
  val coalSprite: Sprite = Sprite(41, 5, entityLayer)
  val ironSprite: Sprite = Sprite(42, 5, entityLayer)
  val copperSprite: Sprite = Sprite(43, 5, entityLayer)
  val silverSprite: Sprite = Sprite(44, 5, entityLayer)
  val goldSprite: Sprite = Sprite(45, 5, entityLayer)
  val platinumSprite: Sprite = Sprite(46, 5, entityLayer)
  val mithrilSprite: Sprite = Sprite(47, 5, entityLayer)
  val adamantSprite: Sprite = Sprite(48, 5, entityLayer)

  // Row 6 sprites
  val daggerSprite: Sprite = Sprite(0, 6, entityLayer)
  val knifeSprite: Sprite = Sprite(1, 6, entityLayer)
  val shortswordSprite: Sprite = Sprite(2, 6, entityLayer)
  val swordSprite: Sprite = Sprite(3, 6, entityLayer)
  val longswordSprite: Sprite = Sprite(4, 6, entityLayer)
  val greatswordSprite: Sprite = Sprite(5, 6, entityLayer)
  val scimitarSprite: Sprite = Sprite(6, 6, entityLayer)
  val katanaSprite: Sprite = Sprite(7, 6, entityLayer)
  val rapierSprite: Sprite = Sprite(8, 6, entityLayer)
  val saberSprite: Sprite = Sprite(9, 6, entityLayer)
  val cutlassSprite: Sprite = Sprite(10, 6, entityLayer)
  val falchionSprite: Sprite = Sprite(11, 6, entityLayer)
  val claymoreSprite: Sprite = Sprite(12, 6, entityLayer)
  val bladeSprite: Sprite = Sprite(13, 6, entityLayer)
  val axeSprite: Sprite = Sprite(14, 6, entityLayer)
  val handaxeSprite: Sprite = Sprite(15, 6, entityLayer)
  val battleaxeSprite: Sprite = Sprite(16, 6, entityLayer)
  val waraxeSprite: Sprite = Sprite(17, 6, entityLayer)
  val halberdSprite: Sprite = Sprite(18, 6, entityLayer)
  val poleaxeSprite: Sprite = Sprite(19, 6, entityLayer)
  val pikeSprite: Sprite = Sprite(20, 6, entityLayer)
  val spearSprite: Sprite = Sprite(21, 6, entityLayer)
  val javelinSprite: Sprite = Sprite(22, 6, entityLayer)
  val lanceSprite: Sprite = Sprite(23, 6, entityLayer)
  val tridentSprite: Sprite = Sprite(24, 6, entityLayer)
  val quarterstaffSprite: Sprite = Sprite(25, 6, entityLayer)
  val maceSprite: Sprite = Sprite(26, 6, entityLayer)
  val morningstarSprite: Sprite = Sprite(27, 6, entityLayer)
  val flailSprite: Sprite = Sprite(28, 6, entityLayer)
  val clubSprite: Sprite = Sprite(29, 6, entityLayer)
  val hammerSprite: Sprite = Sprite(30, 6, entityLayer)
  val warhammerSprite: Sprite = Sprite(31, 6, entityLayer)
  val basicSwordSprite: Sprite = Sprite(32, 6, uiLayer)
  val bow_6_33Sprite: Sprite = Sprite(33, 6, entityLayer)
  val longbowSprite: Sprite = Sprite(34, 6, entityLayer)
  val shortbowSprite: Sprite = Sprite(35, 6, entityLayer)
  val crossbowSprite: Sprite = Sprite(36, 6, entityLayer)
  val bowSprite: Sprite = Sprite(37, 6, uiLayer)
  val arbalestSprite: Sprite = Sprite(38, 6, entityLayer)
  val slingSprite: Sprite = Sprite(39, 6, entityLayer)
  val blowgunSprite: Sprite = Sprite(40, 6, entityLayer)
  val dartSprite: Sprite = Sprite(41, 6, entityLayer)
  val shurikenSprite: Sprite = Sprite(42, 6, entityLayer)
  val chakramSprite: Sprite = Sprite(43, 6, entityLayer)
  val boomerangSprite: Sprite = Sprite(44, 6, entityLayer)
  val whipSprite: Sprite = Sprite(45, 6, entityLayer)
  val nunchakuSprite: Sprite = Sprite(46, 6, entityLayer)
  val flail_6_47Sprite: Sprite = Sprite(47, 6, entityLayer)
  val chain_6_48Sprite: Sprite = Sprite(48, 6, entityLayer)

  // Row 7 sprites
  val clothArmorSprite: Sprite = Sprite(0, 7, entityLayer)
  val leatherArmorSprite: Sprite = Sprite(1, 7, entityLayer)
  val studdedSprite: Sprite = Sprite(2, 7, entityLayer)
  val hideSprite: Sprite = Sprite(3, 7, entityLayer)
  val chainmailSprite: Sprite = Sprite(4, 7, entityLayer)
  val scalemailSprite: Sprite = Sprite(5, 7, entityLayer)
  val ringmailSprite: Sprite = Sprite(6, 7, entityLayer)
  val splintSprite: Sprite = Sprite(7, 7, entityLayer)
  val plateSprite: Sprite = Sprite(8, 7, entityLayer)
  val fullPlateSprite: Sprite = Sprite(9, 7, entityLayer)
  val shield_7_10Sprite: Sprite = Sprite(10, 7, entityLayer)
  val buckler_7_11Sprite: Sprite = Sprite(11, 7, entityLayer)
  val towerSprite: Sprite = Sprite(12, 7, entityLayer)
  val kiteSprite: Sprite = Sprite(13, 7, entityLayer)
  val roundSprite: Sprite = Sprite(14, 7, entityLayer)
  val helmetSprite: Sprite = Sprite(15, 7, entityLayer)
  val helm_7_16Sprite: Sprite = Sprite(16, 7, entityLayer)
  val capSprite: Sprite = Sprite(17, 7, entityLayer)
  val coifSprite: Sprite = Sprite(18, 7, entityLayer)
  val cowlSprite: Sprite = Sprite(19, 7, entityLayer)
  val hoodSprite: Sprite = Sprite(20, 7, entityLayer)
  val maskSprite: Sprite = Sprite(21, 7, entityLayer)
  val visorSprite: Sprite = Sprite(22, 7, entityLayer)
  val faceplateSprite: Sprite = Sprite(23, 7, entityLayer)
  val gauntletsSprite: Sprite = Sprite(24, 7, entityLayer)
  val glovesSprite: Sprite = Sprite(25, 7, entityLayer)
  val bracersSprite: Sprite = Sprite(26, 7, entityLayer)
  val vambracesSprite: Sprite = Sprite(27, 7, entityLayer)
  val greavesSprite: Sprite = Sprite(28, 7, entityLayer)
  val boots_7_29Sprite: Sprite = Sprite(29, 7, entityLayer)
  val sabatonsSprite: Sprite = Sprite(30, 7, entityLayer)
  val cuissesSprite: Sprite = Sprite(31, 7, entityLayer)
  val ironSwordSprite: Sprite = Sprite(32, 7, uiLayer)
  val pauldronsSprite: Sprite = Sprite(33, 7, entityLayer)
  val cuirassSprite: Sprite = Sprite(34, 7, entityLayer)
  val breastplateSprite: Sprite = Sprite(35, 7, entityLayer)
  val chestplateSprite: Sprite = Sprite(36, 7, entityLayer)
  val gorgetSprite: Sprite = Sprite(37, 7, entityLayer)
  val capeSprite: Sprite = Sprite(38, 7, entityLayer)
  val cloakSprite: Sprite = Sprite(39, 7, entityLayer)
  val robeSprite: Sprite = Sprite(40, 7, entityLayer)
  val tunicSprite: Sprite = Sprite(41, 7, entityLayer)
  val surcoatSprite: Sprite = Sprite(42, 7, entityLayer)
  val tabardSprite: Sprite = Sprite(43, 7, entityLayer)
  val vestSprite: Sprite = Sprite(44, 7, entityLayer)
  val jerkinSprite: Sprite = Sprite(45, 7, entityLayer)
  val doubletSprite: Sprite = Sprite(46, 7, entityLayer)
  val hauberkSprite: Sprite = Sprite(47, 7, entityLayer)
  val sprite48_7: Sprite = Sprite(48, 7, entityLayer)

  // Row 8 sprites
  val batSprite: Sprite = Sprite(0, 8, entityLayer)
  val spiderSprite: Sprite = Sprite(1, 8, entityLayer)
  val scorpionSprite: Sprite = Sprite(2, 8, entityLayer)
  val beetleSprite: Sprite = Sprite(3, 8, entityLayer)
  val antSprite: Sprite = Sprite(4, 8, entityLayer)
  val waspSprite: Sprite = Sprite(5, 8, entityLayer)
  val beeSprite: Sprite = Sprite(6, 8, entityLayer)
  val hornetSprite: Sprite = Sprite(7, 8, entityLayer)
  val mothSprite: Sprite = Sprite(8, 8, entityLayer)
  val butterflySprite: Sprite = Sprite(9, 8, entityLayer)
  val dragonflySprite: Sprite = Sprite(10, 8, entityLayer)
  val centipedeSprite: Sprite = Sprite(11, 8, entityLayer)
  val millipedeSprite: Sprite = Sprite(12, 8, entityLayer)
  val wormSprite: Sprite = Sprite(13, 8, entityLayer)
  val slugSprite: Sprite = Sprite(14, 8, entityLayer)
  val snailSprite: Sprite = Sprite(15, 8, entityLayer)
  val leechSprite: Sprite = Sprite(16, 8, entityLayer)
  val tickSprite: Sprite = Sprite(17, 8, entityLayer)
  val fleaSprite: Sprite = Sprite(18, 8, entityLayer)
  val miteSprite: Sprite = Sprite(19, 8, entityLayer)
  val maggotSprite: Sprite = Sprite(20, 8, entityLayer)
  val larvaSprite: Sprite = Sprite(21, 8, entityLayer)
  val cocoonSprite: Sprite = Sprite(22, 8, entityLayer)
  val eggSprite: Sprite = Sprite(23, 8, entityLayer)
  val pupaSprite: Sprite = Sprite(24, 8, entityLayer)
  val chrysalisSprite: Sprite = Sprite(25, 8, entityLayer)
  val hiveSprite: Sprite = Sprite(26, 8, entityLayer)
  val snake_8_27Sprite: Sprite = Sprite(27, 8, entityLayer)
  val snakeSprite: Sprite = Sprite(28, 8, entityLayer)
  val slimeSprite: Sprite = Sprite(29, 8, entityLayer)
  val slimeletSprite: Sprite = Sprite(30, 8, entityLayer)
  val ratSprite: Sprite = Sprite(31, 8, entityLayer)
  val mouseSprite: Sprite = Sprite(32, 8, entityLayer)
  val voleSprite: Sprite = Sprite(33, 8, entityLayer)
  val shrewSprite: Sprite = Sprite(34, 8, entityLayer)
  val moleSprite: Sprite = Sprite(35, 8, entityLayer)
  val badgerSprite: Sprite = Sprite(36, 8, entityLayer)
  val weaselSprite: Sprite = Sprite(37, 8, entityLayer)
  val ferretSprite: Sprite = Sprite(38, 8, entityLayer)
  val otterSprite: Sprite = Sprite(39, 8, entityLayer)
  val beaverSprite: Sprite = Sprite(40, 8, entityLayer)
  val rabbitSprite: Sprite = Sprite(41, 8, entityLayer)
  val hareSprite: Sprite = Sprite(42, 8, entityLayer)
  val squirrelSprite: Sprite = Sprite(43, 8, entityLayer)
  val chipmunkSprite: Sprite = Sprite(44, 8, entityLayer)
  val hedgehogSprite: Sprite = Sprite(45, 8, entityLayer)
  val porcupineSprite: Sprite = Sprite(46, 8, entityLayer)
  val armadilloSprite: Sprite = Sprite(47, 8, entityLayer)
  val critterSprite: Sprite = Sprite(48, 8, entityLayer)

  // Row 9 sprites
  val blueDoorSprite: Sprite = Sprite(0, 9, entityLayer)
  val dogSprite: Sprite = Sprite(1, 9, entityLayer)
  val foxSprite: Sprite = Sprite(2, 9, entityLayer)
  val coyoteSprite: Sprite = Sprite(3, 9, entityLayer)
  val jackalSprite: Sprite = Sprite(4, 9, entityLayer)
  val hyenaSprite: Sprite = Sprite(5, 9, entityLayer)
  val bearSprite: Sprite = Sprite(6, 9, entityLayer)
  val lionSprite: Sprite = Sprite(7, 9, entityLayer)
  val tigerSprite: Sprite = Sprite(8, 9, entityLayer)
  val leopardSprite: Sprite = Sprite(9, 9, entityLayer)
  val pantherSprite: Sprite = Sprite(10, 9, entityLayer)
  val cheetahSprite: Sprite = Sprite(11, 9, entityLayer)
  val empty12_9Sprite: Sprite = Sprite(12, 9, entityLayer)
  val lynxSprite: Sprite = Sprite(13, 9, entityLayer)
  val bobcatSprite: Sprite = Sprite(14, 9, entityLayer)
  val wildcatSprite: Sprite = Sprite(15, 9, entityLayer)
  val jaguarSprite: Sprite = Sprite(16, 9, entityLayer)
  val pumaSprite: Sprite = Sprite(17, 9, entityLayer)
  val caracalSprite: Sprite = Sprite(18, 9, entityLayer)
  val servalSprite: Sprite = Sprite(19, 9, entityLayer)
  val mongooseSprite: Sprite = Sprite(20, 9, entityLayer)
  val boarSprite: Sprite = Sprite(21, 9, entityLayer)
  val pigSprite: Sprite = Sprite(22, 9, entityLayer)
  val warthogSprite: Sprite = Sprite(23, 9, entityLayer)
  val deerSprite: Sprite = Sprite(24, 9, entityLayer)
  val elkSprite: Sprite = Sprite(25, 9, entityLayer)
  val mooseSprite: Sprite = Sprite(26, 9, entityLayer)
  val caribouSprite: Sprite = Sprite(27, 9, entityLayer)
  val reindeerSprite: Sprite = Sprite(28, 9, entityLayer)
  val antelopeSprite: Sprite = Sprite(29, 9, entityLayer)
  val gazelleSprite: Sprite = Sprite(30, 9, entityLayer)
  val impalaSprite: Sprite = Sprite(31, 9, entityLayer)
  val buffaloSprite: Sprite = Sprite(32, 9, entityLayer)
  val bisonSprite: Sprite = Sprite(33, 9, entityLayer)
  val oxSprite: Sprite = Sprite(34, 9, entityLayer)
  val bullSprite: Sprite = Sprite(35, 9, entityLayer)
  val cowSprite: Sprite = Sprite(36, 9, entityLayer)
  val yakSprite: Sprite = Sprite(37, 9, entityLayer)
  val goatSprite: Sprite = Sprite(38, 9, entityLayer)
  val sheepSprite: Sprite = Sprite(39, 9, entityLayer)
  val ramSprite: Sprite = Sprite(40, 9, entityLayer)
  val llamaSprite: Sprite = Sprite(41, 9, entityLayer)
  val alpacaSprite: Sprite = Sprite(42, 9, entityLayer)
  val camelSprite: Sprite = Sprite(43, 9, entityLayer)
  val horseSprite: Sprite = Sprite(44, 9, entityLayer)
  val ponySprite: Sprite = Sprite(45, 9, entityLayer)
  val donkeySprite: Sprite = Sprite(46, 9, entityLayer)
  val muleSprite: Sprite = Sprite(47, 9, entityLayer)
  val zebraSprite: Sprite = Sprite(48, 9, entityLayer)

  // Row 10 sprites
  val redDoorSprite: Sprite = Sprite(0, 10, entityLayer)
  val breadSprite: Sprite = Sprite(1, 10, uiLayer)
  val cheeseSprite: Sprite = Sprite(2, 10, uiLayer)
  val meatSprite: Sprite = Sprite(3, 10, uiLayer)
  val fishSprite: Sprite = Sprite(4, 10, uiLayer)
  val chickenSprite: Sprite = Sprite(5, 10, uiLayer)
  val turkeySprite: Sprite = Sprite(6, 10, uiLayer)
  val hamSprite: Sprite = Sprite(7, 10, uiLayer)
  val baconSprite: Sprite = Sprite(8, 10, uiLayer)
  val sausageSprite: Sprite = Sprite(9, 10, uiLayer)
  val egg_10_10Sprite: Sprite = Sprite(10, 10, uiLayer)
  val milkSprite: Sprite = Sprite(11, 10, uiLayer)
  val butterSprite: Sprite = Sprite(12, 10, uiLayer)
  val honeySprite: Sprite = Sprite(13, 10, uiLayer)
  val jamSprite: Sprite = Sprite(14, 10, uiLayer)
  val jellySprite: Sprite = Sprite(15, 10, uiLayer)
  val empty16_10Sprite: Sprite = Sprite(16, 10, uiLayer)
  val pieSprite: Sprite = Sprite(17, 10, uiLayer)
  val cakeSprite: Sprite = Sprite(18, 10, uiLayer)
  val cookieSprite: Sprite = Sprite(19, 10, uiLayer)
  val biscuitSprite: Sprite = Sprite(20, 10, uiLayer)
  val crackerSprite: Sprite = Sprite(21, 10, uiLayer)
  val pastrySprite: Sprite = Sprite(22, 10, uiLayer)
  val tartSprite: Sprite = Sprite(23, 10, uiLayer)
  val muffinSprite: Sprite = Sprite(24, 10, uiLayer)
  val sconeSprite: Sprite = Sprite(25, 10, uiLayer)
  val bagelSprite: Sprite = Sprite(26, 10, uiLayer)
  val donutSprite: Sprite = Sprite(27, 10, uiLayer)
  val croissantSprite: Sprite = Sprite(28, 10, uiLayer)
  val pretzelSprite: Sprite = Sprite(29, 10, uiLayer)
  val waferSprite: Sprite = Sprite(30, 10, uiLayer)
  val waffleSprite: Sprite = Sprite(31, 10, uiLayer)
  val pancakeSprite: Sprite = Sprite(32, 10, uiLayer)
  val fruitSprite: Sprite = Sprite(33, 10, uiLayer)
  val vegetableSprite: Sprite = Sprite(34, 10, uiLayer)
  val grainSprite: Sprite = Sprite(35, 10, uiLayer)
  val riceSprite: Sprite = Sprite(36, 10, uiLayer)
  val wheatSprite: Sprite = Sprite(37, 10, uiLayer)
  val cornSprite: Sprite = Sprite(38, 10, uiLayer)
  val barleySprite: Sprite = Sprite(39, 10, uiLayer)
  val emptyHeartSprite: Sprite = Sprite(40, 10, uiLayer)
  val halfHeartSprite: Sprite = Sprite(41, 10, uiLayer)
  val fullHeartSprite: Sprite = Sprite(42, 10, uiLayer)
  val coinSprite: Sprite = Sprite(43, 10, uiLayer)
  val gold_10_44Sprite: Sprite = Sprite(44, 10, uiLayer)
  val silver_10_45Sprite: Sprite = Sprite(45, 10, uiLayer)
  val copper_10_46Sprite: Sprite = Sprite(46, 10, uiLayer)
  val bronzeSprite: Sprite = Sprite(47, 10, uiLayer)
  val treasureSprite: Sprite = Sprite(48, 10, uiLayer)

  // Row 11 sprites
  val yellowDoorSprite: Sprite = Sprite(0, 11, entityLayer)
  val scroll_11_1Sprite: Sprite = Sprite(1, 11, uiLayer)
  val tomeSprite: Sprite = Sprite(2, 11, uiLayer)
  val grimoireSprite: Sprite = Sprite(3, 11, uiLayer)
  val codexSprite: Sprite = Sprite(4, 11, uiLayer)
  val manuscriptSprite: Sprite = Sprite(5, 11, uiLayer)
  val parchmentSprite: Sprite = Sprite(6, 11, uiLayer)
  val paperSprite: Sprite = Sprite(7, 11, uiLayer)
  val letterSprite: Sprite = Sprite(8, 11, uiLayer)
  val yellowDoor_11_9Sprite: Sprite = Sprite(9, 11, uiLayer)
  val blueDoor_11_10Sprite: Sprite = Sprite(10, 11, uiLayer)
  val redDoor_11_11Sprite: Sprite = Sprite(11, 11, uiLayer)
  val mapSprite: Sprite = Sprite(12, 11, uiLayer)
  val chartSprite: Sprite = Sprite(13, 11, uiLayer)
  val atlasSprite: Sprite = Sprite(14, 11, uiLayer)
  val compassSprite: Sprite = Sprite(15, 11, uiLayer)
  val sextantSprite: Sprite = Sprite(16, 11, uiLayer)
  val astrolabeSprite: Sprite = Sprite(17, 11, uiLayer)
  val telescopeSprite: Sprite = Sprite(18, 11, uiLayer)
  val spyglassSprite: Sprite = Sprite(19, 11, uiLayer)
  val magnifierSprite: Sprite = Sprite(20, 11, uiLayer)
  val lensSprite: Sprite = Sprite(21, 11, uiLayer)
  val prismSprite: Sprite = Sprite(22, 11, uiLayer)
  val mirrorSprite: Sprite = Sprite(23, 11, uiLayer)
  val hourglassSprite: Sprite = Sprite(24, 11, uiLayer)
  val sundialSprite: Sprite = Sprite(25, 11, uiLayer)
  val clockSprite: Sprite = Sprite(26, 11, uiLayer)
  val watchSprite: Sprite = Sprite(27, 11, uiLayer)
  val projectileSprite: Sprite = Sprite(28, 11, entityLayer)
  val arrow_11_29Sprite: Sprite = Sprite(29, 11, uiLayer)
  val boltSprite: Sprite = Sprite(30, 11, uiLayer)
  val yellowKey_11_31Sprite: Sprite = Sprite(31, 11, uiLayer)
  val yellowKeySprite: Sprite = Sprite(32, 11, uiLayer)
  val blueKeySprite: Sprite = Sprite(33, 11, uiLayer)
  val redKeySprite: Sprite = Sprite(34, 11, uiLayer)
  val purpleKeySprite: Sprite = Sprite(35, 11, uiLayer)
  val whiteKeySprite: Sprite = Sprite(36, 11, uiLayer)
  val blackKeySprite: Sprite = Sprite(37, 11, uiLayer)
  val keySprite: Sprite = Sprite(38, 11, uiLayer)
  val lockpickSprite: Sprite = Sprite(39, 11, uiLayer)
  val crowbarSprite: Sprite = Sprite(40, 11, uiLayer)
  val pickSprite: Sprite = Sprite(41, 11, uiLayer)
  val shovelSprite: Sprite = Sprite(42, 11, uiLayer)
  val hoeSprite: Sprite = Sprite(43, 11, uiLayer)
  val rakeSprite: Sprite = Sprite(44, 11, uiLayer)
  val sickleSprite: Sprite = Sprite(45, 11, uiLayer)
  val scytheSprite: Sprite = Sprite(46, 11, uiLayer)
  val pitchforkSprite: Sprite = Sprite(47, 11, uiLayer)
  val toolSprite: Sprite = Sprite(48, 11, uiLayer)

  // Row 12 sprites
  val vialSprite: Sprite = Sprite(0, 12, uiLayer)
  val flaskSprite: Sprite = Sprite(1, 12, uiLayer)
  val bottleSprite: Sprite = Sprite(2, 12, uiLayer)
  val phialSprite: Sprite = Sprite(3, 12, uiLayer)
  val elixirSprite: Sprite = Sprite(4, 12, uiLayer)
  val tonicSprite: Sprite = Sprite(5, 12, uiLayer)
  val draughtSprite: Sprite = Sprite(6, 12, uiLayer)
  val brewSprite: Sprite = Sprite(7, 12, uiLayer)
  val mixtureSprite: Sprite = Sprite(8, 12, uiLayer)
  val concoctionSprite: Sprite = Sprite(9, 12, uiLayer)
  val empty10_12Sprite: Sprite = Sprite(10, 12, uiLayer)
  val empty11_12Sprite: Sprite = Sprite(11, 12, uiLayer)
  val empty13_12Sprite: Sprite = Sprite(13, 12, uiLayer)
  val antidoteSprite: Sprite = Sprite(14, 12, uiLayer)
  val remedySprite: Sprite = Sprite(15, 12, uiLayer)
  val cureSprite: Sprite = Sprite(16, 12, uiLayer)
  val salveSprite: Sprite = Sprite(17, 12, uiLayer)
  val ointmentSprite: Sprite = Sprite(18, 12, uiLayer)
  val balmSprite: Sprite = Sprite(19, 12, uiLayer)
  val oilSprite: Sprite = Sprite(20, 12, uiLayer)
  val perfumeSprite: Sprite = Sprite(21, 12, uiLayer)
  val essenceSprite: Sprite = Sprite(22, 12, uiLayer)
  val extractSprite: Sprite = Sprite(23, 12, uiLayer)
  val tinctureSprite: Sprite = Sprite(24, 12, uiLayer)
  val serumSprite: Sprite = Sprite(25, 12, uiLayer)
  val toxinSprite: Sprite = Sprite(26, 12, uiLayer)
  val poison_12_27Sprite: Sprite = Sprite(27, 12, uiLayer)
  val venomSprite: Sprite = Sprite(28, 12, uiLayer)
  val acid_12_29Sprite: Sprite = Sprite(29, 12, uiLayer)
  val reagentSprite: Sprite = Sprite(30, 12, uiLayer)
  val catalystSprite: Sprite = Sprite(31, 12, uiLayer)
  val solventSprite: Sprite = Sprite(32, 12, uiLayer)
  val solutionSprite: Sprite = Sprite(33, 12, uiLayer)
  val gelSprite: Sprite = Sprite(34, 12, uiLayer)
  val pasteSprite: Sprite = Sprite(35, 12, uiLayer)
  val powderSprite: Sprite = Sprite(36, 12, uiLayer)
  val crystalSprite: Sprite = Sprite(37, 12, uiLayer)
  val shardSprite: Sprite = Sprite(38, 12, uiLayer)
  val fragmentSprite: Sprite = Sprite(39, 12, uiLayer)
  val dustSprite: Sprite = Sprite(40, 12, uiLayer)
  val ash_12_41Sprite: Sprite = Sprite(41, 12, uiLayer)
  val saltSprite: Sprite = Sprite(42, 12, uiLayer)
  val spiceSprite: Sprite = Sprite(43, 12, uiLayer)
  val herbSprite: Sprite = Sprite(44, 12, uiLayer)
  val rootSprite: Sprite = Sprite(45, 12, uiLayer)
  val seedSprite: Sprite = Sprite(46, 12, uiLayer)
  val flowerSprite: Sprite = Sprite(47, 12, uiLayer)
  val leafSprite: Sprite = Sprite(48, 12, uiLayer)

  // Row 13 sprites
  val wandSprite: Sprite = Sprite(0, 13, uiLayer)
  val rodSprite: Sprite = Sprite(1, 13, uiLayer)
  val staffSprite: Sprite = Sprite(2, 13, uiLayer)
  val scepterSprite: Sprite = Sprite(3, 13, uiLayer)
  val orbSprite: Sprite = Sprite(4, 13, uiLayer)
  val crystal_13_5Sprite: Sprite = Sprite(5, 13, uiLayer)
  val sphereSprite: Sprite = Sprite(6, 13, uiLayer)
  val globeSprite: Sprite = Sprite(7, 13, uiLayer)
  val ballSprite: Sprite = Sprite(8, 13, uiLayer)
  val ringSprite: Sprite = Sprite(9, 13, uiLayer)
  val amulet_13_10Sprite: Sprite = Sprite(10, 13, uiLayer)
  val talismanSprite: Sprite = Sprite(11, 13, uiLayer)
  val charmSprite: Sprite = Sprite(12, 13, uiLayer)
  val totemSprite: Sprite = Sprite(13, 13, uiLayer)
  val fetishSprite: Sprite = Sprite(14, 13, uiLayer)
  val idolSprite: Sprite = Sprite(15, 13, uiLayer)
  val figurineSprite: Sprite = Sprite(16, 13, uiLayer)
  val statuetteSprite: Sprite = Sprite(17, 13, uiLayer)
  val trophySprite: Sprite = Sprite(18, 13, uiLayer)
  val relicSprite: Sprite = Sprite(19, 13, uiLayer)
  val artifactSprite: Sprite = Sprite(20, 13, uiLayer)
  val heirloomSprite: Sprite = Sprite(21, 13, uiLayer)
  val antiqueSprite: Sprite = Sprite(22, 13, uiLayer)
  val scroll_13_23Sprite: Sprite = Sprite(23, 13, uiLayer)
  val spellbookSprite: Sprite = Sprite(24, 13, uiLayer)
  val runestoneSprite: Sprite = Sprite(25, 13, uiLayer)
  val tabletSprite: Sprite = Sprite(26, 13, uiLayer)
  val obeliskSprite: Sprite = Sprite(27, 13, uiLayer)
  val monolithSprite: Sprite = Sprite(28, 13, uiLayer)
  val pillarSprite: Sprite = Sprite(29, 13, uiLayer)
  val columnSprite: Sprite = Sprite(30, 13, uiLayer)
  val totem_13_31Sprite: Sprite = Sprite(31, 13, uiLayer)
  val ankhSprite: Sprite = Sprite(32, 13, uiLayer)
  val potionSprite: Sprite = Sprite(33, 13, uiLayer)
  val pentacleSprite: Sprite = Sprite(34, 13, uiLayer)
  val hexagramSprite: Sprite = Sprite(35, 13, uiLayer)
  val runeSprite: Sprite = Sprite(36, 13, uiLayer)
  val defaultItemSprite: Sprite = Sprite(37, 13, uiLayer)
  val sigilSprite: Sprite = Sprite(38, 13, uiLayer)
  val symbolSprite: Sprite = Sprite(39, 13, uiLayer)
  val markSprite: Sprite = Sprite(40, 13, uiLayer)
  val emblemSprite: Sprite = Sprite(41, 13, uiLayer)
  val insigniaSprite: Sprite = Sprite(42, 13, uiLayer)
  val badgeSprite: Sprite = Sprite(43, 13, uiLayer)
  val crestSprite: Sprite = Sprite(44, 13, uiLayer)
  val sealSprite: Sprite = Sprite(45, 13, uiLayer)
  val stampSprite: Sprite = Sprite(46, 13, uiLayer)
  val tokenSprite: Sprite = Sprite(47, 13, uiLayer)
  val medallionSprite: Sprite = Sprite(48, 13, uiLayer)

  // Row 14 sprites
  val checkSprite: Sprite = Sprite(0, 14, uiLayer)
  val crossSprite: Sprite = Sprite(1, 14, uiLayer)
  val plusSprite: Sprite = Sprite(2, 14, uiLayer)
  val minusSprite: Sprite = Sprite(3, 14, uiLayer)
  val multiplySprite: Sprite = Sprite(4, 14, uiLayer)
  val divideSprite: Sprite = Sprite(5, 14, uiLayer)
  val equalsSprite: Sprite = Sprite(6, 14, uiLayer)
  val percentSprite: Sprite = Sprite(7, 14, uiLayer)
  val dollarSprite: Sprite = Sprite(8, 14, uiLayer)
  val poundSprite: Sprite = Sprite(9, 14, uiLayer)
  val euroSprite: Sprite = Sprite(10, 14, uiLayer)
  val yenSprite: Sprite = Sprite(11, 14, uiLayer)
  val questionSprite: Sprite = Sprite(12, 14, uiLayer)
  val exclamationSprite: Sprite = Sprite(13, 14, uiLayer)
  val periodSprite: Sprite = Sprite(14, 14, uiLayer)
  val commaSprite: Sprite = Sprite(15, 14, uiLayer)
  val colonSprite: Sprite = Sprite(16, 14, uiLayer)
  val semicolonSprite: Sprite = Sprite(17, 14, uiLayer)
  val apostropheSprite: Sprite = Sprite(18, 14, uiLayer)
  val quoteSprite: Sprite = Sprite(19, 14, uiLayer)
  val empty20_14Sprite: Sprite = Sprite(20, 14, uiLayer)
  val empty21_14Sprite: Sprite = Sprite(21, 14, uiLayer)
  val empty22_14Sprite: Sprite = Sprite(22, 14, uiLayer)
  val empty23_14Sprite: Sprite = Sprite(23, 14, uiLayer)
  val empty24_14Sprite: Sprite = Sprite(24, 14, uiLayer)
  val empty25_14Sprite: Sprite = Sprite(25, 14, uiLayer)
  val empty26_14Sprite: Sprite = Sprite(26, 14, uiLayer)
  val empty27_14Sprite: Sprite = Sprite(27, 14, uiLayer)
  val cursor_14_28Sprite: Sprite = Sprite(28, 14, uiLayer)
  val cursorSprite: Sprite = Sprite(29, 14, uiLayer)
  val handSprite: Sprite = Sprite(30, 14, uiLayer)
  val fingerSprite: Sprite = Sprite(31, 14, uiLayer)
  val arrow_14_32Sprite: Sprite = Sprite(32, 14, uiLayer)
  val chevronSprite: Sprite = Sprite(33, 14, uiLayer)
  val caretSprite: Sprite = Sprite(34, 14, uiLayer)
  val angleSprite: Sprite = Sprite(35, 14, uiLayer)
  val bracketSprite: Sprite = Sprite(36, 14, uiLayer)
  val parenSprite: Sprite = Sprite(37, 14, uiLayer)
  val braceSprite: Sprite = Sprite(38, 14, uiLayer)
  val squareSprite: Sprite = Sprite(39, 14, uiLayer)
  val circleSprite: Sprite = Sprite(40, 14, uiLayer)
  val triangleSprite: Sprite = Sprite(41, 14, uiLayer)
  val diamond_14_42Sprite: Sprite = Sprite(42, 14, uiLayer)
  val starSprite: Sprite = Sprite(43, 14, uiLayer)
  val asteriskSprite: Sprite = Sprite(44, 14, uiLayer)
  val hashSprite: Sprite = Sprite(45, 14, uiLayer)
  val atSprite: Sprite = Sprite(46, 14, uiLayer)
  val ampersandSprite: Sprite = Sprite(47, 14, uiLayer)
  val tildeSprite: Sprite = Sprite(48, 14, uiLayer)

  // Row 15 sprites
  val deadSprite: Sprite = Sprite(0, 15, backgroundLayer)
  val corpseSprite: Sprite = Sprite(1, 15, backgroundLayer)
  val remainsSprite: Sprite = Sprite(2, 15, backgroundLayer)
  val bonesSprite: Sprite = Sprite(3, 15, backgroundLayer)
  val skullSprite: Sprite = Sprite(4, 15, backgroundLayer)
  val empty5_15Sprite: Sprite = Sprite(5, 15, backgroundLayer)
  val empty6_15Sprite: Sprite = Sprite(6, 15, backgroundLayer)
  val bloodSprite: Sprite = Sprite(7, 15, backgroundLayer)
  val goreSprite: Sprite = Sprite(8, 15, backgroundLayer)
  val splatterSprite: Sprite = Sprite(9, 15, backgroundLayer)
  val stainSprite: Sprite = Sprite(10, 15, backgroundLayer)
  val puddleSprite: Sprite = Sprite(11, 15, entityLayer)
  val pool_15_12Sprite: Sprite = Sprite(12, 15, entityLayer)
  val slashSprite: Sprite = Sprite(13, 15, entityLayer)
  val cutSprite: Sprite = Sprite(14, 15, entityLayer)
  val woundSprite: Sprite = Sprite(15, 15, entityLayer)
  val scarSprite: Sprite = Sprite(16, 15, entityLayer)
  val bruiseSprite: Sprite = Sprite(17, 15, entityLayer)
  val burnSprite: Sprite = Sprite(18, 15, entityLayer)
  val scorchSprite: Sprite = Sprite(19, 15, entityLayer)
  val charSprite: Sprite = Sprite(20, 15, entityLayer)
  val ash_15_21Sprite: Sprite = Sprite(21, 15, entityLayer)
  val smokeSprite: Sprite = Sprite(22, 15, entityLayer)
  val fireSprite: Sprite = Sprite(23, 15, entityLayer)
  val flameSprite: Sprite = Sprite(24, 15, entityLayer)
  val sparkSprite: Sprite = Sprite(25, 15, entityLayer)
  val emberSprite: Sprite = Sprite(26, 15, entityLayer)
  val glowSprite: Sprite = Sprite(27, 15, entityLayer)
  val shimmerSprite: Sprite = Sprite(28, 15, entityLayer)
  val glitterSprite: Sprite = Sprite(29, 15, entityLayer)
  val sparkleSprite: Sprite = Sprite(30, 15, entityLayer)
  val twinkleSprite: Sprite = Sprite(31, 15, entityLayer)
  val flashSprite: Sprite = Sprite(32, 15, entityLayer)
  val scrollSprite: Sprite = Sprite(33, 15, uiLayer)
  val explosionSprite: Sprite = Sprite(34, 15, entityLayer)
  val impactSprite: Sprite = Sprite(35, 15, entityLayer)
  val shockwaveSprite: Sprite = Sprite(36, 15, entityLayer)
  val rippleSprite: Sprite = Sprite(37, 15, entityLayer)
  val waveSprite: Sprite = Sprite(38, 15, entityLayer)
  val pulseSprite: Sprite = Sprite(39, 15, entityLayer)
  val auraSprite: Sprite = Sprite(40, 15, entityLayer)
  val haloSprite: Sprite = Sprite(41, 15, entityLayer)
  val ring_15_42Sprite: Sprite = Sprite(42, 15, entityLayer)
  val circle_15_43Sprite: Sprite = Sprite(43, 15, entityLayer)
  val spiralSprite: Sprite = Sprite(44, 15, entityLayer)
  val vortex_15_45Sprite: Sprite = Sprite(45, 15, entityLayer)
  val swirlSprite: Sprite = Sprite(46, 15, entityLayer)
  val tornadoSprite: Sprite = Sprite(47, 15, entityLayer)
  val whirlwindSprite: Sprite = Sprite(48, 15, entityLayer)

  // Row 16 sprites
  val cornerTLSprite: Sprite = Sprite(0, 16, entityLayer)
  val cornerTRSprite: Sprite = Sprite(1, 16, entityLayer)
  val cornerBLSprite: Sprite = Sprite(2, 16, entityLayer)
  val cornerBRSprite: Sprite = Sprite(3, 16, entityLayer)
  val edgeTopSprite: Sprite = Sprite(4, 16, entityLayer)
  val edgeBottomSprite: Sprite = Sprite(5, 16, entityLayer)
  val edgeLeftSprite: Sprite = Sprite(6, 16, entityLayer)
  val edgeRightSprite: Sprite = Sprite(7, 16, entityLayer)
  val tJuncUpSprite: Sprite = Sprite(8, 16, entityLayer)
  val tJuncDownSprite: Sprite = Sprite(9, 16, entityLayer)
  val tJuncLeftSprite: Sprite = Sprite(10, 16, entityLayer)
  val tJuncRightSprite: Sprite = Sprite(11, 16, entityLayer)
  val cross_16_12Sprite: Sprite = Sprite(12, 16, entityLayer)
  val endNorthSprite: Sprite = Sprite(13, 16, entityLayer)
  val endSouthSprite: Sprite = Sprite(14, 16, entityLayer)
  val endEastSprite: Sprite = Sprite(15, 16, entityLayer)
  val endWestSprite: Sprite = Sprite(16, 16, entityLayer)
  val corridorSprite: Sprite = Sprite(17, 16, entityLayer)
  val passageSprite: Sprite = Sprite(18, 16, entityLayer)
  val hallwaySprite: Sprite = Sprite(19, 16, entityLayer)
  val tunnelSprite: Sprite = Sprite(20, 16, entityLayer)
  val alcoveSprite: Sprite = Sprite(21, 16, entityLayer)
  val nicheSprite: Sprite = Sprite(22, 16, entityLayer)
  val recessSprite: Sprite = Sprite(23, 16, entityLayer)
  val chamberSprite: Sprite = Sprite(24, 16, entityLayer)
  val roomSprite: Sprite = Sprite(25, 16, entityLayer)
  val vaultSprite: Sprite = Sprite(26, 16, entityLayer)
  val cryptSprite: Sprite = Sprite(27, 16, entityLayer)
  val tombSprite: Sprite = Sprite(28, 16, entityLayer)
  val graveSprite: Sprite = Sprite(29, 16, entityLayer)
  val sarcophagusSprite: Sprite = Sprite(30, 16, entityLayer)
  val coffinSprite: Sprite = Sprite(31, 16, entityLayer)
  val casketSprite: Sprite = Sprite(32, 16, entityLayer)
  val urn_16_33Sprite: Sprite = Sprite(33, 16, entityLayer)
  val reliquarySprite: Sprite = Sprite(34, 16, entityLayer)
  val shrineSprite: Sprite = Sprite(35, 16, entityLayer)
  val sanctumSprite: Sprite = Sprite(36, 16, entityLayer)
  val templeSprite: Sprite = Sprite(37, 16, entityLayer)
  val chapelSprite: Sprite = Sprite(38, 16, entityLayer)
  val cathedralSprite: Sprite = Sprite(39, 16, entityLayer)
  val monasterySprite: Sprite = Sprite(40, 16, entityLayer)
  val abbeySprite: Sprite = Sprite(41, 16, entityLayer)
  val cloisterSprite: Sprite = Sprite(42, 16, entityLayer)
  val refectorySprite: Sprite = Sprite(43, 16, entityLayer)
  val librarySprite: Sprite = Sprite(44, 16, entityLayer)
  val archiveSprite: Sprite = Sprite(45, 16, entityLayer)
  val treasurySprite: Sprite = Sprite(46, 16, entityLayer)
  val armorySprite: Sprite = Sprite(47, 16, entityLayer)
  val vault_16_48Sprite: Sprite = Sprite(48, 16, entityLayer)

  // Row 17 sprites
  val wallTopSprite: Sprite = Sprite(0, 17, entityLayer)
  val wallBottomSprite: Sprite = Sprite(1, 17, entityLayer)
  val wallLeftSprite: Sprite = Sprite(2, 17, entityLayer)
  val wallRightSprite: Sprite = Sprite(3, 17, entityLayer)
  val wallCornerTLSprite: Sprite = Sprite(4, 17, entityLayer)
  val stairsSprite: Sprite = Sprite(5, 17, entityLayer)
  val wallCornerSprite: Sprite = Sprite(6, 17, entityLayer)
  val wallJuncSprite: Sprite = Sprite(7, 17, entityLayer)
  val wallCrossSprite: Sprite = Sprite(8, 17, entityLayer)
  val wallDiagSprite: Sprite = Sprite(9, 17, entityLayer)
  val wallSprite: Sprite = Sprite(10, 17, entityLayer)
  val wallSideSprite: Sprite = Sprite(11, 17, entityLayer)
  val wallBackSprite: Sprite = Sprite(12, 17, entityLayer)
  val wallInnerSprite: Sprite = Sprite(13, 17, entityLayer)
  val wallOuterSprite: Sprite = Sprite(14, 17, entityLayer)
  val doorFrameSprite: Sprite = Sprite(15, 17, entityLayer)
  val doorwaySprite: Sprite = Sprite(16, 17, entityLayer)
  val entranceSprite: Sprite = Sprite(17, 17, entityLayer)
  val exitSprite: Sprite = Sprite(18, 17, entityLayer)
  val portalSprite: Sprite = Sprite(19, 17, entityLayer)
  val archSprite: Sprite = Sprite(20, 17, entityLayer)
  val gate_17_21Sprite: Sprite = Sprite(21, 17, entityLayer)
  val windowSprite: Sprite = Sprite(22, 17, entityLayer)
  val openingSprite: Sprite = Sprite(23, 17, entityLayer)
  val passage_17_24Sprite: Sprite = Sprite(24, 17, entityLayer)
  val doorOpenSprite: Sprite = Sprite(25, 17, entityLayer)
  val doorClosedSprite: Sprite = Sprite(26, 17, entityLayer)
  val doorLockedSprite: Sprite = Sprite(27, 17, entityLayer)
  val doorBrokenSprite: Sprite = Sprite(28, 17, entityLayer)
  val doorSecretSprite: Sprite = Sprite(29, 17, entityLayer)
  val windowOpenSprite: Sprite = Sprite(30, 17, entityLayer)
  val windowClosedSprite: Sprite = Sprite(31, 17, entityLayer)
  val windowBrokenSprite: Sprite = Sprite(32, 17, entityLayer)
  val windowBarredSprite: Sprite = Sprite(33, 17, entityLayer)
  val portcullisSprite: Sprite = Sprite(34, 17, entityLayer)
  val empty35_17Sprite: Sprite = Sprite(35, 17, entityLayer)
  val empty36_17Sprite: Sprite = Sprite(36, 17, entityLayer)
  val drawbridgeSprite: Sprite = Sprite(37, 17, entityLayer)
  val rampSprite: Sprite = Sprite(38, 17, entityLayer)
  val platformSprite: Sprite = Sprite(39, 17, entityLayer)
  val bridge_17_40Sprite: Sprite = Sprite(40, 17, entityLayer)
  val walkwaySprite: Sprite = Sprite(41, 17, entityLayer)
  val plank_17_42Sprite: Sprite = Sprite(42, 17, entityLayer)
  val beam_17_43Sprite: Sprite = Sprite(43, 17, entityLayer)
  val supportSprite: Sprite = Sprite(44, 17, entityLayer)
  val postSprite: Sprite = Sprite(47, 17, entityLayer)
  val pierSprite: Sprite = Sprite(48, 17, entityLayer)

  // Row 18 sprites
  val bossTLSprite: Sprite = Sprite(0, 18, entityLayer)
  val bossTRSprite: Sprite = Sprite(1, 18, entityLayer)
  val bossBLSprite: Sprite = Sprite(2, 18, entityLayer)
  val bossBRSprite: Sprite = Sprite(3, 18, entityLayer)
  val largeCreatureSprite: Sprite = Sprite(4, 18, entityLayer)
  val largeMonsterSprite: Sprite = Sprite(5, 18, entityLayer)
  val largeObjectSprite: Sprite = Sprite(6, 18, entityLayer)
  val largeStructureSprite: Sprite = Sprite(7, 18, entityLayer)
  val largeBuildingSprite: Sprite = Sprite(8, 18, entityLayer)
  val largeDecoSprite: Sprite = Sprite(9, 18, entityLayer)
  val largeTreeSprite: Sprite = Sprite(10, 18, entityLayer)
  val largeBoulderSprite: Sprite = Sprite(11, 18, entityLayer)
  val largeRockSprite: Sprite = Sprite(12, 18, entityLayer)
  val largeStatueSprite: Sprite = Sprite(13, 18, entityLayer)
  val largeMonumentSprite: Sprite = Sprite(14, 18, entityLayer)
  val largeFountainSprite: Sprite = Sprite(15, 18, entityLayer)
  val largeTowerSprite: Sprite = Sprite(16, 18, entityLayer)
  val largeCastleSprite: Sprite = Sprite(17, 18, entityLayer)
  val largeGateSprite: Sprite = Sprite(18, 18, entityLayer)
  val largeDoorSprite: Sprite = Sprite(19, 18, entityLayer)
  val largeWindowSprite: Sprite = Sprite(20, 18, entityLayer)
  val largeArchSprite: Sprite = Sprite(21, 18, entityLayer)
  val largePillarSprite: Sprite = Sprite(22, 18, entityLayer)
  val largeColumnSprite: Sprite = Sprite(23, 18, entityLayer)
  val largeWallSprite: Sprite = Sprite(24, 18, entityLayer)
  val largeTileSprite: Sprite = Sprite(25, 18, entityLayer)
  val largeFloorSprite: Sprite = Sprite(26, 18, entityLayer)
  val largeRoofSprite: Sprite = Sprite(27, 18, entityLayer)
  val largeChimneySprite: Sprite = Sprite(28, 18, entityLayer)
  val largeBannerSprite: Sprite = Sprite(29, 18, entityLayer)
  val largeflagSprite: Sprite = Sprite(30, 18, entityLayer)
  val largeSignSprite: Sprite = Sprite(31, 18, entityLayer)
  val largePlaqueSprite: Sprite = Sprite(32, 18, entityLayer)
  val largeCrestSprite: Sprite = Sprite(33, 18, entityLayer)
  val largeShieldSprite: Sprite = Sprite(34, 18, entityLayer)
  val largeSwordSprite: Sprite = Sprite(35, 18, entityLayer)
  val largeAxeSprite: Sprite = Sprite(36, 18, entityLayer)
  val largeHammerSprite: Sprite = Sprite(37, 18, entityLayer)
  val empty38_18Sprite: Sprite = Sprite(38, 18, entityLayer)
  val largeBowSprite: Sprite = Sprite(39, 18, entityLayer)
  val largeCrossbowSprite: Sprite = Sprite(40, 18, entityLayer)
  val largeStaffSprite: Sprite = Sprite(41, 18, entityLayer)
  val largeCrownSprite: Sprite = Sprite(42, 18, entityLayer)
  val largeTreasureSprite: Sprite = Sprite(43, 18, entityLayer)
  val largeChestSprite: Sprite = Sprite(44, 18, entityLayer)
  val largeCrateSprite: Sprite = Sprite(45, 18, entityLayer)
  val largeBarrelSprite: Sprite = Sprite(46, 18, entityLayer)
  val largeUrnSprite: Sprite = Sprite(47, 18, entityLayer)
  val largeVaseSprite: Sprite = Sprite(48, 18, entityLayer)

  // Row 19 sprites
  val fire_19_0Sprite: Sprite = Sprite(0, 19, entityLayer)
  val fireAnimSprite: Sprite = Sprite(1, 19, entityLayer)
  val fireBurstSprite: Sprite = Sprite(2, 19, entityLayer)
  val fireExplosionSprite: Sprite = Sprite(3, 19, entityLayer)
  val water_19_4Sprite: Sprite = Sprite(4, 19, entityLayer)
  val waterRippleSprite: Sprite = Sprite(5, 19, entityLayer)
  val waterSplashSprite: Sprite = Sprite(6, 19, entityLayer)
  val waterWaveSprite: Sprite = Sprite(7, 19, entityLayer)
  val earthSprite: Sprite = Sprite(8, 19, entityLayer)
  val earthShakeSprite: Sprite = Sprite(9, 19, entityLayer)
  val earthCrackSprite: Sprite = Sprite(10, 19, entityLayer)
  val earthSpikeSprite: Sprite = Sprite(11, 19, entityLayer)
  val airSprite: Sprite = Sprite(12, 19, entityLayer)
  val airGustSprite: Sprite = Sprite(13, 19, entityLayer)
  val airTornadoSprite: Sprite = Sprite(14, 19, entityLayer)
  val airWhirlwindSprite: Sprite = Sprite(15, 19, entityLayer)
  val ice_19_16Sprite: Sprite = Sprite(16, 19, entityLayer)
  val iceShatterSprite: Sprite = Sprite(17, 19, entityLayer)
  val iceSpikeSprite: Sprite = Sprite(18, 19, entityLayer)
  val iceBlastSprite: Sprite = Sprite(19, 19, entityLayer)
  val lightningSprite: Sprite = Sprite(20, 19, entityLayer)
  val lightningBoltSprite: Sprite = Sprite(21, 19, entityLayer)
  val lightningArcSprite: Sprite = Sprite(22, 19, entityLayer)
  val lightningStrikeSprite: Sprite = Sprite(23, 19, entityLayer)
  val lightSprite: Sprite = Sprite(24, 19, entityLayer)
  val lightBeamSprite: Sprite = Sprite(25, 19, entityLayer)
  val lightFlashSprite: Sprite = Sprite(26, 19, entityLayer)
  val lightGlowSprite: Sprite = Sprite(27, 19, entityLayer)
  val darkSprite: Sprite = Sprite(28, 19, entityLayer)
  val darkShadowSprite: Sprite = Sprite(29, 19, entityLayer)
  val darkVoidSprite: Sprite = Sprite(30, 19, entityLayer)
  val darkAuraSprite: Sprite = Sprite(31, 19, entityLayer)
  val natureSprite: Sprite = Sprite(32, 19, entityLayer)
  val natureGrowthSprite: Sprite = Sprite(33, 19, entityLayer)
  val natureVineSprite: Sprite = Sprite(34, 19, entityLayer)
  val natureThornSprite: Sprite = Sprite(35, 19, entityLayer)
  val holySprite: Sprite = Sprite(36, 19, entityLayer)
  val holyLightSprite: Sprite = Sprite(37, 19, entityLayer)
  val holyAuraSprite: Sprite = Sprite(38, 19, entityLayer)
  val holyBlessingSprite: Sprite = Sprite(39, 19, entityLayer)
  val shadowSprite: Sprite = Sprite(40, 19, entityLayer)
  val shadowStepSprite: Sprite = Sprite(41, 19, entityLayer)
  val shadowCloakSprite: Sprite = Sprite(42, 19, entityLayer)
  val shadowBladeSprite: Sprite = Sprite(43, 19, entityLayer)
  val arcaneSprite: Sprite = Sprite(44, 19, entityLayer)
  val arcaneBoltSprite: Sprite = Sprite(45, 19, entityLayer)
  val arcaneMissileSprite: Sprite = Sprite(46, 19, entityLayer)
  val arcaneShieldSprite: Sprite = Sprite(47, 19, entityLayer)
  val chaosSprite: Sprite = Sprite(48, 19, entityLayer)

  // Row 20 sprites
  val particleSprite: Sprite = Sprite(0, 20, uiLayer)
  val particleSmallSprite: Sprite = Sprite(1, 20, uiLayer)
  val particleMediumSprite: Sprite = Sprite(2, 20, uiLayer)
  val particleLargeSprite: Sprite = Sprite(3, 20, uiLayer)
  val particleTinySprite: Sprite = Sprite(4, 20, uiLayer)
  val particleDotSprite: Sprite = Sprite(5, 20, uiLayer)
  val particleCircleSprite: Sprite = Sprite(6, 20, uiLayer)
  val particleSquareSprite: Sprite = Sprite(7, 20, uiLayer)
  val particleTriSprite: Sprite = Sprite(8, 20, uiLayer)
  val particleStarSprite: Sprite = Sprite(9, 20, uiLayer)
  val particleCrossSprite: Sprite = Sprite(10, 20, uiLayer)
  val particlePlusSprite: Sprite = Sprite(11, 20, uiLayer)
  val particleDiamondSprite: Sprite = Sprite(12, 20, uiLayer)
  val particleHexSprite: Sprite = Sprite(13, 20, uiLayer)
  val particleOctSprite: Sprite = Sprite(14, 20, uiLayer)
  val particleGlowSprite: Sprite = Sprite(15, 20, uiLayer)
  val empty16_20Sprite: Sprite = Sprite(16, 20, uiLayer)
  val dust_20_17Sprite: Sprite = Sprite(17, 20, uiLayer)
  val dustCloudSprite: Sprite = Sprite(18, 20, uiLayer)
  val empty19_20Sprite: Sprite = Sprite(19, 20, uiLayer)
  val smoke_20_20Sprite: Sprite = Sprite(20, 20, uiLayer)
  val smokeCloudSprite: Sprite = Sprite(21, 20, uiLayer)
  val smokePuffSprite: Sprite = Sprite(22, 20, uiLayer)
  val mistSprite: Sprite = Sprite(23, 20, uiLayer)
  val mistCloudSprite: Sprite = Sprite(24, 20, uiLayer)
  val fogSprite: Sprite = Sprite(25, 20, uiLayer)
  val fogBankSprite: Sprite = Sprite(26, 20, uiLayer)
  val cloudSprite: Sprite = Sprite(27, 20, uiLayer)
  val cloudWhiteSprite: Sprite = Sprite(28, 20, uiLayer)
  val steamSprite: Sprite = Sprite(29, 20, uiLayer)
  val vaporSprite: Sprite = Sprite(30, 20, uiLayer)
  val hazeSprite: Sprite = Sprite(31, 20, uiLayer)
  val smogSprite: Sprite = Sprite(32, 20, uiLayer)
  val empty33_20Sprite: Sprite = Sprite(33, 20, uiLayer)
  val empty34_20Sprite: Sprite = Sprite(34, 20, uiLayer)
  val empty35_20Sprite: Sprite = Sprite(35, 20, uiLayer)
  val bubbleSprite: Sprite = Sprite(36, 20, uiLayer)
  val bubblePopSprite: Sprite = Sprite(37, 20, uiLayer)
  val empty38_20Sprite: Sprite = Sprite(38, 20, uiLayer)
  val empty39_20Sprite: Sprite = Sprite(39, 20, uiLayer)
  val empty40_20Sprite: Sprite = Sprite(40, 20, uiLayer)
  val spark_20_41Sprite: Sprite = Sprite(41, 20, uiLayer)
  val sparkGlitterSprite: Sprite = Sprite(42, 20, uiLayer)
  val empty43_20Sprite: Sprite = Sprite(43, 20, uiLayer)
  val empty44_20Sprite: Sprite = Sprite(44, 20, uiLayer)
  val empty45_20Sprite: Sprite = Sprite(45, 20, uiLayer)
  val empty46_20Sprite: Sprite = Sprite(46, 20, uiLayer)
  val glow_20_47Sprite: Sprite = Sprite(47, 20, uiLayer)
  val glowBrightSprite: Sprite = Sprite(48, 20, uiLayer)

  // Row 21 sprites
  val num0Sprite: Sprite = Sprite(0, 21, uiLayer)
  val num1Sprite: Sprite = Sprite(1, 21, uiLayer)
  val num2Sprite: Sprite = Sprite(2, 21, uiLayer)
  val num3Sprite: Sprite = Sprite(3, 21, uiLayer)
  val num4Sprite: Sprite = Sprite(4, 21, uiLayer)
  val num5Sprite: Sprite = Sprite(5, 21, uiLayer)
  val num6Sprite: Sprite = Sprite(6, 21, uiLayer)
  val num7Sprite: Sprite = Sprite(7, 21, uiLayer)
  val num8Sprite: Sprite = Sprite(8, 21, uiLayer)
  val num9Sprite: Sprite = Sprite(9, 21, uiLayer)
  val letterASprite: Sprite = Sprite(10, 21, uiLayer)
  val letterBSprite: Sprite = Sprite(11, 21, uiLayer)
  val letterCSprite: Sprite = Sprite(12, 21, uiLayer)
  val letterDSprite: Sprite = Sprite(13, 21, uiLayer)
  val letterESprite: Sprite = Sprite(14, 21, uiLayer)
  val letterFSprite: Sprite = Sprite(15, 21, uiLayer)
  val letterGSprite: Sprite = Sprite(16, 21, uiLayer)
  val letterHSprite: Sprite = Sprite(17, 21, uiLayer)
  val letterISprite: Sprite = Sprite(18, 21, uiLayer)
  val letterJSprite: Sprite = Sprite(19, 21, uiLayer)
  val letterKSprite: Sprite = Sprite(20, 21, uiLayer)
  val letterLSprite: Sprite = Sprite(21, 21, uiLayer)
  val letterMSprite: Sprite = Sprite(22, 21, uiLayer)
  val letterNSprite: Sprite = Sprite(23, 21, uiLayer)
  val letterOSprite: Sprite = Sprite(24, 21, uiLayer)
  val letterPSprite: Sprite = Sprite(25, 21, uiLayer)
  val letterQSprite: Sprite = Sprite(26, 21, uiLayer)
  val letterRSprite: Sprite = Sprite(27, 21, uiLayer)
  val letterSSprite: Sprite = Sprite(28, 21, uiLayer)
  val letterTSprite: Sprite = Sprite(29, 21, uiLayer)
  val letterUSprite: Sprite = Sprite(30, 21, uiLayer)
  val letterVSprite: Sprite = Sprite(31, 21, uiLayer)
  val letterWSprite: Sprite = Sprite(32, 21, uiLayer)
  val letterXSprite: Sprite = Sprite(33, 21, uiLayer)
  val letterYSprite: Sprite = Sprite(34, 21, uiLayer)
  val errorSprite: Sprite = Sprite(35, 21, uiLayer)
  val warningSprite: Sprite = Sprite(36, 21, uiLayer)
  val infoSprite: Sprite = Sprite(37, 21, uiLayer)
  val debugSprite: Sprite = Sprite(38, 21, uiLayer)
  val traceSprite: Sprite = Sprite(39, 21, uiLayer)
  val log_21_40Sprite: Sprite = Sprite(40, 21, uiLayer)
  val console_21_41Sprite: Sprite = Sprite(41, 21, uiLayer)
  val terminal_21_42Sprite: Sprite = Sprite(42, 21, uiLayer)
  val shellSprite: Sprite = Sprite(43, 21, uiLayer)
  val promptSprite: Sprite = Sprite(44, 21, uiLayer)
  val commandSprite: Sprite = Sprite(45, 21, uiLayer)
  val outputSprite: Sprite = Sprite(46, 21, uiLayer)
  val inputSprite: Sprite = Sprite(47, 21, uiLayer)
  val bufferSprite: Sprite = Sprite(48, 21, uiLayer)

}

