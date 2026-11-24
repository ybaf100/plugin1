package io.github.ybaf100.weapon.logic;

import io.github.ybaf100.weapon.model.AbilityEntry;
import io.github.ybaf100.weapon.model.AbilityGrade;
import io.github.ybaf100.weapon.model.AbilityType;
import io.github.ybaf100.weapon.model.GearData;
import io.github.ybaf100.weapon.model.Grade;
import io.github.ybaf100.weapon.model.Ticket;
import java.util.List;
import java.util.Random;

/** 장비 데이터에 진화, 고유능력 아이템을 적용하는 규칙. Bukkit에 의존하지 않는다. */
public final class GearRules {

    private GearRules() {
    }

    /** 진화에 성공해서 등급을 올린다. 슬롯이 새로 생기면 일반 능력을 전부 다시 뽑는다. */
    public static boolean applyEvolution(GearData gear, Random random) {
        Grade next = gear.grade().next();
        if (next == null) {
            return false;
        }
        int before = gear.abilitySlots();
        gear.setGrade(next);
        int after = gear.abilitySlots();
        if (after > before) {
            gear.setAbilities(AbilityRoller.rerollAll(gear, -1, next.legendaryAllowed(), random));
        }
        return after > before;
    }

    /** 데이터가 등급과 맞지 않을 때 (예: 관리자 지급) 슬롯을 채운다. */
    public static void normalize(GearData gear, Random random) {
        int slots = gear.abilitySlots();
        List<AbilityEntry> list = gear.abilities();
        while (list.size() > slots) {
            list.remove(list.size() - 1);
        }
        if (list.size() < slots) {
            gear.setAbilities(AbilityRoller.rerollAll(gear, -1, gear.grade().legendaryAllowed(), random));
        }
        if (!gear.type().hasMasterSlot(gear.grade())) {
            gear.setMaster(null);
        }
        for (int i = 0; i < list.size(); i++) {
            AbilityEntry entry = list.get(i);
            AbilityGrade cap = gear.capFor(entry.type());
            if (entry.grade().ordinal() > cap.ordinal()) {
                list.set(i, entry.withGrade(cap));
            }
        }
    }

    public enum Result { OK, FAILED, NOT_APPLICABLE, AT_CAP }

    /**
     * 고유능력 아이템을 쓴다.
     *
     * @param target 대상 능력 인덱스. Master는 {@link #MASTER_INDEX}. 대상이 필요 없으면 -1.
     */
    public static Result applyTicket(GearData gear, Ticket ticket, int target, Random random) {
        if (gear.abilitySlots() == 0) {
            return Result.NOT_APPLICABLE;
        }
        switch (ticket) {
            case ABILITY_UP, ABILITY_UP_HIGH -> {
                AbilityEntry entry = entryAt(gear, target);
                if (entry == null) {
                    return Result.NOT_APPLICABLE;
                }
                AbilityGrade cap = gear.capFor(entry.type());
                if (entry.grade().ordinal() >= cap.ordinal()) {
                    return Result.AT_CAP;
                }
                if (ticket == Ticket.ABILITY_UP && random.nextDouble() >= 0.10) {
                    return Result.FAILED;
                }
                setEntry(gear, target, entry.withGrade(entry.grade().next()));
                return Result.OK;
            }
            case REROLL -> {
                gear.setAbilities(AbilityRoller.rerollAll(gear, -1, false, random));
                return Result.OK;
            }
            case REROLL_HIGH -> {
                gear.setAbilities(AbilityRoller.rerollAll(gear, -1, gear.grade().legendaryAllowed(), random));
                return Result.OK;
            }
            case REROLL_LOCK -> {
                if (target < 0 || target >= gear.abilities().size()) {
                    return Result.NOT_APPLICABLE;
                }
                gear.setAbilities(AbilityRoller.rerollAll(gear, target, false, random));
                return Result.OK;
            }
            case REROLL_SELECT -> {
                if (target < 0 || target >= gear.abilities().size()) {
                    return Result.NOT_APPLICABLE;
                }
                gear.abilities().set(target, AbilityRoller.rerollSlot(gear, target, false, random));
                return Result.OK;
            }
            case MASTER_CREATE -> {
                if (!gear.type().hasMasterSlot(gear.grade()) || gear.master() != null) {
                    return Result.NOT_APPLICABLE;
                }
                gear.setMaster(new AbilityEntry(AbilityType.ARTISAN, AbilityGrade.D));
                return Result.OK;
            }
            default -> {
                return Result.NOT_APPLICABLE;
            }
        }
    }

    public static final int MASTER_INDEX = 100;

    private static AbilityEntry entryAt(GearData gear, int index) {
        if (index == MASTER_INDEX) {
            return gear.master();
        }
        return index >= 0 && index < gear.abilities().size() ? gear.abilities().get(index) : null;
    }

    private static void setEntry(GearData gear, int index, AbilityEntry entry) {
        if (index == MASTER_INDEX) {
            gear.setMaster(entry);
        } else {
            gear.abilities().set(index, entry);
        }
    }
}
