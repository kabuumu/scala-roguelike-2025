package indigoengine.view.ui

import indigo.*
import indigo.Batch.toBatch
import indigoengine.view.UIUtils
import indigoengine.view.BlockBar
import indigoengine.view.components.MenuComponent
import _root_.ui.UIConfig.*
import _root_.ui.GameController
import _root_.ui.UIState
import game.entity.Entity
import generated.Assets
import generated.PixelFont

object Menus {

  def mainMenu(model: GameController): Batch[SceneNode] = {
    model.uiState match {
      case mainMenu: UIState.MainMenu =>
        val titleText = "Scala Roguelike 2025"
        val titleX =
          (canvasWidth - (titleText.length * 8)) / 2 // Center title roughly
        val titleY = canvasHeight / 3

        val title = UIUtils.text(titleText, titleX, titleY)

        val menuOptions = mainMenu.options.zipWithIndex.map {
          case (option, index) =>
            val optionY = titleY + (spriteScale * 3) + (index * spriteScale * 2)
            val optionX =
              (canvasWidth - (option.length * 8)) / 2 // Center options roughly
            val isSelected = index == mainMenu.selectedOption

            val optionText = if (isSelected) s"> $option <" else s"  $option  "
            UIUtils.text(optionText, optionX - 16, optionY)
        }

        val instructions = UIUtils.text(
          "Use Arrow Keys and Enter",
          (canvasWidth - 160) / 2,
          canvasHeight - spriteScale * 2
        )

        Batch(title) ++ menuOptions.toBatch ++ Batch(instructions)
      case _ =>
        Batch.empty
    }
  }

  def renderDebugMenu(model: GameController): Batch[SceneNode] = {
    model.uiState match {
      case debugMenu: UIState.DebugMenu =>
        MenuComponent.renderScrollingMenu(
          "DEBUG MENU",
          debugMenu.options,
          debugMenu.selectedOption,
          canvasWidth,
          canvasHeight
        )
      case _ => Batch.empty
    }
  }

  def gameOverScreen(
      model: GameController,
      player: Entity
  ): Batch[SceneNode] = {
    import game.entity.EventMemory.*
    import game.entity.MemoryEvent
    import game.entity.Equipment.*
    import game.entity.EntityType
    import data.Sprites
    import indigoengine.SpriteExtension.*

    val spriteSheet =
      Graphic(0, 0, 784, 352, Material.Bitmap(AssetName("sprites")))

    // Game Over title - large text centered at top
    val titleText = "GAME OVER"
    val titleX = (canvasWidth - (titleText.length * 16)) / 2
    val titleY = spriteScale * 2
    val title = UIUtils.text(titleText, titleX, titleY)

    // Statistics section
    val statsStartY = titleY + spriteScale * 4

    // Get events from player memory
    val memoryEvents = player.getMemoryEvents
    val enemiesDefeated =
      player.getMemoryEventsByType[MemoryEvent.EnemyDefeated]
    val stepsTaken = player.getStepCount
    val itemsUsed = player.getMemoryEventsByType[MemoryEvent.ItemUsed].length

    // Group enemies by type and count them
    val enemyStats = enemiesDefeated
      .groupBy(_.enemyType)
      .view
      .mapValues(_.length)
      .toSeq
      .sortBy(_._1)

    // Enemies defeated section
    val enemiesHeaderY = statsStartY
    val enemiesHeader =
      UIUtils.text("Enemies defeated:", uiXOffset, enemiesHeaderY)

    val enemyDisplayElements = if (enemyStats.nonEmpty) {
      enemyStats.zipWithIndex.flatMap { case ((enemyType, count), index) =>
        val spriteY = enemiesHeaderY + spriteScale + (index * spriteScale)
        val sprite = enemyType match {
          case "Rat"      => Sprites.ratSprite
          case "Slimelet" => Sprites.slimeletSprite
          case "Slime"    => Sprites.slimeSprite
          case "Snake"    => Sprites.snakeSprite
          case "Boss"     => Sprites.bossSpriteTL // Use top-left boss sprite
          case "Enemy"    => Sprites.enemySprite // Fallback for generic enemies
          case _          => Sprites.enemySprite // Default fallback
        }

        Seq(
          spriteSheet.fromSprite(sprite).moveTo(uiXOffset, spriteY),
          UIUtils.text(s"x$count", uiXOffset + spriteScale, spriteY)
        )
      }
    } else {
      Seq(
        UIUtils.text(
          "None",
          uiXOffset + spriteScale,
          enemiesHeaderY + spriteScale
        )
      )
    }

    // Other statistics
    val statsY =
      enemiesHeaderY + spriteScale + (enemyStats.length * spriteScale) + spriteScale
    val stepsText =
      UIUtils.text(s"Steps travelled: $stepsTaken", uiXOffset, statsY)
    val itemsText =
      UIUtils.text(s"Items used: $itemsUsed", uiXOffset, statsY + spriteScale)

    // Coins collected
    import game.entity.Coins.*
    val totalCoins = player.totalCoinsCollected
    val coinsText = UIUtils.text(
      s"Coins collected: $totalCoins",
      uiXOffset,
      statsY + (spriteScale * 2)
    )

    // Final equipment section
    val equipmentY = statsY + spriteScale * 4
    val equipmentHeader =
      UIUtils.text("Final equipment:", uiXOffset, equipmentY)

    val equipment = player.equipment
    val equipmentElements = Seq(
      // Helmet
      equipment.helmet.map { helmet =>
        val sprite = helmet.itemName match {
          case "Leather Helmet" => Sprites.leatherHelmetSprite
          case "Iron Helmet"    => Sprites.ironHelmetSprite
          case _                => Sprites.defaultItemSprite
        }
        spriteSheet
          .fromSprite(sprite)
          .moveTo(uiXOffset, equipmentY + spriteScale)
      },
      // Armor
      equipment.armor.map { armor =>
        val sprite = armor.itemName match {
          case "Leather Armor" =>
            Sprites.defaultItemSprite // Use default since leatherArmorSprite doesn't exist
          case "Chainmail Armor" => Sprites.chainmailArmorSprite
          case "Plate Armor"     => Sprites.plateArmorSprite
          case _                 => Sprites.defaultItemSprite
        }
        spriteSheet
          .fromSprite(sprite)
          .moveTo(uiXOffset + spriteScale, equipmentY + spriteScale)
      },
      // Weapon
      equipment.weapon.map { weapon =>
        val sprite = weapon.itemName match {
          case "Basic Sword" => Sprites.basicSwordSprite
          case "Iron Sword"  => Sprites.ironSwordSprite
          case _             => Sprites.defaultItemSprite
        }
        spriteSheet
          .fromSprite(sprite)
          .moveTo(uiXOffset + spriteScale * 2, equipmentY + spriteScale)
      },
      // Gloves
      equipment.gloves.map { gloves =>
        val sprite = gloves.itemName match {
          case "Leather Gloves" => Sprites.leatherGlovesSprite
          case "Iron Gloves"    => Sprites.ironGlovesSprite
          case _                => Sprites.defaultItemSprite
        }
        spriteSheet
          .fromSprite(sprite)
          .moveTo(uiXOffset + spriteScale * 3, equipmentY + spriteScale)
      },
      // Boots
      equipment.boots.map { boots =>
        val sprite = boots.itemName match {
          case "Leather Boots" => Sprites.leatherBootsSprite
          case "Iron Boots"    => Sprites.ironBootsSprite
          case _               => Sprites.defaultItemSprite
        }
        spriteSheet
          .fromSprite(sprite)
          .moveTo(uiXOffset + spriteScale * 4, equipmentY + spriteScale)
      }
    ).flatten

    // Instructions
    val instructionsY = canvasHeight - spriteScale * 2
    val instructions = UIUtils.text(
      "Press Space/Enter/E to return to main menu",
      uiXOffset,
      instructionsY
    )

    Batch(
      title,
      enemiesHeader,
      stepsText,
      itemsText,
      coinsText,
      equipmentHeader,
      instructions
    ) ++
      enemyDisplayElements.toBatch ++ equipmentElements.toBatch
  }
}
