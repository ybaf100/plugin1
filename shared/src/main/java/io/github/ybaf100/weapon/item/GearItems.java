package io.github.ybaf100.weapon.item;

import io.github.ybaf100.weapon.compat.Compat;
import io.github.ybaf100.weapon.logic.Enhancement;
import io.github.ybaf100.weapon.model.AbilityEntry;
import io.github.ybaf100.weapon.model.GearData;
import io.github.ybaf100.weapon.model.GearType;
import io.github.ybaf100.weapon.model.Grade;
import io.github.ybaf100.weapon.util.Fmt;
import io.github.ybaf100.weapon.util.Text;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/** 시스템 장비 아이템 읽기, 쓰기, 표시. */
public final class GearItems {
    private static NamespacedKey key;

    private GearItems() {
    }

    public static void init(Plugin plugin) {
        key = new NamespacedKey(plugin, "gear");
    }

    public static GearData read(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            return null;
        }
        String raw = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
        return GearData.deserialize(raw);
    }

    public static boolean isGear(ItemStack item) {
        return read(item) != null;
    }

    /** 바닐라 재료로 쓸 수 있는 순수한 다이아 장비인지 (시스템 데이터와 이름이 없음). */
    public static boolean isPlain(ItemStack item, Material material) {
        if (item == null || item.getType() != material) {
            return false;
        }
        if (!item.hasItemMeta()) {
            return true;
        }
        ItemMeta meta = item.getItemMeta();
        return !meta.getPersistentDataContainer().has(key, PersistentDataType.STRING) && !meta.hasDisplayName();
    }

    public static ItemStack create(GearData data) {
        ItemStack item = new ItemStack(Compat.material(data.type().material()));
        write(item, data);
        return item;
    }

    /** 데이터를 아이템에 저장하고 이름, 로어, 속성을 다시 그린다. */
    public static void write(ItemStack item, GearData data) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, data.serialize());
        meta.displayName(Text.c(name(data)));
        meta.lore(Text.lines(lore(data)));
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
        applyAttributes(meta, data);
        item.setItemMeta(meta);
    }

    public static String name(GearData data) {
        return data.grade().display() + "급 " + data.type().koreanName();
    }

    private static List<String> lore(GearData data) {
        List<String> lore = new ArrayList<>();
        GearType type = data.type();
        Grade grade = data.grade();
        double mult = Enhancement.multiplier(data.enhance());
        lore.add(" ");
        lore.add("&f등급 : " + grade.starsDisplay() + " " + grade.display());
        StringBuilder bar = new StringBuilder("&f강화 : &e");
        for (int i = 1; i <= GearData.MAX_ENHANCE; i++) {
            if (i == data.enhance() + 1) {
                bar.append("&8");
            }
            bar.append(i <= data.enhance() ? "●" : "○");
        }
        bar.append(" &7(").append(data.enhance()).append("강, ").append(Fmt.num(mult * 100)).append("%)");
        lore.add(bar.toString());
        switch (type.category()) {
            case WEAPON -> lore.add("&7공격력 : &f" + Fmt.num(type.baseDamage() * mult));
            case ARMOR -> lore.add("&7방어력 : &f" + Fmt.num(type.baseArmor() * mult)
                    + " &8/ &7방어 강도 : &f" + Fmt.num(type.baseToughness() * mult));
            case TOOL -> lore.add("&7효율 : &f" + Math.max(0, data.enhance() - 1));
        }
        if (type.category() == GearType.Category.TOOL) {
            return lore;
        }
        lore.add(" ");
        lore.add("&f고유능력");
        int slots = data.abilitySlots();
        if (slots == 0) {
            lore.add("&8 • B급부터 해금");
        }
        for (AbilityEntry entry : data.abilities()) {
            lore.add(abilityLine(entry));
            lore.add("&8    " + entry.type().describe(entry.grade()));
        }
        for (int i = data.abilities().size(); i < slots; i++) {
            lore.add("&8 • (비어 있음)");
        }
        if (type.hasMasterSlot(grade)) {
            if (data.master() == null) {
                lore.add("&8 • Master: 생성권으로 해금");
            } else {
                lore.add(abilityLine(data.master()));
                lore.add("&8    " + data.master().type().describe(data.master().grade()));
            }
        }
        return lore;
    }

    public static String abilityLine(AbilityEntry entry) {
        return "&7 • " + entry.type().rarity().color() + entry.type().tagName() + " " + entry.grade().display()
                + " &8(" + entry.type().rarity().label() + ")";
    }

    private static void applyAttributes(ItemMeta meta, GearData data) {
        GearType type = data.type();
        double mult = Enhancement.multiplier(data.enhance());
        for (Attribute attribute : new Attribute[]{Compat.attackDamage(), Compat.attackSpeed(),
                Compat.armor(), Compat.armorToughness()}) {
            meta.removeAttributeModifier(attribute);
        }
        meta.removeEnchant(Compat.efficiency());
        switch (type.category()) {
            case WEAPON -> {
                // 플레이어 기본 공격력 1, 기본 공격 속도 4를 뺀 값.
                meta.addAttributeModifier(Compat.attackDamage(),
                        modifier(type, "damage", type.baseDamage() * mult - 1.0));
                meta.addAttributeModifier(Compat.attackSpeed(),
                        modifier(type, "speed", type.attackSpeed() - 4.0));
            }
            case ARMOR -> {
                meta.addAttributeModifier(Compat.armor(), modifier(type, "armor", type.baseArmor() * mult));
                meta.addAttributeModifier(Compat.armorToughness(),
                        modifier(type, "toughness", type.baseToughness() * mult));
            }
            case TOOL -> {
                // 곡괭이는 바닐라 공격력을 그대로 쓰기 위해 기본값을 다시 넣는다.
                meta.addAttributeModifier(Compat.attackDamage(), modifier(type, "damage", 4.0));
                meta.addAttributeModifier(Compat.attackSpeed(), modifier(type, "speed", 1.2 - 4.0));
                int efficiency = data.enhance() - 1;
                if (efficiency > 0) {
                    meta.addEnchant(Compat.efficiency(), efficiency, true);
                }
            }
        }
    }

    private static AttributeModifier modifier(GearType type, String name, double amount) {
        return Compat.itemModifier(type.name().toLowerCase() + "." + name, amount, type.slot());
    }
}
