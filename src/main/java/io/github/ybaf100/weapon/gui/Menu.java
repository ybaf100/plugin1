package io.github.ybaf100.weapon.gui;

import io.github.ybaf100.weapon.WeaponPlugin;
import io.github.ybaf100.weapon.item.Items;
import io.github.ybaf100.weapon.util.Text;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/** 모든 GUI의 기반. 인벤토리 홀더로 메뉴를 구분하고, 슬롯마다 클릭 동작을 건다. */
public abstract class Menu implements InventoryHolder {
    protected final WeaponPlugin plugin;
    protected final Player player;
    private final Inventory inventory;
    private final Map<Integer, Consumer<InventoryClickEvent>> actions = new HashMap<>();

    protected Menu(WeaponPlugin plugin, Player player, int rows, String title) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, rows * 9, Text.c(title));
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    /** 내용을 다시 그린다. */
    protected abstract void draw();

    public void open() {
        redraw();
        player.openInventory(inventory);
    }

    public void redraw() {
        actions.clear();
        inventory.clear();
        draw();
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, Items.filler());
            }
        }
    }

    protected void set(int slot, ItemStack item) {
        inventory.setItem(slot, item);
    }

    protected void set(int slot, ItemStack item, Consumer<InventoryClickEvent> action) {
        inventory.setItem(slot, item);
        actions.put(slot, action);
    }

    protected void backButton(int slot, Runnable action) {
        set(slot, Items.button(Material.BARRIER, "&c돌아가기", "&7이전 메뉴로 돌아갑니다."), e -> action.run());
    }

    void handleClick(InventoryClickEvent event) {
        Consumer<InventoryClickEvent> action = actions.get(event.getRawSlot());
        if (action != null) {
            action.accept(event);
        }
    }

    /** 메뉴가 닫힐 때. 필요한 메뉴만 덮어쓴다. */
    void onClose() {
    }
}
