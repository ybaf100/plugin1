package io.github.ybaf100.weapon.combat;

/** PvP 밸런스 상한값 (기획서 6.6절, B12). */
public final class CombatRules {
    /** P3: 받는 피해 감소 합계 상한. */
    public static final double DAMAGE_REDUCTION_CAP = 0.40;
    /** B12: 보호 합산 상한. */
    public static final double PROTECTION_CAP = 0.15;
    /** B12: 이동속도 합산 상한 (무기 + 방어구). */
    public static final double SPEED_CAP = 0.10;
    /** B12: 방어구 활력 합산 상한. */
    public static final double ARMOR_HEALTH_CAP = 12;
    /** P4: 최대 체력 증가 합계 상한. */
    public static final double TOTAL_HEALTH_CAP = 20;
    /** P6: 플레이어가 한 번에 받는 피해 상한. */
    public static final double PVP_HIT_CAP = 16;

    /** 액티브 능력 쿨타임. */
    public static final long ACTIVE_COOLDOWN_MS = 45_000;
    public static final long REACTIVE_COOLDOWN_MS = 20_000;
    public static final long REACTIVE_WINDOW_MS = 3_000;
    public static final long REACTIVE_DURATION_MS = 2_000;
    public static final long GUARDIAN_COOLDOWN_MS = 120_000;
    public static final long GUARDIAN_DURATION_MS = 3_000;
    public static final double GUARDIAN_THRESHOLD = 0.30;
    public static final double EXECUTE_THRESHOLD = 0.30;
    public static final double LAST_STAND_HEALTH = 5;
    public static final double BLEED_CHANCE = 0.15;
    public static final long BLEED_DURATION_MS = 3_000;
    public static final long WOUND_DURATION_MS = 4_000;
    public static final long DASH_WINDOW_MS = 3_000;
    public static final long COMBO_WINDOW_MS = 3_000;
    public static final long REGEN_DELAY_MS = 6_000;
    public static final long UNDYING_INVULNERABLE_MS = 1_000;
    public static final double WHIRLWIND_RADIUS = 3;

    public static final String CD_ACTIVE = "active";
    public static final String CD_UNDYING = "undying";
    public static final String CD_GUARDIAN = "guardian";
    public static final String CD_REACTIVE = "reactive";

    private CombatRules() {
    }
}
