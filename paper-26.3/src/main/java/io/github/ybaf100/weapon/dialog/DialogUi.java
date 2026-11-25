package io.github.ybaf100.weapon.dialog;

import io.github.ybaf100.weapon.WeaponPlugin;
import io.github.ybaf100.weapon.WeaponUi;
import io.github.ybaf100.weapon.compat.Compat;
import io.github.ybaf100.weapon.data.PlayerData;
import io.github.ybaf100.weapon.item.GearItems;
import io.github.ybaf100.weapon.logic.Enhancement;
import io.github.ybaf100.weapon.logic.Evolution;
import io.github.ybaf100.weapon.logic.Gacha;
import io.github.ybaf100.weapon.logic.Gacha.Product;
import io.github.ybaf100.weapon.logic.GearRules;
import io.github.ybaf100.weapon.model.AbilityEntry;
import io.github.ybaf100.weapon.model.GearData;
import io.github.ybaf100.weapon.model.GearType;
import io.github.ybaf100.weapon.model.Grade;
import io.github.ybaf100.weapon.model.ShardType;
import io.github.ybaf100.weapon.model.Ticket;
import io.github.ybaf100.weapon.sell.SellMenu;
import io.github.ybaf100.weapon.service.WeaponActions;
import io.github.ybaf100.weapon.util.Fmt;
import io.github.ybaf100.weapon.util.Text;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.title.Title;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

/** 최신 버전: 상자 GUI 대신 마인크래프트 대화창(Dialog)으로 메뉴를 띄운다. */
public final class DialogUi implements WeaponUi {
    private static final int TEXT_WIDTH = 360;
    private static final int BUTTON_WIDTH = 170;
    private static final ClickCallback.Options CLICK_OPTIONS = ClickCallback.Options.builder()
            .uses(1)
            .lifetime(Duration.ofMinutes(15))
            .build();

    /** 결과 공개: 이 간격(틱)마다 결과창을 다시 띄우고, 최대 이 횟수 안에 모두 보여 준다. */
    private static final long REVEAL_INTERVAL_TICKS = 3;
    private static final int REVEAL_STEPS = 14;

    private final WeaponPlugin plugin;
    private final WeaponActions actions;
    private final Map<UUID, BukkitTask> reveals = new HashMap<>();

    public DialogUi(WeaponPlugin plugin) {
        this.plugin = plugin;
        this.actions = plugin.actions();
    }

    @Override
    public void enable() {
    }

    @Override
    public void disable() {
        reveals.values().forEach(BukkitTask::cancel);
        reveals.clear();
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.closeDialog();
        }
    }

    @Override
    public void openMain(Player player) {
        main(player, null);
    }

    // ================================================================== 화면

    private void main(Player player, String message) {
        List<DialogBody> body = new ArrayList<>();
        body.add(text("&f무기 파편을 뽑고, 장비를 조합하고, 강화하고, 진화시킵니다."));
        body.add(text("&7강화, 진화, 고유능력은 &e손에 든 장비&7에 적용됩니다."));
        body.add(text(walletLine(player)));
        addMessage(body, message);

        List<ActionButton> buttons = List.of(
                button("&6뽑기", "여러 무기 파편을 뽑습니다.", (p, v) -> gacha(p, null)),
                button("&a강화", "손에 든 장비를 강화합니다.", (p, v) -> enhance(p, null)),
                button("&c진화", "손에 든 장비의 등급을 올립니다.", (p, v) -> evolve(p, null)),
                button("&b무기 조합", "무기 파편으로 장비와 아이템을 만듭니다.", (p, v) -> craft(p, null)),
                button("&d무기 고유능력", "손에 든 장비의 고유능력을 강화하거나 바꿉니다.", (p, v) -> ability(p, null)),
                button("&e인벤토리", "보유한 파편과 아이템을 확인합니다.", (p, v) -> storage(p)),
                button("&2판매", "농작물과 광물을 팝니다. (/판매 전체 로 빠른 판매)", (p, v) -> new SellMenu(plugin, p).open()));
        show(player, "&a무기 메뉴", body, List.of(), menu(buttons, closeButton("닫기"), 2));
    }

    // ------------------------------------------------------------------ 뽑기

    private void gacha(Player player, String message) {
        List<DialogBody> body = new ArrayList<>();
        body.add(text(probabilityLine("일반 뽑기", Gacha.Kind.COMMON)));
        body.add(text(probabilityLine("고급 뽑기", Gacha.Kind.EPIC)));
        body.add(text(probabilityLine("마일리지 뽑기", Gacha.Kind.MILEAGE)));
        body.add(text(walletLine(player)));
        addMessage(body, message);

        List<ActionButton> buttons = new ArrayList<>();
        for (Product product : Product.values()) {
            String label = product.mileageCost() > 0
                    ? product.displayName() + " &d" + Fmt.money(product.mileageCost()) + "M"
                    : product.displayName() + " &a" + Fmt.money(product.price()) + "원";
            String tooltip = "파편 " + product.count() + "개" + (product.mileageGain() > 0
                    ? ", 마일리지 " + Fmt.money(product.mileageGain()) + " 적립" : "");
            buttons.add(button(label, tooltip, 200, (p, v) -> confirmGacha(p, product)));
        }
        show(player, "&6무기 뽑기", body, List.of(), menu(buttons, button("돌아가기", null, (p, v) -> main(p, null)), 2));
    }

    private void confirmGacha(Player player, Product product) {
        String cost = product.mileageCost() > 0
                ? "마일리지 &d" + Fmt.money(product.mileageCost()) + "M"
                : "&a" + Fmt.money(product.price()) + "원";
        List<DialogBody> body = List.of(
                text("&f" + cost + "&f을 사용해 파편 " + product.count() + "개를 뽑습니다."),
                text("&7결과는 확인을 누르는 순간 정해지고 바로 저장됩니다."),
                text(walletLine(player)));
        ActionButton yes = button("&a확인", null, (p, v) -> {
            WeaponActions.GachaOutcome result = actions.buyGacha(p, product);
            if (result.outcome().ok()) {
                // 창이 닫힌 뒤 월드 연출(소용돌이)을 보여 주고 결과 창을 연다. 웅크리면 건너뛴다.
                plugin.flyIn().play(p, product, result.rolls(), skipped -> startReveal(p, product, result, skipped));
            } else {
                gacha(p, result.outcome().message());
            }
        });
        ActionButton no = button("취소", null, (p, v) -> gacha(p, null));
        show(player, product.displayName(), body, List.of(), DialogType.confirmation(yes, no));
    }

    /** 결과를 몇 개씩 묶어 차례로 보여 준다. 결과 자체는 이미 정해져 저장된 상태다. */
    private void startReveal(Player player, Product product, WeaponActions.GachaOutcome result, boolean showTitles) {
        cancelReveal(player);
        int total = result.rolls().size();
        int step = Math.max(1, (total + REVEAL_STEPS - 1) / REVEAL_STEPS);
        int[] shown = {0};
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                cancelReveal(player);
                return;
            }
            int before = shown[0];
            shown[0] = Math.min(total, before + step);
            ShardType best = null;
            for (int i = before; i < shown[0]; i++) {
                ShardType type = result.rolls().get(i).type();
                if (best == null || type.ordinal() > best.ordinal()) {
                    best = type;
                }
            }
            WeaponActions.playGachaSound(player, best);
            if (showTitles && (best == ShardType.S || best == ShardType.SP)) {
                player.showTitle(Title.title(Text.c(best.displayName() + "!"),
                        Text.c("&7" + product.displayName() + "&7에서 획득"),
                        Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(1500), Duration.ofMillis(400))));
            }
            gachaResult(player, product, result, shown[0]);
            if (shown[0] >= total) {
                cancelReveal(player);
            }
        }, 0L, REVEAL_INTERVAL_TICKS);
        reveals.put(player.getUniqueId(), task);
    }

    private void cancelReveal(Player player) {
        BukkitTask task = reveals.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    private void gachaResult(Player player, Product product, WeaponActions.GachaOutcome result, int revealed) {
        int total = result.rolls().size();
        boolean done = revealed >= total;
        Map<ShardType, Integer> counts = new EnumMap<>(ShardType.class);
        long overflow = 0;
        ShardType best = null;
        for (int i = 0; i < revealed; i++) {
            WeaponActions.GachaRoll roll = result.rolls().get(i);
            counts.merge(roll.type(), 1, Integer::sum);
            overflow += roll.overflowMileage();
            if (best == null || roll.type().ordinal() > best.ordinal()) {
                best = roll.type();
            }
        }
        List<DialogBody> body = new ArrayList<>();
        body.add(text("&7" + product.displayName() + " &7· " + total + "개" + (done ? "" : " &e(" + revealed + " / " + total + ")")));
        if (best == ShardType.S || best == ShardType.SP) {
            body.add(text(best.color() + "&l★ " + best.label() + "급 무기 파편! ★"));
        }
        counts.forEach((type, n) -> body.add(DialogBody.item(new ItemStack(Compat.material(type.material()), Math.min(64, n)))
                .description(DialogBody.plainMessage(Text.c(type.displayName() + " &f×" + n), TEXT_WIDTH))
                .showTooltip(false)
                .build()));
        if (done && overflow > 0) {
            body.add(text("&d최대 보유량 초과분 +" + Fmt.money(overflow) + " 마일리지"));
        }
        if (done) {
            body.add(text(walletLine(player)));
        }

        List<ActionButton> buttons = done
                ? List.of(
                        button("&6한 번 더", null, (p, v) -> confirmGacha(p, product)),
                        button("돌아가기", null, (p, v) -> gacha(p, null)))
                : List.of(button("건너뛰기", null, (p, v) -> gachaResult(p, product, result, total)));
        show(player, "&6뽑기 결과", body, List.of(), menu(buttons, null, done ? 2 : 1));
    }

    // ------------------------------------------------------------------ 강화

    private void enhance(Player player, String message) {
        GearData gear = WeaponActions.heldGear(player);
        if (gear == null) {
            main(player, "&c강화할 시스템 장비를 손에 들고 열어 주세요.");
            return;
        }
        UUID id = gear.id();
        PlayerData data = plugin.data().get(player.getUniqueId());
        List<DialogBody> body = new ArrayList<>();
        body.add(heldItem(player));
        int level = gear.enhance();
        List<ActionButton> buttons = new ArrayList<>();
        if (Enhancement.isMax(level)) {
            body.add(text("&6최대 강화 단계입니다."));
        } else {
            double success = Enhancement.successChance(level);
            double fail = 1 - success;
            body.add(text("&f" + level + "강 → " + (level + 1) + "강  &7배율 " + Fmt.num(Enhancement.multiplier(level) * 100)
                    + "% → &a" + Fmt.num(Enhancement.multiplier(level + 1) * 100) + "%"));
            StringBuilder odds = new StringBuilder("&a성공 " + Fmt.pct(success));
            if (fail > 0) {
                odds.append("  &e유지 ").append(Fmt.pct(fail * Enhancement.KEEP_ON_FAIL));
                if (level > 1) {
                    odds.append("  &c하락 1~").append(level - 1).append("강 각 ")
                            .append(Fmt.pct(fail * (1 - Enhancement.KEEP_ON_FAIL) / (level - 1)));
                }
            }
            body.add(text(odds.toString()));
            body.add(text("&7재료 " + costLine(Enhancement.cost(level), data)));
            buttons.add(button("&a강화하기", null, (p, v) -> enhance(p, actions.enhance(p, id).message())));
        }
        addMessage(body, message);
        buttons.add(button("돌아가기", null, (p, v) -> main(p, null)));
        show(player, "&a장비 강화", body, List.of(), menu(buttons, null, 2));
    }

    // ------------------------------------------------------------------ 진화

    private void evolve(Player player, String message) {
        GearData gear = WeaponActions.heldGear(player);
        if (gear == null) {
            main(player, message != null ? message : "&c진화할 시스템 장비를 손에 들고 열어 주세요.");
            return;
        }
        UUID id = gear.id();
        PlayerData data = plugin.data().get(player.getUniqueId());
        List<DialogBody> body = new ArrayList<>();
        List<DialogInput> inputs = new ArrayList<>();
        List<ActionButton> buttons = new ArrayList<>();
        body.add(heldItem(player));

        Evolution.Step step = Evolution.step(gear.grade());
        if (step == null) {
            body.add(text("&6최고 등급입니다."));
        } else {
            body.add(text("&f" + gear.grade().display() + " &f→ " + step.to().display()));
            StringBuilder odds = new StringBuilder("&a성공 " + Fmt.pct(step.success()));
            if (step.keep() > 0.0001) {
                odds.append("  &e유지 ").append(Fmt.pct(step.keep()));
            }
            if (step.protectable()) {
                odds.append("  &c파괴 ").append(Fmt.pct(step.destroy()));
            }
            body.add(text(odds.toString()));
            body.add(text("&7재료 " + costLine(step.cost(), data)));
            int before = gear.abilitySlots();
            int after = gear.type().abilitySlots(step.to());
            if (after > before) {
                body.add(text("&d성공하면 고유능력 슬롯이 " + before + " → " + after + "개가 되고, 일반 고유능력을 전부 다시 뽑습니다."));
            }
            if (step.confirm() != null) {
                body.add(text("&7확정 상승권을 켜면 성공 100%가 됩니다."));
                inputs.add(DialogInput.bool("confirm", Text.c(step.confirm().displayName() + " 사용 &7(보유 "
                        + data.tickets(step.confirm()) + ")")).initial(false).build());
            }
            if (step.protectable()) {
                body.add(text("&7파괴 방지권을 켜면 파괴가 나와도 유지됩니다. 시도할 때마다 1개씩 씁니다."));
                inputs.add(DialogInput.bool("protect", Text.c("파괴 방지권 사용 &7(보유 "
                        + data.tickets(Ticket.PROTECT) + ")")).initial(false).build());
            }
            buttons.add(button("&c진화하기", null, (p, v) -> {
                boolean confirm = Boolean.TRUE.equals(v.getBoolean("confirm"));
                boolean protect = Boolean.TRUE.equals(v.getBoolean("protect"));
                String result = actions.evolve(p, id, confirm, protect).message();
                GearData now = WeaponActions.heldGear(p);
                if (now == null || !now.id().equals(id)) {
                    main(p, result);
                } else {
                    evolve(p, result);
                }
            }));
        }
        addMessage(body, message);
        buttons.add(button("돌아가기", null, (p, v) -> main(p, null)));
        show(player, "&c장비 진화", body, inputs, menu(buttons, null, 2));
    }

    // ------------------------------------------------------------------ 고유능력

    private void ability(Player player, String message) {
        GearData gear = WeaponActions.heldGear(player);
        if (gear == null || gear.abilitySlots() == 0) {
            main(player, gear == null ? "&c시스템 장비를 손에 들고 열어 주세요."
                    : "&c이 장비는 아직 고유능력이 없습니다. (무기, 방어구 B급부터)");
            return;
        }
        UUID id = gear.id();
        PlayerData data = plugin.data().get(player.getUniqueId());
        List<DialogBody> body = new ArrayList<>();
        body.add(heldItem(player));
        List<SingleOptionDialogInput.OptionEntry> targets = new ArrayList<>();
        for (int i = 0; i < gear.abilities().size(); i++) {
            AbilityEntry entry = gear.abilities().get(i);
            body.add(text(GearItems.abilityLine(entry) + " &8/ 최대 " + gear.capFor(entry.type()).display()));
            body.add(text("&8    " + entry.type().describe(entry.grade())));
            targets.add(SingleOptionDialogInput.OptionEntry.create(String.valueOf(i),
                    Text.c("슬롯 " + (i + 1) + " · " + entry.type().koreanName() + " " + entry.grade().display()), i == 0));
        }
        if (gear.type().hasMasterSlot(gear.grade())) {
            if (gear.master() == null) {
                body.add(text("&8 • Master 슬롯: 생성권으로 엽니다."));
            } else {
                body.add(text(GearItems.abilityLine(gear.master())));
                body.add(text("&8    " + gear.master().type().describe(gear.master().grade())));
                targets.add(SingleOptionDialogInput.OptionEntry.create(String.valueOf(GearRules.MASTER_INDEX),
                        Text.c("Master · " + gear.master().type().koreanName() + " " + gear.master().grade().display()), false));
            }
        }
        body.add(text("&8강화권, 고정/선택 변경권은 대상 능력에 쓰고, 나머지는 대상을 무시합니다. 다시 뽑은 능력은 D급부터 시작합니다."));
        addMessage(body, message);

        List<SingleOptionDialogInput.OptionEntry> tickets = new ArrayList<>();
        boolean first = true;
        for (Ticket ticket : Ticket.values()) {
            if (ticket.group() != Ticket.Group.ABILITY) {
                continue;
            }
            tickets.add(SingleOptionDialogInput.OptionEntry.create(ticket.name(),
                    Text.c(ticket.displayName() + " &7(보유 " + data.tickets(ticket) + ")"), first));
            first = false;
        }
        List<DialogInput> inputs = new ArrayList<>();
        inputs.add(DialogInput.singleOption("ticket", Text.c("사용할 아이템"), tickets).width(320).build());
        if (!targets.isEmpty()) {
            inputs.add(DialogInput.singleOption("target", Text.c("대상 능력"), targets).width(320).build());
        }
        List<ActionButton> buttons = List.of(
                button("&d사용", null, (p, v) -> {
                    Ticket ticket = parseTicket(v.getText("ticket"));
                    if (ticket == null) {
                        ability(p, "&c아이템을 골라 주세요.");
                        return;
                    }
                    int target = ticket.needsTarget() ? parseInt(v.getText("target"), 0) : -1;
                    ability(p, actions.useTicket(p, id, ticket, target).message());
                }),
                button("돌아가기", null, (p, v) -> main(p, null)));
        show(player, "&d무기 고유능력", body, inputs, menu(buttons, null, 2));
    }

    // ------------------------------------------------------------------ 조합

    private void craft(Player player, String message) {
        PlayerData data = plugin.data().get(player.getUniqueId());
        List<DialogBody> body = new ArrayList<>();
        body.add(text(shardLine(data)));
        List<SingleOptionDialogInput.OptionEntry> options = new ArrayList<>();
        boolean first = true;
        for (GearType type : GearType.values()) {
            options.add(SingleOptionDialogInput.OptionEntry.create("G:" + type.name(),
                    Text.c(Grade.D.display() + "급 " + type.koreanName()), first));
            first = false;
        }
        for (Ticket ticket : Ticket.values()) {
            options.add(SingleOptionDialogInput.OptionEntry.create("T:" + ticket.name(),
                    Text.c(ticket.displayName() + " &7(보유 " + data.tickets(ticket) + ")"), false));
        }
        body.add(text("&f장비: &7다이아몬드 장비 1개 + " + ShardType.D.displayName() + " &710개 &8(이름 없는 일반 장비만 재료로 쓰입니다)"));
        for (Ticket ticket : Ticket.values()) {
            body.add(text("&f" + ticket.displayName() + "&7: " + costLine(ticket.cost(), data)));
        }
        addMessage(body, message);

        List<DialogInput> inputs = List.of(
                DialogInput.singleOption("item", Text.c("만들 아이템"), options).width(320).build(),
                DialogInput.numberRange("qty", Text.c("수량"), 1, 64).step(1f).initial(1f).width(320).build());
        List<ActionButton> buttons = List.of(
                button("&b조합", null, (p, v) -> {
                    String item = v.getText("item");
                    Float qtyRaw = v.getFloat("qty");
                    int qty = qtyRaw == null ? 1 : Math.max(1, Math.min(64, Math.round(qtyRaw)));
                    WeaponActions.Outcome outcome;
                    if (item != null && item.startsWith("G:")) {
                        outcome = actions.craftGear(p, GearType.valueOf(item.substring(2)), qty);
                    } else if (item != null && item.startsWith("T:")) {
                        outcome = actions.craftTicket(p, Ticket.valueOf(item.substring(2)), qty);
                    } else {
                        outcome = new WeaponActions.Outcome(false, "&c아이템을 골라 주세요.");
                    }
                    craft(p, outcome.message());
                }),
                button("돌아가기", null, (p, v) -> main(p, null)));
        show(player, "&b무기 조합", body, inputs, menu(buttons, null, 2));
    }

    // ------------------------------------------------------------------ 인벤토리

    private void storage(Player player) {
        PlayerData data = plugin.data().get(player.getUniqueId());
        List<DialogBody> body = new ArrayList<>();
        for (ShardType type : ShardType.values()) {
            body.add(DialogBody.item(new ItemStack(Compat.material(type.material()), Math.max(1, Math.min(64, data.shards(type)))))
                    .description(DialogBody.plainMessage(Text.c(type.displayName() + " &f" + data.shards(type) + " &7/ "
                            + ShardType.MAX_AMOUNT), TEXT_WIDTH))
                    .showTooltip(false)
                    .build());
        }
        body.add(text("&d마일리지 " + Fmt.money(data.mileage()) + "M"));
        for (Ticket ticket : Ticket.values()) {
            body.add(text("&f" + ticket.displayName() + " &e" + data.tickets(ticket) + "개"));
        }
        show(player, "&e인벤토리", body, List.of(), menu(List.of(button("돌아가기", null, (p, v) -> main(p, null))), null, 1));
    }

    // ================================================================== 도우미

    private void show(Player player, String title, List<DialogBody> body, List<DialogInput> inputs, DialogType type) {
        DialogBase base = DialogBase.builder(Text.c(title))
                .canCloseWithEscape(true)
                .pause(false)
                .afterAction(DialogBase.DialogAfterAction.CLOSE)
                .body(body)
                .inputs(inputs)
                .build();
        player.showDialog(Dialog.create(builder -> builder.empty().base(base).type(type)));
    }

    private static DialogType menu(List<ActionButton> buttons, ActionButton exit, int columns) {
        var builder = DialogType.multiAction(buttons).columns(columns);
        if (exit != null) {
            builder.exitAction(exit);
        }
        return builder.build();
    }

    private ActionButton button(String label, String tooltip, BiConsumer<Player, DialogResponseView> handler) {
        return button(label, tooltip, BUTTON_WIDTH, handler);
    }

    /** 누르면 메인 스레드에서 handler를 실행하는 버튼. */
    private ActionButton button(String label, String tooltip, int width, BiConsumer<Player, DialogResponseView> handler) {
        DialogAction action = DialogAction.customClick((view, audience) -> {
            if (audience instanceof Player player) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) {
                        cancelReveal(player);
                        handler.accept(player, view);
                    }
                });
            }
        }, CLICK_OPTIONS);
        ActionButton.Builder builder = ActionButton.builder(Text.c(label)).width(width).action(action);
        if (tooltip != null) {
            builder.tooltip(Text.c("&7" + tooltip));
        }
        return builder.build();
    }

    /** 동작 없이 창만 닫는 버튼. */
    private static ActionButton closeButton(String label) {
        return ActionButton.builder(Text.c(label)).width(BUTTON_WIDTH).build();
    }

    private static DialogBody text(String legacy) {
        return DialogBody.plainMessage(Text.c(legacy), TEXT_WIDTH);
    }

    private static void addMessage(List<DialogBody> body, String message) {
        if (message != null && !message.isBlank()) {
            body.add(text(" "));
            for (String line : message.split("\n")) {
                body.add(text(line));
            }
        }
    }

    private static DialogBody heldItem(Player player) {
        return DialogBody.item(player.getInventory().getItemInMainHand().clone()).showTooltip(true).build();
    }

    private String walletLine(Player player) {
        PlayerData data = plugin.data().get(player.getUniqueId());
        Economy economy = plugin.economy();
        String money = economy == null ? "&c경제 플러그인 없음" : "&a" + Fmt.money(economy.getBalance(player)) + "원";
        return "&7보유 " + money + " &8· &7마일리지 &d" + Fmt.money(data.mileage()) + "M";
    }

    private static String shardLine(PlayerData data) {
        StringBuilder sb = new StringBuilder("&7보유 파편: ");
        for (ShardType type : ShardType.values()) {
            sb.append(type.color()).append(type.label()).append(" &f").append(data.shards(type)).append("  ");
        }
        return sb.toString();
    }

    private static String costLine(Map<ShardType, Integer> cost, PlayerData data) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<ShardType, Integer> entry : cost.entrySet()) {
            if (!sb.isEmpty()) {
                sb.append(" &7+ ");
            }
            int have = data.shards(entry.getKey());
            sb.append(have >= entry.getValue() ? "&a" : "&c").append(entry.getKey().label()).append("급 ")
                    .append(entry.getValue()).append("개 &8(보유 ").append(have).append(")");
        }
        return sb.toString();
    }

    private static String probabilityLine(String name, Gacha.Kind kind) {
        StringBuilder sb = new StringBuilder("&f" + name + " &8· ");
        for (Map.Entry<ShardType, Integer> entry : kind.weights().entrySet()) {
            sb.append(entry.getKey().color()).append(entry.getKey().label()).append(" &f")
                    .append(Fmt.num(entry.getValue() / 10.0)).append("%  ");
        }
        return sb.toString();
    }

    private static Ticket parseTicket(String raw) {
        try {
            return raw == null ? null : Ticket.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static int parseInt(String raw, int fallback) {
        try {
            return raw == null ? fallback : Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
