package io.github.ybaf100.weapon.logic;

import io.github.ybaf100.weapon.model.AbilityEntry;
import io.github.ybaf100.weapon.model.AbilityGrade;
import io.github.ybaf100.weapon.model.AbilityType;
import io.github.ybaf100.weapon.model.GearData;
import io.github.ybaf100.weapon.model.Rarity;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** 고유능력 뽑기. */
public final class AbilityRoller {

    private static final Map<Rarity, Integer> WEIGHTS_NORMAL = new EnumMap<>(Rarity.class);
    private static final Map<Rarity, Integer> WEIGHTS_LEGENDARY = new EnumMap<>(Rarity.class);

    static {
        WEIGHTS_NORMAL.put(Rarity.COMMON, 60);
        WEIGHTS_NORMAL.put(Rarity.RARE, 28);
        WEIGHTS_NORMAL.put(Rarity.EPIC, 12);

        WEIGHTS_LEGENDARY.put(Rarity.COMMON, 55);
        WEIGHTS_LEGENDARY.put(Rarity.RARE, 27);
        WEIGHTS_LEGENDARY.put(Rarity.EPIC, 15);
        WEIGHTS_LEGENDARY.put(Rarity.LEGENDARY, 3);
    }

    private AbilityRoller() {
    }

    public static Map<Rarity, Integer> weights(boolean legendary) {
        return legendary ? WEIGHTS_LEGENDARY : WEIGHTS_NORMAL;
    }

    /** 이미 붙은 능력들과 규칙상 함께 붙을 수 있는 후보. */
    public static List<AbilityType> eligible(AbilityType.Pool pool, Rarity rarity, List<AbilityType> taken) {
        List<AbilityType> result = new ArrayList<>();
        outer:
        for (AbilityType type : AbilityType.values()) {
            if (type.pool() != pool || type.rarity() != rarity) {
                continue;
            }
            for (AbilityType other : taken) {
                if (type.conflictsWith(other)) {
                    continue outer;
                }
            }
            result.add(type);
        }
        return result;
    }

    /** 능력 하나를 뽑는다. 붙일 수 있는 능력이 하나도 없으면 null. */
    public static AbilityType rollOne(AbilityType.Pool pool, List<AbilityType> taken, boolean legendary, Random random) {
        Map<Rarity, Integer> weights = weights(legendary);
        Map<Rarity, List<AbilityType>> candidates = new EnumMap<>(Rarity.class);
        int total = 0;
        for (Map.Entry<Rarity, Integer> entry : weights.entrySet()) {
            List<AbilityType> list = eligible(pool, entry.getKey(), taken);
            if (!list.isEmpty()) {
                candidates.put(entry.getKey(), list);
                total += entry.getValue();
            }
        }
        if (total == 0) {
            return null;
        }
        int r = random.nextInt(total);
        for (Map.Entry<Rarity, List<AbilityType>> entry : candidates.entrySet()) {
            r -= weights.get(entry.getKey());
            if (r < 0) {
                List<AbilityType> list = entry.getValue();
                return list.get(random.nextInt(list.size()));
            }
        }
        throw new IllegalStateException("unreachable");
    }

    /**
     * 슬롯 하나를 다시 뽑는다. 나머지 능력과 Master는 그대로 두고 규칙 검사에만 쓴다.
     *
     * @return 새 능력 (D급). 뽑을 수 없으면 원래 능력.
     */
    public static AbilityEntry rerollSlot(GearData gear, int index, boolean legendary, Random random) {
        List<AbilityType> taken = new ArrayList<>();
        for (int i = 0; i < gear.abilities().size(); i++) {
            if (i != index) {
                taken.add(gear.abilities().get(i).type());
            }
        }
        if (gear.master() != null) {
            taken.add(gear.master().type());
        }
        AbilityType type = rollOne(gear.type().abilityPool(), taken, legendary, random);
        return type == null ? gear.abilities().get(index) : new AbilityEntry(type, AbilityGrade.D);
    }

    /**
     * 일반 슬롯을 전부 다시 뽑는다. keepIndex의 능력은 고정한다 (-1이면 고정 없음).
     */
    public static List<AbilityEntry> rerollAll(GearData gear, int keepIndex, boolean legendary, Random random) {
        int slots = gear.abilitySlots();
        AbilityType.Pool pool = gear.type().abilityPool();
        List<AbilityEntry> result = new ArrayList<>();
        AbilityEntry kept = keepIndex >= 0 && keepIndex < gear.abilities().size() ? gear.abilities().get(keepIndex) : null;
        List<AbilityType> taken = new ArrayList<>();
        if (gear.master() != null) {
            taken.add(gear.master().type());
        }
        if (kept != null) {
            taken.add(kept.type());
        }
        for (int i = 0; i < slots; i++) {
            if (kept != null && i == keepIndex) {
                result.add(kept);
                continue;
            }
            AbilityType type = pool == null ? null : rollOne(pool, taken, legendary, random);
            if (type == null) {
                break;
            }
            taken.add(type);
            result.add(new AbilityEntry(type, AbilityGrade.D));
        }
        return result;
    }
}
