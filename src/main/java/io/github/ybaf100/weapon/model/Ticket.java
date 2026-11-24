package io.github.ybaf100.weapon.model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import org.bukkit.Material;

/** 조합으로 만드는 보조 아이템. 플레이어 데이터에 개수로 보관된다. */
public enum Ticket {
    CONFIRM_LOW("등급 확정 상승권 (B~A)", Group.EVOLUTION, Material.PAPER,
            "B→B+, B+→A-, A-→A 진화를 100% 성공시킵니다.", cost(ShardType.B, 10)),
    CONFIRM_HIGH("등급 확정 상승권 (A+~S+)", Group.EVOLUTION, Material.MAP,
            "A→A+, A+→S-, S-→S, S→S+ 진화를 100% 성공시킵니다.", cost(ShardType.A, 20, ShardType.S, 10)),
    PROTECT("파괴 방지권 (A~S+)", Group.EVOLUTION, Material.TOTEM_OF_UNDYING,
            "A 이상 진화에서 파괴가 나오면 유지로 바꿉니다.", cost(ShardType.A, 10, ShardType.S, 1)),
    ABILITY_UP("고유능력 일반 강화권", Group.ABILITY, Material.LAPIS_LAZULI,
            "고른 능력의 등급 +1 (성공 10%)", cost(ShardType.B, 5)),
    ABILITY_UP_HIGH("고유능력 고급 강화권", Group.ABILITY, Material.AMETHYST_SHARD,
            "고른 능력의 등급 +1 (성공 100%)", cost(ShardType.A, 8)),
    REROLL("고유능력 변경권", Group.ABILITY, Material.BOOK,
            "Master를 뺀 모든 능력을 다시 뽑습니다. (Legendary 미등장)", cost(ShardType.B, 15)),
    REROLL_LOCK("고유능력 고정 변경권", Group.ABILITY, Material.WRITABLE_BOOK,
            "고른 능력 1개를 고정하고 나머지를 다시 뽑습니다. (Legendary 미등장)", cost(ShardType.A, 5)),
    REROLL_SELECT("고유능력 선택 변경권", Group.ABILITY, Material.ENCHANTED_BOOK,
            "고른 능력 1개만 다시 뽑습니다. (Legendary 미등장)", cost(ShardType.S, 1)),
    REROLL_HIGH("고급 고유능력 변경권", Group.ABILITY, Material.KNOWLEDGE_BOOK,
            "Master를 뺀 모든 능력을 다시 뽑습니다. (A+ 이상은 Legendary 등장)", cost(ShardType.A, 15)),
    MASTER_CREATE("Master 고유능력 생성권", Group.ABILITY, Material.NETHER_STAR,
            "SP 무기의 Master 슬롯을 열고 '장인'을 부여합니다.", cost(ShardType.S, 5));

    public enum Group { EVOLUTION, ABILITY }

    private final String displayName;
    private final Group group;
    private final Material icon;
    private final String description;
    private final Map<ShardType, Integer> cost;

    Ticket(String displayName, Group group, Material icon, String description, Map<ShardType, Integer> cost) {
        this.displayName = displayName;
        this.group = group;
        this.icon = icon;
        this.description = description;
        this.cost = cost;
    }

    public String displayName() {
        return displayName;
    }

    public Group group() {
        return group;
    }

    public Material icon() {
        return icon;
    }

    public String description() {
        return description;
    }

    public Map<ShardType, Integer> cost() {
        return cost;
    }

    /** 대상 능력을 골라야 하는 아이템인지. */
    public boolean needsTarget() {
        return this == ABILITY_UP || this == ABILITY_UP_HIGH || this == REROLL_LOCK || this == REROLL_SELECT;
    }

    static Map<ShardType, Integer> cost(Object... pairs) {
        EnumMap<ShardType, Integer> map = new EnumMap<>(ShardType.class);
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((ShardType) pairs[i], (Integer) pairs[i + 1]);
        }
        return Collections.unmodifiableMap(map);
    }
}
