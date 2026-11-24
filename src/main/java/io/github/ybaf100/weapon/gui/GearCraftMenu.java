package io.github.ybaf100.weapon.gui;

import io.github.ybaf100.weapon.WeaponPlugin;
import io.github.ybaf100.weapon.data.PlayerData;
import io.github.ybaf100.weapon.item.GearItems;
import io.github.ybaf100.weapon.item.Items;
import io.github.ybaf100.weapon.model.GearData;
import io.github.ybaf100.weapon.model.GearType;
import io.github.ybaf100.weapon.model.Grade;
import io.github.ybaf100.weapon.model.ShardType;
import io.github.ybaf100.weapon.util.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/** D급 장비 조합: 순수한 다이아 장비 1개 + D급 파편 10개. */
public final class GearCraftMenu extends Menu {
    private static final Map<ShardType, Integer> COST = Map.of(ShardType.D, 10);

    public GearCraftMenu(WeaponPlugin plugin, Player player) {
        super(plugin, player, 3, "&c장비 조합");
    }

    @Override
    protected void draw() {
        PlayerData data = plugin.data().get(player.getUniqueId());
        int slot = 10;
        for (GearType type : GearType.values()) {
            boolean hasItem = findPlain(type) >= 0;
            List<String> lore = new ArrayList<>();
            lore.add(" ");
            lore.add("&f재료");
            lore.add((hasItem ? "&a✔ " : "&c✘ ") + "&b다이아몬드 " + type.koreanName() + " &f1개");
            lore.addAll(MenuUtil.costLines(COST, data));
            lore.add(" ");
            lore.add("&8이름이 없고 시스템 장비가 아닌 다이아 장비만 재료로 쓰입니다.");
            lore.add("&a클릭하면 조합합니다.");
            set(slot++, Items.button(type.material(), 1, Grade.D.display() + "급 " + type.koreanName(), lore),
                    e -> craft(type));
        }
        backButton(18, () -> new CraftHubMenu(plugin, player).open());
    }

    private int findPlain(GearType type) {
        PlayerInventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getStorageContents().length; i++) {
            if (GearItems.isPlain(inventory.getItem(i), type.material())) {
                return i;
            }
        }
        return -1;
    }

    private void craft(GearType type) {
        PlayerData data = plugin.data().get(player.getUniqueId());
        int index = findPlain(type);
        if (index < 0) {
            MenuUtil.error(player, "재료로 쓸 다이아 " + type.koreanName() + "이(가) 인벤토리에 없습니다.");
            return;
        }
        if (!data.takeShards(COST)) {
            MenuUtil.error(player, "D급 무기 파편이 부족합니다.");
            return;
        }
        ItemStack material = player.getInventory().getItem(index);
        material.setAmount(material.getAmount() - 1);
        player.getInventory().setItem(index, material.getAmount() <= 0 ? null : material);

        ItemStack result = GearItems.create(GearData.create(type));
        player.getInventory().addItem(result).values()
                .forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
        player.playSound(player.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 1f, 1f);
        Text.send(player, "&a" + GearItems.name(GearData.create(type)) + "&a을(를) 조합했습니다.");
        redraw();
    }
}
