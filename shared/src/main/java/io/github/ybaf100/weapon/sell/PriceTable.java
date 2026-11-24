package io.github.ybaf100.weapon.sell;

import io.github.ybaf100.weapon.item.GearItems;
import java.io.File;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

/** plugins/WeaponSystem/prices.yml 의 판매 가격. */
public final class PriceTable {
    /** 가격표 화면에 보여 줄 분류 이름. */
    private static final Map<String, String> CATEGORY_NAMES = Map.of(
            "crops", "농작물",
            "ores", "광물",
            "ore_blocks", "광물 블록");

    private final JavaPlugin plugin;
    private final Map<Material, Double> prices = new EnumMap<>(Material.class);
    private final Map<String, List<Material>> categories = new LinkedHashMap<>();

    public PriceTable(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /** @return 읽은 가격 수 */
    public int load() {
        File file = new File(plugin.getDataFolder(), "prices.yml");
        if (!file.exists()) {
            plugin.saveResource("prices.yml", false);
        }
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        prices.clear();
        categories.clear();
        for (String category : yml.getKeys(false)) {
            ConfigurationSection section = yml.getConfigurationSection(category);
            if (section == null) {
                continue;
            }
            List<Material> list = new java.util.ArrayList<>();
            for (String key : section.getKeys(false)) {
                Material material = Material.matchMaterial(key);
                double price = section.getDouble(key);
                if (material == null || !material.isItem()) {
                    plugin.getLogger().warning("prices.yml: 알 수 없는 아이템 '" + key + "' (" + category + ")");
                    continue;
                }
                if (price > 0) {
                    prices.put(material, price);
                    list.add(material);
                }
            }
            categories.put(category, list);
        }
        return prices.size();
    }

    /** 개당 가격. 팔 수 없으면 0. */
    public double unitPrice(ItemStack item) {
        if (item == null || item.getType().isAir() || !isPlain(item)) {
            return 0;
        }
        return prices.getOrDefault(item.getType(), 0.0);
    }

    public double unitPrice(Material material) {
        return prices.getOrDefault(material, 0.0);
    }

    public Map<String, List<Material>> categories() {
        return Collections.unmodifiableMap(categories);
    }

    public static String categoryName(String key) {
        return CATEGORY_NAMES.getOrDefault(key, key);
    }

    /** 이름, 로어, 인챈트가 없고 시스템 장비가 아닌 아이템만 판다. */
    private static boolean isPlain(ItemStack item) {
        if (!item.hasItemMeta()) {
            return true;
        }
        if (GearItems.isGear(item)) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return !meta.hasDisplayName() && !meta.hasLore() && !meta.hasEnchants();
    }
}
