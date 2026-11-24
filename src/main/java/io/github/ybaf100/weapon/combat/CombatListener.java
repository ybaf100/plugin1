package io.github.ybaf100.weapon.combat;

import io.github.ybaf100.weapon.WeaponPlugin;
import io.github.ybaf100.weapon.data.PlayerData;
import io.github.ybaf100.weapon.model.AbilityType;
import io.github.ybaf100.weapon.model.GearData;
import io.github.ybaf100.weapon.util.Text;
import java.util.Map;
import java.util.WeakHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDamageEvent.DamageModifier;
import org.bukkit.event.entity.EntityRegainHealthEvent;

/**
 * 고유능력 피해 계산.
 * <ol>
 *   <li>HIGH: 공격자 무기 능력 (추가 피해, 치명타, 사냥꾼) → 방어력 적용 전 피해(BASE)에 더한다 (P1).</li>
 *   <li>HIGHEST: 피격자 방어구 능력 (피해 감소, 원소, 깃털), 관통, PvP 한 방 상한 (P6), 불굴.</li>
 *   <li>MONITOR: 실제로 들어간 피해로 흡혈 (P2), 출혈, 상처, 가시, 반응 장갑, 수호.</li>
 * </ol>
 */
@SuppressWarnings("deprecation") // DamageModifier: 관통(방어력 일부 무시)에 필요
public final class CombatListener implements Listener {

    /** 한 번의 근접 공격에서 다음 단계로 넘길 정보. */
    private static final class AttackContext {
        Player attacker;
        double sharpShare;
        double lifesteal;
        double pierce;
        double bleed;
        double wound;
    }

    private final WeaponPlugin plugin;
    private final StatusEffects status;
    private final Map<EntityDamageEvent, AttackContext> contexts = new WeakHashMap<>();

    public CombatListener(WeaponPlugin plugin) {
        this.plugin = plugin;
        this.status = plugin.status();
    }

    // ------------------------------------------------------------------ 공격자

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (SkillDamage.active() || event.getCause() != DamageCause.ENTITY_ATTACK) {
            return;
        }
        if (!(event.getDamager() instanceof Player attacker) || !(event.getEntity() instanceof LivingEntity victim)) {
            return;
        }
        GearStats stats = GearStats.of(attacker);
        GearData weapon = stats.weapon();
        if (weapon == null) {
            return;
        }
        long now = System.currentTimeMillis();
        double base = event.getDamage();

        // 공격 쿨타임이 덜 찬 상태로 연타하면 추가 피해도 같은 비율로 줄인다.
        AttributeInstance attackAttr = attacker.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        double fullBase = attackAttr == null ? base : attackAttr.getValue();
        double scale = fullBase <= 0 ? 1 : Math.max(0.1, Math.min(1.0, base / fullBase));

        double sharp = StatusEffects.active(status.sharpBuff, attacker.getUniqueId())
                + stats.weaponValue(AbilityType.SHARPNESS_PLUS);
        double other = 0;
        if (stats.weaponGrade(AbilityType.COMBO) != null && status.comboHit(attacker.getUniqueId(), victim.getUniqueId())) {
            other += stats.weaponValue(AbilityType.COMBO);
        }
        double victimMax = maxHealth(victim);
        if (stats.weaponGrade(AbilityType.EXECUTE) != null && victim.getHealth() <= victimMax * CombatRules.EXECUTE_THRESHOLD) {
            other += stats.weaponValue(AbilityType.EXECUTE);
        }
        if (stats.weaponGrade(AbilityType.LAST_STAND) != null && attacker.getHealth() < CombatRules.LAST_STAND_HEALTH) {
            other += stats.weaponValue(AbilityType.LAST_STAND);
        }
        other += stats.weaponValue(AbilityType.ARTISAN, 0);
        StatusEffects.Timed dash = status.dashBonus.remove(attacker.getUniqueId());
        if (dash != null && now < dash.until()) {
            other += dash.value();
        }

        double sharpScaled = sharp * scale;
        double total = base + sharpScaled + other * scale;
        double sharpShare = total <= 0 ? 0 : sharpScaled / total;

        double critChance = stats.weaponValue(AbilityType.CRITICAL);
        if (critChance > 0 && plugin.random().nextDouble() < critChance) {
            total *= 1.5;
            victim.getWorld().spawnParticle(Particle.CRIT_MAGIC, victim.getLocation().add(0, 1, 0), 15, 0.3, 0.4, 0.3, 0.2);
            attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1.3f);
        }
        if (!(victim instanceof Player)) {
            total *= 1 + stats.weaponValue(AbilityType.HUNTER);
        }
        event.setDamage(total);

        AttackContext ctx = new AttackContext();
        ctx.attacker = attacker;
        ctx.sharpShare = sharpShare;
        ctx.lifesteal = Math.max(StatusEffects.active(status.lifestealBuff, attacker.getUniqueId()),
                Math.max(stats.weaponValue(AbilityType.LIFESTEAL_PLUS), stats.weaponValue(AbilityType.ARTISAN, 1)));
        ctx.pierce = stats.weaponValue(AbilityType.PIERCE);
        ctx.bleed = stats.weaponValue(AbilityType.BLEED);
        ctx.wound = stats.weaponValue(AbilityType.WOUND);
        contexts.put(event, ctx);
    }

    // ------------------------------------------------------------------ 피격자

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDefend(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        Long invulnerableUntil = status.invulnerable.get(entity.getUniqueId());
        if (invulnerableUntil != null) {
            if (System.currentTimeMillis() < invulnerableUntil) {
                event.setCancelled(true);
                return;
            }
            status.invulnerable.remove(entity.getUniqueId());
        }

        AttackContext ctx = contexts.get(event);
        if (entity instanceof Player victim) {
            GearStats stats = GearStats.of(victim);
            double multiplier = 1.0;
            switch (event.getCause()) {
                case FIRE, FIRE_TICK, LAVA, HOT_FLOOR, BLOCK_EXPLOSION, ENTITY_EXPLOSION ->
                        multiplier *= 1 - stats.armorMax(AbilityType.ELEMENTAL);
                case FALL -> multiplier *= 1 - stats.armorMax(AbilityType.FEATHER);
                default -> {
                }
            }
            double reduction = Math.min(CombatRules.PROTECTION_CAP, stats.armorSum(AbilityType.PROTECTION))
                    + StatusEffects.active(status.reactiveActive, victim.getUniqueId())
                    + StatusEffects.active(status.guardianActive, victim.getUniqueId());
            multiplier *= 1 - Math.min(CombatRules.DAMAGE_REDUCTION_CAP, reduction);
            if (multiplier < 1.0) {
                event.setDamage(event.getDamage() * multiplier);
            }
        }

        double pierce = ctx == null ? 0 : ctx.pierce;
        applyPierce(event, pierce);

        // P6: 플레이어가 플레이어에게 받는 한 번의 피해 상한.
        if (entity instanceof Player && event instanceof EntityDamageByEntityEvent byEntity && sourcePlayer(byEntity) != null) {
            for (int i = 0; i < 4 && event.getFinalDamage() > CombatRules.PVP_HIT_CAP + 0.01; i++) {
                event.setDamage(event.getDamage() * CombatRules.PVP_HIT_CAP / event.getFinalDamage());
                applyPierce(event, pierce);
            }
        }

        if (entity instanceof Player victim) {
            tryUndying(event, victim);
        }
    }

    private static void applyPierce(EntityDamageEvent event, double pierce) {
        if (pierce > 0 && event.isApplicable(DamageModifier.ARMOR)) {
            event.setDamage(DamageModifier.ARMOR, event.getDamage(DamageModifier.ARMOR) * (1 - pierce));
        }
    }

    private void tryUndying(EntityDamageEvent event, Player victim) {
        if (event.getCause() == DamageCause.VOID || event.getCause() == DamageCause.SUICIDE) {
            return;
        }
        if (event.getFinalDamage() < victim.getHealth() + victim.getAbsorptionAmount()) {
            return;
        }
        GearStats stats = GearStats.of(victim);
        if (stats.weaponGrade(AbilityType.UNDYING) == null) {
            return;
        }
        PlayerData data = plugin.data().get(victim.getUniqueId());
        long now = System.currentTimeMillis();
        if (now < data.cooldownUntil(CombatRules.CD_UNDYING)) {
            return;
        }
        event.setCancelled(true);
        victim.setHealth(1.0);
        status.invulnerable.put(victim.getUniqueId(), now + CombatRules.UNDYING_INVULNERABLE_MS);
        long cooldownMs = (long) (stats.weaponValue(AbilityType.UNDYING) * 1000);
        data.setCooldown(CombatRules.CD_UNDYING, now + cooldownMs);
        victim.getWorld().spawnParticle(Particle.TOTEM, victim.getLocation().add(0, 1, 0), 40, 0.4, 0.6, 0.4, 0.3);
        victim.playSound(victim.getLocation(), Sound.ITEM_TOTEM_USE, 0.7f, 1.3f);
        Text.send(victim, "&6불굴&e이 발동했습니다! &7(쿨타임 " + (cooldownMs / 1000) + "초)");
    }

    // ------------------------------------------------------------------ 피해 후

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamaged(EntityDamageEvent event) {
        double dealt = event.getFinalDamage();
        AttackContext ctx = contexts.remove(event);
        Entity entity = event.getEntity();

        if (entity instanceof Player victim && dealt > 0) {
            afterPlayerHit(event, victim, dealt);
        }

        if (ctx == null || !(entity instanceof LivingEntity victim) || dealt <= 0) {
            return;
        }
        if (ctx.lifesteal > 0) {
            status.heal(ctx.attacker, dealt * (1 - ctx.sharpShare) * ctx.lifesteal);
        }
        if (ctx.bleed > 0 && plugin.random().nextDouble() < CombatRules.BLEED_CHANCE) {
            status.applyBleed(victim, ctx.bleed);
        }
        if (ctx.wound > 0) {
            status.applyWound(victim, ctx.wound);
        }
    }

    private void afterPlayerHit(EntityDamageEvent event, Player victim, double dealt) {
        GearStats stats = GearStats.of(victim);
        PlayerData data = plugin.data().get(victim.getUniqueId());
        long now = System.currentTimeMillis();
        int hits = status.recordHit(victim.getUniqueId());

        double reactive = stats.armorMax(AbilityType.REACTIVE);
        if (reactive > 0 && hits >= 3 && now >= data.cooldownUntil(CombatRules.CD_REACTIVE)) {
            status.put(status.reactiveActive, victim.getUniqueId(), reactive, CombatRules.REACTIVE_DURATION_MS);
            data.setCooldown(CombatRules.CD_REACTIVE, now + CombatRules.REACTIVE_COOLDOWN_MS);
            victim.playSound(victim.getLocation(), Sound.ITEM_ARMOR_EQUIP_NETHERITE, 1f, 1.5f);
        }

        double guardian = stats.armorMax(AbilityType.GUARDIAN);
        double after = victim.getHealth() - dealt;
        if (guardian > 0 && after > 0 && after <= maxHealth(victim) * CombatRules.GUARDIAN_THRESHOLD
                && now >= data.cooldownUntil(CombatRules.CD_GUARDIAN)) {
            status.put(status.guardianActive, victim.getUniqueId(), guardian, CombatRules.GUARDIAN_DURATION_MS);
            data.setCooldown(CombatRules.CD_GUARDIAN, now + CombatRules.GUARDIAN_COOLDOWN_MS);
            victim.playSound(victim.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1f, 1.5f);
            Text.send(victim, "&6수호&e가 발동했습니다! 3초 동안 받는 피해 -" + Math.round(guardian * 100) + "%");
        }

        double thorns = stats.armorMax(AbilityType.THORNS);
        if (thorns > 0 && !SkillDamage.active() && event instanceof EntityDamageByEntityEvent byEntity
                && event.getCause() == DamageCause.ENTITY_ATTACK && byEntity.getDamager() instanceof LivingEntity attacker
                && attacker != victim) {
            double reflect = dealt * thorns;
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!attacker.isDead()) {
                    SkillDamage.run(() -> attacker.damage(reflect));
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onRegain(EntityRegainHealthEvent event) {
        double wound = StatusEffects.active(status.wound, event.getEntity().getUniqueId());
        if (wound > 0) {
            event.setAmount(event.getAmount() * (1 - wound));
        }
    }

    // ------------------------------------------------------------------ 도우미

    static Player sourcePlayer(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }

    static double maxHealth(LivingEntity entity) {
        AttributeInstance attr = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        return attr == null ? 20 : attr.getValue();
    }
}
