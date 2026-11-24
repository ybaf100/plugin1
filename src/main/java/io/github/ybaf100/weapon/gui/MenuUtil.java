package io.github.ybaf100.weapon.gui;

import io.github.ybaf100.weapon.data.PlayerData;
import io.github.ybaf100.weapon.item.GearItems;
import io.github.ybaf100.weapon.model.GearData;
import io.github.ybaf100.weapon.model.ShardType;
import io.github.ybaf100.weapon.util.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

final class MenuUtil {

    private MenuUtil() {
    }

    /** 재료 목록을 보유량과 함께 표시한다. */
    static List<String> costLines(Map<ShardType, Integer> cost, PlayerData data) {
        List<String> lines = new ArrayList<>();
        for (Map.Entry<ShardType, Integer> entry : cost.entrySet()) {
            int have = data.shards(entry.getKey());
            String mark = have >= entry.getValue() ? "&a✔ " : "&c✘ ";
            lines.add(mark + entry.getKey().displayName() + " &f" + entry.getValue() + "개 &8(보유 " + have + ")");
        }
        return lines;
    }

    /** 손에 든 시스템 장비. 없으면 null. */
    static GearData heldGear(Player player) {
        return GearItems.read(player.getInventory().getItemInMainHand());
    }

    /** 메뉴를 연 뒤 손에 든 장비가 바뀌지 않았는지 확인한다. */
    static GearData requireSameGear(Player player, UUID id) {
        GearData gear = heldGear(player);
        if (gear == null || !gear.id().equals(id)) {
            Text.send(player, "&c메뉴를 연 뒤 손에 든 장비가 바뀌었습니다. 다시 열어 주세요.");
            player.closeInventory();
            return null;
        }
        return gear;
    }

    static void saveHeld(Player player, GearData gear) {
        ItemStack item = player.getInventory().getItemInMainHand();
        GearItems.write(item, gear);
        player.getInventory().setItemInMainHand(item);
    }

    static void click(Player player) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.5f);
    }

    static void error(Player player, String message) {
        Text.send(player, "&c" + message);
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1f);
    }
}
