package io.github.ybaf100.weapon.model;

/** 고유능력 희귀도. */
public enum Rarity {
    COMMON("Common", "&f"),
    RARE("Rare", "&9"),
    EPIC("Epic", "&5"),
    LEGENDARY("Legendary", "&6"),
    MASTER("Master", "&c");

    private final String label;
    private final String color;

    Rarity(String label, String color) {
        this.label = label;
        this.color = color;
    }

    public String label() {
        return label;
    }

    public String color() {
        return color;
    }
}
