package io.github.ybaf100.weapon.combat;

import io.github.ybaf100.weapon.model.AbilityType;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.EntityEffect;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/** 전투 중 잠깐 유지되는 상태 (버프, 디버프, 연타 카운트 등). 서버를 다시 켜면 사라진다. */
public final class StatusEffects {

    /** 값과 끝나는 시각. */
    public record Timed(double value, long until) {
        boolean active(long now) {
            return now < until;
        }
    }

    private record Combo(UUID target, int count, long last) {
    }

    public final Map<UUID, Timed> sharpBuff = new ConcurrentHashMap<>();
    public final Map<UUID, Timed> lifestealBuff = new ConcurrentHashMap<>();
    public final Map<UUID, Timed> dashBonus = new ConcurrentHashMap<>();
    public final Map<UUID, Timed> reactiveActive = new ConcurrentHashMap<>();
    public final Map<UUID, Timed> guardianActive = new ConcurrentHashMap<>();
    public final Map<UUID, Timed> bleed = new ConcurrentHashMap<>();
    public final Map<UUID, Timed> wound = new ConcurrentHashMap<>();
    public final Map<UUID, Long> invulnerable = new ConcurrentHashMap<>();
    public final Map<UUID, Long> lastHit = new ConcurrentHashMap<>();
    private final Map<UUID, Deque<Long>> recentHits = new ConcurrentHashMap<>();
    private final Map<UUID, Combo> combo = new ConcurrentHashMap<>();

    public static double active(Map<UUID, Timed> map, UUID id) {
        Timed timed = map.get(id);
        if (timed == null) {
            return 0;
        }
        if (!timed.active(System.currentTimeMillis())) {
            map.remove(id);
            return 0;
        }
        return timed.value();
    }

    public void put(Map<UUID, Timed> map, UUID id, double value, long durationMs) {
        map.put(id, new Timed(value, System.currentTimeMillis() + durationMs));
    }

    /** 연타: 같은 대상을 연속으로 치면 카운트가 오르고, 3번째에 true. */
    public boolean comboHit(UUID attacker, UUID target) {
        long now = System.currentTimeMillis();
        Combo current = combo.get(attacker);
        int count = 1;
        if (current != null && current.target().equals(target) && now - current.last() <= CombatRules.COMBO_WINDOW_MS) {
            count = current.count() + 1;
        }
        if (count >= 3) {
            combo.remove(attacker);
            return true;
        }
        combo.put(attacker, new Combo(target, count, now));
        return false;
    }

    /** 반응 장갑용: 최근 피격 시각을 기록하고, 창 안의 피격 수를 돌려준다. */
    public int recordHit(UUID victim) {
        long now = System.currentTimeMillis();
        lastHit.put(victim, now);
        Deque<Long> hits = recentHits.computeIfAbsent(victim, k -> new ArrayDeque<>());
        hits.addLast(now);
        while (!hits.isEmpty() && now - hits.peekFirst() > CombatRules.REACTIVE_WINDOW_MS) {
            hits.pollFirst();
        }
        return hits.size();
    }

    /** 정화 수치 (플레이어 방어구만). */
    public static double purify(Entity entity) {
        return entity instanceof Player player ? GearStats.of(player).armorMax(AbilityType.PURIFY) : 0;
    }

    public void applyBleed(LivingEntity target, double dps) {
        long duration = (long) (CombatRules.BLEED_DURATION_MS * (1 - purify(target)));
        Timed current = bleed.get(target.getUniqueId());
        double value = current != null && current.active(System.currentTimeMillis()) ? Math.max(dps, current.value()) : dps;
        put(bleed, target.getUniqueId(), value, duration);
    }

    public void applyWound(LivingEntity target, double pct) {
        long duration = (long) (CombatRules.WOUND_DURATION_MS * (1 - purify(target)));
        Timed current = wound.get(target.getUniqueId());
        double value = current != null && current.active(System.currentTimeMillis()) ? Math.max(pct, current.value()) : pct;
        put(wound, target.getUniqueId(), value, duration);
    }

    /** 상처를 반영해 회복시킨다. */
    public void heal(LivingEntity entity, double amount) {
        if (amount <= 0 || entity.isDead()) {
            return;
        }
        double actual = amount * (1 - active(wound, entity.getUniqueId()));
        AttributeInstance max = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        double cap = max == null ? 20 : max.getValue();
        entity.setHealth(Math.min(cap, entity.getHealth() + actual));
    }

    /** 1초마다: 출혈 피해. */
    public void tickBleed() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, Timed>> it = bleed.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Timed> entry = it.next();
            Entity entity = Bukkit.getEntity(entry.getKey());
            if (!entry.getValue().active(now) || !(entity instanceof LivingEntity living) || living.isDead()) {
                it.remove();
                continue;
            }
            double dps = entry.getValue().value();
            living.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, living.getLocation().add(0, 1, 0), 3, 0.2, 0.3, 0.2, 0);
            if (living.getHealth() - dps > 0.5) {
                living.setHealth(living.getHealth() - dps);
                living.playEffect(EntityEffect.HURT);
            } else {
                SkillDamage.run(() -> living.damage(dps + 100));
            }
        }
    }

    public void forget(UUID id) {
        sharpBuff.remove(id);
        lifestealBuff.remove(id);
        dashBonus.remove(id);
        reactiveActive.remove(id);
        guardianActive.remove(id);
        bleed.remove(id);
        wound.remove(id);
        invulnerable.remove(id);
        lastHit.remove(id);
        recentHits.remove(id);
        combo.remove(id);
    }
}
