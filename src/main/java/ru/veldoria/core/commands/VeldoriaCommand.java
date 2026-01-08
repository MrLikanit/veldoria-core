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
import ru.veldoria.core.utils.ColorUtils;

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

        if (args[0].equalsIgnoreCase("reload")) {
            VeldoriaCore.getInstance().reloadConfig();
            VeldoriaCore.getInstance().loadMessages();

            sender.sendMessage(Component.text("Конфигурация перезагружена!", NamedTextColor.GREEN));
            return true;
        }

        if (args[0].equalsIgnoreCase("give")) {
            if (args.length < 2) {
                sender.sendMessage(Component.text("Укажите предмет!", NamedTextColor.RED));
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
                    player.sendMessage(ColorUtils.format("&aВыдана кирка."));
                    break;
                case "trap":
                    player.getInventory().addItem(VeldoriaItems.getPvpTrap());
                    player.sendMessage(ColorUtils.format("&aВыдана ловушка."));
                    break;
                case "random_scroll":
                    player.getInventory().addItem(VeldoriaItems.getRandomDisenchanter());
                    player.sendMessage(ColorUtils.format("&aВыдан свиток (Рандом)."));
                    break;
                case "select_scroll":
                    player.getInventory().addItem(VeldoriaItems.getSelectDisenchanter());
                    player.sendMessage(ColorUtils.format("&aВыдан свиток (Выбор)."));
                    break;
                case "clock":
                    player.getInventory().addItem(VeldoriaItems.getDeathClock());
                    player.sendMessage(ColorUtils.format("&aВыдана Хроносфера."));
                    break;
                case "prism":
                    player.getInventory().addItem(VeldoriaItems.getEmptyPrism());
                    player.sendMessage(ColorUtils.format("&aВыдана Призма Души."));
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
            return List.of("pickaxe", "trap", "random_scroll", "select_scroll", "clock", "prism");
        }

        return List.of();
    }
}