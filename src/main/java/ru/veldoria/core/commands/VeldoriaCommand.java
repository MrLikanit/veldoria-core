package ru.veldoria.core.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.veldoria.core.VeldoriaCore;
import ru.veldoria.core.items.VeldoriaItems;

import java.util.List;

public class VeldoriaCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("veldoria.admin")) {
            sender.sendMessage(Component.text("Unknown command. Type \"/help\" for help.", NamedTextColor.WHITE));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(Component.text("Использование: /veldoriacore <give|reload>", NamedTextColor.RED));
            return true;
        }

        // --- КОМАНДА RELOAD ---
        if (args[0].equalsIgnoreCase("reload")) {
            VeldoriaCore.getInstance().reloadConfig();
            sender.sendMessage(Component.text("Конфигурация VeldoriaCore перезагружена!", NamedTextColor.GREEN));
            return true;
        }

        // --- КОМАНДА GIVE ---
        if (args[0].equalsIgnoreCase("give")) {
            if (args.length < 2) {
                sender.sendMessage(Component.text("Укажите предмет: /veldoriacore give pickaxe", NamedTextColor.RED));
                return true;
            }

            if (!(sender instanceof Player player)) {
                sender.sendMessage("Только для игроков.");
                return true;
            }

            String itemType = args[1].toLowerCase();
            switch (itemType) {
                case "pickaxe":
                    player.getInventory().addItem(VeldoriaItems.getSpawnerPickaxe());
                    player.sendMessage(Component.text("Вы получили Экстрактор.", NamedTextColor.GREEN));
                    break;
                default:
                    player.sendMessage(Component.text("Предмет не найден: " + itemType, NamedTextColor.RED));
            }
            return true;
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("veldoria.admin")) {
            return List.of();
        }

        if (args.length == 1) {
            return List.of("give", "reload");
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return List.of("pickaxe");
        }

        return List.of();
    }
}