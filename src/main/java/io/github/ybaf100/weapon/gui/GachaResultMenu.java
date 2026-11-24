package io.github.ybaf100.weapon.gui;

import io.github.ybaf100.weapon.WeaponPlugin;
import io.github.ybaf100.weapon.data.PlayerData;
import io.github.ybaf100.weapon.item.Items;
import io.github.ybaf100.weapon.logic.Gacha.Product;
import io.github.ybaf100.weapon.model.ShardType;
import io.github.ybaf100.weapon.util.Fmt;
import io.github.ybaf100.weapon.util.Text;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
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

    private record Result(ShardType type, long overflowMileage) {
    }

    private final Product product;
    private final List<Result> results = new ArrayList<>();
    private int revealed;
    private BukkitTask task;

    public GachaResultMenu(WeaponPlugin plugin, Player player, Product product) {
        super(plugin, player, 6, "&6뽑기 결과");
        this.product = product;
        PlayerData data = plugin.data().get(player.getUniqueId());
        Map<ShardType, Integer> summary = new EnumMap<>(ShardType.class);
        long overflowTotal = 0;
        for (int i = 0; i < product.count(); i++) {
            ShardType type = product.kind().roll(plugin.random());
            long overflow = data.addShards(type, 1);
            overflowTotal += overflow;
            results.add(new Result(type, overflow));
            summary.merge(type, 1, Integer::sum);
            if (type.ordinal() >= ShardType.A.ordinal()) {
                Text.broadcast("&f" + player.getName() + "&7님이 " + product.kind().label() + "&7 뽑기에서 "
                        + type.displayName() + "&7을 획득했습니다!");
            }
        }
        StringBuilder sb = new StringBuilder("&7결과: ");
        summary.forEach((type, n) -> sb.append(type.color()).append(type.label()).append(" &f×").append(n).append("  "));
        if (overflowTotal > 0) {
            sb.append("&d(초과분 +").append(Fmt.money(overflowTotal)).append("M)");
        }
        Text.send(player, sb.toString());
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
        Result result = results.get(revealed);
        revealed++;
        playSound(result.type());
        if (revealed % SLOTS.length == 0 && revealed < results.size()) {
            pause = PAGE_PAUSE_TICKS;
        }
        redraw();
    }

    @Override
    protected void draw() {
        int pageStart = revealed == 0 ? 0 : ((revealed - 1) / SLOTS.length) * SLOTS.length;
        for (int i = pageStart; i < revealed && i - pageStart < SLOTS.length; i++) {
            Result result = results.get(i);
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
        set(45, Items.button(org.bukkit.Material.PAPER, "&f" + product.displayName(),
                "&7" + revealed + " / " + results.size()));
        backButton(49, () -> new GachaMenu(plugin, player).open());
    }

    private void playSound(ShardType type) {
        switch (type) {
            case D -> player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 2f);
            case C -> player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 2f);
            case B -> player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1f, 2f);
            case A -> player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
            case S, SP -> {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    online.playSound(online.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.7f,
                            type == ShardType.SP ? 1f : 2f);
                }
            }
        }
    }

    @Override
    void onClose() {
        if (task != null) {
            task.cancel();
        }
    }
}
