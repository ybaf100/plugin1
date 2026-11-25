package io.github.ybaf100.weapon.fx;

import io.github.ybaf100.weapon.WeaponPlugin;
import io.github.ybaf100.weapon.compat.Compat;
import io.github.ybaf100.weapon.logic.Gacha.Product;
import io.github.ybaf100.weapon.model.ShardType;
import io.github.ybaf100.weapon.service.WeaponActions;
import io.github.ybaf100.weapon.service.WeaponActions.GachaRoll;
import io.github.ybaf100.weapon.util.Text;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * 뽑기 월드 연출 (후보 A · 소용돌이).
 * 결과 파편이 플레이어 발밑 둘레에서 떠올라 회전하며 나선을 그리고 가슴으로 빨려 들어온다.
 * 파편은 아이템 디스플레이 엔티티라 주울 수 없고, 저장되지 않으며, 닿으면 사라진다.
 * 웅크리기로 건너뛸 수 있다. 결과는 이미 정해져 저장된 상태다.
 */
public final class GachaFlyIn implements Listener {
    private static final int MAX_SHOWN = 12;
    private static final int NORMAL_DURATION = 30;
    private static final int SPECIAL_DURATION = 38;
    private static final double CHEST_HEIGHT = 1.15;
    private static final int FINISH_DELAY = 6;

    private final WeaponPlugin plugin;
    private final NamespacedKey fxKey;
    private final Map<UUID, Session> sessions = new HashMap<>();

    public GachaFlyIn(WeaponPlugin plugin) {
        this.plugin = plugin;
        this.fxKey = new NamespacedKey(plugin, "fx");
    }

    /**
     * 연출을 시작한다. 끝나거나 건너뛰면 onDone(건너뛰었는지)를 한 번 부른다.
     * 플레이어가 나가면 부르지 않는다.
     */
    public void play(Player player, Product product, List<GachaRoll> rolls, Consumer<Boolean> onDone) {
        stop(player.getUniqueId(), false);
        Session session = new Session(player, product, schedule(pick(rolls)), onDone);
        sessions.put(player.getUniqueId(), session);
        session.task = Bukkit.getScheduler().runTaskTimer(plugin, session::tick, 1L, 1L);
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 0.6f);
    }

    public boolean isPlaying(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    public void stopAll() {
        for (UUID id : new ArrayList<>(sessions.keySet())) {
            stop(id, false);
        }
    }

    /** @param skipped true면 onDone(true)를 부른다 (건너뛰기). false면 부르지 않고 정리만 한다. */
    private void stop(UUID id, boolean skipped) {
        Session session = sessions.remove(id);
        if (session == null) {
            return;
        }
        session.cleanup();
        if (skipped) {
            session.onDone.accept(true);
        }
    }

    // ------------------------------------------------------------------ 시간표

    /** 많이 뽑으면 가장 높은 등급을 포함해 12개만 날린다. */
    static List<ShardType> pick(List<GachaRoll> rolls) {
        List<ShardType> all = new ArrayList<>();
        for (GachaRoll roll : rolls) {
            all.add(roll.type());
        }
        if (all.size() <= MAX_SHOWN) {
            return all;
        }
        ShardType best = all.get(0);
        for (ShardType type : all) {
            if (type.ordinal() > best.ordinal()) {
                best = type;
            }
        }
        List<ShardType> rest = new ArrayList<>(all);
        rest.remove(best);
        List<ShardType> out = new ArrayList<>();
        for (int i = 0; i < MAX_SHOWN - 1; i++) {
            out.add(rest.get(i * rest.size() / (MAX_SHOWN - 1)));
        }
        out.add(best);
        return out;
    }

    private record Piece(ShardType type, int delay, int duration, double startAngle, double startRadius, double turns, float scale) {
        boolean special() {
            return type.ordinal() >= ShardType.S.ordinal();
        }

        int end() {
            return delay + duration;
        }
    }

    static List<Piece> schedule(List<ShardType> shown) {
        List<ShardType> normal = new ArrayList<>();
        List<ShardType> special = new ArrayList<>();
        for (ShardType type : shown) {
            (type.ordinal() >= ShardType.S.ordinal() ? special : normal).add(type);
        }
        List<Piece> pieces = new ArrayList<>();
        int n = Math.max(1, normal.size());
        int last = 0;
        for (int i = 0; i < normal.size(); i++) {
            ShardType type = normal.get(i);
            Piece piece = new Piece(type, (int) Math.round(i * 1.7), NORMAL_DURATION, Math.PI * 2 * i / n + 0.4, 3.2, 3.2,
                    type == ShardType.A ? 0.6f : 0.5f);
            pieces.add(piece);
            last = Math.max(last, piece.end());
        }
        for (int j = 0; j < special.size(); j++) {
            pieces.add(new Piece(special.get(j), Math.max(0, last - 10) + j * 12, SPECIAL_DURATION, 1.2 + j, 3.8, 4.2, 0.8f));
        }
        return pieces;
    }

    private static double easeInOut(double p) {
        return p < 0.5 ? 2 * p * p : 1 - Math.pow(-2 * p + 2, 2) / 2;
    }

    /** 진행도 p(0~1)일 때 플레이어 발 기준 상대 위치. */
    private static Vector3f offset(Piece piece, double p) {
        double e = easeInOut(p);
        double r = piece.startRadius() * Math.pow(1 - e, 1.15);
        double a = piece.startAngle() + e * Math.PI * piece.turns();
        double y = e * CHEST_HEIGHT + Math.sin(e * Math.PI) * 1.4;
        return new Vector3f((float) (r * Math.cos(a)), (float) y, (float) (r * Math.sin(a)));
    }

    // ------------------------------------------------------------------ 진행

    private final class Session {
        final Player player;
        final Product product;
        final List<Piece> pieces;
        final Consumer<Boolean> onDone;
        final ItemDisplay[] displays;
        final boolean[] absorbed;
        final int end;
        int tick;
        BukkitTask task;

        Session(Player player, Product product, List<Piece> pieces, Consumer<Boolean> onDone) {
            this.player = player;
            this.product = product;
            this.pieces = pieces;
            this.onDone = onDone;
            this.displays = new ItemDisplay[pieces.size()];
            this.absorbed = new boolean[pieces.size()];
            int max = 0;
            for (Piece piece : pieces) {
                max = Math.max(max, piece.end());
            }
            this.end = max;
        }

        void tick() {
            if (!player.isOnline() || player.isDead()) {
                stop(player.getUniqueId(), false);
                return;
            }
            Location anchor = player.getLocation();
            anchor.setYaw(0);
            anchor.setPitch(0);
            for (int i = 0; i < pieces.size(); i++) {
                Piece piece = pieces.get(i);
                if (absorbed[i] || tick < piece.delay()) {
                    continue;
                }
                int age = tick - piece.delay();
                if (age >= piece.duration()) {
                    absorb(i, piece);
                    continue;
                }
                double p = (double) age / piece.duration();
                Vector3f pos = offset(piece, p);
                float pop = Math.min(1f, (age + 1) / 3f);
                float spin = (float) ((tick + i * 3) * 0.35);
                ItemDisplay display = displays[i];
                if (display == null || !display.isValid()) {
                    display = spawn(anchor, piece);
                    displays[i] = display;
                } else {
                    display.teleport(anchor);
                }
                display.setInterpolationDelay(0);
                display.setInterpolationDuration(age == 0 ? 0 : 1);
                display.setTransformation(new Transformation(pos,
                        new Quaternionf(new AxisAngle4f(spin, 0, 1, 0)),
                        new Vector3f(piece.scale() * pop),
                        new Quaternionf()));
                if (piece.special() && tick % 2 == 0) {
                    Location trail = anchor.clone().add(pos.x(), pos.y(), pos.z());
                    trail.getWorld().spawnParticle(Particle.END_ROD, trail, 1, 0.05, 0.05, 0.05, 0);
                }
            }
            tick++;
            if (tick >= end + FINISH_DELAY) {
                sessions.remove(player.getUniqueId());
                cleanup();
                onDone.accept(false);
            }
        }

        ItemDisplay spawn(Location anchor, Piece piece) {
            ItemStack item = new ItemStack(Compat.material(piece.type().material()));
            return anchor.getWorld().spawn(anchor, ItemDisplay.class, display -> {
                display.setPersistent(false);
                display.setItemStack(item);
                display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.GROUND);
                display.setBillboard(Display.Billboard.FIXED);
                display.setBrightness(new Display.Brightness(15, 15));
                display.setTransformation(new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(0.01f), new Quaternionf()));
                if (piece.type().ordinal() >= ShardType.A.ordinal()) {
                    display.setGlowing(true);
                    display.setGlowColorOverride(glowColor(piece.type()));
                }
                display.getPersistentDataContainer().set(fxKey, PersistentDataType.BYTE, (byte) 1);
            });
        }

        void absorb(int index, Piece piece) {
            absorbed[index] = true;
            if (displays[index] != null) {
                displays[index].remove();
                displays[index] = null;
            }
            ShardType type = piece.type();
            Location chest = player.getLocation().add(0, CHEST_HEIGHT, 0);
            World world = chest.getWorld();
            WeaponActions.playGachaSound(player, type);
            switch (type) {
                case D, C, B -> world.spawnParticle(Particle.CRIT, chest, 6, 0.2, 0.2, 0.2, 0.15);
                case A -> world.spawnParticle(Compat.critParticle(), chest, 14, 0.25, 0.3, 0.25, 0.2);
                case S, SP -> celebrate(type, chest);
            }
        }

        void celebrate(ShardType type, Location chest) {
            World world = chest.getWorld();
            world.spawnParticle(Compat.totemParticle(), chest, 50, 0.4, 0.6, 0.4, 0.35);
            if (type == ShardType.SP) {
                for (int k = 0; k < 36; k++) {
                    double a = k * Math.PI / 9;
                    world.spawnParticle(Particle.END_ROD, chest.clone().add(Math.cos(a) * 1.2, k * 0.06 - 0.6, Math.sin(a) * 1.2),
                            1, 0, 0, 0, 0.02);
                }
                world.playSound(chest, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.35f, 1.2f);
            }
            firework(chest, type == ShardType.SP);
            player.showTitle(Title.title(Text.c(type.displayName() + "!"),
                    Text.c("&7" + product.displayName() + "&7에서 획득"),
                    Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(1500), Duration.ofMillis(400))));
        }

        void firework(Location at, boolean large) {
            Firework firework = at.getWorld().spawn(at.clone().add(0, 0.6, 0), Firework.class, fw -> {
                fw.setPersistent(false);
                fw.getPersistentDataContainer().set(fxKey, PersistentDataType.BYTE, (byte) 1);
                FireworkMeta meta = fw.getFireworkMeta();
                meta.addEffect(FireworkEffect.builder()
                        .with(large ? FireworkEffect.Type.BALL_LARGE : FireworkEffect.Type.BURST)
                        .withColor(large ? Color.fromRGB(0x55FFFF) : Color.fromRGB(0xFFB733))
                        .withFade(Color.WHITE)
                        .trail(true)
                        .flicker(large)
                        .build());
                fw.setFireworkMeta(meta);
            });
            firework.detonate();
        }

        void cleanup() {
            if (task != null) {
                task.cancel();
            }
            for (ItemDisplay display : displays) {
                if (display != null && display.isValid()) {
                    display.remove();
                }
            }
        }
    }

    private static Color glowColor(ShardType type) {
        return switch (type) {
            case A -> Color.fromRGB(0xFF55FF);
            case S -> Color.fromRGB(0xFFAA00);
            case SP -> Color.fromRGB(0x55FFFF);
            default -> Color.WHITE;
        };
    }

    // ------------------------------------------------------------------ 이벤트

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        if (event.isSneaking() && sessions.containsKey(event.getPlayer().getUniqueId())) {
            stop(event.getPlayer().getUniqueId(), true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        stop(event.getPlayer().getUniqueId(), false);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        stop(event.getEntity().getUniqueId(), false);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        stop(event.getPlayer().getUniqueId(), true);
    }

    /** 연출용 폭죽은 피해를 주지 않는다. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onFireworkDamage(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        if (damager instanceof Firework && damager.getPersistentDataContainer().has(fxKey, PersistentDataType.BYTE)) {
            event.setCancelled(true);
        }
    }
}
