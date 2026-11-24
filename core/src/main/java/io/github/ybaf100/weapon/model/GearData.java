package io.github.ybaf100.weapon.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** 시스템 장비 하나에 저장되는 데이터. 아이템의 PersistentDataContainer에 문자열로 들어간다. */
public final class GearData {
    public static final int MIN_ENHANCE = 1;
    public static final int MAX_ENHANCE = 10;

    private final UUID id;
    private final GearType type;
    private Grade grade;
    private int enhance;
    private final List<AbilityEntry> abilities;
    private AbilityEntry master;

    public GearData(UUID id, GearType type, Grade grade, int enhance, List<AbilityEntry> abilities, AbilityEntry master) {
        this.id = id;
        this.type = type;
        this.grade = grade;
        this.enhance = enhance;
        this.abilities = new ArrayList<>(abilities);
        this.master = master;
    }

    public static GearData create(GearType type) {
        return new GearData(UUID.randomUUID(), type, Grade.D, MIN_ENHANCE, List.of(), null);
    }

    public UUID id() {
        return id;
    }

    public GearType type() {
        return type;
    }

    public Grade grade() {
        return grade;
    }

    public void setGrade(Grade grade) {
        this.grade = grade;
    }

    public int enhance() {
        return enhance;
    }

    public void setEnhance(int enhance) {
        this.enhance = Math.max(MIN_ENHANCE, Math.min(MAX_ENHANCE, enhance));
    }

    /** 일반 슬롯의 고유능력 (Master 제외). 수정 가능한 리스트. */
    public List<AbilityEntry> abilities() {
        return abilities;
    }

    public void setAbilities(List<AbilityEntry> list) {
        abilities.clear();
        abilities.addAll(list);
    }

    /** Master 고유능력. 생성 전이면 null. */
    public AbilityEntry master() {
        return master;
    }

    public void setMaster(AbilityEntry master) {
        this.master = master;
    }

    public int abilitySlots() {
        return type.abilitySlots(grade);
    }

    /** 고유능력 등급 상한: 장비 등급 상한과 능력 자체 상한 중 낮은 쪽. */
    public AbilityGrade capFor(AbilityType ability) {
        AbilityGrade cap = grade.abilityCap();
        if (ability.rarity() == Rarity.MASTER || cap == null) {
            cap = cap == null ? ability.maxGrade() : cap;
        }
        return AbilityGrade.min(cap, ability.maxGrade());
    }

    /** 이 장비에서 해당 능력의 등급. 없으면 null. Master도 포함. */
    public AbilityGrade gradeOf(AbilityType ability) {
        for (AbilityEntry entry : abilities) {
            if (entry.type() == ability) {
                return entry.grade();
            }
        }
        if (master != null && master.type() == ability) {
            return master.grade();
        }
        return null;
    }

    public String serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append(id).append('|').append(type.name()).append('|').append(grade.name()).append('|').append(enhance).append('|');
        for (int i = 0; i < abilities.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(abilities.get(i).serialize());
        }
        sb.append('|').append(master == null ? "" : master.serialize());
        return sb.toString();
    }

    public static GearData deserialize(String raw) {
        if (raw == null) {
            return null;
        }
        String[] parts = raw.split("\\|", -1);
        if (parts.length != 6) {
            return null;
        }
        try {
            UUID id = UUID.fromString(parts[0]);
            GearType type = GearType.valueOf(parts[1]);
            Grade grade = Grade.valueOf(parts[2]);
            int enhance = Integer.parseInt(parts[3]);
            List<AbilityEntry> list = new ArrayList<>();
            if (!parts[4].isEmpty()) {
                for (String token : parts[4].split(",")) {
                    AbilityEntry entry = AbilityEntry.deserialize(token);
                    if (entry != null) {
                        list.add(entry);
                    }
                }
            }
            AbilityEntry master = parts[5].isEmpty() ? null : AbilityEntry.deserialize(parts[5]);
            return new GearData(id, type, grade, enhance, list, master);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
