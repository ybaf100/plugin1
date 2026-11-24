package io.github.ybaf100.weapon.compat;

import io.github.ybaf100.weapon.WeaponPlugin;
import io.github.ybaf100.weapon.WeaponUi;
import io.github.ybaf100.weapon.dialog.DialogUi;
import io.github.ybaf100.weapon.model.GearType;
import java.util.Locale;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.potion.PotionEffectType;

/** Paper 26.3 API 이름. shared 코드는 버전마다 다른 이름을 이 클래스로만 쓴다. */
public final class Compat {
    private static final String NAMESPACE = "weaponsystem";
    private static final Set<PotionEffectType> NEGATIVE = Set.of(
            PotionEffectType.POISON, PotionEffectType.WITHER, PotionEffectType.SLOWNESS, PotionEffectType.MINING_FATIGUE,
            PotionEffectType.WEAKNESS, PotionEffectType.BLINDNESS, PotionEffectType.NAUSEA, PotionEffectType.HUNGER,
            PotionEffectType.LEVITATION, PotionEffectType.DARKNESS);

    private Compat() {
    }

    public static WeaponUi createUi(WeaponPlugin plugin) {
        return new DialogUi(plugin);
    }

    public static Material material(String name) {
        return Material.valueOf(name);
    }

    public static Attribute maxHealth() {
        return Attribute.MAX_HEALTH;
    }

    public static Attribute attackDamage() {
        return Attribute.ATTACK_DAMAGE;
    }

    public static Attribute attackSpeed() {
        return Attribute.ATTACK_SPEED;
    }

    public static Attribute armor() {
        return Attribute.ARMOR;
    }

    public static Attribute armorToughness() {
        return Attribute.ARMOR_TOUGHNESS;
    }

    public static Attribute movementSpeed() {
        return Attribute.MOVEMENT_SPEED;
    }

    public static Attribute knockbackResistance() {
        return Attribute.KNOCKBACK_RESISTANCE;
    }

    /** 장비 아이템에 붙이는 속성 (그 칸에 들거나 입었을 때만 적용). */
    public static AttributeModifier itemModifier(String id, double amount, GearType.Slot slot) {
        return new AttributeModifier(key(id), amount, AttributeModifier.Operation.ADD_NUMBER, slot(slot));
    }

    /** 플레이어에게 직접 붙이는 속성. */
    public static AttributeModifier playerModifier(String id, double amount, AttributeModifier.Operation operation) {
        return new AttributeModifier(key(id), amount, operation);
    }

    public static boolean isModifier(AttributeModifier modifier, String id) {
        return modifier.getKey().equals(key(id));
    }

    public static Enchantment efficiency() {
        return Enchantment.EFFICIENCY;
    }

    public static Particle critParticle() {
        return Particle.ENCHANTED_HIT;
    }

    public static Particle enchantParticle() {
        return Particle.ENCHANT;
    }

    public static Particle totemParticle() {
        return Particle.TOTEM_OF_UNDYING;
    }

    public static Set<PotionEffectType> negativeEffects() {
        return NEGATIVE;
    }

    public static void hurtEffect(LivingEntity entity) {
        entity.playHurtAnimation(0);
    }

    private static EquipmentSlotGroup slot(GearType.Slot slot) {
        return switch (slot) {
            case HAND -> EquipmentSlotGroup.MAINHAND;
            case HEAD -> EquipmentSlotGroup.HEAD;
            case CHEST -> EquipmentSlotGroup.CHEST;
            case LEGS -> EquipmentSlotGroup.LEGS;
            case FEET -> EquipmentSlotGroup.FEET;
        };
    }

    private static NamespacedKey key(String id) {
        return new NamespacedKey(NAMESPACE, id.toLowerCase(Locale.ROOT));
    }
}
