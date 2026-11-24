package io.github.ybaf100.weapon.logic;

import io.github.ybaf100.weapon.model.ShardType;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;
import org.bukkit.Material;

/** 뽑기 확률과 상품. 확률은 1000분율. */
public final class Gacha {

    public enum Kind {
        COMMON("일반", table(ShardType.D, 540, ShardType.C, 320, ShardType.B, 130, ShardType.A, 10)),
        EPIC("&c&l고급&r", table(ShardType.B, 819, ShardType.A, 176, ShardType.S, 5)),
        MILEAGE("&d마일리지&r", table(ShardType.A, 800, ShardType.S, 190, ShardType.SP, 10));

        private final String label;
        private final Map<ShardType, Integer> weights;

        Kind(String label, Map<ShardType, Integer> weights) {
            this.label = label;
            this.weights = weights;
            int total = weights.values().stream().mapToInt(Integer::intValue).sum();
            if (total != 1000) {
                throw new IllegalStateException(name() + " weights sum to " + total);
            }
        }

        public String label() {
            return label;
        }

        public Map<ShardType, Integer> weights() {
            return weights;
        }

        public ShardType roll(Random random) {
            int r = random.nextInt(1000);
            for (Map.Entry<ShardType, Integer> entry : weights.entrySet()) {
                r -= entry.getValue();
                if (r < 0) {
                    return entry.getKey();
                }
            }
            throw new IllegalStateException("unreachable");
        }
    }

    public enum Product {
        COMMON_1("&7일반 뽑기", Kind.COMMON, Material.IRON_INGOT, 1, 5_000, 0, 1, 5),
        COMMON_11("&7일반 뽑기 &c10&d+1", Kind.COMMON, Material.IRON_INGOT, 10, 50_000, 0, 11, 50),
        EPIC_1("&6고급 뽑기", Kind.EPIC, Material.GOLD_INGOT, 1, 50_000, 0, 1, 50),
        EPIC_11("&6고급 뽑기 &c10&d+1", Kind.EPIC, Material.GOLD_INGOT, 10, 500_000, 0, 11, 500),
        EPIC_28("&6고급 뽑기 &c25&d+3", Kind.EPIC, Material.GOLD_INGOT, 25, 1_250_000, 0, 28, 1_250),
        EPIC_56("&6고급 뽑기 &c50&d+6", Kind.EPIC, Material.GOLD_INGOT, 50, 2_500_000, 0, 56, 2_500),
        MILEAGE_2("&d&l마일리지 뽑기", Kind.MILEAGE, Material.DIAMOND, 1, 0, 5_000, 2, 0);

        private final String displayName;
        private final Kind kind;
        private final Material icon;
        private final int iconAmount;
        private final long price;
        private final long mileageCost;
        private final int count;
        private final long mileageGain;

        Product(String displayName, Kind kind, Material icon, int iconAmount, long price, long mileageCost,
                int count, long mileageGain) {
            this.displayName = displayName;
            this.kind = kind;
            this.icon = icon;
            this.iconAmount = iconAmount;
            this.price = price;
            this.mileageCost = mileageCost;
            this.count = count;
            this.mileageGain = mileageGain;
        }

        public String displayName() {
            return displayName;
        }

        public Kind kind() {
            return kind;
        }

        public Material icon() {
            return icon;
        }

        public int iconAmount() {
            return iconAmount;
        }

        public long price() {
            return price;
        }

        public long mileageCost() {
            return mileageCost;
        }

        public int count() {
            return count;
        }

        public long mileageGain() {
            return mileageGain;
        }
    }

    private Gacha() {
    }

    private static Map<ShardType, Integer> table(Object... pairs) {
        EnumMap<ShardType, Integer> map = new EnumMap<>(ShardType.class);
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((ShardType) pairs[i], (Integer) pairs[i + 1]);
        }
        return Collections.unmodifiableMap(map);
    }
}
