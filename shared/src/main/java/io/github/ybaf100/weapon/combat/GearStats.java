package io.github.ybaf100.weapon.combat;

import io.github.ybaf100.weapon.item.GearItems;
import io.github.ybaf100.weapon.model.AbilityEntry;
import io.github.ybaf100.weapon.model.AbilityGrade;
import io.github.ybaf100.weapon.model.AbilityType;
import io.github.ybaf100.weapon.model.GearData;
import io.github.ybaf100.weapon.model.GearType;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

/** 한 생명체가 착용한 시스템 장비의 고유능력을 모아 본다. */
public final class GearStats {
    private static final GearStats EMPTY = new GearStats(null, List.of());

    private final GearData weapon;
    private final List<GearData> armor;

    private GearStats(GearData weapon, List<GearData> armor) {
        this.weapon = weapon;
        this.armor = armor;
    }

    public static GearStats of(LivingEntity entity) {
        EntityEquipment equipment = entity.getEquipment();
        if (equipment == null) {
            return EMPTY;
        }
        GearData weapon = GearItems.read(equipment.getItemInMainHand());
        if (weapon != null && weapon.type().category() != GearType.Category.WEAPON) {
            weapon = null;
        }
        List<GearData> armor = new ArrayList<>(4);
        for (ItemStack item : equipment.getArmorContents()) {
            GearData data = GearItems.read(item);
            if (data != null && data.type().category() == GearType.Category.ARMOR) {
                armor.add(data);
            }
        }
        return new GearStats(weapon, armor);
    }

    public GearData weapon() {
        return weapon;
    }

    /** 손에 든 무기에서 능력 등급. 없으면 null. */
    public AbilityGrade weaponGrade(AbilityType type) {
        return weapon == null ? null : weapon.gradeOf(type);
    }

    /** 무기 능력 수치. 없으면 0. */
    public double weaponValue(AbilityType type, int index) {
        AbilityGrade grade = weaponGrade(type);
        return grade == null ? 0 : type.value(index, grade);
    }

    public double weaponValue(AbilityType type) {
        return weaponValue(type, 0);
    }

    /** 방어구 여러 부위의 같은 능력을 합산. */
    public double armorSum(AbilityType type) {
        double sum = 0;
        for (GearData data : armor) {
            AbilityGrade grade = data.gradeOf(type);
            if (grade != null) {
                sum += type.value(grade);
            }
        }
        return sum;
    }

    /** 방어구 여러 부위의 같은 능력 중 가장 높은 값. */
    public double armorMax(AbilityType type) {
        double max = 0;
        for (GearData data : armor) {
            AbilityGrade grade = data.gradeOf(type);
            if (grade != null) {
                max = Math.max(max, type.value(grade));
            }
        }
        return max;
    }

    /** 무기에 붙은 액티브 능력. 없으면 null. */
    public AbilityEntry weaponActive() {
        if (weapon == null) {
            return null;
        }
        for (AbilityEntry entry : weapon.abilities()) {
            if (entry.type().isActive()) {
                return entry;
            }
        }
        return null;
    }
}
