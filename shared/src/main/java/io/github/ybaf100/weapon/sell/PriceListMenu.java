package io.github.ybaf100.weapon.sell;

import io.github.ybaf100.weapon.WeaponPlugin;
import io.github.ybaf100.weapon.util.Fmt;
import io.github.ybaf100.weapon.util.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** 읽기 전용 가격표. 45칸씩 쪽을 나눈다. */
public final class PriceListMenu implements InventoryHolder {
    private static final int PER_PAGE = 45;
    static final int PREV = 45;
    static final int BACK = 49;
    static final int NEXT = 53;

    private final WeaponPlugin plugin;
    private final Player player;
    private final int page;
    private final Inventory inventory;
    private final int pages;

    private record Entry(Material material, String category, double price) {
    }

    public PriceListMenu(WeaponPlugin plugin, Player player, int page) {
        this.plugin = plugin;
        this.player = player;
        List<Entry> entries = entries(plugin.prices());
        this.pages = Math.max(1, (entries.size() + PER_PAGE - 1) / PER_PAGE);
        this.page = Math.max(0, Math.min(page, pages - 1));
        this.inventory = Bukkit.createInventory(this, 54, Text.c("&2가격표 &8(" + (this.page + 1) + "/" + pages + ")"));
        draw(entries);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open() {
        player.openInventory(inventory);
    }

    private static List<Entry> entries(PriceTable prices) {
        List<Entry> list = new ArrayList<>();
        for (Map.Entry<String, List<Material>> category : prices.categories().entrySet()) {
            for (Material material : category.getValue()) {
                list.add(new Entry(material, PriceTable.categoryName(category.getKey()), prices.unitPrice(material)));
            }
        }
        return list;
    }

    private void draw(List<Entry> entries) {
        int start = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE && start + i < entries.size(); i++) {
            Entry entry = entries.get(start + i);
            ItemStack item = new ItemStack(entry.material());
            ItemMeta meta = item.getItemMeta();
            meta.lore(Text.lines(List.of(
                    "&8" + entry.category(),
                    "&7개당 &a" + Fmt.money(entry.price()) + "원",
                    "&764개 &a" + Fmt.money(entry.price() * 64) + "원")));
            meta.addItemFlags(ItemFlag.values());
            item.setItemMeta(meta);
            inventory.setItem(i, item);
        }
        ItemStack filler = SellMenu.button(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = PER_PAGE; i < 54; i++) {
            inventory.setItem(i, filler);
        }
        if (page > 0) {
            inventory.setItem(PREV, SellMenu.button(Material.ARROW, "&f이전 쪽"));
        }
        if (page < pages - 1) {
            inventory.setItem(NEXT, SellMenu.button(Material.ARROW, "&f다음 쪽"));
        }
        inventory.setItem(BACK, SellMenu.button(Material.CHEST, "&a판매함으로", "&7판매함을 다시 엽니다."));
    }

    void click(int slot) {
        switch (slot) {
            case PREV -> {
                if (page > 0) {
                    new PriceListMenu(plugin, player, page - 1).open();
                }
            }
            case NEXT -> {
                if (page < pages - 1) {
                    new PriceListMenu(plugin, player, page + 1).open();
                }
            }
            case BACK -> new SellMenu(plugin, player).open();
            default -> {
            }
        }
    }
}
