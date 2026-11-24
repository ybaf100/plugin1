package io.github.ybaf100.weapon.combat;

import io.github.ybaf100.weapon.WeaponPlugin;
import io.github.ybaf100.weapon.model.AbilityType;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;

/**
 * 0.5초마다 착용한 장비의 능력으로 최대 체력, 이동속도, 넉백 저항을 다시 계산하고,
 * 2초마다 재생을 적용한다. 합산 상한(B12, P4)은 여기서 처리한다.
 */
public final class StatTask {
    private static final UUID HEALTH_ID = uuid("health");
    private static final UUID SPEED_ID = uuid("speed");
    private static final UUID KNOCKBACK_ID = uuid("knockback");

    private final WeaponPlugin plugin;
    private int ticks;

    public StatTask(WeaponPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::run, 10L, 10L);
    }

    private void run() {
        ticks += 10;
        boolean regenTick = ticks % 40 == 0;
        boolean bleedTick = ticks % 20 == 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            GearStats stats = GearStats.of(player);
            double weaponHealth = stats.weaponValue(AbilityType.MAX_HEALTH) + stats.weaponValue(AbilityType.ARTISAN, 2);
            double armorHealth = Math.min(CombatRules.ARMOR_HEALTH_CAP, stats.armorSum(AbilityType.VITALITY));
            double health = Math.min(CombatRules.TOTAL_HEALTH_CAP, weaponHealth + armorHealth);
            double speed = Math.min(CombatRules.SPEED_CAP,
                    stats.weaponValue(AbilityType.SWIFT_WEAPON) + stats.armorSum(AbilityType.SWIFT_ARMOR));
            double knockback = stats.armorMax(AbilityType.STURDY);

            apply(player, Attribute.GENERIC_MAX_HEALTH, HEALTH_ID, health, AttributeModifier.Operation.ADD_NUMBER);
            apply(player, Attribute.GENERIC_MOVEMENT_SPEED, SPEED_ID, speed, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
            apply(player, Attribute.GENERIC_KNOCKBACK_RESISTANCE, KNOCKBACK_ID, knockback, AttributeModifier.Operation.ADD_NUMBER);

            double max = CombatListener.maxHealth(player);
            if (player.getHealth() > max) {
                player.setHealth(max);
            }

            if (regenTick) {
                double regen = stats.armorMax(AbilityType.REGEN);
                Long last = plugin.status().lastHit.get(player.getUniqueId());
                boolean outOfCombat = last == null || System.currentTimeMillis() - last >= CombatRules.REGEN_DELAY_MS;
                if (regen > 0 && outOfCombat && !player.isDead() && player.getHealth() < max) {
                    plugin.status().heal(player, regen);
                }
            }
        }
        if (bleedTick) {
            plugin.status().tickBleed();
        }
    }

    private static void apply(Player player, Attribute attribute, UUID id, double amount, AttributeModifier.Operation op) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        AttributeModifier existing = null;
        for (AttributeModifier modifier : instance.getModifiers()) {
            if (modifier.getUniqueId().equals(id)) {
                existing = modifier;
                break;
            }
        }
        if (existing != null && Math.abs(existing.getAmount() - amount) < 1.0E-6) {
            return;
        }
        if (existing != null) {
            instance.removeModifier(existing);
        }
        if (amount != 0) {
            instance.addModifier(new AttributeModifier(id, "weaponsystem." + attribute.name(), amount, op));
        }
    }

    /** 서버 종료나 퇴장 때 플레이어에게 붙인 속성을 지운다. */
    public static void clear(Player player) {
        apply(player, Attribute.GENERIC_MAX_HEALTH, HEALTH_ID, 0, AttributeModifier.Operation.ADD_NUMBER);
        apply(player, Attribute.GENERIC_MOVEMENT_SPEED, SPEED_ID, 0, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
        apply(player, Attribute.GENERIC_KNOCKBACK_RESISTANCE, KNOCKBACK_ID, 0, AttributeModifier.Operation.ADD_NUMBER);
    }

    private static UUID uuid(String name) {
        return UUID.nameUUIDFromBytes(("weaponsystem:player:" + name).getBytes(StandardCharsets.UTF_8));
    }
}
