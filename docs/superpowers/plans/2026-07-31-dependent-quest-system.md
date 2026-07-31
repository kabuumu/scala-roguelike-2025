# Dependent Quest System Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a persisted, prerequisite-aware quest lifecycle in which item and kill quests accept, progress, complete, reward, chain, and generate reliably across world seeds.

**Architecture:** Static `Quest` definitions describe prerequisites and follow-ups. Persisted `QuestState` records status and the progress baseline captured at acceptance. `QuestSystem` becomes the single lifecycle authority; `ConversationSystem` delegates acceptance and completion to it.

**Tech Stack:** Scala 3.6.4, ScalaTest, Scala.js, uPickle, Indigo, sbt

---

## File Structure

- `src/main/scala/game/quest/Quest.scala`: quest definitions, runtime state,
  availability, and repository validation.
- `src/main/scala/game/system/QuestSystem.scala`: progress, acceptance,
  completion, reward, item-consumption, and dialogue transitions.
- `src/main/scala/game/system/ConversationSystem.scala`: delegates quest
  actions to `QuestSystem`.
- `src/main/scala/game/GameState.scala`: stores `Map[String, QuestState]`.
- `src/main/scala/game/save/SaveModel.scala`: persists runtime quest state.
- `src/main/scala/data/Quests.scala`: dependent quest chain content.
- `src/main/scala/data/Entities.scala`: quest-giver starting dialogue.
- `src/main/scala/map/Village.scala`: guarantees enough generic buildings.
- `src/main/scala/game/StartingState.scala`: deterministic giver placement.
- Existing UI files consume `QuestState.status`.
- Focused tests cover lifecycle, validation, persistence, UI compatibility, and
  deterministic multi-seed generation.

### Task 1: Quest runtime model and repository validation

**Files:**
- Modify: `src/main/scala/game/quest/Quest.scala`
- Modify: `src/main/scala/game/GameState.scala`
- Create: `src/test/scala/game/quest/QuestRepositoryTest.scala`
- Modify: quest tests that construct `GameState.quests`

- [ ] **Step 1: Write failing repository and state tests**

Create tests asserting:

```scala
val valid = Map(
  "first" -> Quest("first", "First", "...", KillEnemyGoal("Rat"), rewards, followUpQuestId = Some("second")),
  "second" -> Quest("second", "Second", "...", KillEnemyGoal("Snake"), rewards, prerequisiteQuestIds = Set("first"))
)
assert(QuestRepository.validate(valid) == Right(()))
assert(QuestRepository.validate(valid.updated("second", valid("second").copy(prerequisiteQuestIds = Set("missing")))).isLeft)
assert(QuestRepository.validate(valid.updated("first", valid("first").copy(prerequisiteQuestIds = Set("second")))).isLeft)
```

Also assert `GameState.acceptQuest("id", baseline = 4)` stores:

```scala
QuestState(QuestStatus.Active, progressBaseline = 4)
```

- [ ] **Step 2: Run tests and confirm RED**

Run:

```bash
sbt "testOnly game.quest.QuestRepositoryTest game.system.QuestFlowTest game.system.QuestSystemRatQuestTest"
```

Expected: compilation fails because `QuestState`, dependency fields, and
`validate` do not exist.

- [ ] **Step 3: Implement the runtime model**

Add:

```scala
case class QuestState(
    status: QuestStatus,
    progressBaseline: Int = 0
)

enum QuestAvailability:
  case Locked(incompletePrerequisiteIds: Set[String])
  case Available, Active, Completed, Failed
```

Extend `Quest`:

```scala
prerequisiteQuestIds: Set[String] = Set.empty,
followUpQuestId: Option[String] = None
```

Change `GameState.quests` to `Map[String, QuestState]`, update helpers to read
`.status`, and make `acceptQuest(questId, baseline)` store a `QuestState`.

- [ ] **Step 4: Add repository validation**

Implement `QuestRepository.validate(quests)` with explicit checks for key/id
mismatch, missing references, non-positive goal amounts, negative rewards,
follow-up/precondition disagreement, and dependency cycles. Add:

```scala
require(validate(quests).isRight, validate(quests).left.getOrElse(""))
```

to fail fast for malformed static data.

- [ ] **Step 5: Update status consumers and tests**

Replace direct status values with `QuestState(status)` in test fixtures and
read `questState.status` in `CharacterScreen`, `Game`, `WorldMapUI`, and
`QuestSystem`.

- [ ] **Step 6: Run focused and full tests**

```bash
sbt "testOnly game.quest.QuestRepositoryTest game.system.QuestFlowTest game.system.QuestFlowEdgeCaseTest game.system.QuestSystemRatQuestTest"
sbt test
```

Expected: all tests pass.

- [ ] **Step 7: Commit and push**

```bash
git add src/main/scala src/test/scala
git commit -m "feat: add validated quest runtime state"
git push origin main
```

### Task 2: Centralized acceptance and completion

**Files:**
- Modify: `src/main/scala/game/system/QuestSystem.scala`
- Modify: `src/main/scala/game/system/ConversationSystem.scala`
- Modify: `src/test/scala/game/system/QuestFlowTest.scala`
- Modify: `src/test/scala/game/system/QuestSystemRatQuestTest.scala`
- Create: `src/test/scala/game/system/DependentQuestFlowTest.scala`

- [ ] **Step 1: Write failing lifecycle tests**

Test these exact behaviors:

```scala
// Locked dependency
val (lockedState, lockedEvents) =
  QuestSystem.acceptQuest(state, giver, "kill_rats")
assert(!lockedState.isQuestActive("kill_rats"))
assert(lockedState.messages.head.contains("The Missing Statue"))
assert(lockedEvents.isEmpty)

// Kill baseline
val stateWithOldKills = state.updateEntity(player.id, playerWithThreeRatKills)
val (accepted, _) = QuestSystem.acceptQuest(stateWithOldKills, giver, "kill_rats")
assert(accepted.quests("kill_rats").progressBaseline == 3)
assert(!QuestSystem.isGoalSatisfied(accepted, "kill_rats"))

// Kill completion after new kills
val (completed, events) = QuestSystem.completeQuest(stateWithNewKills, giver, "kill_rats")
assert(completed.isQuestCompleted("kill_rats"))
assert(completed.playerEntity.get[Coins].exists(_.current == 50))
assert(events.count(_.isInstanceOf[AddExperienceEvent]) == 1)

// Idempotence
val (again, repeatedEvents) = QuestSystem.completeQuest(completed, giver, "kill_rats")
assert(again == completed)
assert(repeatedEvents.isEmpty)
```

- [ ] **Step 2: Run tests and confirm RED**

```bash
sbt "testOnly game.system.DependentQuestFlowTest game.system.QuestFlowTest game.system.QuestSystemRatQuestTest"
```

Expected: failures because prerequisite checks, baseline-aware progress, and
kill completion are absent.

- [ ] **Step 3: Implement lifecycle API in `QuestSystem`**

Add:

```scala
def availability(gameState: GameState, questId: String): QuestAvailability
def progress(gameState: GameState, questId: String): Int
def isGoalSatisfied(gameState: GameState, questId: String): Boolean
def acceptQuest(gameState: GameState, giver: Entity, questId: String):
  (GameState, Seq[GameSystemEvent])
def completeQuest(gameState: GameState, giver: Entity, questId: String):
  (GameState, Seq[GameSystemEvent])
```

For kills, return:

```scala
matchingKillCount - gameState.quests(questId).progressBaseline
```

For items, return the current inventory count. Completion consumes item goals,
does not consume kill goals, awards coins and one `AddExperienceEvent`, marks
completion, and updates giver dialogue.

- [ ] **Step 4: Delegate from `ConversationSystem`**

Replace goal-specific `AcceptQuest` and `CompleteQuest` branches with:

```scala
case AcceptQuest(questId) =>
  QuestSystem.acceptQuest(gameState, entity, questId)
case CompleteQuest(questId) =>
  QuestSystem.completeQuest(gameState, entity, questId)
```

- [ ] **Step 5: Make periodic dialogue updates use shared satisfaction**

Refactor `QuestSystem.update` to use `isGoalSatisfied` and one helper that
builds ready-to-complete dialogue. Preserve one-time “Quest Updated” messaging.

- [ ] **Step 6: Run tests**

```bash
sbt "testOnly game.system.DependentQuestFlowTest game.system.QuestFlowTest game.system.QuestFlowEdgeCaseTest game.system.QuestSystemRatQuestTest"
sbt test
```

Expected: all tests pass, including kill reward/idempotence assertions.

- [ ] **Step 7: Commit and push**

```bash
git add src/main/scala/game/system src/test/scala/game/system
git commit -m "fix: centralize reliable quest completion"
git push origin main
```

### Task 3: Persist quest runtime state

**Files:**
- Modify: `src/main/scala/game/save/SaveModel.scala`
- Modify: `src/test/scala/game/save/SaveGameJsonRoundTripTest.scala`

- [ ] **Step 1: Write failing save tests**

Add a round-trip test:

```scala
val state = StartingState.startAdventure(123L)
  .copy(quests = Map(
    "retrieve_statue" -> QuestState(QuestStatus.Completed),
    "kill_rats" -> QuestState(QuestStatus.Active, progressBaseline = 2)
  ))
val restored = SaveGameJson.deserialize(SaveGameJson.serialize(state))
assert(restored.quests == state.quests)
```

Add backward compatibility by removing the `"quests"` property from serialized
JSON and asserting deserialization returns `Map.empty`.

- [ ] **Step 2: Run test and confirm RED**

```bash
sbt "testOnly game.save.SaveGameJsonRoundTripTest"
```

Expected: round-trip loses quest state.

- [ ] **Step 3: Add persistence DTOs**

Add:

```scala
final case class PersistedQuestState(status: String, progressBaseline: Int = 0)
```

and:

```scala
quests: Map[String, PersistedQuestState] = Map.empty
```

to `PersistedGameState`, with a derived `ReadWriter`.

- [ ] **Step 4: Convert quest state both directions**

Serialize status with `.toString`; deserialize known enum values explicitly.
Unknown values should return a descriptive conversion error rather than
silently becoming active.

- [ ] **Step 5: Run save and full tests**

```bash
sbt "testOnly game.save.SaveGameJsonRoundTripTest game.save.SaveSystemIntegrationTest"
sbt test
```

Expected: quest status/baseline survives round-trip and old JSON still loads.

- [ ] **Step 6: Commit and push**

```bash
git add src/main/scala/game/save/SaveModel.scala src/test/scala/game/save/SaveGameJsonRoundTripTest.scala
git commit -m "fix: persist quest progress across saves"
git push origin main
```

### Task 4: Add the dependent quest chain

**Files:**
- Modify: `src/main/scala/data/Quests.scala`
- Modify: `src/main/scala/data/Entities.scala`
- Modify: `src/main/scala/game/system/QuestSystem.scala`
- Modify: `src/test/scala/game/system/DependentQuestFlowTest.scala`

- [ ] **Step 1: Write failing chain tests**

Assert the repository contains:

```scala
retrieve_statue -> kill_rats -> slime_cleanup -> snake_hunt
```

with each arrow represented by both `followUpQuestId` and the inverse
`prerequisiteQuestIds`. Complete each quest and assert the same giver's
completion dialogue offers `AcceptQuest(nextId)` when applicable.

- [ ] **Step 2: Run and confirm RED**

```bash
sbt "testOnly game.system.DependentQuestFlowTest game.quest.QuestRepositoryTest"
```

Expected: missing quest definitions/follow-up choices.

- [ ] **Step 3: Define chain content**

Use:

```scala
"retrieve_statue": followUpQuestId = Some("kill_rats")
"kill_rats": prerequisite = "retrieve_statue", followUp = "slime_cleanup"
"slime_cleanup": KillEnemyGoal("Slimelet", 3), prerequisite = "kill_rats", followUp = "snake_hunt"
"snake_hunt": KillEnemyGoal("Snake", 2), prerequisite = "slime_cleanup"
```

Use non-negative rewards increasing modestly through the chain.

- [ ] **Step 4: Add follow-up dialogue**

When `quest.followUpQuestId` belongs to the same `giverName`, append:

```scala
ConversationChoice("What else can I do?", AcceptQuest(nextQuestId))
```

Otherwise leave the completed giver with `Goodbye`; the next NPC remains
visible but locked until prerequisites are met.

- [ ] **Step 5: Run focused and full tests**

```bash
sbt "testOnly game.system.DependentQuestFlowTest game.quest.QuestRepositoryTest"
sbt test
```

Expected: full chain tests pass.

- [ ] **Step 6: Commit and push**

```bash
git add src/main/scala/data src/main/scala/game/system/QuestSystem.scala src/test/scala/game
git commit -m "feat: add a dependent village quest chain"
git push origin main
```

### Task 5: Guarantee world content across seeds

**Files:**
- Modify: `src/main/scala/map/Village.scala`
- Modify: `src/main/scala/game/StartingState.scala`
- Modify: `src/test/scala/map/VillageTest.scala`
- Create: `src/test/scala/game/QuestWorldGenerationTest.scala`
- Conditionally modify: `src/main/scala/map/DungeonConfig.scala`
- Conditionally modify: `src/main/scala/game/StartingState.scala`

- [ ] **Step 1: Write failing multi-seed tests**

For seeds `1L to 25L`, assert:

```scala
val state = StartingState.startAdventure(seed)
assert(named(state, "Elder").size == 1)
assert(named(state, "Quest Giver").size == 1)
assert(state.worldMap.allItems.exists(_._2 == ItemReference.GoldenStatue))
assert(enemyCount(state, EnemyReference.Rat) >= 3)
assert(enemyCount(state, EnemyReference.Slimelet) >= 3)
assert(enemyCount(state, EnemyReference.Snake) >= 2)
```

Generate the same seed twice and compare giver positions, dungeon items, and
enemy type/position tuples for determinism.

- [ ] **Step 2: Run and inspect RED evidence**

```bash
sbt "testOnly game.QuestWorldGenerationTest map.VillageTest"
```

Expected: some seeds lack the second giver and possibly required enemy counts.
Record which guarantee fails before changing generation.

- [ ] **Step 3: Guarantee giver buildings**

Change villages to four-to-six buildings and mandatory types:

```scala
Seq(Farmland, Generic, Generic, Generic)
```

Update comments/tests. With at most one spawn-containing generic building,
`StartingState` has two eligible buildings for the two unique quest givers.

- [ ] **Step 4: Guarantee enemy objectives only if evidence requires it**

If a tested enemy count is below its quest amount, add declarative
`requiredEnemies` to `DungeonConfig` and place only the missing count in
walkable non-start rooms. IDs must include dungeon seed, enemy type, and index
to remain deterministic. Do not add this path if natural generation satisfies
all 25 seeds.

- [ ] **Step 5: Run seed, generation, and full tests**

```bash
sbt "testOnly game.QuestWorldGenerationTest map.VillageTest map.GlobalFeaturePlannerTest map.WorldMapDeterminismTest"
sbt test
```

Expected: all 25 seeds satisfy every quest-content invariant and identical
seeds generate identical quest content.

- [ ] **Step 6: Commit and push**

```bash
git add src/main/scala/map src/main/scala/game/StartingState.scala src/test/scala/game/QuestWorldGenerationTest.scala src/test/scala/map/VillageTest.scala
git commit -m "fix: guarantee quest content across world seeds"
git push origin main
```

### Task 6: Final UI, coverage, and web build validation

**Files:**
- Modify if required by compilation: `src/main/scala/indigoengine/Game.scala`
- Modify if required by compilation: `src/main/scala/indigoengine/view/ui/WorldMapUI.scala`
- Modify if required by compilation: `src/main/scala/indigoengine/view/ui/CharacterScreen.scala`
- Modify: `COVERAGE.md` only if the existing analyzer updates documented metrics

- [ ] **Step 1: Run compile and resolve exhaustive typed consumers**

```bash
sbt compile
```

Expected: success with no quest-related warnings. Update UI consumers to use
`questState.status` and baseline-aware `QuestSystem.isGoalSatisfied`.

- [ ] **Step 2: Run the complete automated validation**

```bash
sbt test
python3 scripts/analyze_coverage.py
sbt build
```

Expected: all tests pass, estimated coverage remains at least 65%, and web
output is generated under `target/indigoBuild/`.

- [ ] **Step 3: Inspect final diff and repository state**

```bash
git diff --check
git status --short
git log --oneline -8
```

Expected: no whitespace errors and only intended files are changed.

- [ ] **Step 4: Commit any final UI compatibility changes and push**

```bash
git add src/main/scala/indigoengine COVERAGE.md
git commit -m "fix: align quest UI with persisted progress"
git push origin main
```

Skip the commit if there are no remaining changes.
