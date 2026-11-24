package io.github.ybaf100.weapon.compat;

import io.github.ybaf100.weapon.WeaponPlugin;
import io.github.ybaf100.weapon.WeaponUi;
import io.github.ybaf100.weapon.gui.ChestUi;
import io.github.ybaf100.weapon.model.GearType;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;
import org.bukkit.EntityEffect;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffectType;

/** Paper 1.19.4 API 이름. shared 코드는 버전마다 다른 이름을 이 클래스로만 쓴다. */
public final class Compat {
    private static final Set<PotionEffectType> NEGATIVE = Set.of(
            PotionEffectType.POISON, PotionEffectType.WITHER, PotionEffectType.SLOW, PotionEffectType.SLOW_DIGGING,
            PotionEffectType.WEAKNESS, PotionEffectType.BLINDNESS, PotionEffectType.CONFUSION, PotionEffectType.HUNGER,
            PotionEffectType.LEVITATION, PotionEffectType.DARKNESS);

    private Compat() {
    }

    public static WeaponUi createUi(WeaponPlugin plugin) {
        return new ChestUi(plugin);
    }

    public static Material material(String name) {
        return Material.valueOf(name);
    }

    public static Attribute maxHealth() {
        return Attribute.GENERIC_MAX_HEALTH;
    }

    public static Attribute attackDamage() {
        return Attribute.GENERIC_ATTACK_DAMAGE;
    }

    public static Attribute attackSpeed() {
        return Attribute.GENERIC_ATTACK_SPEED;
    }

    public static Attribute armor() {
        return Attribute.GENERIC_ARMOR;
    }

    public static Attribute armorToughness() {
        return Attribute.GENERIC_ARMOR_TOUGHNESS;
    }

    public static Attribute movementSpeed() {
        return Attribute.GENERIC_MOVEMENT_SPEED;
    }

    public static Attribute knockbackResistance() {
        return Attribute.GENERIC_KNOCKBACK_RESISTANCE;
    }

    /** 장비 아이템에 붙이는 속성 (그 칸에 들거나 입었을 때만 적용). */
    public static AttributeModifier itemModifier(String id, double amount, GearType.Slot slot) {
        return new AttributeModifier(uuid(id), "weaponsystem." + id, amount, AttributeModifier.Operation.ADD_NUMBER, slot(slot));
    }

    /** 플레이어에게 직접 붙이는 속성. */
    public static AttributeModifier playerModifier(String id, double amount, AttributeModifier.Operation operation) {
        return new AttributeModifier(uuid(id), "weaponsystem." + id, amount, operation);
    }

    public static boolean isModifier(AttributeModifier modifier, String id) {
        return modifier.getUniqueId().equals(uuid(id));
    }

    public static Enchantment efficiency() {
        return Enchantment.DIG_SPEED;
    }

    public static Particle critParticle() {
        return Particle.CRIT_MAGIC;
    }

    public static Particle enchantParticle() {
        return Particle.ENCHANTMENT_TABLE;
    }

    public static Particle totemParticle() {
        return Particle.TOTEM;
    }

    public static Set<PotionEffectType> negativeEffects() {
        return NEGATIVE;
    }

    @SuppressWarnings("deprecation")
    public static void hurtEffect(LivingEntity entity) {
        entity.playEffect(EntityEffect.HURT);
    }

    private static EquipmentSlot slot(GearType.Slot slot) {
        return switch (slot) {
            case HAND -> EquipmentSlot.HAND;
            case HEAD -> EquipmentSlot.HEAD;
            case CHEST -> EquipmentSlot.CHEST;
            case LEGS -> EquipmentSlot.LEGS;
            case FEET -> EquipmentSlot.FEET;
        };
    }

    private static UUID uuid(String id) {
        return UUID.nameUUIDFromBytes(("weaponsystem:" + id).getBytes(StandardCharsets.UTF_8));
    }
}
