package io.github.ybaf100.weapon.gui;

import io.github.ybaf100.weapon.WeaponPlugin;
import io.github.ybaf100.weapon.item.Items;
import io.github.ybaf100.weapon.sell.SellMenu;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** /무기 메인 메뉴. */
public final class MainMenu extends Menu {

    public MainMenu(WeaponPlugin plugin, Player player) {
        super(plugin, player, 3, "&a무기 메뉴");
    }

    @Override
    protected void draw() {
        set(10, Items.button(Material.ENDER_CHEST, "&6&l뽑기", " ", "&6여러 &c&l무기 파편&6들을 뽑습니다."),
                e -> new GachaMenu(plugin, player).open());
        set(11, Items.button(Material.ANVIL, "&a&l강화", " ", "&a손에 든 장비를 강화합니다."),
                e -> new EnhanceMenu(plugin, player).open());
        set(12, Items.glow(Items.button(Material.DIAMOND_SWORD, "&c&l진화", " ", "&c손에 든 장비의 등급을 올립니다.")),
                e -> new EvolveMenu(plugin, player).open());
        set(14, Items.button(Material.CRAFTING_TABLE, "&b&l무기 조합", " ", "&b무기 파편으로 장비와 아이템을 만듭니다."),
                e -> new CraftHubMenu(plugin, player).open());
        set(15, Items.button(Material.GOLD_INGOT, "&d&l무기 고유능력", " ", "&d손에 든 장비의 고유능력을 강화하거나 바꿉니다."),
                e -> new AbilityMenu(plugin, player).open());
        set(16, Items.button(Material.CHEST, "&e&l인벤토리", " ", "&e보유한 무기 파편과 아이템을 확인합니다."),
                e -> new StorageMenu(plugin, player).open());
        set(22, Items.button(Material.EMERALD, "&2&l판매", " ", "&2농작물과 광물을 팝니다.", "&7/판매 전체 로 빠르게 팔 수도 있습니다."),
                e -> new SellMenu(plugin, player).open());
    }
}
