package io.github.ybaf100.weapon.command;

import io.github.ybaf100.weapon.WeaponPlugin;
import io.github.ybaf100.weapon.util.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /무기 */
public final class WeaponCommand implements CommandExecutor {
    /** 맞은 뒤 이 시간 안에는 메뉴를 열 수 없다 (메뉴를 연 채로 맞는 것을 막기 위해). */
    public static final long COMBAT_LOCK_MS = 10_000;

    private final WeaponPlugin plugin;

    public WeaponCommand(WeaponPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Text.send(sender, "&c플레이어만 쓸 수 있습니다.");
            return true;
        }
        Long lastHit = plugin.status().lastHit.get(player.getUniqueId());
        if (lastHit != null) {
            long left = COMBAT_LOCK_MS - (System.currentTimeMillis() - lastHit);
            if (left > 0) {
                Text.send(player, "&c전투 중에는 메뉴를 열 수 없습니다. &7(" + ((left + 999) / 1000) + "초 남음)");
                return true;
            }
        }
        plugin.ui().openMain(player);
        return true;
    }
}
