package ru.veldoria.core.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import ru.veldoria.core.VeldoriaCore;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtils {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    public static Component format(String text) {
        if (text == null) return Component.empty();

        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuilder buffer = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, "&x"
                    + "&" + matcher.group(1).charAt(0)
                    + "&" + matcher.group(1).charAt(1)
                    + "&" + matcher.group(1).charAt(2)
                    + "&" + matcher.group(1).charAt(3)
                    + "&" + matcher.group(1).charAt(4)
                    + "&" + matcher.group(1).charAt(5));
        }
        matcher.appendTail(buffer);
        text = buffer.toString();

        Component component;
        if (text.contains("<") && text.contains(">")) {
            component = MiniMessage.miniMessage().deserialize(text);
        } else {
            component = LegacyComponentSerializer.legacyAmpersand().deserialize(text);
        }

        return component.decoration(TextDecoration.ITALIC, false);
    }

    public static Component getMsg(String path) {
        String raw = VeldoriaCore.getInstance().getMessages().getString(path, "&cMessage not found: " + path);
        return format(raw);
    }

    public static Component getMsg(String path, String placeholder, String value) {
        String raw = VeldoriaCore.getInstance().getMessages().getString(path, "&cMessage not found: " + path);
        return format(raw.replace(placeholder, value));
    }

    public static void sendActionBar(Player player, String configPath) {
        player.sendActionBar(getMsg(configPath));
    }

    public static void sendActionBar(Player player, Component message) {
        player.sendActionBar(message);
    }
}