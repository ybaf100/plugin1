package io.github.ybaf100.weapon.gui;

import io.github.ybaf100.weapon.WeaponPlugin;
import io.github.ybaf100.weapon.item.Items;
import io.github.ybaf100.weapon.logic.Gacha.Product;
import io.github.ybaf100.weapon.model.ShardType;
import io.github.ybaf100.weapon.service.WeaponActions;
import io.github.ybaf100.weapon.util.Text;
import java.time.Duration;
import net.kyori.adventure.title.Title;
import io.github.ybaf100.weapon.util.Fmt;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/**
 * 뽑기 결과 화면. 결과는 화면을 열기 전에 전부 정해지고 데이터에 바로 반영되므로,
 * 도중에 창을 닫아도 결과를 잃지 않는다.
 */
public final class GachaResultMenu extends Menu {
    private static final int[] SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43};
    private static final int REVEAL_INTERVAL_TICKS = 2;
    private static final int PAGE_PAUSE_TICKS = 40;

    private final Product product;
    private final List<WeaponActions.GachaRoll> results;
    private int revealed;
    private BukkitTask task;

    /** 월드 연출에서 이미 타이틀을 보여 줬으면 false. */
    private final boolean showTitles;

    public GachaResultMenu(WeaponPlugin plugin, Player player, Product product, List<WeaponActions.GachaRoll> results,
                           boolean showTitles) {
        super(plugin, player, 6, "&6뽑기 결과");
        this.product = product;
        this.results = results;
        this.showTitles = showTitles;
    }

    @Override
    public void open() {
        super.open();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, REVEAL_INTERVAL_TICKS, REVEAL_INTERVAL_TICKS);
    }

    private int pause;

    private void tick() {
        if (pause > 0) {
            pause -= REVEAL_INTERVAL_TICKS;
            if (pause <= 0) {
                redraw();
            }
            return;
        }
        if (revealed >= results.size()) {
            task.cancel();
            return;
        }
        WeaponActions.GachaRoll result = results.get(revealed);
        revealed++;
        WeaponActions.playGachaSound(player, result.type());
        if (showTitles && (result.type() == ShardType.S || result.type() == ShardType.SP)) {
            player.showTitle(Title.title(Text.c(result.type().displayName() + "!"),
                    Text.c("&7" + product.displayName() + "&7에서 획득"),
                    Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(1500), Duration.ofMillis(400))));
        }
        if (revealed % SLOTS.length == 0 && revealed < results.size()) {
            pause = PAGE_PAUSE_TICKS;
        }
        redraw();
    }

    @Override
    protected void draw() {
        int pageStart = revealed == 0 ? 0 : ((revealed - 1) / SLOTS.length) * SLOTS.length;
        for (int i = pageStart; i < revealed && i - pageStart < SLOTS.length; i++) {
            WeaponActions.GachaRoll result = results.get(i);
            List<String> lore = new ArrayList<>();
            lore.add(" ");
            if (result.overflowMileage() > 0) {
                lore.add("&c최대 보유량 초과!");
                lore.add("&d" + Fmt.money(result.overflowMileage()) + " 마일리지 지급");
            } else {
                lore.add("&a" + result.type().label() + "급 파편 +1");
            }
            set(SLOTS[i - pageStart], Items.button(result.type().material(), 1, result.type().displayName(), lore));
        }
        set(45, Items.button("PAPER", "&f" + product.displayName(),
                "&7" + revealed + " / " + results.size()));
        backButton(49, () -> new GachaMenu(plugin, player).open());
    }

    @Override
    void onClose() {
        if (task != null) {
            task.cancel();
        }
    }
}
