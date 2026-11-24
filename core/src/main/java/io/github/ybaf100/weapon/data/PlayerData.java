package io.github.ybaf100.weapon.data;

import io.github.ybaf100.weapon.model.ShardType;
import io.github.ybaf100.weapon.model.Ticket;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** 플레이어 한 명의 파편, 마일리지, 보조 아이템, 쿨타임. */
public final class PlayerData {
    private final UUID uuid;
    private final EnumMap<ShardType, Integer> shards = new EnumMap<>(ShardType.class);
    private final EnumMap<Ticket, Integer> tickets = new EnumMap<>(Ticket.class);
    private final Map<String, Long> cooldowns = new HashMap<>();
    private long mileage;
    private boolean dirty;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID uuid() {
        return uuid;
    }

    public int shards(ShardType type) {
        return shards.getOrDefault(type, 0);
    }

    public void setShards(ShardType type, int amount) {
        shards.put(type, Math.max(0, Math.min(ShardType.MAX_AMOUNT, amount)));
        dirty = true;
    }

    /**
     * 파편을 더한다. 최대 보유량을 넘친 만큼은 마일리지로 바꾼다.
     *
     * @return 넘쳐서 받은 마일리지
     */
    public long addShards(ShardType type, int amount) {
        int current = shards(type);
        int room = ShardType.MAX_AMOUNT - current;
        int added = Math.min(room, amount);
        int overflow = amount - added;
        shards.put(type, current + added);
        long gained = (long) overflow * type.overflowMileage();
        mileage += gained;
        dirty = true;
        return gained;
    }

    public boolean hasShards(Map<ShardType, Integer> cost) {
        for (Map.Entry<ShardType, Integer> entry : cost.entrySet()) {
            if (shards(entry.getKey()) < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    public boolean takeShards(Map<ShardType, Integer> cost) {
        if (!hasShards(cost)) {
            return false;
        }
        for (Map.Entry<ShardType, Integer> entry : cost.entrySet()) {
            shards.put(entry.getKey(), shards(entry.getKey()) - entry.getValue());
        }
        dirty = true;
        return true;
    }

    public long mileage() {
        return mileage;
    }

    public void setMileage(long mileage) {
        this.mileage = Math.max(0, mileage);
        dirty = true;
    }

    public void addMileage(long amount) {
        setMileage(mileage + amount);
    }

    public int tickets(Ticket ticket) {
        return tickets.getOrDefault(ticket, 0);
    }

    public void setTickets(Ticket ticket, int amount) {
        tickets.put(ticket, Math.max(0, amount));
        dirty = true;
    }

    public void addTickets(Ticket ticket, int amount) {
        setTickets(ticket, tickets(ticket) + amount);
    }

    public boolean takeTicket(Ticket ticket) {
        if (tickets(ticket) <= 0) {
            return false;
        }
        setTickets(ticket, tickets(ticket) - 1);
        return true;
    }

    /** 쿨타임이 끝나는 시각 (epoch ms). */
    public long cooldownUntil(String key) {
        return cooldowns.getOrDefault(key, 0L);
    }

    public void setCooldown(String key, long untilMillis) {
        cooldowns.put(key, untilMillis);
        dirty = true;
    }

    public Map<ShardType, Integer> shardMap() {
        return shards;
    }

    public Map<Ticket, Integer> ticketMap() {
        return tickets;
    }

    public Map<String, Long> cooldownMap() {
        return cooldowns;
    }

    public void reset() {
        shards.clear();
        tickets.clear();
        mileage = 0;
        dirty = true;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void markClean() {
        dirty = false;
    }
}
