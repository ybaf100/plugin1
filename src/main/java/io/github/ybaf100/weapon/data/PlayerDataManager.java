package io.github.ybaf100.weapon.data;

import io.github.ybaf100.weapon.model.ShardType;
import io.github.ybaf100.weapon.model.Ticket;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/** 플레이어 데이터를 plugins/WeaponSystem/players/<UUID>.yml 에 저장한다. */
public final class PlayerDataManager {
    private final JavaPlugin plugin;
    private final File folder;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();

    public PlayerDataManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "players");
        if (!folder.exists() && !folder.mkdirs()) {
            plugin.getLogger().warning("Could not create " + folder);
        }
    }

    /** 캐시에 있으면 그대로, 없으면 파일에서 읽는다 (오프라인 플레이어도 가능). */
    public PlayerData get(UUID uuid) {
        return cache.computeIfAbsent(uuid, this::load);
    }

    public void unload(UUID uuid) {
        PlayerData data = cache.remove(uuid);
        if (data != null) {
            save(data);
        }
    }

    public void saveAll() {
        for (PlayerData data : cache.values()) {
            if (data.isDirty()) {
                save(data);
            }
        }
    }

    private PlayerData load(UUID uuid) {
        PlayerData data = new PlayerData(uuid);
        File file = file(uuid);
        if (!file.exists()) {
            return data;
        }
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        for (ShardType type : ShardType.values()) {
            data.setShards(type, yml.getInt("shards." + type.name(), 0));
        }
        data.setMileage(yml.getLong("mileage", 0));
        for (Ticket ticket : Ticket.values()) {
            data.setTickets(ticket, yml.getInt("tickets." + ticket.name(), 0));
        }
        ConfigurationSection cooldowns = yml.getConfigurationSection("cooldowns");
        if (cooldowns != null) {
            for (String key : cooldowns.getKeys(false)) {
                data.setCooldown(key, cooldowns.getLong(key));
            }
        }
        data.markClean();
        return data;
    }

    private void save(PlayerData data) {
        YamlConfiguration yml = new YamlConfiguration();
        for (Map.Entry<ShardType, Integer> entry : data.shardMap().entrySet()) {
            yml.set("shards." + entry.getKey().name(), entry.getValue());
        }
        yml.set("mileage", data.mileage());
        for (Map.Entry<Ticket, Integer> entry : data.ticketMap().entrySet()) {
            yml.set("tickets." + entry.getKey().name(), entry.getValue());
        }
        long now = System.currentTimeMillis();
        for (Map.Entry<String, Long> entry : data.cooldownMap().entrySet()) {
            if (entry.getValue() > now) {
                yml.set("cooldowns." + entry.getKey(), entry.getValue());
            }
        }
        try {
            yml.save(file(data.uuid()));
            data.markClean();
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Could not save player data " + data.uuid(), ex);
        }
    }

    private File file(UUID uuid) {
        return new File(folder, uuid + ".yml");
    }
}
