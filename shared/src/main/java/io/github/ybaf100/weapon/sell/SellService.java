package io.github.ybaf100.weapon.sell;

import io.github.ybaf100.weapon.WeaponPlugin;
import io.github.ybaf100.weapon.util.Fmt;
import io.github.ybaf100.weapon.util.Text;
import java.util.LinkedHashMap;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/** 아이템을 팔고 Vault로 돈을 준다. */
public final class SellService {

    /** 판매 합계. 팔린 아이템 종류별 개수와 금액. */
    public static final class Sale {
        private final Map<Material, Integer> counts = new LinkedHashMap<>();
        private final Map<Material, Double> money = new LinkedHashMap<>();
        private double total;

        void add(Material material, int amount, double value) {
            counts.merge(material, amount, Integer::sum);
            money.merge(material, value, Double::sum);
            total += value;
        }

        public double total() {
            return total;
        }

        public boolean isEmpty() {
            return counts.isEmpty();
        }

        public int itemCount() {
            return counts.values().stream().mapToInt(Integer::intValue).sum();
        }
    }

    private final WeaponPlugin plugin;

    public SellService(WeaponPlugin plugin) {
        this.plugin = plugin;
    }

    public PriceTable prices() {
        return plugin.prices();
    }

    /** 팔지 않고 금액만 계산한다. */
    public Sale preview(Inventory inventory, int fromSlot, int toSlot) {
        Sale sale = new Sale();
        for (int i = fromSlot; i < toSlot; i++) {
            ItemStack item = inventory.getItem(i);
            double unit = prices().unitPrice(item);
            if (unit > 0) {
                sale.add(item.getType(), item.getAmount(), unit * item.getAmount());
            }
        }
        return sale;
    }

    /**
     * inventory의 [fromSlot, toSlot) 칸에서 팔 수 있는 아이템을 모두 팔고 칸을 비운다.
     * 팔 수 없는 아이템은 그대로 둔다.
     */
    public Sale sell(Player player, Inventory inventory, int fromSlot, int toSlot) {
        return sell(player, inventory, fromSlot, toSlot, false);
    }

    /**
     * @param explainUnsold true면 팔지 못한 아이템과 그 이유를 알려 준다 (판매함, 손에 든 아이템).
     */
    public Sale sell(Player player, Inventory inventory, int fromSlot, int toSlot, boolean explainUnsold) {
        Economy economy = plugin.economy();
        if (economy == null) {
            Text.send(player, "&c경제 플러그인이 없어 판매할 수 없습니다.");
            return new Sale();
        }
        Sale sale = preview(inventory, fromSlot, toSlot);
        if (sale.isEmpty()) {
            Text.send(player, "&c팔 수 있는 아이템이 없습니다. &7(/판매 가격 으로 가격표를 볼 수 있습니다)");
            if (explainUnsold) {
                explainUnsold(player, inventory, fromSlot, toSlot);
            }
            return sale;
        }
        if (!economy.depositPlayer(player, sale.total()).transactionSuccess()) {
            Text.send(player, "&c입금에 실패해서 판매를 취소했습니다.");
            return new Sale();
        }
        for (int i = fromSlot; i < toSlot; i++) {
            if (prices().unitPrice(inventory.getItem(i)) > 0) {
                inventory.setItem(i, null);
            }
        }
        report(player, sale);
        if (explainUnsold) {
            explainUnsold(player, inventory, fromSlot, toSlot);
        }
        return sale;
    }

    /** 칸에 남은(팔지 못한) 아이템을 이유별로 알려 준다. */
    private void explainUnsold(Player player, Inventory inventory, int fromSlot, int toSlot) {
        Map<String, Component> byReason = new LinkedHashMap<>();
        for (int i = fromSlot; i < toSlot; i++) {
            ItemStack item = inventory.getItem(i);
            String reason = prices().unsellableReason(item);
            if (reason == null) {
                continue;
            }
            Component name = item.getItemMeta() != null && item.getItemMeta().hasDisplayName()
                    ? item.getItemMeta().displayName()
                    : Component.translatable(item.getType().translationKey(), NamedTextColor.WHITE);
            Component entry = name.append(Component.text(" ×" + item.getAmount(), NamedTextColor.GRAY));
            byReason.merge(reason, entry, (a, b) -> a.append(Component.text(", ", NamedTextColor.DARK_GRAY)).append(b));
        }
        for (Map.Entry<String, Component> entry : byReason.entrySet()) {
            player.sendMessage(Text.c(Text.PREFIX + "&c판매 불가 &7(" + entry.getKey() + ") ").append(entry.getValue()));
        }
    }

    /** 빠른 판매: 플레이어 인벤토리(핫바 포함, 갑옷과 왼손 제외)의 팔 수 있는 아이템을 모두 판다. */
    public Sale quickSell(Player player) {
        return sell(player, player.getInventory(), 0, 36);
    }

    /** 손에 든 아이템만 판다. */
    public Sale sellHand(Player player) {
        int slot = player.getInventory().getHeldItemSlot();
        return sell(player, player.getInventory(), slot, slot + 1, true);
    }

    private void report(Player player, Sale sale) {
        Component line = Text.c(Text.PREFIX + "&a판매 완료 &7(" + sale.itemCount() + "개) ");
        boolean first = true;
        for (Map.Entry<Material, Integer> entry : sale.counts.entrySet()) {
            if (!first) {
                line = line.append(Component.text(", ", NamedTextColor.DARK_GRAY));
            }
            first = false;
            line = line.append(Component.translatable(entry.getKey().translationKey(), NamedTextColor.WHITE))
                    .append(Component.text(" ×" + entry.getValue(), NamedTextColor.GRAY));
        }
        player.sendMessage(line);
        Text.send(player, "&a+" + Fmt.money(sale.total()) + "원");
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
    }
}
