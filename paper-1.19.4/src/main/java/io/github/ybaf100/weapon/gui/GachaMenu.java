package io.github.ybaf100.weapon.gui;

import io.github.ybaf100.weapon.WeaponPlugin;
import io.github.ybaf100.weapon.data.PlayerData;
import io.github.ybaf100.weapon.item.Items;
import io.github.ybaf100.weapon.logic.Gacha;
import io.github.ybaf100.weapon.logic.Gacha.Product;
import io.github.ybaf100.weapon.model.ShardType;
import io.github.ybaf100.weapon.service.WeaponActions;
import io.github.ybaf100.weapon.util.Fmt;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** 뽑기 상점. */
public final class GachaMenu extends Menu {

    public GachaMenu(WeaponPlugin plugin, Player player) {
        super(plugin, player, 3, "&6무기 뽑기");
    }

    @Override
    protected void draw() {
        PlayerData data = plugin.data().get(player.getUniqueId());
        Economy economy = plugin.economy();
        String money = economy == null ? "&c경제 플러그인 없음" : "&a" + Fmt.money(economy.getBalance(player)) + "원 보유중";

        product(10, Product.COMMON_1);
        product(12, Product.COMMON_11);
        product(14, Product.EPIC_1);
        product(16, Product.EPIC_11);
        product(22, Product.EPIC_28);
        product(24, Product.EPIC_56);

        List<String> mileageLore = new ArrayList<>();
        mileageLore.add(" ");
        mileageLore.add("&d" + Fmt.money(data.mileage()) + " / 5,000");
        mileageLore.add(" ");
        mileageLore.add("&e마일리지 5,000점으로 &dA&f ~ &b&lSP&e 등급 파편 뽑기 2회");
        mileageLore.addAll(probabilityLines(Gacha.Kind.MILEAGE));
        set(18, Items.button("DIAMOND", 1, "&d&l마일리지 뽑기", mileageLore), e -> buy(Product.MILEAGE_2));

        set(26, Items.button(Material.EMERALD, money));
        backButton(0, () -> new MainMenu(plugin, player).open());
    }

    private void product(int slot, Product product) {
        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.add("&7파편 " + product.count() + "개를 뽑습니다.");
        lore.addAll(probabilityLines(product.kind()));
        lore.add("&7마일리지 " + Fmt.money(product.mileageGain()) + "점 적립");
        lore.add(" ");
        lore.add("&a" + Fmt.money(product.price()) + "원");
        set(slot, Items.button(product.icon(), product.iconAmount(), product.displayName(), lore), e -> buy(product));
    }

    static List<String> probabilityLines(Gacha.Kind kind) {
        List<String> lines = new ArrayList<>();
        StringBuilder sb = new StringBuilder("&7확률: ");
        boolean first = true;
        for (Map.Entry<ShardType, Integer> entry : kind.weights().entrySet()) {
            if (!first) {
                sb.append("&8, ");
            }
            first = false;
            sb.append(entry.getKey().color()).append(entry.getKey().label()).append(" &f")
                    .append(Fmt.num(entry.getValue() / 10.0)).append("%");
        }
        lines.add(sb.toString());
        return lines;
    }

    private void buy(Product product) {
        WeaponActions.GachaOutcome result = plugin.actions().buyGacha(player, product);
        MenuUtil.show(player, result.outcome());
        if (result.outcome().ok()) {
            // 메뉴를 닫고 월드 연출(소용돌이)을 보여 준 뒤 결과 창을 연다. 웅크리면 건너뛴다.
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                player.closeInventory();
                plugin.flyIn().play(player, product, result.rolls(),
                        skipped -> new GachaResultMenu(plugin, player, product, result.rolls(), skipped).open());
            });
        }
    }
}
