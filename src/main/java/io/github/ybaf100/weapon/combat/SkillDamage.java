package io.github.ybaf100.weapon.combat;

/**
 * 능력이 직접 주는 피해(회오리, 출혈, 가시)를 표시한다.
 * 이 피해로는 공격 능력(추가 피해, 흡혈, 출혈 등)이 다시 발동하지 않는다.
 */
public final class SkillDamage {
    private static int depth;

    private SkillDamage() {
    }

    public static boolean active() {
        return depth > 0;
    }

    public static void run(Runnable action) {
        depth++;
        try {
            action.run();
        } finally {
            depth--;
        }
    }
}
