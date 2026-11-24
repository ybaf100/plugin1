package io.github.ybaf100.weapon.util;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

/** & 색 코드 문자열을 Adventure 컴포넌트로 바꾸는 도우미. */
public final class Text {
    public static final String PREFIX = "&6[무기] &r";
    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    private Text() {
    }

    /** 아이템 이름과 로어용. 기본 기울임을 끈다. */
    public static Component c(String legacy) {
        return SERIALIZER.deserialize(legacy).decoration(TextDecoration.ITALIC, false);
    }

    public static List<Component> lines(List<String> legacy) {
        List<Component> result = new ArrayList<>(legacy.size());
        for (String line : legacy) {
            result.add(c(line));
        }
        return result;
    }

    public static void send(CommandSender sender, String legacy) {
        sender.sendMessage(SERIALIZER.deserialize(PREFIX + legacy));
    }

    public static void broadcast(String legacy) {
        Bukkit.broadcast(SERIALIZER.deserialize(PREFIX + legacy));
    }
}
