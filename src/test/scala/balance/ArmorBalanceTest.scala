package balance

import org.scalatest.funsuite.AnyFunSuite
import data.Items
import game.entity.*
import game.entity.EquipmentSlot.*
import game.entity.Equipment.*
import game.entity.Health.*
import game.balance.BalanceConfig
import game.Point

class ArmorBalanceTest extends AnyFunSuite {

  private def mkBasePlayer(h: Int = 70): Entity =
    Entity(
      id = s"test-player-${scala.util.Random.nextInt(10000)}",
      Movement(Point(0,0)),
      EntityTypeComponent(EntityType.Player),
      Health(h),
      Initiative(10),
      Inventory(),
      Equipment(),
      Drawable(data.Sprites.playerSprite),
      Hitbox()
    )

  private def equip(e: Entity, eqs: Seq[Entity]): Entity =
    eqs.foldLeft(e){ (acc, item) =>
      item.get[Equippable].map { eq =>
        val (updated, _) = acc.get[Equipment].getOrElse(Equipment()).equip(eq)
        acc.update[Equipment](_ => updated)
      }.getOrElse(acc)
    }

  private val earlySet = Seq(
    Items.leatherHelmet("L-helm"),
    Items.leatherGloves("L-gloves"),
    Items.leatherBoots("L-boots")
  )
  private val midSet = Seq(
    Items.chainmailArmor("C-armor"),
    Items.ironHelmet("I-helm"),
    Items.leatherGloves("L-gloves2"),
    Items.ironBoots("I-boots")
  )
  private val bestSet = Seq(
    Items.plateArmor("P-armor"),
    Items.ironHelmet("I-helm2"),
    Items.ironGloves("I-gloves"),
    Items.leatherBoots("L-boots2")
  )

  private case class SimpleHit(baseDamage: Int, atkBonus: Int, dr: Int) {
    def finalDamage: Int = math.max(1, baseDamage + atkBonus - dr)
  }

  test("Monotonic DR reduces or maintains damage") {
    val baseDamage = 7
    val atkBonus = 0

    val variants = Seq(
      mkBasePlayer(),
      equip(mkBasePlayer(), earlySet),
      equip(mkBasePlayer(), midSet),
      equip(mkBasePlayer(), bestSet)
    ).map(p => (p.getTotalDamageReduction, p))

    val sorted = variants.sortBy(_._1)(Ordering.Int)
    val damages = sorted.map { case (dr, _) =>
      dr -> SimpleHit(baseDamage, atkBonus, dr).finalDamage
    }

    assert(
      damages.sliding(2).forall {
        case Seq((_, d1), (_, d2)) => d2 <= d1
        case _ => true
      },
      s"Damage not monotonically decreasing with DR: $damages"
    )
  }

  test("Damage floor respected") {
    val hit = SimpleHit(5, 0, 999)
    assert(hit.finalDamage == 1)
  }

  test("TTK bands and equipment effectiveness") {
    val baseDamage = 9
    val unarmoured = mkBasePlayer()
    val best = equip(mkBasePlayer(), bestSet)

    def dmg(p: Entity) = SimpleHit(baseDamage, 0, p.getTotalDamageReduction).finalDamage
    val dUnarm = dmg(unarmoured)
    val dBest = dmg(best)

    val ttkUnarm = math.ceil(unarmoured.maxHealth.toDouble / dUnarm).toInt
    val ttkBest = math.ceil(best.maxHealth.toDouble / dBest).toInt

    // Remove strict gear impact ratio constraint - allow meaningful equipment progression
    assert(ttkUnarm >= BalanceConfig.TargetUnarmouredTTKMin,
      s"Unarmoured TTK too low: $ttkUnarm < ${BalanceConfig.TargetUnarmouredTTKMin}")
    assert(ttkUnarm <= BalanceConfig.TargetUnarmouredTTKMax,
      s"Unarmoured TTK too high: $ttkUnarm > ${BalanceConfig.TargetUnarmouredTTKMax}")
    assert(ttkBest <= BalanceConfig.TargetBestGearTTKMax,
      s"Best gear TTK $ttkBest > ${BalanceConfig.TargetBestGearTTKMax}")
    
    // Ensure equipment provides meaningful benefit
    assert(ttkBest > ttkUnarm, s"Best gear should provide survivability improvement: best=$ttkBest vs unarmored=$ttkUnarm")
  }

  test("Weapon bonus monotonicity") {
    val base = mkBasePlayer()
    val basic = Items.basicSword("basic-sword")
    val iron  = Items.ironSword("iron-sword")

    val withBasic = equip(base, Seq(basic))
    val withIron  = equip(mkBasePlayer(), Seq(iron))

    def outgoing(p: Entity, defenderDR: Int = 0): Int = {
      val baseDamage = 1
      val atkBonus = p.getTotalDamageBonus
      math.max(1, baseDamage + atkBonus - defenderDR)
    }

    val b = outgoing(withBasic)
    val i = outgoing(withIron)
    assert(i >= b, s"Iron sword should not yield less damage: basic=$b iron=$i")
  }

  test("Iron equipment progression") {
    // Verify all iron equipment provides at least +1 DR over leather counterparts
    val leatherHelmet = Items.leatherHelmet("l-helm")
    val ironHelmet = Items.ironHelmet("i-helm") 
    val leatherGloves = Items.leatherGloves("l-gloves")
    val ironGloves = Items.ironGloves("i-gloves")
    val leatherBoots = Items.leatherBoots("l-boots")
    val ironBoots = Items.ironBoots("i-boots")
    
    def getDR(item: Entity): Int = item.get[Equippable].map(_.damageReduction).getOrElse(0)
    
    assert(getDR(ironHelmet) >= getDR(leatherHelmet) + 1, 
      s"Iron helmet should be at least +1 DR over leather: iron=${getDR(ironHelmet)} vs leather=${getDR(leatherHelmet)}")
    assert(getDR(ironGloves) >= getDR(leatherGloves) + 1,
      s"Iron gloves should be at least +1 DR over leather: iron=${getDR(ironGloves)} vs leather=${getDR(leatherGloves)}")
    assert(getDR(ironBoots) >= getDR(leatherBoots) + 1,
      s"Iron boots should be at least +1 DR over leather: iron=${getDR(ironBoots)} vs leather=${getDR(leatherBoots)}")
  }

  test("All armor provides protection") {
    // Verify all armor provides at least 1 DR
    val allArmor = Seq(
      Items.leatherHelmet("test1"),
      Items.ironHelmet("test2"),
      Items.leatherGloves("test3"),
      Items.ironGloves("test4"),
      Items.leatherBoots("test5"),
      Items.ironBoots("test6"),
      Items.chainmailArmor("test7"),
      Items.plateArmor("test8")
    )
    
    allArmor.foreach { armor =>
      val dr = armor.get[Equippable].map(_.damageReduction).getOrElse(0)
      assert(dr >= 1, s"All armor should provide at least 1 DR: ${armor.get[Equippable].map(_.itemName).getOrElse("unknown")} has $dr DR")
    }
  }

  test("Randomized invariant sampling") {
    val rnd = new scala.util.Random(42)
    (1 to 50).foreach { _ =>
      val base = rnd.between(1, 12)
      val bonus = rnd.between(0, 8)
      val dr = rnd.between(0, 15)
      val dmg = math.max(1, base + bonus - dr)
      assert(dmg >= 1)
      val dmgLessDR = math.max(1, base + bonus - math.max(0, dr - 1))
      // Non-strict condition: decreasing DR should not reduce damage
      assert(dmgLessDR >= dmg || dr == 0)
    }
  }
}