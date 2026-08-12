# Focused Initial Quest Flow Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ensure the opening game flow is clear, quest-driven, and accessible with connected paths and aligned narrative text from Player Spawn -> Village Elder -> Opening Dungeon -> Golden Statue Quest Item.

**Architecture:** Initialize starting welcome messages, position the Elder in the building closest to spawn, generate clear road paths connecting spawn, Elder, and opening dungeon, refine dungeon enemy/item distribution, and render quest UI markers.

**Tech Stack:** Scala 3, ScalaTest, Indigo Game Engine, sbt.

---

### Task 1: Add Initial Game Log Welcome Message & Text Alignment

**Files:**
- Modify: `src/main/scala/game/StartingState.scala:525-534`
- Modify: `src/main/scala/data/Entities.scala:223-244`
- Modify: `src/main/scala/data/Quests.scala:8-18`

- [ ] **Step 1: Update `StartingState.scala` to populate starting message**

In `src/main/scala/game/StartingState.scala`, update `GameState` initialization to include initial guidance message:

```scala
    GameState(
      playerEntityId = playerEntity.id,
      entities = Vector(
        playerEntity
      ) ++ playerStartingItems ++ playerStartingEquipment ++ enemies ++ items ++ lockedDoors ++ allSpitAbilities.values ++ dungeonTraders ++ villageTraders ++ wildAnimals ++ crops ++ farmers ++ caravans,
      messages = Seq("Welcome to the village! Speak with the Village Elder nearby to begin your quest."),
      worldMap = worldMap,
      dungeonFloor = dungeonFloor,
      gameMode = gameMode
    )
```

- [ ] **Step 2: Update Elder dialogue text in `Entities.scala`**

In `src/main/scala/data/Entities.scala`, update `questGiver` dialogue text:

```scala
  def questGiver(id: String, position: game.Point): Entity = {
    CharacterFactory.create(
      id,
      position,
      Sprites.playerSprite,
      EntityType.Villager,
      name = Some(NameComponent("Elder", "The village elder, looks worried.")),
      extraComponents = Seq(
        Conversation(
          "Help! Thieves have stolen our sacred Golden Statue and fled into the cave dungeon to the east. Can you travel there, defeat them, and retrieve our statue?",
          Seq(
            ConversationChoice(
              "I will find it.",
              AcceptQuest("retrieve_statue")
            ),
            ConversationChoice("Maybe later.", CloseAction)
          )
        ),
        Portrait(Sprite(1, 0, 0))
      )
    )
  }
```

- [ ] **Step 3: Update quest text in `Quests.scala`**

In `src/main/scala/data/Quests.scala`, refine `retrieve_statue` quest description and texts:

```scala
    "retrieve_statue" -> Quest(
      id = "retrieve_statue",
      title = "The Missing Statue",
      description = "Follow the road to the cave dungeon to the east, defeat the enemies within, and retrieve the Golden Statue for the Village Elder.",
      goal = RetrieveItemGoal(ItemReference.GoldenStatue, 1),
      rewards = QuestRewards(experience = 500, coins = 100),
      giverName = Some("Elder"),
      readyToCompleteText = Some("You found the Golden Statue! Please, give it to me."),
      completionText = Some("Thank you so much! You have saved our village."),
      followUpQuestId = Some("kill_rats")
    ),
```

- [ ] **Step 4: Run test suite to verify text & quest definitions**

Run: `sbt test`
Expected: PASS

- [ ] **Step 5: Commit narrative text updates**

```bash
git add src/main/scala/game/StartingState.scala src/main/scala/data/Entities.scala src/main/scala/data/Quests.scala
git commit -m "feat: add initial welcome message and align Elder quest text"
```

---

### Task 2: Elder Placement & Village Path Connectivity

**Files:**
- Modify: `src/main/scala/game/StartingState.scala:405-445`
- Modify: `src/main/scala/map/WorldMutator.scala:463-540`
- Modify: `src/test/scala/map/TownDungeonRoadTest.scala`

- [ ] **Step 1: Place Elder in spawn village generic building closest to player spawn**

In `src/main/scala/game/StartingState.scala`, sort eligible generic buildings by distance to `playerSpawnPoint`:

```scala
        val eligibleGenericBuildingIndices = if (isSpawnVillage) {
          village.buildings.zipWithIndex.collect {
            case (b, idx) if b.buildingType == map.BuildingType.Generic &&
                !(playerSpawnPoint.x >= b.bounds._1.x && playerSpawnPoint.x <= b.bounds._2.x &&
                  playerSpawnPoint.y >= b.bounds._1.y && playerSpawnPoint.y <= b.bounds._2.y) =>
              (idx, b.centerTile.getChebyshevDistance(playerSpawnPoint))
          }.sortBy(_._2).map(_._1)
        } else {
          Seq.empty
        }
```

- [ ] **Step 2: Add explicit path connectivity test in `TownDungeonRoadTest.scala`**

Update `src/test/scala/map/TownDungeonRoadTest.scala` to verify paths exist between player spawn, Village Elder, and dungeon entrance:

```scala
  test("Player spawn, Village Elder, and opening dungeon are connected by walkable paths") {
    val seed = 100L
    val state = StartingState.startAdventure(seed)
    val worldMap = state.worldMap

    val elder = state.entities.find(_.get[game.entity.NameComponent].exists(_.name == "Elder")).flatMap(_.position).get
    val dungeonApproach = Dungeon.getApproachTile(worldMap.dungeons.head.startPoint, worldMap.dungeons.head.entranceSide)
    val playerPos = state.playerEntity.position

    assert(isWalkablePath(playerPos, elder, worldMap), s"Walkable path must exist between player spawn $playerPos and Elder $elder")
    assert(isWalkablePath(elder, dungeonApproach, worldMap), s"Walkable path must exist between Elder $elder and dungeon approach $dungeonApproach")
  }
```

- [ ] **Step 3: Run test suite**

Run: `sbt test`
Expected: PASS

- [ ] **Step 4: Commit Elder placement and path connectivity updates**

```bash
git add src/main/scala/game/StartingState.scala src/test/scala/map/TownDungeonRoadTest.scala
git commit -m "feat: place Elder in nearest generic building and verify player -> Elder -> dungeon path connectivity"
```

---

### Task 3: Opening Dungeon Enemy & Tactical Item Balance

**Files:**
- Modify: `src/main/scala/game/StartingState.scala:542-556`

- [ ] **Step 1: Refine depth-based enemy progression in opening dungeon**

In `StartingState.scala` `EnemyGeneration.enemiesForDepth`:

```scala
    def enemiesForDepth(depth: Int): EnemyGroup = depth match {
      case d if d == Int.MaxValue => EnemyGroup(Seq(EnemyReference.Boss))
      case 1                      => EnemyGroup(Seq(EnemyReference.Slimelet))
      case 2                      => EnemyGroup(Seq(EnemyReference.Slimelet, EnemyReference.Rat))
      case 3                      => EnemyGroup(Seq(EnemyReference.Slime))
      case 4                      => EnemyGroup(Seq(EnemyReference.Slime, EnemyReference.Snake))
      case 5                      => EnemyGroup(Seq(EnemyReference.Rat, EnemyReference.Rat))
      case 6                      => EnemyGroup(Seq(EnemyReference.Snake, EnemyReference.Snake))
      case _                      => EnemyGroup(Seq(EnemyReference.Slimelet))
    }
```

- [ ] **Step 2: Verify test suite**

Run: `sbt test`
Expected: PASS

- [ ] **Step 3: Commit opening dungeon balance**

```bash
git add src/main/scala/game/StartingState.scala
git commit -m "balance: refine opening dungeon enemy progression"
```

---

### Task 4: World Map UI Quest Marker for Elder Before Acceptance

**Files:**
- Modify: `src/main/scala/indigoengine/view/ui/WorldMapUI.scala:142-190`

- [ ] **Step 1: Add Elder marker when `retrieve_statue` quest is unaccepted**

In `WorldMapUI.scala`, check if `retrieve_statue` quest is not active or completed, and add a marker for the Elder:

```scala
    val unacceptedElderMarker = if (!model.gameState.isQuestActive("retrieve_statue") && !model.gameState.isQuestCompleted("retrieve_statue")) {
      model.gameState.entities
        .find(e => e.get[game.entity.NameComponent].exists(_.name == "Elder"))
        .flatMap(_.position)
        .map(elderPos => (elderPos, RGBA.Yellow))
    } else None
```

Include `unacceptedElderMarker` in the rendered markers list.

- [ ] **Step 2: Run test suite**

Run: `sbt test`
Expected: PASS

- [ ] **Step 3: Commit WorldMapUI quest marker updates**

```bash
git add src/main/scala/indigoengine/view/ui/WorldMapUI.scala
git commit -m "feat: show yellow marker for Village Elder on WorldMap UI when initial quest is available"
```

---

### Task 5: Full Verification & Integration Checkpoint

- [ ] **Step 1: Run complete test suite**

Run: `sbt test`
Expected: PASS (`All tests passed`, 161+ succeeded, 0 failed)

- [ ] **Step 2: Push changes to remote main**

```bash
git push origin main
```
