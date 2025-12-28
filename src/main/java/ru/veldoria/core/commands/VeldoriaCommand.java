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
import ru.veldoria.core.items.VeldoriaItems;

import java.util.List;

public class VeldoriaCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("veldoria.admin")) {
            // Для игрока без прав команды как бы не существует
            sender.sendMessage(Component.text("Unknown command. Type \"/help\" for help.", NamedTextColor.WHITE));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Использование: /veldoriacore give <item>", NamedTextColor.RED));
            return true;
        }

        if (args[0].equalsIgnoreCase("give")) {
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
        // Если нет прав — не показываем ничего
        if (!sender.hasPermission("veldoria.admin")) {
            return List.of();
        }

        if (args.length == 1) {
            return List.of("give");
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return List.of("pickaxe");
        }

        return List.of();
    }
}