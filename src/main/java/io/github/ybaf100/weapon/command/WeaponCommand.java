package io.github.ybaf100.weapon.command;

import io.github.ybaf100.weapon.WeaponPlugin;
import io.github.ybaf100.weapon.gui.MainMenu;
import io.github.ybaf100.weapon.util.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /무기 */
public final class WeaponCommand implements CommandExecutor {
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
        new MainMenu(plugin, player).open();
        return true;
    }
}
