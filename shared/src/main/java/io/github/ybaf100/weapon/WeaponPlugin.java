package io.github.ybaf100.weapon;

import io.github.ybaf100.weapon.combat.ActiveListener;
import io.github.ybaf100.weapon.combat.CombatListener;
import io.github.ybaf100.weapon.combat.PurifyListener;
import io.github.ybaf100.weapon.combat.StatTask;
import io.github.ybaf100.weapon.combat.StatusEffects;
import io.github.ybaf100.weapon.command.AdminCommand;
import io.github.ybaf100.weapon.command.SellCommand;
import io.github.ybaf100.weapon.command.WeaponCommand;
import io.github.ybaf100.weapon.compat.Compat;
import io.github.ybaf100.weapon.data.PlayerDataManager;
import io.github.ybaf100.weapon.fx.GachaFlyIn;
import io.github.ybaf100.weapon.item.GearItems;
import io.github.ybaf100.weapon.item.ProtectionListener;
import io.github.ybaf100.weapon.sell.PriceTable;
import io.github.ybaf100.weapon.sell.SellListener;
import io.github.ybaf100.weapon.sell.SellMenu;
import io.github.ybaf100.weapon.sell.SellService;
import io.github.ybaf100.weapon.service.WeaponActions;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class WeaponPlugin extends JavaPlugin implements Listener {
    private static final long AUTOSAVE_TICKS = 20L * 60 * 5;

    private PlayerDataManager data;
    private StatusEffects status;
    private Economy economy;
    private WeaponActions actions;
    private WeaponUi ui;
    private PriceTable prices;
    private SellService sell;
    private GachaFlyIn flyIn;

    @Override
    public void onEnable() {
        GearItems.init(this);
        data = new PlayerDataManager(this);
        status = new StatusEffects();
        actions = new WeaponActions(this);
        prices = new PriceTable(this);
        getLogger().info("판매 가격 " + prices.load() + "개를 읽었습니다.");
        sell = new SellService(this);
        flyIn = new GachaFlyIn(this);
        hookEconomy();

        var pm = getServer().getPluginManager();
        pm.registerEvents(this, this);
        pm.registerEvents(new ProtectionListener(), this);
        pm.registerEvents(new CombatListener(this), this);
        pm.registerEvents(new ActiveListener(this), this);
        pm.registerEvents(new PurifyListener(), this);
        pm.registerEvents(new SellListener(this), this);
        pm.registerEvents(flyIn, this);

        Objects.requireNonNull(getCommand("무기")).setExecutor(new WeaponCommand(this));
        AdminCommand admin = new AdminCommand(this);
        Objects.requireNonNull(getCommand("무기관리")).setExecutor(admin);
        Objects.requireNonNull(getCommand("무기관리")).setTabCompleter(admin);
        SellCommand sellCommand = new SellCommand(this);
        Objects.requireNonNull(getCommand("판매")).setExecutor(sellCommand);
        Objects.requireNonNull(getCommand("판매")).setTabCompleter(sellCommand);

        ui = Compat.createUi(this);
        ui.enable();

        new StatTask(this).start();
        Bukkit.getScheduler().runTaskTimer(this, data::saveAll, AUTOSAVE_TICKS, AUTOSAVE_TICKS);
        for (Player player : Bukkit.getOnlinePlayers()) {
            data.get(player.getUniqueId());
        }
    }

    @Override
    public void onDisable() {
        SellMenu.closeAll();
        if (flyIn != null) {
            flyIn.stopAll();
        }
        if (ui != null) {
            ui.disable();
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            StatTask.clear(player);
        }
        if (data != null) {
            data.saveAll();
        }
    }

    private void hookEconomy() {
        RegisteredServiceProvider<Economy> provider = getServer().getServicesManager().getRegistration(Economy.class);
        if (provider == null) {
            getLogger().warning("Vault 경제 플러그인을 찾지 못했습니다. 돈으로 하는 뽑기는 쓸 수 없습니다.");
            return;
        }
        economy = provider.getProvider();
        getLogger().info("경제 연동: " + economy.getName());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        data.get(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        StatTask.clear(player);
        status.forget(player.getUniqueId());
        data.unload(player.getUniqueId());
    }

    public PlayerDataManager data() {
        return data;
    }

    public StatusEffects status() {
        return status;
    }

    public WeaponActions actions() {
        return actions;
    }

    public WeaponUi ui() {
        return ui;
    }

    public PriceTable prices() {
        return prices;
    }

    public SellService sell() {
        return sell;
    }

    public GachaFlyIn flyIn() {
        return flyIn;
    }

    /** Vault 경제. 없으면 null. */
    public Economy economy() {
        if (economy == null) {
            hookEconomy();
        }
        return economy;
    }

    public Random random() {
        return ThreadLocalRandom.current();
    }
}
