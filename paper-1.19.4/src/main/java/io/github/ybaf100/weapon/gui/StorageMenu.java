package io.github.ybaf100.weapon.gui;

import io.github.ybaf100.weapon.WeaponPlugin;
import io.github.ybaf100.weapon.data.PlayerData;
import io.github.ybaf100.weapon.item.Items;
import io.github.ybaf100.weapon.model.ShardType;
import io.github.ybaf100.weapon.model.Ticket;
import io.github.ybaf100.weapon.util.Fmt;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** 보유한 파편, 마일리지, 보조 아이템 확인. */
public final class StorageMenu extends Menu {

    public StorageMenu(WeaponPlugin plugin, Player player) {
        super(plugin, player, 6, "&e인벤토리");
    }

    @Override
    protected void draw() {
        PlayerData data = plugin.data().get(player.getUniqueId());
        int slot = 10;
        for (ShardType type : ShardType.values()) {
            int amount = data.shards(type);
            set(slot++, Items.button(type.material(), Math.max(1, Math.min(64, amount)), type.displayName(),
                    java.util.List.of(" ", type.color() + amount + " / " + ShardType.MAX_AMOUNT + " 보유중",
                            "&8초과분은 개당 " + type.overflowMileage() + "M으로 지급")));
        }
        set(16, Items.button(Material.DIAMOND, "&d&l마일리지", " ", "&d" + Fmt.money(data.mileage()) + "M"));

        int[] ticketSlots = {28, 29, 30, 31, 32, 33, 34, 37, 38, 39};
        int i = 0;
        for (Ticket ticket : Ticket.values()) {
            int amount = data.tickets(ticket);
            set(ticketSlots[i++], Items.button(ticket.icon(), Math.max(1, Math.min(64, amount)), "&f" + ticket.displayName(),
                    java.util.List.of(" ", "&7" + ticket.description(), " ", "&e" + amount + "개 보유중")));
        }
        backButton(45, () -> new MainMenu(plugin, player).open());
    }
}
