package io.github.ybaf100.weapon.gui;

import io.github.ybaf100.weapon.WeaponPlugin;
import io.github.ybaf100.weapon.data.PlayerData;
import io.github.ybaf100.weapon.item.Items;
import io.github.ybaf100.weapon.model.GearType;
import io.github.ybaf100.weapon.model.Grade;
import io.github.ybaf100.weapon.service.WeaponActions;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.Player;

/** D급 장비 조합: 순수한 다이아 장비 1개 + D급 파편 10개. */
public final class GearCraftMenu extends Menu {
    public GearCraftMenu(WeaponPlugin plugin, Player player) {
        super(plugin, player, 3, "&c장비 조합");
    }

    @Override
    protected void draw() {
        PlayerData data = plugin.data().get(player.getUniqueId());
        int slot = 10;
        for (GearType type : GearType.values()) {
            boolean hasItem = WeaponActions.findPlain(player, type) >= 0;
            List<String> lore = new ArrayList<>();
            lore.add(" ");
            lore.add("&f재료");
            lore.add((hasItem ? "&a✔ " : "&c✘ ") + "&b다이아몬드 " + type.koreanName() + " &f1개");
            lore.addAll(MenuUtil.costLines(WeaponActions.GEAR_CRAFT_COST, data));
            lore.add(" ");
            lore.add("&8이름이 없고 시스템 장비가 아닌 다이아 장비만 재료로 쓰입니다.");
            lore.add("&a클릭하면 조합합니다.");
            set(slot++, Items.button(type.material(), 1, Grade.D.display() + "급 " + type.koreanName(), lore),
                    e -> craft(type));
        }
        backButton(18, () -> new CraftHubMenu(plugin, player).open());
    }



    private void craft(GearType type) {
        MenuUtil.show(player, plugin.actions().craftGear(player, type, 1));
        redraw();
    }
}
