package io.github.ybaf100.weapon.gui;

import io.github.ybaf100.weapon.WeaponPlugin;
import io.github.ybaf100.weapon.data.PlayerData;
import io.github.ybaf100.weapon.item.Items;
import io.github.ybaf100.weapon.model.Ticket;
import io.github.ybaf100.weapon.util.Text;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

/** 보조 아이템 조합. 왼쪽 클릭 1개, 쉬프트 클릭 10개. */
public final class TicketCraftMenu extends Menu {
    private final Ticket.Group group;

    public TicketCraftMenu(WeaponPlugin plugin, Player player, Ticket.Group group) {
        super(plugin, player, 3, group == Ticket.Group.ABILITY ? "&d고유능력 아이템 조합" : "&a진화 아이템 조합");
        this.group = group;
    }

    @Override
    protected void draw() {
        PlayerData data = plugin.data().get(player.getUniqueId());
        int slot = group == Ticket.Group.ABILITY ? 10 : 11;
        for (Ticket ticket : Ticket.values()) {
            if (ticket.group() != group) {
                continue;
            }
            List<String> lore = new ArrayList<>();
            lore.add(" ");
            lore.add("&7" + ticket.description());
            lore.add(" ");
            lore.add("&f재료");
            lore.addAll(MenuUtil.costLines(ticket.cost(), data));
            lore.add(" ");
            lore.add("&e보유 " + data.tickets(ticket) + "개");
            lore.add("&a클릭: 1개 조합 &7/ &a쉬프트 클릭: 10개 조합");
            set(slot, Items.button(ticket.icon(), 1, "&f" + ticket.displayName(), lore),
                    e -> craft(ticket, e.getClick() == ClickType.SHIFT_LEFT || e.getClick() == ClickType.SHIFT_RIGHT ? 10 : 1));
            slot += group == Ticket.Group.ABILITY ? 1 : 2;
        }
        backButton(18, () -> new CraftHubMenu(plugin, player).open());
    }

    private void craft(Ticket ticket, int amount) {
        PlayerData data = plugin.data().get(player.getUniqueId());
        int made = 0;
        for (int i = 0; i < amount; i++) {
            if (!data.takeShards(ticket.cost())) {
                break;
            }
            data.addTickets(ticket, 1);
            made++;
        }
        if (made == 0) {
            MenuUtil.error(player, "재료가 부족합니다.");
            return;
        }
        player.playSound(player.getLocation(), Sound.BLOCK_SMITHING_TABLE_USE, 1f, 1.2f);
        Text.send(player, "&a" + ticket.displayName() + " " + made + "개를 조합했습니다.");
        redraw();
    }
}
