package io.github.ybaf100.weapon.command;

import io.github.ybaf100.weapon.WeaponPlugin;
import io.github.ybaf100.weapon.sell.PriceListMenu;
import io.github.ybaf100.weapon.sell.SellMenu;
import io.github.ybaf100.weapon.util.Text;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

/**
 * /판매         판매함 열기
 * /판매 전체    빠른 판매 (인벤토리의 팔 수 있는 아이템 전부)
 * /판매 손      손에 든 아이템만 판매
 * /판매 가격    가격표
 */
public final class SellCommand implements TabExecutor {
    private static final List<String> SUBS = List.of("전체", "손", "가격");

    private final WeaponPlugin plugin;

    public SellCommand(WeaponPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Text.send(sender, "&c플레이어만 쓸 수 있습니다.");
            return true;
        }
        String sub = args.length == 0 ? "" : args[0];
        switch (sub) {
            case "" -> new SellMenu(plugin, player).open();
            case "전체", "all" -> plugin.sell().quickSell(player);
            case "손", "hand" -> plugin.sell().sellHand(player);
            case "가격", "prices" -> new PriceListMenu(plugin, player, 0).open();
            default -> {
                Text.send(player, "&e/" + label + " &7판매함 열기");
                Text.send(player, "&e/" + label + " 전체 &7인벤토리의 팔 수 있는 아이템 전부 판매");
                Text.send(player, "&e/" + label + " 손 &7손에 든 아이템만 판매");
                Text.send(player, "&e/" + label + " 가격 &7가격표");
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<>();
        if (args.length == 1) {
            for (String sub : SUBS) {
                if (sub.startsWith(args[0])) {
                    result.add(sub);
                }
            }
        }
        return result;
    }
}
