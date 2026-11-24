package io.github.ybaf100.weapon.model;

import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;

/** 시스템 장비 종류. */
public enum GearType {
    SWORD("검", Material.DIAMOND_SWORD, Category.WEAPON, EquipmentSlot.HAND, 7.0, 1.6, 0, 0),
    AXE("도끼", Material.DIAMOND_AXE, Category.WEAPON, EquipmentSlot.HAND, 9.0, 1.0, 0, 0),
    PICKAXE("곡괭이", Material.DIAMOND_PICKAXE, Category.TOOL, EquipmentSlot.HAND, 0, 0, 0, 0),
    HELMET("투구", Material.DIAMOND_HELMET, Category.ARMOR, EquipmentSlot.HEAD, 0, 0, 3, 2),
    CHESTPLATE("흉갑", Material.DIAMOND_CHESTPLATE, Category.ARMOR, EquipmentSlot.CHEST, 0, 0, 8, 2),
    LEGGINGS("레깅스", Material.DIAMOND_LEGGINGS, Category.ARMOR, EquipmentSlot.LEGS, 0, 0, 6, 2),
    BOOTS("부츠", Material.DIAMOND_BOOTS, Category.ARMOR, EquipmentSlot.FEET, 0, 0, 3, 2);

    public enum Category { WEAPON, TOOL, ARMOR }

    private final String koreanName;
    private final Material material;
    private final Category category;
    private final EquipmentSlot slot;
    private final double baseDamage;
    private final double attackSpeed;
    private final double baseArmor;
    private final double baseToughness;

    GearType(String koreanName, Material material, Category category, EquipmentSlot slot,
             double baseDamage, double attackSpeed, double baseArmor, double baseToughness) {
        this.koreanName = koreanName;
        this.material = material;
        this.category = category;
        this.slot = slot;
        this.baseDamage = baseDamage;
        this.attackSpeed = attackSpeed;
        this.baseArmor = baseArmor;
        this.baseToughness = baseToughness;
    }

    public String koreanName() {
        return koreanName;
    }

    public Material material() {
        return material;
    }

    public Category category() {
        return category;
    }

    public EquipmentSlot slot() {
        return slot;
    }

    public double baseDamage() {
        return baseDamage;
    }

    public double attackSpeed() {
        return attackSpeed;
    }

    public double baseArmor() {
        return baseArmor;
    }

    public double baseToughness() {
        return baseToughness;
    }

    /** 이 등급에서 일반 고유능력 슬롯 수 (Master 제외). */
    public int abilitySlots(Grade grade) {
        return switch (category) {
            case WEAPON -> grade.weaponSlots();
            case ARMOR -> grade.armorSlots();
            case TOOL -> 0;
        };
    }

    /** 이 등급에서 Master 슬롯이 있는지 (SP 무기만). */
    public boolean hasMasterSlot(Grade grade) {
        return category == Category.WEAPON && grade == Grade.SP;
    }

    /** 고유능력을 뽑을 때 쓰는 능력 풀. 곡괭이는 없음. */
    public AbilityType.Pool abilityPool() {
        return switch (category) {
            case WEAPON -> AbilityType.Pool.WEAPON;
            case ARMOR -> AbilityType.Pool.ARMOR;
            case TOOL -> null;
        };
    }

    public static GearType byMaterial(Material material) {
        for (GearType type : values()) {
            if (type.material == material) {
                return type;
            }
        }
        return null;
    }

    public static GearType byName(String name) {
        for (GearType type : values()) {
            if (type.name().equalsIgnoreCase(name) || type.koreanName.equals(name)) {
                return type;
            }
        }
        return null;
    }
}
