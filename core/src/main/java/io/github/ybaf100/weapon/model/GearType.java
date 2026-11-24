package io.github.ybaf100.weapon.model;

/** 시스템 장비 종류. */
public enum GearType {
    SWORD("검", "DIAMOND_SWORD", Category.WEAPON, Slot.HAND, 7.0, 1.6, 0, 0),
    AXE("도끼", "DIAMOND_AXE", Category.WEAPON, Slot.HAND, 9.0, 1.0, 0, 0),
    PICKAXE("곡괭이", "DIAMOND_PICKAXE", Category.TOOL, Slot.HAND, 0, 0, 0, 0),
    HELMET("투구", "DIAMOND_HELMET", Category.ARMOR, Slot.HEAD, 0, 0, 3, 2),
    CHESTPLATE("흉갑", "DIAMOND_CHESTPLATE", Category.ARMOR, Slot.CHEST, 0, 0, 8, 2),
    LEGGINGS("레깅스", "DIAMOND_LEGGINGS", Category.ARMOR, Slot.LEGS, 0, 0, 6, 2),
    BOOTS("부츠", "DIAMOND_BOOTS", Category.ARMOR, Slot.FEET, 0, 0, 3, 2);

    public enum Category { WEAPON, TOOL, ARMOR }

    /** 장비를 착용하는 칸. */
    public enum Slot { HAND, HEAD, CHEST, LEGS, FEET }

    private final String koreanName;
    private final String material;
    private final Category category;
    private final Slot slot;
    private final double baseDamage;
    private final double attackSpeed;
    private final double baseArmor;
    private final double baseToughness;

    GearType(String koreanName, String material, Category category, Slot slot,
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

    /** 바닐라 아이템의 Material 이름. */
    public String material() {
        return material;
    }

    public Category category() {
        return category;
    }

    public Slot slot() {
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

    public static GearType byMaterial(String material) {
        for (GearType type : values()) {
            if (type.material.equals(material)) {
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
