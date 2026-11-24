package io.github.ybaf100.weapon.logic;

import io.github.ybaf100.weapon.model.Grade;
import io.github.ybaf100.weapon.model.ShardType;
import io.github.ybaf100.weapon.model.Ticket;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

/** 진화 규칙. */
public final class Evolution {

    public enum Outcome { SUCCESS, KEEP, PROTECTED, DESTROYED }

    /**
     * @param cost    진화 재료
     * @param success 성공 확률
     * @param destroy 파괴 확률 (나머지는 유지)
     * @param confirm 확정 상승권 종류, 쓸 수 없으면 null
     */
    public record Step(Grade from, Grade to, Map<ShardType, Integer> cost, double success, double destroy, Ticket confirm) {
        public double keep() {
            return 1.0 - success - destroy;
        }

        public boolean protectable() {
            return destroy > 0;
        }
    }

    private static final Map<Grade, Step> STEPS = new EnumMap<>(Grade.class);

    static {
        add(Grade.D, cost(ShardType.D, 3, ShardType.C, 1), 1.0, 0, null);
        add(Grade.C, cost(ShardType.C, 3, ShardType.B, 1), 1.0, 0, null);
        add(Grade.B, cost(ShardType.B, 5), 0.9, 0, Ticket.CONFIRM_LOW);
        add(Grade.B_PLUS, cost(ShardType.B, 5, ShardType.A, 1), 0.8, 0, Ticket.CONFIRM_LOW);
        add(Grade.A_MINUS, cost(ShardType.A, 3), 0.7, 0, Ticket.CONFIRM_LOW);
        add(Grade.A, cost(ShardType.A, 5), 0.6, 0.03, Ticket.CONFIRM_HIGH);
        add(Grade.A_PLUS, cost(ShardType.A, 7, ShardType.S, 1), 0.5, 0.05, Ticket.CONFIRM_HIGH);
        add(Grade.S_MINUS, cost(ShardType.S, 3), 0.4, 0.08, Ticket.CONFIRM_HIGH);
        add(Grade.S, cost(ShardType.S, 5), 0.3, 0.12, Ticket.CONFIRM_HIGH);
        add(Grade.S_PLUS, cost(ShardType.S, 7, ShardType.SP, 1), 0.5, 0.15, null);
    }

    private Evolution() {
    }

    /** 현재 등급에서 다음 등급으로 가는 단계. 최고 등급이면 null. */
    public static Step step(Grade from) {
        return STEPS.get(from);
    }

    public static Outcome roll(Step step, boolean useConfirm, boolean useProtect, Random random) {
        if (useConfirm && step.confirm() != null) {
            return Outcome.SUCCESS;
        }
        double r = random.nextDouble();
        if (r < step.success()) {
            return Outcome.SUCCESS;
        }
        if (r < step.success() + step.destroy()) {
            return useProtect ? Outcome.PROTECTED : Outcome.DESTROYED;
        }
        return Outcome.KEEP;
    }

    private static void add(Grade from, Map<ShardType, Integer> cost, double success, double destroy, Ticket confirm) {
        STEPS.put(from, new Step(from, from.next(), cost, success, destroy, confirm));
    }

    private static Map<ShardType, Integer> cost(Object... pairs) {
        EnumMap<ShardType, Integer> map = new EnumMap<>(ShardType.class);
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((ShardType) pairs[i], (Integer) pairs[i + 1]);
        }
        return Collections.unmodifiableMap(map);
    }
}
