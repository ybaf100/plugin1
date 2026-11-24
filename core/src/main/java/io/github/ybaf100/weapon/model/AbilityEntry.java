package io.github.ybaf100.weapon.model;

/** 장비에 붙은 고유능력 하나. */
public record AbilityEntry(AbilityType type, AbilityGrade grade) {

    public AbilityEntry withGrade(AbilityGrade newGrade) {
        return new AbilityEntry(type, newGrade);
    }

    public String serialize() {
        return type.name() + ":" + grade.name();
    }

    public static AbilityEntry deserialize(String raw) {
        String[] parts = raw.split(":");
        if (parts.length != 2) {
            return null;
        }
        try {
            return new AbilityEntry(AbilityType.valueOf(parts[0]), AbilityGrade.valueOf(parts[1]));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
