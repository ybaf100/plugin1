package io.github.ybaf100.weapon.item;

import io.github.ybaf100.weapon.util.Text;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.inventory.ItemStack;

/** 시스템 장비는 모루, 인챈트 테이블, 대장장이 작업대, 제작대 수리에 쓸 수 없다. */
public final class ProtectionListener implements Listener {

    @EventHandler
    public void onAnvil(PrepareAnvilEvent event) {
        if (containsGear(event.getInventory().getContents())) {
            event.setResult(null);
        }
    }

    @EventHandler
    public void onPrepareEnchant(PrepareItemEnchantEvent event) {
        if (GearItems.isGear(event.getItem())) {
            event.setCancelled(true);
            warn(event.getEnchanter());
        }
    }

    @EventHandler
    public void onEnchant(EnchantItemEvent event) {
        if (GearItems.isGear(event.getItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSmithing(PrepareSmithingEvent event) {
        if (containsGear(event.getInventory().getContents())) {
            event.setResult(null);
        }
    }

    @EventHandler
    public void onCraft(PrepareItemCraftEvent event) {
        if (containsGear(event.getInventory().getMatrix())) {
            event.getInventory().setResult(null);
        }
    }

    private static boolean containsGear(ItemStack[] items) {
        for (ItemStack item : items) {
            if (GearItems.isGear(item)) {
                return true;
            }
        }
        return false;
    }

    private static void warn(HumanEntity entity) {
        Text.send(entity, "&c[Error] 시스템 장비는 인챈트할 수 없습니다.");
    }
}
