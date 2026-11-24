package io.github.ybaf100.weapon.sell;

import io.github.ybaf100.weapon.WeaponPlugin;
import io.github.ybaf100.weapon.util.Fmt;
import io.github.ybaf100.weapon.util.Text;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * 판매함. 위 5줄(45칸)에 팔 아이템을 넣고 아래 줄 버튼으로 판다.
 * 팔지 않은 아이템은 창을 닫을 때 돌려준다.
 */
public final class SellMenu implements InventoryHolder {
    public static final int BOX_SIZE = 45;
    static final int PRICES = 45;
    static final int QUICK_SELL = 47;
    static final int SELL = 49;
    static final int CLOSE = 53;

    private static final Set<SellMenu> OPEN = ConcurrentHashMap.newKeySet();

    private final WeaponPlugin plugin;
    private final Player player;
    private final Inventory inventory;
    private boolean returned;

    public SellMenu(WeaponPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54, Text.c("&2판매함 &8- 팔 아이템을 넣으세요"));
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open() {
        refresh();
        OPEN.add(this);
        player.openInventory(inventory);
    }

    /** 아래 줄 버튼을 다시 그린다 (합계 표시 갱신). */
    void refresh() {
        SellService sell = plugin.sell();
        SellService.Sale box = sell.preview(inventory, 0, BOX_SIZE);
        SellService.Sale all = sell.preview(player.getInventory(), 0, 36);
        ItemStack filler = button(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = BOX_SIZE; i < 54; i++) {
            inventory.setItem(i, filler);
        }
        inventory.setItem(PRICES, button(Material.BOOK, "&f가격표", "&7팔 수 있는 아이템과 가격을 봅니다.",
                "&8창을 바꾸면 판매함에 넣은 아이템은 돌려받습니다."));
        inventory.setItem(QUICK_SELL, button(Material.HOPPER, "&e&l빠른 판매",
                "&7내 인벤토리의 팔 수 있는 아이템을 한 번에 팝니다.",
                "&7(갑옷 칸, 왼손 제외)",
                " ",
                "&f" + all.itemCount() + "개 &7· 예상 &a" + Fmt.money(all.total()) + "원"));
        inventory.setItem(SELL, button(Material.EMERALD, "&a&l판매하기",
                "&7판매함에 넣은 아이템을 팝니다.",
                " ",
                "&f" + box.itemCount() + "개 &7· 합계 &a" + Fmt.money(box.total()) + "원",
                "&8팔 수 없는 아이템은 창을 닫을 때 돌려받습니다."));
        inventory.setItem(CLOSE, button(Material.BARRIER, "&c닫기", "&7넣은 아이템을 돌려받고 닫습니다."));
    }

    void click(int slot) {
        switch (slot) {
            case PRICES -> new PriceListMenu(plugin, player, 0).open();
            case QUICK_SELL -> {
                plugin.sell().quickSell(player);
                refresh();
            }
            case SELL -> {
                plugin.sell().sell(player, inventory, 0, BOX_SIZE);
                refresh();
            }
            case CLOSE -> player.closeInventory();
            default -> {
            }
        }
    }

    /** 판매함에 남은 아이템을 플레이어에게 돌려준다. 한 번만 동작한다. */
    void returnItems() {
        if (returned) {
            return;
        }
        returned = true;
        OPEN.remove(this);
        List<ItemStack> left = new ArrayList<>();
        for (int i = 0; i < BOX_SIZE; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && !item.getType().isAir()) {
                left.add(item);
                inventory.setItem(i, null);
            }
        }
        if (left.isEmpty()) {
            return;
        }
        player.getInventory().addItem(left.toArray(new ItemStack[0])).values()
                .forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        Text.send(player, "&7판매함에 남은 아이템 " + left.stream().mapToInt(ItemStack::getAmount).sum() + "개를 돌려받았습니다.");
    }

    /** 서버 종료 때 열린 판매함의 아이템을 모두 돌려준다. */
    public static void closeAll() {
        for (SellMenu menu : new ArrayList<>(OPEN)) {
            menu.returnItems();
            menu.player.closeInventory();
        }
    }

    static ItemStack button(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Text.c(name));
        if (lore.length > 0) {
            meta.lore(Text.lines(Arrays.asList(lore)));
        }
        meta.addItemFlags(ItemFlag.values());
        item.setItemMeta(meta);
        return item;
    }
}
