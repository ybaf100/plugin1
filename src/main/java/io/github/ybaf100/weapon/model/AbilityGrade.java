package io.github.ybaf100.weapon.model;

/** 고유능력 등급 (D ~ S). */
public enum AbilityGrade {
    D("&8"),
    C("&7"),
    B("&a"),
    A("&d"),
    S("&6");

    private final String color;

    AbilityGrade(String color) {
        this.color = color;
    }

    public String color() {
        return color;
    }

    public String display() {
        return color + name();
    }

    public AbilityGrade next() {
        AbilityGrade[] values = values();
        return ordinal() + 1 < values.length ? values[ordinal() + 1] : null;
    }

    public static AbilityGrade min(AbilityGrade a, AbilityGrade b) {
        return a.ordinal() <= b.ordinal() ? a : b;
    }
}
