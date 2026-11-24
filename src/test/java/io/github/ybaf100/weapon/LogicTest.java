package io.github.ybaf100.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.ybaf100.weapon.data.PlayerData;
import io.github.ybaf100.weapon.logic.AbilityRoller;
import io.github.ybaf100.weapon.logic.Enhancement;
import io.github.ybaf100.weapon.logic.Evolution;
import io.github.ybaf100.weapon.logic.Gacha;
import io.github.ybaf100.weapon.logic.GearRules;
import io.github.ybaf100.weapon.model.AbilityEntry;
import io.github.ybaf100.weapon.model.AbilityGrade;
import io.github.ybaf100.weapon.model.AbilityType;
import io.github.ybaf100.weapon.model.GearData;
import io.github.ybaf100.weapon.model.GearType;
import io.github.ybaf100.weapon.model.Grade;
import io.github.ybaf100.weapon.model.Rarity;
import io.github.ybaf100.weapon.model.ShardType;
import io.github.ybaf100.weapon.model.Ticket;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LogicTest {
    private final Random random = new Random(12345);

    @Test
    void gachaRatesMatchDesign() {
        int n = 2_000_000;
        Map<ShardType, Integer> counts = new EnumMap<>(ShardType.class);
        for (int i = 0; i < n; i++) {
            counts.merge(Gacha.Kind.EPIC.roll(random), 1, Integer::sum);
        }
        assertEquals(0.819, counts.get(ShardType.B) / (double) n, 0.002);
        assertEquals(0.176, counts.get(ShardType.A) / (double) n, 0.002);
        assertEquals(0.005, counts.get(ShardType.S) / (double) n, 0.0005);
        assertNull(counts.get(ShardType.SP));
    }

    @Test
    void enhancementNeverLeavesRangeAndKeepsThirtyPercentOfFailures() {
        int keeps = 0;
        int fails = 0;
        for (int i = 0; i < 200_000; i++) {
            int after = Enhancement.roll(9, random);
            assertTrue(after >= 1 && after <= 10);
            if (after != 10) {
                fails++;
                if (after == 9) {
                    keeps++;
                }
            }
        }
        assertEquals(0.30, keeps / (double) fails, 0.01);
        assertEquals(2, Enhancement.roll(1, random), "1→2 is always a success");
        assertEquals(10, Enhancement.roll(10, random));
    }

    @Test
    void evolutionTicketsBehave() {
        Evolution.Step sToSPlus = Evolution.step(Grade.S);
        assertEquals(Grade.S_PLUS, sToSPlus.to());
        assertEquals(0.12, sToSPlus.destroy(), 1e-9);
        assertEquals(Ticket.CONFIRM_HIGH, Evolution.step(Grade.A).confirm());
        assertNull(Evolution.step(Grade.S_PLUS).confirm(), "S+ → SP cannot be confirmed");
        assertNull(Evolution.step(Grade.SP));
        for (int i = 0; i < 1000; i++) {
            assertEquals(Evolution.Outcome.SUCCESS, Evolution.roll(sToSPlus, true, false, random));
            assertFalse(Evolution.roll(sToSPlus, false, true, random) == Evolution.Outcome.DESTROYED);
        }
        for (Grade grade : Grade.values()) {
            Evolution.Step step = Evolution.step(grade);
            if (step != null) {
                assertTrue(step.keep() >= -1e-9, grade + " probabilities exceed 100%");
            }
        }
    }

    @Test
    void abilityRollsFollowRules() {
        for (int i = 0; i < 20_000; i++) {
            GearData gear = new GearData(UUID.randomUUID(), GearType.SWORD, Grade.S, 1, List.of(), null);
            boolean legendary = i % 2 == 0;
            List<AbilityEntry> rolled = AbilityRoller.rerollAll(gear, -1, legendary, random);
            assertEquals(3, rolled.size());
            Set<AbilityType> seen = new HashSet<>();
            int actives = 0;
            for (AbilityEntry entry : rolled) {
                assertTrue(seen.add(entry.type()), "duplicate ability");
                assertEquals(AbilityType.Pool.WEAPON, entry.type().pool());
                assertEquals(AbilityGrade.D, entry.grade());
                assertFalse(entry.type().rarity() == Rarity.MASTER);
                if (!legendary) {
                    assertFalse(entry.type().rarity() == Rarity.LEGENDARY);
                }
                if (entry.type().isActive()) {
                    actives++;
                }
            }
            assertTrue(actives <= 1);
            assertFalse(seen.contains(AbilityType.LIFESTEAL_ACTIVE) && seen.contains(AbilityType.LIFESTEAL_PLUS));
        }
    }

    @Test
    void armorGetsFewerSlotsAndArmorPool() {
        GearData helmet = new GearData(UUID.randomUUID(), GearType.HELMET, Grade.A, 1, List.of(), null);
        assertEquals(1, helmet.abilitySlots());
        helmet.setGrade(Grade.SP);
        assertEquals(2, helmet.abilitySlots());
        assertFalse(GearType.HELMET.hasMasterSlot(Grade.SP));
        List<AbilityEntry> rolled = AbilityRoller.rerollAll(helmet, -1, true, random);
        assertEquals(2, rolled.size());
        rolled.forEach(e -> assertEquals(AbilityType.Pool.ARMOR, e.type().pool()));
    }

    @Test
    void evolutionRerollsOnlyWhenSlotsGrow() {
        GearData gear = new GearData(UUID.randomUUID(), GearType.SWORD, Grade.C, 5, List.of(), null);
        assertTrue(GearRules.applyEvolution(gear, random));
        assertEquals(Grade.B, gear.grade());
        assertEquals(1, gear.abilities().size());
        assertEquals(5, gear.enhance(), "enhancement is kept");

        AbilityEntry before = gear.abilities().get(0);
        assertFalse(GearRules.applyEvolution(gear, random)); // B → B+: same slot count
        assertEquals(before, gear.abilities().get(0));

        gear.setGrade(Grade.S_PLUS);
        GearRules.normalize(gear, random);
        assertFalse(GearRules.applyEvolution(gear, random)); // S+ → SP: master slot stays locked
        assertEquals(Grade.SP, gear.grade());
        assertNull(gear.master());
        assertEquals(GearRules.Result.OK, GearRules.applyTicket(gear, Ticket.MASTER_CREATE, -1, random));
        assertEquals(AbilityType.ARTISAN, gear.master().type());
        assertEquals(GearRules.Result.NOT_APPLICABLE, GearRules.applyTicket(gear, Ticket.MASTER_CREATE, -1, random));
    }

    @Test
    void abilityUpgradeStopsAtCap() {
        GearData gear = new GearData(UUID.randomUUID(), GearType.SWORD, Grade.B, 1,
                List.of(new AbilityEntry(AbilityType.EXECUTE, AbilityGrade.D)), null);
        assertEquals(GearRules.Result.OK, GearRules.applyTicket(gear, Ticket.ABILITY_UP_HIGH, 0, random));
        assertEquals(GearRules.Result.OK, GearRules.applyTicket(gear, Ticket.ABILITY_UP_HIGH, 0, random));
        assertEquals(AbilityGrade.B, gear.abilities().get(0).grade());
        assertEquals(GearRules.Result.AT_CAP, GearRules.applyTicket(gear, Ticket.ABILITY_UP_HIGH, 0, random),
                "B weapon caps abilities at B");
    }

    @Test
    void lockAndSelectRerollKeepTheRightSlots() {
        GearData gear = new GearData(UUID.randomUUID(), GearType.SWORD, Grade.A_PLUS, 1, List.of(
                new AbilityEntry(AbilityType.EXECUTE, AbilityGrade.A),
                new AbilityEntry(AbilityType.COMBO, AbilityGrade.B),
                new AbilityEntry(AbilityType.CRITICAL, AbilityGrade.C)), null);
        GearRules.applyTicket(gear, Ticket.REROLL_LOCK, 0, random);
        assertEquals(new AbilityEntry(AbilityType.EXECUTE, AbilityGrade.A), gear.abilities().get(0));
        assertEquals(3, gear.abilities().size());

        AbilityEntry first = gear.abilities().get(0);
        AbilityEntry third = gear.abilities().get(2);
        GearRules.applyTicket(gear, Ticket.REROLL_SELECT, 1, random);
        assertEquals(first, gear.abilities().get(0));
        assertEquals(third, gear.abilities().get(2));
        assertEquals(AbilityGrade.D, gear.abilities().get(1).grade());
    }

    @Test
    void gearDataRoundTrips() {
        GearData gear = new GearData(UUID.randomUUID(), GearType.AXE, Grade.SP, 7, List.of(
                new AbilityEntry(AbilityType.PIERCE, AbilityGrade.S),
                new AbilityEntry(AbilityType.DASH, AbilityGrade.A)), new AbilityEntry(AbilityType.ARTISAN, AbilityGrade.C));
        GearData copy = GearData.deserialize(gear.serialize());
        assertNotNull(copy);
        assertEquals(gear.serialize(), copy.serialize());
        assertNull(GearData.deserialize("garbage"));
    }

    @Test
    void shardOverflowBecomesMileage() {
        PlayerData data = new PlayerData(UUID.randomUUID());
        data.setShards(ShardType.S, 999);
        assertEquals(500, data.addShards(ShardType.S, 2));
        assertEquals(1000, data.shards(ShardType.S));
        assertEquals(500, data.mileage());
        assertTrue(data.takeShards(Map.of(ShardType.S, 5)));
        assertFalse(data.takeShards(Map.of(ShardType.SP, 1)));
    }

    @Test
    void everyAbilityDescribesEveryGrade() {
        for (AbilityType type : AbilityType.values()) {
            for (AbilityGrade grade : AbilityGrade.values()) {
                if (grade.ordinal() <= type.maxGrade().ordinal()) {
                    assertFalse(type.describe(grade).isBlank());
                }
            }
        }
    }
}
