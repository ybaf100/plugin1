package io.github.ybaf100.weapon.model;

/** 무기 파편 종류. */
public enum ShardType {
    D("D", "COPPER_INGOT", "&8", 10),
    C("C", "IRON_INGOT", "&7", 20),
    B("B", "GOLD_INGOT", "&a", 50),
    A("A", "DIAMOND", "&d", 100),
    S("S", "EMERALD", "&6", 500),
    SP("SP", "NETHERITE_INGOT", "&b&l", 5000);

    /** 종류별 최대 보유량. 넘치면 마일리지로 바뀐다. */
    public static final int MAX_AMOUNT = 1000;

    private final String label;
    private final String material;
    private final String color;
    private final int overflowMileage;

    ShardType(String label, String material, String color, int overflowMileage) {
        this.label = label;
        this.material = material;
        this.color = color;
        this.overflowMileage = overflowMileage;
    }

    public String label() {
        return label;
    }

    /** 아이콘 아이템의 Material 이름. */
    public String material() {
        return material;
    }

    public String color() {
        return color;
    }

    public int overflowMileage() {
        return overflowMileage;
    }

    public String displayName() {
        return color + label + "급 무기 파편";
    }

    public static ShardType byLabel(String label) {
        for (ShardType type : values()) {
            if (type.label.equalsIgnoreCase(label)) {
                return type;
            }
        }
        return null;
    }
}
