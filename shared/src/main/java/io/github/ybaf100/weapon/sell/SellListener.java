package io.github.ybaf100.weapon.sell;

import io.github.ybaf100.weapon.WeaponPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.InventoryHolder;

/** 판매함: 위 45칸은 자유롭게 넣고 빼고, 아래 줄은 버튼. 가격표는 읽기 전용. */
public final class SellListener implements Listener {
    private final WeaponPlugin plugin;

    public SellListener(WeaponPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        boolean top = event.getClickedInventory() == event.getView().getTopInventory();
        if (holder instanceof PriceListMenu menu) {
            event.setCancelled(true);
            if (top) {
                int slot = event.getRawSlot();
                Bukkit.getScheduler().runTask(plugin, () -> menu.click(slot));
            }
            return;
        }
        if (!(holder instanceof SellMenu menu)) {
            return;
        }
        if (top && event.getRawSlot() >= SellMenu.BOX_SIZE) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            Bukkit.getScheduler().runTask(plugin, () -> menu.click(slot));
            return;
        }
        if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            // 더블클릭으로 아래 줄 버튼까지 끌어오지 않도록 막는다.
            event.setCancelled(true);
            return;
        }
        Bukkit.getScheduler().runTask(plugin, menu::refresh);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (holder instanceof PriceListMenu) {
            event.setCancelled(true);
            return;
        }
        if (!(holder instanceof SellMenu menu)) {
            return;
        }
        for (int slot : event.getRawSlots()) {
            if (slot >= SellMenu.BOX_SIZE && slot < 54) {
                event.setCancelled(true);
                return;
            }
        }
        Bukkit.getScheduler().runTask(plugin, menu::refresh);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof SellMenu menu) {
            menu.returnItems();
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (player.getOpenInventory().getTopInventory().getHolder() instanceof SellMenu menu) {
            menu.returnItems();
        }
    }
}
