package io.github.ybaf100.weapon.logic;

import io.github.ybaf100.weapon.model.GearData;
import io.github.ybaf100.weapon.model.ShardType;
import java.util.Map;
import java.util.Random;

/** 강화 규칙 (1 ~ 10강). */
public final class Enhancement {
    /** 실패했을 때 현재 단계를 유지할 확률. 나머지는 아래 단계로 균일하게 떨어진다. */
    public static final double KEEP_ON_FAIL = 0.30;

    /** 인덱스 = 현재 강화 단계. [1]은 1→2 성공 확률. */
    private static final double[] SUCCESS = {0, 1.00, 0.81, 0.64, 0.50, 0.26, 0.15, 0.07, 0.04, 0.02};

    /** 인덱스 = 강화 단계. 효과 배율. */
    private static final double[] MULTIPLIER = {0, 1.00, 1.05, 1.15, 1.30, 1.50, 1.75, 2.05, 2.45, 2.95, 3.50};

    /** 인덱스 = 현재 강화 단계. 다음 단계로 가는 재료. */
    @SuppressWarnings("unchecked")
    private static final Map<ShardType, Integer>[] COST = new Map[]{
            null,
            Map.of(ShardType.D, 3),
            Map.of(ShardType.D, 5),
            Map.of(ShardType.C, 3),
            Map.of(ShardType.C, 5),
            Map.of(ShardType.B, 3),
            Map.of(ShardType.B, 5),
            Map.of(ShardType.A, 1),
            Map.of(ShardType.A, 2),
            Map.of(ShardType.A, 3),
    };

    private Enhancement() {
    }

    public static boolean isMax(int level) {
        return level >= GearData.MAX_ENHANCE;
    }

    public static double successChance(int level) {
        return SUCCESS[level];
    }

    public static double multiplier(int level) {
        return MULTIPLIER[Math.max(1, Math.min(GearData.MAX_ENHANCE, level))];
    }

    public static Map<ShardType, Integer> cost(int level) {
        return COST[level];
    }

    /** 강화를 한 번 시도한 뒤의 단계. */
    public static int roll(int level, Random random) {
        if (isMax(level)) {
            return level;
        }
        if (random.nextDouble() < SUCCESS[level]) {
            return level + 1;
        }
        if (level <= 1 || random.nextDouble() < KEEP_ON_FAIL) {
            return level;
        }
        return 1 + random.nextInt(level - 1);
    }
}
