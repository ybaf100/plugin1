package io.github.ybaf100.weapon.command;

import io.github.ybaf100.weapon.WeaponPlugin;
import io.github.ybaf100.weapon.data.PlayerData;
import io.github.ybaf100.weapon.item.GearItems;
import io.github.ybaf100.weapon.logic.GearRules;
import io.github.ybaf100.weapon.model.GearData;
import io.github.ybaf100.weapon.model.GearType;
import io.github.ybaf100.weapon.model.Grade;
import io.github.ybaf100.weapon.model.ShardType;
import io.github.ybaf100.weapon.model.Ticket;
import io.github.ybaf100.weapon.util.Fmt;
import io.github.ybaf100.weapon.util.Text;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

/**
 * /무기관리 (권한 weaponsystem.admin)
 * <pre>
 * 파편 &lt;플레이어&gt; &lt;D|C|B|A|S|SP&gt; &lt;수량&gt;   수량만큼 더한다 (음수면 뺀다)
 * 마일리지 &lt;플레이어&gt; &lt;수량&gt;
 * 아이템 &lt;플레이어&gt; &lt;아이템&gt; &lt;수량&gt;
 * 장비 &lt;플레이어&gt; &lt;종류&gt; [등급] [강화]
 * 확인 &lt;플레이어&gt;
 * 초기화 &lt;플레이어&gt;
 * </pre>
 */
public final class AdminCommand implements TabExecutor {
    private static final List<String> SUBS = List.of("파편", "마일리지", "아이템", "장비", "확인", "초기화");

    private final WeaponPlugin plugin;

    public AdminCommand(WeaponPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            help(sender, label);
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            Text.send(sender, "&c" + args[1] + " 플레이어를 찾을 수 없습니다.");
            return true;
        }
        PlayerData data = plugin.data().get(target.getUniqueId());
        try {
            switch (args[0]) {
                case "파편" -> {
                    ShardType type = ShardType.byLabel(args[2]);
                    int amount = Integer.parseInt(args[3]);
                    if (type == null) {
                        Text.send(sender, "&c파편 종류: D, C, B, A, S, SP");
                        return true;
                    }
                    if (amount >= 0) {
                        long overflow = data.addShards(type, amount);
                        Text.send(sender, "&a" + target.getName() + "에게 " + type.displayName() + " &a" + amount + "개 지급"
                                + (overflow > 0 ? " &d(초과분 +" + overflow + "M)" : ""));
                    } else {
                        data.setShards(type, data.shards(type) + amount);
                        Text.send(sender, "&a" + target.getName() + "의 " + type.displayName() + " &a" + (-amount) + "개 회수");
                    }
                }
                case "마일리지" -> {
                    long amount = Long.parseLong(args[2]);
                    data.addMileage(amount);
                    Text.send(sender, "&a" + target.getName() + " 마일리지 " + Fmt.money(data.mileage()) + "M");
                }
                case "아이템" -> {
                    Ticket ticket = Ticket.valueOf(args[2].toUpperCase(Locale.ROOT));
                    int amount = Integer.parseInt(args[3]);
                    data.addTickets(ticket, amount);
                    Text.send(sender, "&a" + target.getName() + "에게 " + ticket.displayName() + " " + amount + "개 지급");
                }
                case "장비" -> {
                    Player online = target.getPlayer();
                    if (online == null) {
                        Text.send(sender, "&c장비는 접속 중인 플레이어에게만 줄 수 있습니다.");
                        return true;
                    }
                    GearType type = GearType.byName(args[2]);
                    Grade grade = args.length > 3 ? Grade.byLabel(args[3]) : Grade.D;
                    int enhance = args.length > 4 ? Integer.parseInt(args[4]) : 1;
                    if (type == null || grade == null) {
                        Text.send(sender, "&c종류: " + Arrays.stream(GearType.values()).map(GearType::koreanName)
                                .collect(Collectors.joining(", ")) + " / 등급: D, C, B, B+, A-, A, A+, S-, S, S+, SP");
                        return true;
                    }
                    GearData gear = GearData.create(type);
                    gear.setGrade(grade);
                    gear.setEnhance(enhance);
                    GearRules.normalize(gear, plugin.random());
                    online.getInventory().addItem(GearItems.create(gear)).values()
                            .forEach(left -> online.getWorld().dropItemNaturally(online.getLocation(), left));
                    Text.send(sender, "&a" + online.getName() + "에게 " + GearItems.name(gear) + " &a(" + gear.enhance() + "강) 지급");
                }
                case "확인" -> {
                    StringBuilder sb = new StringBuilder("&f" + target.getName() + " &7파편: ");
                    for (ShardType type : ShardType.values()) {
                        sb.append(type.color()).append(type.label()).append(" &f").append(data.shards(type)).append("  ");
                    }
                    sb.append("&d마일리지 ").append(Fmt.money(data.mileage())).append("M");
                    Text.send(sender, sb.toString());
                    StringBuilder tickets = new StringBuilder("&7아이템: ");
                    for (Ticket ticket : Ticket.values()) {
                        if (data.tickets(ticket) > 0) {
                            tickets.append("&f").append(ticket.displayName()).append(" ").append(data.tickets(ticket)).append("  ");
                        }
                    }
                    Text.send(sender, tickets.toString());
                }
                case "초기화" -> {
                    data.reset();
                    Text.send(sender, "&a" + target.getName() + "의 파편, 마일리지, 아이템을 초기화했습니다.");
                }
                default -> help(sender, label);
            }
        } catch (ArrayIndexOutOfBoundsException | IllegalArgumentException ex) {
            help(sender, label);
        }
        return true;
    }

    private void help(CommandSender sender, String label) {
        Text.send(sender, "&e/" + label + " 파편 <플레이어> <D|C|B|A|S|SP> <수량>");
        Text.send(sender, "&e/" + label + " 마일리지 <플레이어> <수량>");
        Text.send(sender, "&e/" + label + " 아이템 <플레이어> <아이템> <수량>");
        Text.send(sender, "&e/" + label + " 장비 <플레이어> <종류> [등급] [강화]");
        Text.send(sender, "&e/" + label + " 확인 <플레이어>");
        Text.send(sender, "&e/" + label + " 초기화 <플레이어>");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            options.addAll(SUBS);
        } else if (args.length == 2) {
            Bukkit.getOnlinePlayers().forEach(p -> options.add(p.getName()));
        } else if (args.length == 3) {
            switch (args[0]) {
                case "파편" -> Arrays.stream(ShardType.values()).forEach(t -> options.add(t.label()));
                case "아이템" -> Arrays.stream(Ticket.values()).forEach(t -> options.add(t.name()));
                case "장비" -> Arrays.stream(GearType.values()).forEach(t -> options.add(t.koreanName()));
                default -> {
                }
            }
        } else if (args.length == 4 && args[0].equals("장비")) {
            Arrays.stream(Grade.values()).forEach(g -> options.add(g.label()));
        }
        String last = args[args.length - 1];
        options.removeIf(option -> !option.toLowerCase(Locale.ROOT).startsWith(last.toLowerCase(Locale.ROOT)));
        return options;
    }
}
