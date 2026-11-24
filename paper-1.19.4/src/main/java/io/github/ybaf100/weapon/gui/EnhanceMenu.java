package io.github.ybaf100.weapon.gui;

import io.github.ybaf100.weapon.WeaponPlugin;
import io.github.ybaf100.weapon.data.PlayerData;
import io.github.ybaf100.weapon.item.Items;
import io.github.ybaf100.weapon.logic.Enhancement;
import io.github.ybaf100.weapon.model.GearData;
import io.github.ybaf100.weapon.util.Fmt;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** 손에 든 장비 강화. */
public final class EnhanceMenu extends Menu {
    private UUID gearId;

    public EnhanceMenu(WeaponPlugin plugin, Player player) {
        super(plugin, player, 3, "&a장비 강화");
    }

    @Override
    public void open() {
        GearData gear = MenuUtil.heldGear(player);
        if (gear == null) {
            MenuUtil.error(player, "강화할 시스템 장비를 손에 들고 열어 주세요.");
            return;
        }
        gearId = gear.id();
        super.open();
    }

    @Override
    protected void draw() {
        GearData gear = MenuUtil.heldGear(player);
        if (gear == null || !gear.id().equals(gearId)) {
            return;
        }
        PlayerData data = plugin.data().get(player.getUniqueId());
        set(13, player.getInventory().getItemInMainHand().clone());

        int level = gear.enhance();
        List<String> info = new ArrayList<>();
        info.add(" ");
        if (Enhancement.isMax(level)) {
            info.add("&6최대 강화 단계입니다.");
        } else {
            double success = Enhancement.successChance(level);
            double fail = 1 - success;
            info.add("&f" + level + "강 → " + (level + 1) + "강");
            info.add("&7효과 배율 " + Fmt.num(Enhancement.multiplier(level) * 100) + "% → &a"
                    + Fmt.num(Enhancement.multiplier(level + 1) * 100) + "%");
            info.add(" ");
            info.add("&a성공 " + Fmt.pct(success));
            if (fail > 0) {
                info.add("&e유지 " + Fmt.pct(fail * Enhancement.KEEP_ON_FAIL));
                if (level > 1) {
                    double each = fail * (1 - Enhancement.KEEP_ON_FAIL) / (level - 1);
                    info.add("&c하락 1~" + (level - 1) + "강 각 " + Fmt.pct(each));
                }
            }
        }
        set(11, Items.button(Material.BOOK, 1, "&f강화 정보", info));

        if (!Enhancement.isMax(level)) {
            List<String> lore = new ArrayList<>();
            lore.add(" ");
            lore.add("&f재료");
            lore.addAll(MenuUtil.costLines(Enhancement.cost(level), data));
            lore.add(" ");
            lore.add("&a클릭하면 강화합니다.");
            set(15, Items.button(Material.ANVIL, 1, "&a&l강화하기", lore), e -> enhance());
        }
        backButton(18, () -> new MainMenu(plugin, player).open());
    }

    private void enhance() {
        if (MenuUtil.requireSameGear(player, gearId) == null) {
            return;
        }
        MenuUtil.show(player, plugin.actions().enhance(player, gearId));
        redraw();
    }
}
