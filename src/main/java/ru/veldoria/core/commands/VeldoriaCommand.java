package ru.veldoria.core.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.veldoria.core.VeldoriaCore;

import java.util.List;

public class VeldoriaCommand implements CommandExecutor, TabCompleter {

    private final VeldoriaCore plugin;

    public VeldoriaCommand(VeldoriaCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("veldoria.admin")) {
            sender.sendMessage(Component.text("Нет прав.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(Component.text("Использование: /veldoria <give>", NamedTextColor.YELLOW));
            return true;
        }

        if (args[0].equalsIgnoreCase("give")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Только для игроков.");
                return true;
            }
            givePickaxe(player);
            return true;
        }

        sender.sendMessage(Component.text("Неизвестная подкоманда.", NamedTextColor.RED));
        return true;
    }

    private void givePickaxe(Player player) {
        ItemStack pickaxe = new ItemStack(Material.GOLDEN_PICKAXE);
        ItemMeta meta = pickaxe.getItemMeta();

        meta.displayName(Component.text("⛏ Экстрактор Спавнеров", NamedTextColor.GOLD));
        meta.lore(List.of(
                Component.text("Нажмите ПКМ по спавнеру,", NamedTextColor.GRAY),
                Component.text("чтобы попытаться добыть его.", NamedTextColor.GRAY),
                Component.text("Одноразовый предмет.", NamedTextColor.DARK_RED)
        ));

        // Ставим NBT метку
        meta.getPersistentDataContainer().set(plugin.pickaxeKey, PersistentDataType.BOOLEAN, true);

        pickaxe.setItemMeta(meta);
        player.getInventory().addItem(pickaxe);
        player.sendMessage(Component.text("Вы получили экстрактор спавнеров!", NamedTextColor.GREEN));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("give");
        }
        return List.of();
    }
}