package io.github.ybaf100.weapon.model;

/** 장비 등급 (D ~ SP, 11단계). */
public enum Grade {
    D("D", "&8", "☆", 0, 0, null, false),
    C("C", "&7", "☆☆", 0, 0, null, false),
    B("B", "&a", "☆☆☆", 1, 1, AbilityGrade.B, false),
    B_PLUS("B+", "&a&l", "☆☆☆☆", 1, 1, AbilityGrade.B, false),
    A_MINUS("A-", "&d", "☆☆☆☆☆", 2, 1, AbilityGrade.A, false),
    A("A", "&d", "★", 2, 1, AbilityGrade.A, false),
    A_PLUS("A+", "&d&l", "★★", 3, 2, AbilityGrade.A, true),
    S_MINUS("S-", "&6", "★★★", 3, 2, AbilityGrade.A, true),
    S("S", "&6", "★★★★", 3, 2, AbilityGrade.S, true),
    S_PLUS("S+", "&6&l", "★★★★★", 3, 2, AbilityGrade.S, true),
    SP("SP", "&b&l", "★", 3, 2, AbilityGrade.S, true);

    private final String label;
    private final String color;
    private final String stars;
    private final int weaponSlots;
    private final int armorSlots;
    private final AbilityGrade abilityCap;
    private final boolean legendary;

    Grade(String label, String color, String stars, int weaponSlots, int armorSlots,
          AbilityGrade abilityCap, boolean legendary) {
        this.label = label;
        this.color = color;
        this.stars = stars;
        this.weaponSlots = weaponSlots;
        this.armorSlots = armorSlots;
        this.abilityCap = abilityCap;
        this.legendary = legendary;
    }

    public String label() {
        return label;
    }

    public String color() {
        return color;
    }

    /** 색과 굵기가 들어간 등급 글자. */
    public String display() {
        return color + label;
    }

    /** 색과 굵기가 들어간 별 표시. */
    public String starsDisplay() {
        return color + stars;
    }

    public int weaponSlots() {
        return weaponSlots;
    }

    public int armorSlots() {
        return armorSlots;
    }

    /** 이 등급 장비에 붙은 고유능력의 최대 등급. 고유능력이 없으면 null. */
    public AbilityGrade abilityCap() {
        return abilityCap;
    }

    /** 고유능력을 뽑을 때 Legendary가 나올 수 있는지. */
    public boolean legendaryAllowed() {
        return legendary;
    }

    public Grade next() {
        Grade[] values = values();
        return ordinal() + 1 < values.length ? values[ordinal() + 1] : null;
    }

    public static Grade byLabel(String label) {
        for (Grade grade : values()) {
            if (grade.label.equalsIgnoreCase(label) || grade.name().equalsIgnoreCase(label)) {
                return grade;
            }
        }
        return null;
    }
}
