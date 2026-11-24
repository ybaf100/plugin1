package io.github.ybaf100.weapon.service;

import io.github.ybaf100.weapon.WeaponPlugin;
import io.github.ybaf100.weapon.compat.Compat;
import io.github.ybaf100.weapon.data.PlayerData;
import io.github.ybaf100.weapon.item.GearItems;
import io.github.ybaf100.weapon.logic.Enhancement;
import io.github.ybaf100.weapon.logic.Evolution;
import io.github.ybaf100.weapon.logic.Gacha.Product;
import io.github.ybaf100.weapon.logic.GearRules;
import io.github.ybaf100.weapon.model.AbilityEntry;
import io.github.ybaf100.weapon.model.GearData;
import io.github.ybaf100.weapon.model.GearType;
import io.github.ybaf100.weapon.model.Grade;
import io.github.ybaf100.weapon.model.ShardType;
import io.github.ybaf100.weapon.model.Ticket;
import io.github.ybaf100.weapon.util.Fmt;
import io.github.ybaf100.weapon.util.Text;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * 메뉴에서 하는 실제 동작 (뽑기, 조합, 강화, 진화, 고유능력 아이템).
 * 상자 GUI와 대화창이 같은 규칙을 쓰도록 여기 한 곳에 둔다.
 * 각 동작은 효과음과 서버 공지까지 처리하고, 플레이어에게 보여 줄 문장을 돌려준다.
 */
public final class WeaponActions {

    /** 동작 결과. ok가 false면 아무것도 바뀌지 않았다. */
    public record Outcome(boolean ok, String message) {
        static Outcome fail(String message) {
            return new Outcome(false, "&c" + message);
        }
    }

    /** 뽑기 한 번의 결과. */
    public record GachaRoll(ShardType type, long overflowMileage) {
    }

    public record GachaOutcome(Outcome outcome, List<GachaRoll> rolls) {
    }

    public static final Map<ShardType, Integer> GEAR_CRAFT_COST = Map.of(ShardType.D, 10);

    private final WeaponPlugin plugin;

    public WeaponActions(WeaponPlugin plugin) {
        this.plugin = plugin;
    }

    private PlayerData data(Player player) {
        return plugin.data().get(player.getUniqueId());
    }

    // ------------------------------------------------------------------ 뽑기

    public GachaOutcome buyGacha(Player player, Product product) {
        PlayerData data = data(player);
        String paid;
        if (product.mileageCost() > 0) {
            if (data.mileage() < product.mileageCost()) {
                return new GachaOutcome(Outcome.fail("마일리지가 부족합니다."), List.of());
            }
            data.addMileage(-product.mileageCost());
            paid = "&d-" + Fmt.money(product.mileageCost()) + "M";
        } else {
            Economy economy = plugin.economy();
            if (economy == null) {
                return new GachaOutcome(Outcome.fail("경제 플러그인이 없어 뽑기를 할 수 없습니다."), List.of());
            }
            if (!economy.has(player, product.price())) {
                return new GachaOutcome(Outcome.fail("돈이 부족합니다."), List.of());
            }
            if (!economy.withdrawPlayer(player, product.price()).transactionSuccess()) {
                return new GachaOutcome(Outcome.fail("결제에 실패했습니다."), List.of());
            }
            data.addMileage(product.mileageGain());
            paid = "&c-" + Fmt.money(product.price()) + "원 &d+" + Fmt.money(product.mileageGain()) + "M";
        }

        List<GachaRoll> rolls = new ArrayList<>();
        Map<ShardType, Integer> summary = new EnumMap<>(ShardType.class);
        long overflowTotal = 0;
        for (int i = 0; i < product.count(); i++) {
            ShardType type = product.kind().roll(plugin.random());
            long overflow = data.addShards(type, 1);
            overflowTotal += overflow;
            rolls.add(new GachaRoll(type, overflow));
            summary.merge(type, 1, Integer::sum);
            if (type.ordinal() >= ShardType.A.ordinal()) {
                Text.broadcast("&f" + player.getName() + "&7님이 " + product.kind().label() + "&7 뽑기에서 "
                        + type.displayName() + "&7을 획득했습니다!");
            }
        }

        StringBuilder sb = new StringBuilder(paid).append(" &7결과: ");
        summary.forEach((type, n) -> sb.append(type.color()).append(type.label()).append(" &f×").append(n).append("  "));
        if (overflowTotal > 0) {
            sb.append("&d(초과분 +").append(Fmt.money(overflowTotal)).append("M)");
        }
        return new GachaOutcome(new Outcome(true, sb.toString()), Collections.unmodifiableList(rolls));
    }

    public static void playGachaSound(Player player, ShardType type) {
        if (type == null) {
            return;
        }
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

    // ------------------------------------------------------------------ 조합

    /** 인벤토리에서 재료로 쓸 순수한 다이아 장비 칸. 없으면 -1. */
    public static int findPlain(Player player, GearType type) {
        PlayerInventory inventory = player.getInventory();
        Material material = Compat.material(type.material());
        for (int i = 0; i < inventory.getStorageContents().length; i++) {
            if (GearItems.isPlain(inventory.getItem(i), material)) {
                return i;
            }
        }
        return -1;
    }

    public static int countPlain(Player player, GearType type) {
        PlayerInventory inventory = player.getInventory();
        Material material = Compat.material(type.material());
        int count = 0;
        for (int i = 0; i < inventory.getStorageContents().length; i++) {
            ItemStack item = inventory.getItem(i);
            if (GearItems.isPlain(item, material)) {
                count += item.getAmount();
            }
        }
        return count;
    }

    public Outcome craftGear(Player player, GearType type, int amount) {
        PlayerData data = data(player);
        int made = 0;
        for (int n = 0; n < amount; n++) {
            int index = findPlain(player, type);
            if (index < 0 || !data.hasShards(GEAR_CRAFT_COST)) {
                break;
            }
            data.takeShards(GEAR_CRAFT_COST);
            ItemStack material = player.getInventory().getItem(index);
            material.setAmount(material.getAmount() - 1);
            player.getInventory().setItem(index, material.getAmount() <= 0 ? null : material);
            player.getInventory().addItem(GearItems.create(GearData.create(type))).values()
                    .forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
            made++;
        }
        if (made == 0) {
            return findPlain(player, type) < 0
                    ? Outcome.fail("재료로 쓸 다이아몬드 " + type.koreanName() + "이(가) 인벤토리에 없습니다.")
                    : Outcome.fail("D급 무기 파편이 부족합니다.");
        }
        player.playSound(player.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 1f, 1f);
        return new Outcome(true, "&a" + Grade.D.display() + "급 " + type.koreanName() + " &a" + made + "개를 조합했습니다."
                + (made < amount ? " &7(재료가 모자라 " + amount + "개 중 " + made + "개만)" : ""));
    }

    public Outcome craftTicket(Player player, Ticket ticket, int amount) {
        PlayerData data = data(player);
        int made = 0;
        for (int i = 0; i < amount; i++) {
            if (!data.takeShards(ticket.cost())) {
                break;
            }
            data.addTickets(ticket, 1);
            made++;
        }
        if (made == 0) {
            return Outcome.fail("재료가 부족합니다.");
        }
        player.playSound(player.getLocation(), Sound.BLOCK_SMITHING_TABLE_USE, 1f, 1.2f);
        return new Outcome(true, "&a" + ticket.displayName() + " " + made + "개를 조합했습니다."
                + (made < amount ? " &7(재료가 모자라 " + amount + "개 중 " + made + "개만)" : ""));
    }

    // ------------------------------------------------------------------ 손에 든 장비

    public static GearData heldGear(Player player) {
        return GearItems.read(player.getInventory().getItemInMainHand());
    }

    private static GearData sameGear(Player player, UUID id) {
        GearData gear = heldGear(player);
        return gear != null && gear.id().equals(id) ? gear : null;
    }

    private static void saveHeld(Player player, GearData gear) {
        ItemStack item = player.getInventory().getItemInMainHand();
        GearItems.write(item, gear);
        player.getInventory().setItemInMainHand(item);
    }

    private static final String CHANGED = "메뉴를 연 뒤 손에 든 장비가 바뀌었습니다. 다시 열어 주세요.";

    // ------------------------------------------------------------------ 강화

    public Outcome enhance(Player player, UUID gearId) {
        GearData gear = sameGear(player, gearId);
        if (gear == null) {
            return Outcome.fail(CHANGED);
        }
        int level = gear.enhance();
        if (Enhancement.isMax(level)) {
            return Outcome.fail("최대 강화 단계입니다.");
        }
        if (!data(player).takeShards(Enhancement.cost(level))) {
            return Outcome.fail("재료가 부족합니다.");
        }
        int after = Enhancement.roll(level, plugin.random());
        gear.setEnhance(after);
        saveHeld(player, gear);
        if (after > level) {
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1.4f);
            if (after >= 7) {
                Text.broadcast("&f" + player.getName() + "&7님이 " + GearItems.name(gear) + "&7을(를) &e" + after
                        + "강&7으로 강화했습니다!");
            }
            return new Outcome(true, "&a강화 성공! &f" + level + "강 → " + after + "강");
        }
        if (after == level) {
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.6f, 1f);
            return new Outcome(true, "&e강화 실패. 단계가 유지되었습니다. &f(" + level + "강)");
        }
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_DESTROY, 0.8f, 1f);
        return new Outcome(true, "&c강화 실패. &f" + level + "강 → " + after + "강");
    }

    // ------------------------------------------------------------------ 진화

    /** @return ok=true면 시도는 했다. 장비가 파괴되었는지는 heldGear로 확인한다. */
    public Outcome evolve(Player player, UUID gearId, boolean useConfirm, boolean useProtect) {
        GearData gear = sameGear(player, gearId);
        if (gear == null) {
            return Outcome.fail(CHANGED);
        }
        Evolution.Step step = Evolution.step(gear.grade());
        if (step == null) {
            return Outcome.fail("최고 등급입니다.");
        }
        PlayerData data = data(player);
        boolean confirm = useConfirm && step.confirm() != null;
        boolean protect = useProtect && step.protectable() && !confirm;
        if (!data.hasShards(step.cost())) {
            return Outcome.fail("재료가 부족합니다.");
        }
        if (confirm && data.tickets(step.confirm()) <= 0) {
            return Outcome.fail(step.confirm().displayName() + "이(가) 없습니다.");
        }
        if (protect && data.tickets(Ticket.PROTECT) <= 0) {
            return Outcome.fail("파괴 방지권이 없습니다.");
        }
        data.takeShards(step.cost());
        if (confirm) {
            data.takeTicket(step.confirm());
        }
        if (protect) {
            data.takeTicket(Ticket.PROTECT);
        }

        String before = GearItems.name(gear);
        switch (Evolution.roll(step, confirm, protect, plugin.random())) {
            case SUCCESS -> {
                boolean rerolled = GearRules.applyEvolution(gear, plugin.random());
                saveHeld(player, gear);
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);
                if (gear.grade().ordinal() >= Grade.S_MINUS.ordinal()) {
                    Text.broadcast("&f" + player.getName() + "&7님이 " + GearItems.name(gear) + "&7(으)로 진화했습니다!");
                }
                return new Outcome(true, "&a진화 성공! " + before + " &f→ " + GearItems.name(gear)
                        + (rerolled ? " &d(슬롯이 늘어 고유능력을 다시 뽑았습니다)" : ""));
            }
            case KEEP -> {
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.6f, 1f);
                return new Outcome(true, "&e진화 실패. 등급이 유지되었습니다.");
            }
            case PROTECTED -> {
                player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 0.6f, 1.2f);
                return new Outcome(true, "&e진화 실패. &6파괴 방지권&e이 장비를 지켰습니다.");
            }
            default -> {
                player.getInventory().setItemInMainHand(null);
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 0.6f);
                return new Outcome(true, "&4진화 실패. " + before + "&4이(가) 파괴되었습니다.");
            }
        }
    }

    // ------------------------------------------------------------------ 고유능력 아이템

    public Outcome useTicket(Player player, UUID gearId, Ticket ticket, int target) {
        GearData gear = sameGear(player, gearId);
        if (gear == null) {
            return Outcome.fail(CHANGED);
        }
        PlayerData data = data(player);
        if (data.tickets(ticket) <= 0) {
            return Outcome.fail(ticket.displayName() + "이(가) 없습니다. 조합 메뉴에서 만들 수 있습니다.");
        }
        if (target == GearRules.MASTER_INDEX && ticket != Ticket.ABILITY_UP && ticket != Ticket.ABILITY_UP_HIGH) {
            return Outcome.fail("Master 고유능력은 강화만 할 수 있습니다.");
        }
        switch (GearRules.applyTicket(gear, ticket, target, plugin.random())) {
            case NOT_APPLICABLE -> {
                return Outcome.fail("이 장비나 대상에는 쓸 수 없습니다.");
            }
            case AT_CAP -> {
                return Outcome.fail("이미 최대 등급입니다.");
            }
            case FAILED -> {
                data.takeTicket(ticket);
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.6f, 1f);
                return new Outcome(true, "&e강화 실패. " + ticket.displayName() + "만 소모되었습니다.");
            }
            default -> {
                data.takeTicket(ticket);
                saveHeld(player, gear);
                player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1.2f);
                StringBuilder sb = new StringBuilder("&a" + ticket.displayName() + "을(를) 사용했습니다.");
                for (AbilityEntry entry : gear.abilities()) {
                    sb.append("\n").append(GearItems.abilityLine(entry));
                }
                if (gear.master() != null) {
                    sb.append("\n").append(GearItems.abilityLine(gear.master()));
                }
                return new Outcome(true, sb.toString());
            }
        }
    }
}
