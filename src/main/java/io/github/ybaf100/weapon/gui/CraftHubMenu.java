package io.github.ybaf100.weapon.gui;

import io.github.ybaf100.weapon.WeaponPlugin;
import io.github.ybaf100.weapon.item.Items;
import io.github.ybaf100.weapon.model.Ticket;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** 조합 메뉴 입구. */
public final class CraftHubMenu extends Menu {

    public CraftHubMenu(WeaponPlugin plugin, Player player) {
        super(plugin, player, 3, "&b무기 조합");
    }

    @Override
    protected void draw() {
        set(11, Items.button(Material.NETHERITE_SWORD, "&c장비 조합", " ", "&cD급 장비를 조합합니다."),
                e -> new GearCraftMenu(plugin, player).open());
        set(13, Items.button(Material.NETHERITE_SCRAP, "&d고유능력 아이템", " ", "&d고유능력 강화, 변경에 쓰는 아이템을 조합합니다."),
                e -> new TicketCraftMenu(plugin, player, Ticket.Group.ABILITY).open());
        set(15, Items.button(Material.NETHER_STAR, "&a진화 아이템", " ", "&a확정 상승권과 파괴 방지권을 조합합니다."),
                e -> new TicketCraftMenu(plugin, player, Ticket.Group.EVOLUTION).open());
        backButton(18, () -> new MainMenu(plugin, player).open());
    }
}
