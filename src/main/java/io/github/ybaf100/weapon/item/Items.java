package io.github.ybaf100.weapon.item;

import io.github.ybaf100.weapon.util.Text;
import java.util.Arrays;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** GUI 버튼용 아이템 도우미. */
public final class Items {

    private Items() {
    }

    public static ItemStack button(Material material, String name, String... lore) {
        return button(material, 1, name, Arrays.asList(lore));
    }

    public static ItemStack button(Material material, int amount, String name, List<String> lore) {
        ItemStack item = new ItemStack(material, Math.max(1, Math.min(64, amount)));
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Text.c(name));
        if (!lore.isEmpty()) {
            meta.lore(Text.lines(lore));
        }
        meta.addItemFlags(ItemFlag.values());
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack filler() {
        return button(Material.BLACK_STAINED_GLASS_PANE, " ");
    }

    /** 반짝이는 효과를 준다 (선택된 버튼 표시용). */
    public static ItemStack glow(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        meta.addEnchant(Enchantment.LUCK, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }
}
