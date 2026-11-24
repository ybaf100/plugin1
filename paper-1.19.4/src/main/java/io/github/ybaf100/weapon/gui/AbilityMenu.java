package io.github.ybaf100.weapon.gui;

import io.github.ybaf100.weapon.WeaponPlugin;
import io.github.ybaf100.weapon.data.PlayerData;
import io.github.ybaf100.weapon.item.Items;
import io.github.ybaf100.weapon.logic.GearRules;
import io.github.ybaf100.weapon.model.AbilityEntry;
import io.github.ybaf100.weapon.model.GearData;
import io.github.ybaf100.weapon.model.Ticket;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * 고유능력 메뉴. 아래 줄에서 아이템을 고르고, 대상이 필요한 아이템이면 위 줄의 능력을 클릭한다.
 * 대상이 필요 없는 아이템은 같은 아이템을 한 번 더 누르면 사용된다.
 */
public final class AbilityMenu extends Menu {
    private static final int[] ABILITY_SLOTS = {11, 12, 13};
    private static final int MASTER_SLOT = 15;
    private static final int TICKET_START = 37;

    private UUID gearId;
    private Ticket selected;

    public AbilityMenu(WeaponPlugin plugin, Player player) {
        super(plugin, player, 6, "&d무기 고유능력");
    }

    @Override
    public void open() {
        GearData gear = MenuUtil.heldGear(player);
        if (gear == null) {
            MenuUtil.error(player, "시스템 장비를 손에 들고 열어 주세요.");
            return;
        }
        if (gear.abilitySlots() == 0) {
            MenuUtil.error(player, "이 장비는 아직 고유능력이 없습니다. (무기, 방어구 B급부터)");
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
        set(4, player.getInventory().getItemInMainHand().clone());

        for (int i = 0; i < ABILITY_SLOTS.length; i++) {
            final int index = i;
            if (i >= gear.abilitySlots()) {
                set(ABILITY_SLOTS[i], Items.button(Material.GRAY_DYE, "&8잠긴 슬롯", "&7더 높은 등급에서 열립니다."));
                continue;
            }
            AbilityEntry entry = i < gear.abilities().size() ? gear.abilities().get(i) : null;
            set(ABILITY_SLOTS[i], abilityIcon(gear, entry, "슬롯 " + (i + 1)), e -> target(index));
        }
        if (gear.type().hasMasterSlot(gear.grade())) {
            if (gear.master() == null) {
                set(MASTER_SLOT, Items.button(Material.BLACK_DYE, "&8Master 슬롯 (잠김)", "&7Master 고유능력 생성권으로 엽니다."));
            } else {
                set(MASTER_SLOT, abilityIcon(gear, gear.master(), "Master"), e -> target(GearRules.MASTER_INDEX));
            }
        }

        int slot = TICKET_START;
        for (Ticket ticket : Ticket.values()) {
            if (ticket.group() != Ticket.Group.ABILITY) {
                continue;
            }
            List<String> lore = new ArrayList<>();
            lore.add(" ");
            lore.add("&7" + ticket.description());
            lore.add(" ");
            lore.add("&e보유 " + data.tickets(ticket) + "개");
            if (ticket == selected) {
                lore.add(ticket.needsTarget() ? "&a▶ 위에서 대상 능력을 클릭하세요." : "&a▶ 한 번 더 누르면 사용합니다.");
            } else {
                lore.add("&7클릭해서 선택합니다.");
            }
            ItemStack icon = Items.button(ticket.icon(), 1, "&f" + ticket.displayName(), lore);
            set(slot++, ticket == selected ? Items.glow(icon) : icon, e -> select(ticket));
        }
        set(22, Items.button(Material.OAK_SIGN, "&f사용 방법",
                "&71. 아래 줄에서 아이템을 고릅니다.",
                "&72. 강화권, 고정/선택 변경권은 위에서 대상 능력을 클릭합니다.",
                "&73. 변경권, 고급 변경권, Master 생성권은 한 번 더 누릅니다.",
                "&8다시 뽑은 능력은 D급부터 시작합니다."));
        backButton(45, () -> new MainMenu(plugin, player).open());
    }

    private ItemStack abilityIcon(GearData gear, AbilityEntry entry, String label) {
        if (entry == null) {
            return Items.button(Material.LIGHT_GRAY_DYE, "&7" + label + " (비어 있음)");
        }
        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.add("&7" + entry.type().describe(entry.grade()));
        lore.add(" ");
        lore.add("&7등급 " + entry.grade().display() + " &8/ 최대 " + gear.capFor(entry.type()).display());
        Material material = switch (entry.type().rarity()) {
            case COMMON -> Material.WHITE_DYE;
            case RARE -> Material.BLUE_DYE;
            case EPIC -> Material.PURPLE_DYE;
            case LEGENDARY -> Material.ORANGE_DYE;
            case MASTER -> Material.RED_DYE;
        };
        return Items.button(material, 1, entry.type().rarity().color() + entry.type().tagName() + " "
                + entry.grade().display() + " &8(" + label + ")", lore);
    }

    private void select(Ticket ticket) {
        PlayerData data = plugin.data().get(player.getUniqueId());
        if (data.tickets(ticket) <= 0) {
            MenuUtil.error(player, ticket.displayName() + "이(가) 없습니다. 조합 메뉴에서 만들 수 있습니다.");
            return;
        }
        if (ticket == selected && !ticket.needsTarget()) {
            use(ticket, -1);
            return;
        }
        selected = ticket;
        MenuUtil.click(player);
        redraw();
    }

    private void target(int index) {
        if (selected == null || !selected.needsTarget()) {
            return;
        }
        use(selected, index);
    }

    private void use(Ticket ticket, int target) {
        if (MenuUtil.requireSameGear(player, gearId) == null) {
            return;
        }
        MenuUtil.show(player, plugin.actions().useTicket(player, gearId, ticket, target));
        if (plugin.data().get(player.getUniqueId()).tickets(ticket) <= 0) {
            selected = null;
        }
        redraw();
    }
}
