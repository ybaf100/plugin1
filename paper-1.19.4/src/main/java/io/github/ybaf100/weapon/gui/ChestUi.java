package io.github.ybaf100.weapon.gui;

import io.github.ybaf100.weapon.WeaponPlugin;
import io.github.ybaf100.weapon.WeaponUi;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/** 1.19.4: 상자 GUI. */
public final class ChestUi implements WeaponUi {
    private final WeaponPlugin plugin;

    public ChestUi(WeaponPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void enable() {
        plugin.getServer().getPluginManager().registerEvents(new MenuListener(), plugin);
    }

    @Override
    public void disable() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof Menu) {
                player.closeInventory();
            }
        }
    }

    @Override
    public void openMain(Player player) {
        new MainMenu(plugin, player).open();
    }
}
