package game

import data.Items
import game.entity.{Equipment, Equippable, EquipmentSlot}
import game.entity.Equippable.equippable
import org.scalatest.funsuite.AnyFunSuite

class FullEquipmentSystemTest extends AnyFunSuite {

  test("Equipment slots work for all item types") {
    val equipment = Equipment()
    
    // Test helmet equipment
    val helmet = Equippable.armor(EquipmentSlot.Helmet, 2, "Test Helmet")
    val (newEquip1, _) = equipment.equip(helmet)
    assert(newEquip1.helmet.contains(helmet))
    
    // Test armor equipment
    val armor = Equippable.armor(EquipmentSlot.Armor, 5, "Test Armor")
    val (newEquip2, _) = newEquip1.equip(armor)
    assert(newEquip2.armor.contains(armor))
    
    // Test boots equipment
    val boots = Equippable.armor(EquipmentSlot.Boots, 1, "Test Boots")
    val (newEquip3, _) = newEquip2.equip(boots)
    assert(newEquip3.boots.contains(boots))
    
    // Test gloves equipment
    val gloves = Equippable.armor(EquipmentSlot.Gloves, 1, "Test Gloves")
    val (newEquip4, _) = newEquip3.equip(gloves)
    assert(newEquip4.gloves.contains(gloves))
    
    // Test weapon equipment
    val weapon = Equippable.weapon(3, "Test Sword")
    val (finalEquip, _) = newEquip4.equip(weapon)
    assert(finalEquip.weapon.contains(weapon))
    
    // Verify total damage reduction and bonus
    assert(finalEquip.getTotalDamageReduction == 9) // 2+5+1+1
    assert(finalEquip.getTotalDamageBonus == 3) // 3 from weapon
  }

  test("Equipment replacement drops previous item") {
    val equipment = Equipment()
    
    // Equip initial armor
    val chainmail = Equippable.armor(EquipmentSlot.Armor, 5, "Chainmail")
    val (equip1, _) = equipment.equip(chainmail)
    
    // Equip new armor (should return previous)
    val plate = Equippable.armor(EquipmentSlot.Armor, 8, "Plate Armor")
    val (equip2, previousItem) = equip1.equip(plate)
    
    assert(equip2.armor.contains(plate))
    assert(previousItem.contains(chainmail))
  }

  test("Starting equipment creates correct items") {
    val basicSword = Items.basicSword("test-sword")
    val chainmailArmor = Items.chainmailArmor("test-armor")
    
    assert(basicSword.equippable.exists(_.slot == EquipmentSlot.Weapon))
    assert(basicSword.equippable.exists(_.damageBonus == 3))
    assert(basicSword.equippable.exists(_.damageReduction == 0))
    
    assert(chainmailArmor.equippable.exists(_.slot == EquipmentSlot.Armor))
    assert(chainmailArmor.equippable.exists(_.damageReduction == 1))
    assert(chainmailArmor.equippable.exists(_.damageBonus == 0))
  }

  test("New equipment items have correct properties") {
    val boots = Items.leatherBoots("test-boots")
    val gloves = Items.ironGloves("test-gloves")
    val sword = Items.ironSword("test-sword")
    
    assert(boots.equippable.exists(_.slot == EquipmentSlot.Boots))
    assert(boots.equippable.exists(_.damageReduction == 1))
    
    assert(gloves.equippable.exists(_.slot == EquipmentSlot.Gloves))
    assert(gloves.equippable.exists(_.damageReduction == 2))
    
    assert(sword.equippable.exists(_.slot == EquipmentSlot.Weapon))
    assert(sword.equippable.exists(_.damageBonus == 5))
  }
}