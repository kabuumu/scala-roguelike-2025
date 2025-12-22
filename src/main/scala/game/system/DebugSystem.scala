package game.system

import game.GameState
import game.system.event.GameSystemEvent
import ui.InputAction
import game.entity.Inventory.addItemEntity
import game.entity.Coins.addCoins
import game.entity.Health.*
import game.entity.Experience
import game.entity.Movement.position
import game.status.StatusEffect
import game.status.StatusEffect.addStatusEffect
import game.Point
import scala.util.Random
import game.entity.SightMemory

object DebugSystem extends GameSystem {
  override def update(
      gameState: GameState,
      events: Seq[GameSystemEvent.GameSystemEvent]
  ): (GameState, Seq[GameSystemEvent.GameSystemEvent]) = {
    val (updatedGameState, newEvents) =
      events.foldLeft((gameState, Seq.empty[GameSystemEvent.GameSystemEvent])) {
        case (
              (currentState, currentEvents),
              GameSystemEvent.InputEvent(entityId, action)
            ) =>
          action match {
            case InputAction.DebugGiveItem(itemRef) =>
              // Create item entity
              val itemEntity = itemRef.createEntity(
                s"debug-item-${itemRef.toString}-${Random.nextString(6)}"
              )

              // Place item at player's position (conceptually)
              val playerPos = currentState.playerEntity.position
              val itemWithPos =
                itemEntity.addComponent(game.entity.Movement(playerPos))

              // Add entity to world state
              val stateWithItem = currentState.add(itemWithPos)
              val player = stateWithItem.playerEntity

              import game.entity.Equippable.isEquippable
              import game.entity.Equippable.equippable
              import game.entity.Equipment.equipItemComponent
              import game.entity.{
                Entity,
                Equippable,
                CanPickUp,
                Hitbox,
                Drawable
              }
              import data.Sprites

              if (itemWithPos.isEquippable) {
                // Auto-equip logic
                val equippableComp = itemWithPos.equippable.get
                val (playerWithEquipment, previousItem) =
                  player.equipItemComponent(itemWithPos.id, equippableComp)

                // Add the new valid item reference to inventory as well (Inventory tracks IDs)
                val finalPlayer =
                  playerWithEquipment.addItemEntity(itemWithPos.id)

                // Hide the new item entity (it's "equipped")
                val equippedItemEntity =
                  itemWithPos.removeComponent[game.entity.Movement]

                var interimState = stateWithItem
                  .updateEntity(entityId, finalPlayer)
                  .updateEntity(itemWithPos.id, equippedItemEntity)

                // Handle swapped item
                previousItem match {
                  case Some(equippedItem) =>
                    val oldEquippable = equippedItem.stats
                    // Create entity for old item
                    val oldItemId =
                      s"unequipped-${oldEquippable.itemName.replace(" ", "-")}-${Random.nextString(6)}"
                    // Just basic setup to exist in inventory (no movement needed implicitly if just in inventory, but usually we init with pos then pick up)
                    // We'll create it without movement to signify "in bag" directly
                    import game.entity.Entity

                    // We need a sprite for the old item to be valid?
                    // EquipmentSystem has getEquipmentSprite logic but it's private.
                    // We'll use a default or reuse logic if public. It's not public.
                    // We'll just assign defaultItemSprite for now or copy the matcher if critical.
                    // Actually, let's just use defaultItemSprite for simplicity unless we want to copy that huge matcher.
                    // Better: The ItemReference should probably be used if possible, but we only have Equippable component.
                    // This is a limitation of the component system here: we lost the ItemReference or original Entity.
                    // But we have itemName.

                    val oldItemEntity = Entity(
                      id = oldItemId,
                      // No movement = in inventory/hidden
                      CanPickUp(),
                      oldEquippable,
                      Hitbox(),
                      Drawable(
                        Sprites.defaultItemSprite
                      ) // Placeholder, ideally specific sprite
                    ).addComponent(
                      game.entity.NameComponent(oldEquippable.itemName)
                    ) // Ensure name exists

                    // Add old item to world
                    interimState = interimState.add(oldItemEntity)

                    // Add old item to inventory
                    val p = interimState.playerEntity
                    interimState = interimState.updateEntity(
                      p.id,
                      p.addItemEntity(oldItemId)
                    )

                    val msg = GameSystemEvent.MessageEvent(
                      s"Debug: Gave & Equipped ${itemRef.toString}. Swapped ${oldEquippable.itemName}."
                    )
                    (interimState, currentEvents :+ msg)

                  case None =>
                    val msg = GameSystemEvent.MessageEvent(
                      s"Debug: Gave & Equipped ${itemRef.toString}"
                    )
                    (interimState, currentEvents :+ msg)
                }
              } else {
                // Standard give logic for non-equippables
                val updatedPlayer = player.addItemEntity(itemWithPos.id)
                val carriedItem =
                  itemWithPos.removeComponent[game.entity.Movement]

                val finalState = stateWithItem
                  .updateEntity(entityId, updatedPlayer)
                  .updateEntity(itemWithPos.id, carriedItem)

                val msg = GameSystemEvent.MessageEvent(
                  s"Debug: Gave ${itemRef.toString}"
                )
                (finalState, currentEvents :+ msg)
              }

            case InputAction.DebugGiveGold(amount) =>
              val player = currentState.playerEntity
              val updatedPlayer = player.addCoins(amount)
              val finalState =
                currentState.updateEntity(entityId, updatedPlayer)
              val msg =
                GameSystemEvent.MessageEvent(s"Debug: Gave $amount Gold")
              (finalState, currentEvents :+ msg)

            case InputAction.DebugGiveExperience(amount) =>
              // Emit Experience Event
              val expEvent =
                GameSystemEvent.AddExperienceEvent(entityId, amount)
              val msg = GameSystemEvent.MessageEvent(s"Debug: Gave $amount XP")
              (currentState, currentEvents :+ expEvent :+ msg)

            case InputAction.DebugRestoreHealth =>
              // Emit Heal Event for full health (arbitrary large number or logic)
              // We can just heal significantly
              val healEvent = GameSystemEvent.HealEvent(entityId, 9999)
              val msg = GameSystemEvent.MessageEvent(s"Debug: Restored Health")
              (currentState, currentEvents :+ healEvent :+ msg)

            case InputAction.DebugGivePerk(perk) =>
              val finalState =
                currentState.updateEntity(entityId, _.addStatusEffect(perk))
              val msg =
                GameSystemEvent.MessageEvent(s"Debug: Gave Perk ${perk.name}")
              (finalState, currentEvents :+ msg)

            case InputAction.DebugRevealMap =>
              val player = currentState.playerEntity
              val playerPos = player.position
              val radius = 100

              // Generate all points within radius 100
              val newSeenPoints = (for {
                x <- (playerPos.x - radius) to (playerPos.x + radius)
                y <- (playerPos.y - radius) to (playerPos.y + radius)
                if Math.pow(x - playerPos.x, 2) + Math.pow(
                  y - playerPos.y,
                  2
                ) <= Math.pow(radius, 2)
              } yield Point(x, y)).toSet

              // Update player's SightMemory
              val updatedPlayer = player.update[SightMemory] { sightMemory =>
                sightMemory.copy(seenPoints =
                  sightMemory.seenPoints ++ newSeenPoints
                )
              }

              val finalState =
                currentState.updateEntity(entityId, updatedPlayer)
              val msg =
                GameSystemEvent.MessageEvent("Debug: Revealed Map (100 tiles)")
              (finalState, currentEvents :+ msg)

            case _ =>
              (currentState, currentEvents)
          }
        case ((currentState, currentEvents), _) =>
          (currentState, currentEvents)
      }
    (updatedGameState, newEvents)
  }
}
