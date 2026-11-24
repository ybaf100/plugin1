package io.github.ybaf100.weapon.gui;

import io.github.ybaf100.weapon.WeaponPlugin;
import io.github.ybaf100.weapon.data.PlayerData;
import io.github.ybaf100.weapon.item.GearItems;
import io.github.ybaf100.weapon.item.Items;
import io.github.ybaf100.weapon.logic.Evolution;
import io.github.ybaf100.weapon.logic.GearRules;
import io.github.ybaf100.weapon.model.GearData;
import io.github.ybaf100.weapon.model.Grade;
import io.github.ybaf100.weapon.model.Ticket;
import io.github.ybaf100.weapon.util.Fmt;
import io.github.ybaf100.weapon.util.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** 손에 든 장비 진화. */
public final class EvolveMenu extends Menu {
    private UUID gearId;
    private boolean useConfirm;
    private boolean useProtect;

    public EvolveMenu(WeaponPlugin plugin, Player player) {
        super(plugin, player, 3, "&c장비 진화");
    }

    @Override
    public void open() {
        GearData gear = MenuUtil.heldGear(player);
        if (gear == null) {
            MenuUtil.error(player, "진화할 시스템 장비를 손에 들고 열어 주세요.");
            return;
        }
        gearId = gear.id();
        super.open();
    }

    @Override
    protected void draw() {
        GearData gear = MenuUtil.heldGear(player);
        if (gear == null || !gear.id().equals(gearId)) {
            return;
        }
        PlayerData data = plugin.data().get(player.getUniqueId());
        set(13, player.getInventory().getItemInMainHand().clone());
        backButton(18, () -> new MainMenu(plugin, player).open());

        Evolution.Step step = Evolution.step(gear.grade());
        if (step == null) {
            set(11, Items.button(Material.BOOK, "&6최고 등급입니다."));
            return;
        }
        if (step.confirm() == null) {
            useConfirm = false;
        }
        if (!step.protectable()) {
            useProtect = false;
        }

        List<String> info = new ArrayList<>();
        info.add(" ");
        info.add("&f" + gear.grade().display() + " &f→ " + step.to().display());
        info.add(" ");
        if (useConfirm) {
            info.add("&a성공 100% &7(확정 상승권)");
        } else {
            info.add("&a성공 " + Fmt.pct(step.success()));
            if (step.keep() > 0.0001) {
                info.add("&e유지 " + Fmt.pct(step.keep()));
            }
            if (step.protectable()) {
                info.add((useProtect ? "&7&m" : "&c") + "파괴 " + Fmt.pct(step.destroy())
                        + (useProtect ? "&r &7(방지권 → 유지)" : ""));
            }
        }
        int before = gear.abilitySlots();
        int after = gear.type().abilitySlots(step.to());
        if (after > before) {
            info.add(" ");
            info.add("&d성공하면 고유능력 슬롯이 " + before + " → " + after + "개가 되고,");
            info.add("&d일반 고유능력을 전부 다시 뽑습니다 (D급부터).");
        }
        set(11, Items.button(Material.BOOK, 1, "&f진화 정보", info));

        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.add("&f재료");
        lore.addAll(MenuUtil.costLines(step.cost(), data));
        if (useConfirm) {
            lore.add("&7+ " + step.confirm().displayName() + " 1개");
        }
        if (useProtect) {
            lore.add("&7+ " + Ticket.PROTECT.displayName() + " 1개");
        }
        lore.add(" ");
        lore.add("&a클릭하면 진화합니다.");
        set(15, Items.button(Material.NETHER_STAR, 1, "&c&l진화하기", lore), e -> evolve());

        if (step.confirm() != null) {
            Ticket confirm = step.confirm();
            ItemStack toggle = Items.button(confirm.icon(), 1, (useConfirm ? "&a[사용] " : "&7[미사용] ") + confirm.displayName(),
                    List.of(" ", "&e보유 " + data.tickets(confirm) + "개", "&7클릭해서 사용 여부를 바꿉니다."));
            set(21, useConfirm ? Items.glow(toggle) : toggle, e -> {
                if (!useConfirm && data.tickets(confirm) <= 0) {
                    MenuUtil.error(player, confirm.displayName() + "이(가) 없습니다.");
                    return;
                }
                useConfirm = !useConfirm;
                MenuUtil.click(player);
                redraw();
            });
        }
        if (step.protectable()) {
            ItemStack toggle = Items.button(Ticket.PROTECT.icon(), 1,
                    (useProtect ? "&a[사용] " : "&7[미사용] ") + Ticket.PROTECT.displayName(),
                    List.of(" ", "&e보유 " + data.tickets(Ticket.PROTECT) + "개", "&7클릭해서 사용 여부를 바꿉니다.",
                            "&8진화를 시도할 때마다 1개씩 소모됩니다."));
            set(23, useProtect ? Items.glow(toggle) : toggle, e -> {
                if (!useProtect && data.tickets(Ticket.PROTECT) <= 0) {
                    MenuUtil.error(player, "파괴 방지권이 없습니다.");
                    return;
                }
                useProtect = !useProtect;
                MenuUtil.click(player);
                redraw();
            });
        }
    }

    private void evolve() {
        GearData gear = MenuUtil.requireSameGear(player, gearId);
        if (gear == null) {
            return;
        }
        Evolution.Step step = Evolution.step(gear.grade());
        if (step == null) {
            return;
        }
        PlayerData data = plugin.data().get(player.getUniqueId());
        boolean confirm = useConfirm && step.confirm() != null;
        boolean protect = useProtect && step.protectable() && !confirm;
        if (!data.hasShards(step.cost())) {
            MenuUtil.error(player, "재료가 부족합니다.");
            return;
        }
        if (confirm && data.tickets(step.confirm()) <= 0) {
            MenuUtil.error(player, step.confirm().displayName() + "이(가) 없습니다.");
            useConfirm = false;
            redraw();
            return;
        }
        if (protect && data.tickets(Ticket.PROTECT) <= 0) {
            MenuUtil.error(player, "파괴 방지권이 없습니다.");
            useProtect = false;
            redraw();
            return;
        }
        data.takeShards(step.cost());
        if (confirm) {
            data.takeTicket(step.confirm());
        }
        if (protect) {
            data.takeTicket(Ticket.PROTECT);
        }

        Evolution.Outcome outcome = Evolution.roll(step, confirm, protect, plugin.random());
        String before = GearItems.name(gear);
        switch (outcome) {
            case SUCCESS -> {
                boolean rerolled = GearRules.applyEvolution(gear, plugin.random());
                MenuUtil.saveHeld(player, gear);
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);
                Text.send(player, "&a진화 성공! " + before + " &f→ " + GearItems.name(gear));
                if (rerolled) {
                    Text.send(player, "&d고유능력 슬롯이 늘어나 고유능력을 다시 뽑았습니다.");
                }
                if (gear.grade().ordinal() >= Grade.S_MINUS.ordinal()) {
                    Text.broadcast("&f" + player.getName() + "&7님이 " + GearItems.name(gear) + "&7(으)로 진화했습니다!");
                }
            }
            case KEEP -> {
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.6f, 1f);
                Text.send(player, "&e진화 실패. 등급이 유지되었습니다.");
            }
            case PROTECTED -> {
                player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 0.6f, 1.2f);
                Text.send(player, "&e진화 실패. &6파괴 방지권&e이 장비를 지켰습니다.");
            }
            case DESTROYED -> {
                player.getInventory().setItemInMainHand(null);
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 0.6f);
                Text.send(player, "&4진화 실패. " + before + "&4이(가) 파괴되었습니다.");
                Bukkit.getScheduler().runTask(plugin, () -> player.closeInventory());
                return;
            }
        }
        redraw();
    }
}
