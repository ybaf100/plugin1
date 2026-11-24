package io.github.ybaf100.weapon.combat;

import java.util.Set;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** 정화: 나쁜 포션 효과의 지속시간을 줄인다. */
public final class PurifyListener implements Listener {
    private static final Set<PotionEffectType> NEGATIVE = Set.of(
            PotionEffectType.POISON, PotionEffectType.WITHER, PotionEffectType.SLOW, PotionEffectType.SLOW_DIGGING,
            PotionEffectType.WEAKNESS, PotionEffectType.BLINDNESS, PotionEffectType.CONFUSION, PotionEffectType.HUNGER,
            PotionEffectType.LEVITATION, PotionEffectType.DARKNESS);

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPotion(EntityPotionEffectEvent event) {
        if (event.getCause() == EntityPotionEffectEvent.Cause.PLUGIN || !(event.getEntity() instanceof Player player)) {
            return;
        }
        if (event.getAction() != EntityPotionEffectEvent.Action.ADDED && event.getAction() != EntityPotionEffectEvent.Action.CHANGED) {
            return;
        }
        PotionEffect effect = event.getNewEffect();
        if (effect == null || !NEGATIVE.contains(effect.getType()) || effect.getDuration() <= 1) {
            return;
        }
        double purify = StatusEffects.purify(player);
        if (purify <= 0) {
            return;
        }
        int duration = Math.max(1, (int) Math.round(effect.getDuration() * (1 - purify)));
        event.setCancelled(true);
        player.addPotionEffect(new PotionEffect(effect.getType(), duration, effect.getAmplifier(),
                effect.isAmbient(), effect.hasParticles(), effect.hasIcon()));
    }
}
