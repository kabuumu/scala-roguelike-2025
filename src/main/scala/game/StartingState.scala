package game

import data.Sprites
import game.entity.*
import game.entity.Experience.experienceForLevel
import game.system.event.GameSystemEvent.AddExperienceEvent
import map.{Dungeon, MapGenerator}


object StartingState {
  val dungeon: Dungeon = MapGenerator.generateDungeon(dungeonSize = 20, lockedDoorCount = 3, itemCount = 6)

  // Create player's starting inventory items as entities
  val playerStartingItems: Set[Entity] = Set(
    ItemFactory.createPotion("player-potion-1"),
    ItemFactory.createScroll("player-scroll-1"),
    ItemFactory.createBow("player-bow-1")
  ) ++ (1 to 6).map(i => ItemFactory.createArrow(s"player-arrow-$i"))

  // Create weapons as entities for enemies and player
  val ratWeapons: Map[Int, Entity] = (dungeon.roomGrid - dungeon.startPoint).zipWithIndex.collect {
    case (_, index) if index % 2 == 0 => index -> ItemFactory.createWeapon(s"rat-weapon-$index", 8, Melee)
  }.toMap
  
  val snakeWeapons: Map[Int, Entity] = (dungeon.roomGrid - dungeon.startPoint).zipWithIndex.collect {
    case (_, index) if index % 2 != 0 => index -> ItemFactory.createWeapon(s"snake-weapon-$index", 6, Ranged(4))
  }.toMap
  
  val playerPrimaryWeapon: Entity = ItemFactory.createWeapon("player-primary-weapon", 10, Melee)

  val enemies: Set[Entity] = (dungeon.roomGrid - dungeon.startPoint).zipWithIndex.map {
    case (point, index) if index % 2 == 0 =>
      Entity(
        id = s"Rat $index",
        Movement(position = Point(
          point.x * Dungeon.roomSize + Dungeon.roomSize / 2,
          point.y * Dungeon.roomSize + Dungeon.roomSize / 2
        )),
        EntityTypeComponent(EntityType.Enemy),
        Health(25),
        Initiative(12),
        Inventory(Nil, Some(s"rat-weapon-$index")),
        Drawable(Sprites.ratSprite),
        Hitbox(),
        DeathEvents(deathDetails => deathDetails.killerId.map {
          killerId => AddExperienceEvent(killerId, experienceForLevel(2) / 4)
        }.toSeq
        )
      )
    case (point, index) =>
      Entity(
        id = s"Snake $index",
        Movement(position = Point(
          point.x * Dungeon.roomSize + Dungeon.roomSize / 2,
          point.y * Dungeon.roomSize + Dungeon.roomSize / 2
        )),
        EntityTypeComponent(EntityType.Enemy),
        Health(18),
        Initiative(25),
        Inventory(Nil, Some(s"snake-weapon-$index")),
        Drawable(Sprites.snakeSprite),
        Hitbox(),
        DeathEvents(deathDetails =>
          deathDetails.killerId.map {
            killerId => AddExperienceEvent(killerId, experienceForLevel(2) / 4)
          }.toSeq
        )
      )
      
  }

  val player: Entity = dungeon.startPoint match {
    case point =>
      Entity(
        id = "Player ID",
        Movement(position = Point(
          point.x * Dungeon.roomSize + Dungeon.roomSize / 2,
          point.y * Dungeon.roomSize + Dungeon.roomSize / 2
        )),
        EntityTypeComponent(EntityType.Player),
        Health(100),
        Initiative(10),
        Inventory(
          itemEntityIds = playerStartingItems.map(_.id).toSeq,
          primaryWeaponId = Some("player-primary-weapon"),
          secondaryWeaponId = None
        ),
        Equipment(),
        SightMemory(),
        Drawable(Sprites.playerSprite),
        Hitbox(),
        Experience(),
        DeathEvents()
      )
  }

  val items: Set[Entity] = dungeon.items.zipWithIndex.collect {
    case ((point, item), index) =>
      val basePosition = Point(
        point.x * Dungeon.roomSize + Dungeon.roomSize / 2,
        point.y * Dungeon.roomSize + Dungeon.roomSize / 2
      )
      
      // Map old Item objects to new entity-based system
      item match {
        case keyItem if keyItem.isInstanceOf[game.Item.Key] =>
          val key = keyItem.asInstanceOf[game.Item.Key]
          val keyColour = key.keyColour match {
            case game.Item.KeyColour.Yellow => KeyColour.Yellow
            case game.Item.KeyColour.Blue => KeyColour.Blue  
            case game.Item.KeyColour.Red => KeyColour.Red
          }
          val sprite = keyColour match {
            case KeyColour.Yellow => Sprites.yellowKeySprite
            case KeyColour.Blue => Sprites.blueKeySprite
            case KeyColour.Red => Sprites.redKeySprite
          }
          ItemFactory.placeInWorld(
            ItemFactory.createKey(s"key-$index", keyColour),
            basePosition,
            sprite
          ).addComponent(EntityTypeComponent(EntityType.Key(keyColour)))
          
        case potionItem if potionItem == game.Item.Potion =>
          ItemFactory.placeInWorld(
            ItemFactory.createPotion(s"potion-$index"),
            basePosition,
            Sprites.potionSprite
          )
          
        case scrollItem if scrollItem == game.Item.Scroll =>
          ItemFactory.placeInWorld(
            ItemFactory.createScroll(s"scroll-$index"),
            basePosition,
            Sprites.scrollSprite
          )
          
        case arrowItem if arrowItem == game.Item.Arrow =>
          ItemFactory.placeInWorld(
            ItemFactory.createArrow(s"arrow-$index"),
            basePosition,
            Sprites.arrowSprite
          )
          
        case equippableItem if equippableItem.isInstanceOf[game.Item.EquippableItem] =>
          val equipItem = equippableItem.asInstanceOf[game.Item.EquippableItem]
          val sprite = equipItem match {
            case game.Item.LeatherHelmet => Sprites.leatherHelmetSprite
            case game.Item.IronHelmet => Sprites.ironHelmetSprite
            case game.Item.ChainmailArmor => Sprites.chainmailArmorSprite
            case game.Item.PlateArmor => Sprites.plateArmorSprite
          }
          
          Entity(
            id = s"equipment-$index",
            Movement(basePosition),
            CanPickUp(),
            Equippable(
              slot = equipItem.slot match {
                case game.Item.EquipmentSlot.Helmet => EquipmentSlot.Helmet
                case game.Item.EquipmentSlot.Armor => EquipmentSlot.Armor
              },
              damageReduction = equipItem.damageReduction,
              itemName = equipItem.name
            ),
            Hitbox(),
            Drawable(sprite)
          )
      }
  }

  val lockedDoors: Set[Entity] = dungeon.lockedDoors.map {
    case (point, lockedDoor) =>
      Entity(
        Movement(position = Point(
          point.x,
          point.y
        )),
        EntityTypeComponent(lockedDoor),
        Hitbox(),
        lockedDoor.keyColour match {
          case game.Item.KeyColour.Yellow => Drawable(Sprites.yellowDoorSprite)
          case game.Item.KeyColour.Blue => Drawable(Sprites.blueDoorSprite)
          case game.Item.KeyColour.Red => Drawable(Sprites.redDoorSprite)
        }
      )
  }

  val startingGameState: GameState = GameState(
    playerEntityId = player.id,
    entities = Vector(player) ++ playerStartingItems ++ items ++ enemies ++ lockedDoors ++ 
               ratWeapons.values ++ snakeWeapons.values ++ Seq(playerPrimaryWeapon),
    dungeon = dungeon
  )
}
