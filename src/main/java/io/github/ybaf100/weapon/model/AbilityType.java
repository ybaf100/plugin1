package io.github.ybaf100.weapon.model;

import static io.github.ybaf100.weapon.util.Fmt.num;
import static io.github.ybaf100.weapon.util.Fmt.pct;

import java.util.function.BiFunction;

/**
 * 고유능력 목록. 수치 배열은 능력 등급 D, C, B, A, S 순서이고, 최대 등급까지만 들어 있다.
 * HP 기준 (1하트 = 2HP). 비율은 0.1 = 10%.
 */
public enum AbilityType {
    // ---------- 무기 (검 / 도끼) ----------
    SHARPNESS_ACTIVE("날카로움", Pool.WEAPON, Rarity.COMMON, true, AbilityGrade.B,
            new double[][]{{0.5, 1, 1.5}, {1, 5, 10}},
            (t, g) -> "우클릭: " + num(t.value(1, g)) + "초 동안 타격마다 추가 피해 +" + num(t.value(0, g))),
    LIFESTEAL_ACTIVE("흡혈", Pool.WEAPON, Rarity.COMMON, true, AbilityGrade.A,
            new double[][]{{0.20, 0.25, 0.30, 0.40}, {2, 3, 4, 6}},
            (t, g) -> "우클릭: " + num(t.value(1, g)) + "초 동안 준 피해의 " + pct(t.value(0, g)) + " 흡혈"),
    COMBO("연타", Pool.WEAPON, Rarity.COMMON, false, AbilityGrade.B,
            new double[][]{{1, 1.5, 2}},
            (t, g) -> "같은 대상 연속 3번째 타격에 추가 피해 +" + num(t.value(0, g))),
    SWIFT_WEAPON("신속", Pool.WEAPON, Rarity.COMMON, false, AbilityGrade.B,
            new double[][]{{0.03, 0.05, 0.07}},
            (t, g) -> "들고 있으면 이동속도 +" + pct(t.value(0, g))),
    HUNTER("사냥꾼", Pool.WEAPON, Rarity.COMMON, false, AbilityGrade.A,
            new double[][]{{0.10, 0.15, 0.20, 0.25}},
            (t, g) -> "몹 대상 피해 +" + pct(t.value(0, g))),
    SHARPNESS_PLUS("날카로움+", Pool.WEAPON, Rarity.RARE, false, AbilityGrade.A,
            new double[][]{{1, 1.5, 2, 3}},
            (t, g) -> "타격마다 추가 피해 +" + num(t.value(0, g))),
    CRITICAL("치명타", Pool.WEAPON, Rarity.RARE, false, AbilityGrade.A,
            new double[][]{{0.05, 0.07, 0.10, 0.12}},
            (t, g) -> pct(t.value(0, g)) + " 확률로 피해 ×1.5"),
    BLEED("출혈", Pool.WEAPON, Rarity.RARE, false, AbilityGrade.A,
            new double[][]{{0.5, 0.75, 1, 1.5}},
            (t, g) -> "15% 확률로 3초 출혈, 초당 " + num(t.value(0, g)) + " 피해"),
    DASH("돌진", Pool.WEAPON, Rarity.RARE, true, AbilityGrade.A,
            new double[][]{{1, 1.5, 2, 3}, {4, 5, 6, 7}},
            (t, g) -> "우클릭: 앞으로 " + num(t.value(1, g)) + "칸 돌진, 3초 안의 첫 타격 +" + num(t.value(0, g))),
    LIFESTEAL_PLUS("흡혈+", Pool.WEAPON, Rarity.EPIC, false, AbilityGrade.S,
            new double[][]{{0.10, 0.15, 0.20, 0.25, 0.30}},
            (t, g) -> "준 피해의 " + pct(t.value(0, g)) + " 흡혈"),
    MAX_HEALTH("최대 체력 증가", Pool.WEAPON, Rarity.EPIC, false, AbilityGrade.S,
            new double[][]{{2, 3, 4, 6, 8}},
            (t, g) -> "들고 있으면 최대 체력 +" + num(t.value(0, g))),
    WOUND("상처", Pool.WEAPON, Rarity.EPIC, false, AbilityGrade.S,
            new double[][]{{0.20, 0.25, 0.30, 0.40, 0.50}},
            (t, g) -> "맞은 대상의 회복량 4초 동안 -" + pct(t.value(0, g))),
    EXECUTE("처형", Pool.WEAPON, Rarity.EPIC, false, AbilityGrade.S,
            new double[][]{{1, 1.5, 2, 3, 4}},
            (t, g) -> "대상 체력 30% 이하일 때 추가 피해 +" + num(t.value(0, g))),
    WHIRLWIND("회오리", Pool.WEAPON, Rarity.EPIC, true, AbilityGrade.S,
            new double[][]{{3, 4, 5, 6, 8}},
            (t, g) -> "우클릭: 반경 3칸 안의 적에게 " + num(t.value(0, g)) + " 피해"),
    LAST_STAND("최후의 힘", Pool.WEAPON, Rarity.LEGENDARY, false, AbilityGrade.S,
            new double[][]{{2, 3, 4, 6, 8}},
            (t, g) -> "내 체력이 5 미만일 때 추가 피해 +" + num(t.value(0, g))),
    PIERCE("관통", Pool.WEAPON, Rarity.LEGENDARY, false, AbilityGrade.S,
            new double[][]{{0.10, 0.15, 0.20, 0.25, 0.30}},
            (t, g) -> "대상 방어력 " + pct(t.value(0, g)) + " 무시"),
    UNDYING("불굴", Pool.WEAPON, Rarity.LEGENDARY, false, AbilityGrade.S,
            new double[][]{{300, 270, 240, 210, 180}},
            (t, g) -> "죽을 만큼 맞으면 체력 1로 버팀 (쿨타임 " + num(t.value(0, g)) + "초)"),
    ARTISAN("장인", Pool.WEAPON, Rarity.MASTER, false, AbilityGrade.S,
            new double[][]{{1.5, 2, 2.5, 3, 4}, {0.15, 0.20, 0.25, 0.30, 0.40}, {6, 7, 8, 10, 14}},
            (t, g) -> "추가 피해 +" + num(t.value(0, g)) + ", 흡혈 " + pct(t.value(1, g))
                    + ", 최대 체력 +" + num(t.value(2, g))),

    // ---------- 방어구 ----------
    PROTECTION("보호", Pool.ARMOR, Rarity.COMMON, false, AbilityGrade.B,
            new double[][]{{0.02, 0.03, 0.04}},
            (t, g) -> "받는 피해 -" + pct(t.value(0, g))),
    ELEMENTAL("원소 보호", Pool.ARMOR, Rarity.COMMON, false, AbilityGrade.B,
            new double[][]{{0.10, 0.15, 0.20}},
            (t, g) -> "불, 폭발 피해 -" + pct(t.value(0, g))),
    FEATHER("깃털", Pool.ARMOR, Rarity.COMMON, false, AbilityGrade.B,
            new double[][]{{0.20, 0.30, 0.40}},
            (t, g) -> "낙하 피해 -" + pct(t.value(0, g))),
    SWIFT_ARMOR("신속", Pool.ARMOR, Rarity.COMMON, false, AbilityGrade.B,
            new double[][]{{0.03, 0.05, 0.07}},
            (t, g) -> "이동속도 +" + pct(t.value(0, g))),
    THORNS("가시", Pool.ARMOR, Rarity.RARE, false, AbilityGrade.A,
            new double[][]{{0.05, 0.08, 0.10, 0.15}},
            (t, g) -> "근접 공격자에게 받은 피해의 " + pct(t.value(0, g)) + " 반사"),
    REGEN("재생", Pool.ARMOR, Rarity.RARE, false, AbilityGrade.A,
            new double[][]{{0.5, 0.75, 1, 1.5}},
            (t, g) -> "6초 동안 안 맞으면 2초마다 +" + num(t.value(0, g)) + " 회복"),
    STURDY("견고", Pool.ARMOR, Rarity.RARE, false, AbilityGrade.A,
            new double[][]{{0.10, 0.15, 0.20, 0.25}},
            (t, g) -> "넉백 저항 +" + pct(t.value(0, g))),
    VITALITY("활력", Pool.ARMOR, Rarity.EPIC, false, AbilityGrade.S,
            new double[][]{{1, 1.5, 2, 3, 4}},
            (t, g) -> "최대 체력 +" + num(t.value(0, g))),
    PURIFY("정화", Pool.ARMOR, Rarity.EPIC, false, AbilityGrade.S,
            new double[][]{{0.15, 0.20, 0.25, 0.30, 0.40}},
            (t, g) -> "출혈, 상처, 디버프 지속시간 -" + pct(t.value(0, g))),
    REACTIVE("반응 장갑", Pool.ARMOR, Rarity.EPIC, false, AbilityGrade.S,
            new double[][]{{0.10, 0.15, 0.20, 0.25, 0.30}},
            (t, g) -> "3초 안에 3번 맞으면 2초 동안 받는 피해 -" + pct(t.value(0, g)) + " (쿨 20초)"),
    GUARDIAN("수호", Pool.ARMOR, Rarity.LEGENDARY, false, AbilityGrade.S,
            new double[][]{{0.30, 0.35, 0.40, 0.45, 0.50}},
            (t, g) -> "체력 30% 이하가 되면 3초 동안 받는 피해 -" + pct(t.value(0, g)) + " (쿨 120초)");

    /** 능력이 붙는 장비 풀. */
    public enum Pool { WEAPON, ARMOR }

    private final String koreanName;
    private final Pool pool;
    private final Rarity rarity;
    private final boolean active;
    private final AbilityGrade maxGrade;
    private final double[][] values;
    private final BiFunction<AbilityType, AbilityGrade, String> describer;

    AbilityType(String koreanName, Pool pool, Rarity rarity, boolean active, AbilityGrade maxGrade,
                double[][] values, BiFunction<AbilityType, AbilityGrade, String> describer) {
        this.koreanName = koreanName;
        this.pool = pool;
        this.rarity = rarity;
        this.active = active;
        this.maxGrade = maxGrade;
        this.values = values;
        this.describer = describer;
        for (double[] row : values) {
            if (row.length != maxGrade.ordinal() + 1) {
                throw new IllegalStateException(name() + ": value count does not match max grade");
            }
        }
    }

    public String koreanName() {
        return koreanName;
    }

    public Pool pool() {
        return pool;
    }

    public Rarity rarity() {
        return rarity;
    }

    public boolean isActive() {
        return active;
    }

    public AbilityGrade maxGrade() {
        return maxGrade;
    }

    /** 수치. 등급이 최대 등급보다 높으면 최대 등급 값을 쓴다. */
    public double value(int index, AbilityGrade grade) {
        double[] row = values[index];
        return row[Math.min(grade.ordinal(), row.length - 1)];
    }

    public double value(AbilityGrade grade) {
        return value(0, grade);
    }

    public String describe(AbilityGrade grade) {
        return describer.apply(this, grade);
    }

    /** "[A] 날카로움" 형태의 이름. */
    public String tagName() {
        return (active ? "[A] " : "[P] ") + koreanName;
    }

    /** 흡혈 [A]와 흡혈+ [P]처럼 같이 붙을 수 없는 조합인지. */
    public boolean conflictsWith(AbilityType other) {
        if (this == other) {
            return true;
        }
        if (this.active && other.active) {
            return true;
        }
        return (this == LIFESTEAL_ACTIVE && other == LIFESTEAL_PLUS)
                || (this == LIFESTEAL_PLUS && other == LIFESTEAL_ACTIVE);
    }
}
