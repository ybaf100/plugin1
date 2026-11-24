package io.github.ybaf100.weapon.combat;

import io.github.ybaf100.weapon.WeaponPlugin;
import io.github.ybaf100.weapon.data.PlayerData;
import io.github.ybaf100.weapon.model.AbilityEntry;
import io.github.ybaf100.weapon.model.AbilityGrade;
import io.github.ybaf100.weapon.model.AbilityType;
import io.github.ybaf100.weapon.util.Text;
import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.Vector;

/** 액티브 능력: 무기를 들고 우클릭. 쿨타임 45초. */
public final class ActiveListener implements Listener {
    private final WeaponPlugin plugin;
    private final StatusEffects status;

    public ActiveListener(WeaponPlugin plugin) {
        this.plugin = plugin;
        this.status = plugin.status();
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block != null && block.getType().isInteractable() && !event.getPlayer().isSneaking()) {
            return;
        }
        Player player = event.getPlayer();
        AbilityEntry active = GearStats.of(player).weaponActive();
        if (active == null) {
            return;
        }
        PlayerData data = plugin.data().get(player.getUniqueId());
        long now = System.currentTimeMillis();
        long until = data.cooldownUntil(CombatRules.CD_ACTIVE);
        if (now < until) {
            long left = (until - now + 999) / 1000;
            player.sendActionBar(Text.c("&c" + active.type().koreanName() + " 쿨타임 " + left + "초"));
            return;
        }
        data.setCooldown(CombatRules.CD_ACTIVE, now + CombatRules.ACTIVE_COOLDOWN_MS);
        activate(player, active.type(), active.grade());
        player.sendActionBar(Text.c("&a" + active.type().koreanName() + " 발동!"));
    }

    private void activate(Player player, AbilityType type, AbilityGrade grade) {
        switch (type) {
            case SHARPNESS_ACTIVE -> {
                status.put(status.sharpBuff, player.getUniqueId(), type.value(0, grade), (long) (type.value(1, grade) * 1000));
                player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_RETURN, 1f, 1.5f);
                player.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE, player.getLocation().add(0, 1, 0), 40, 0.4, 0.6, 0.4, 0.5);
            }
            case LIFESTEAL_ACTIVE -> {
                status.put(status.lifestealBuff, player.getUniqueId(), type.value(0, grade), (long) (type.value(1, grade) * 1000));
                player.playSound(player.getLocation(), Sound.ENTITY_WITCH_DRINK, 1f, 0.8f);
                player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 2, 0), 6, 0.4, 0.2, 0.4, 0);
            }
            case DASH -> {
                Vector dir = player.getLocation().getDirection().setY(0);
                if (dir.lengthSquared() < 1.0E-4) {
                    dir = new Vector(0, 0, 1);
                }
                double distance = type.value(1, grade);
                player.setVelocity(dir.normalize().multiply(distance * 0.28).setY(0.25));
                status.put(status.dashBonus, player.getUniqueId(), type.value(0, grade), CombatRules.DASH_WINDOW_MS);
                player.playSound(player.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 1f, 1.4f);
                player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 15, 0.3, 0.1, 0.3, 0.05);
            }
            case WHIRLWIND -> {
                double damage = type.value(0, grade);
                double r = CombatRules.WHIRLWIND_RADIUS;
                for (Entity entity : player.getNearbyEntities(r, r, r)) {
                    if (!(entity instanceof LivingEntity target) || entity instanceof ArmorStand) {
                        continue;
                    }
                    if (target instanceof Player other && (other.getGameMode() == GameMode.CREATIVE
                            || other.getGameMode() == GameMode.SPECTATOR)) {
                        continue;
                    }
                    if (target.getLocation().distanceSquared(player.getLocation()) > r * r) {
                        continue;
                    }
                    SkillDamage.run(() -> target.damage(damage, player));
                }
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.7f);
                player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, player.getLocation().add(0, 1, 0), 12, 1.5, 0.2, 1.5, 0);
            }
            default -> {
            }
        }
    }
}
