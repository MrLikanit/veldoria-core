package ru.veldoria.core.listeners;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import ru.veldoria.core.VeldoriaCore;
import ru.veldoria.core.utils.ColorUtils;

import java.util.ArrayList;
import java.util.List;

public class PrestigeListener implements Listener {


    public PrestigeListener(VeldoriaCore plugin) {
    }

    public static void openPrestigeMenu(Player player) {
        if (!VeldoriaCore.getInstance().getConfig().getBoolean("prestige-system.enabled")) {
            player.sendMessage(ColorUtils.getMsg("prestige.disabled"));
            return;
        }

        Inventory gui = Bukkit.createInventory(new PrestigeHolder(), 27, ColorUtils.format(
                VeldoriaCore.getInstance().getConfig().getString("prestige-system.gui.title", "Prestige")
        ));

        setupGui(player, gui, false);
        player.openInventory(gui);
    }

    private static void setupGui(Player player, Inventory gui, boolean confirmMode) {
        gui.clear();

        ItemStack glass = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) gui.setItem(i, glass);

        if (confirmMode) {
            ItemStack confirm = createItem(Material.LIME_STAINED_GLASS_PANE, "&a&lПОДТВЕРДИТЬ СБРОС");
            ItemStack cancel = createItem(Material.RED_STAINED_GLASS_PANE, "&c&lОТМЕНА");

            gui.setItem(11, confirm);
            gui.setItem(15, cancel);

            ItemStack info = createItem(Material.PAPER, "&eВы уверены?");
            ItemMeta meta = info.getItemMeta();
            meta.lore(List.of(
                    ColorUtils.format(""),
                    ColorUtils.format("&cВаши навыки сбросятся до 0git add .git add . уровня!"),
                    ColorUtils.format("&cЭто действие нельзя отменить.")
            ));
            info.setItemMeta(meta);
            gui.setItem(13, info);

        } else {
            int totalLvl = VeldoriaCore.getInstance().getPrestigeManager().getTotalLevel(player);
            int minLvl = VeldoriaCore.getInstance().getConfig().getInt("prestige-system.min-total-level", 200);
            int reward = VeldoriaCore.getInstance().getPrestigeManager().calculateReward(player);
            boolean ready = totalLvl >= minLvl;

            String matName = VeldoriaCore.getInstance().getConfig().getString("prestige-system.gui.info-item.material", "NETHER_STAR");
            Material mat = Material.getMaterial(matName);
            if (mat == null) mat = Material.NETHER_STAR;

            ItemStack info = createItem(mat, VeldoriaCore.getInstance().getConfig().getString("prestige-system.gui.info-item.name"));

            ItemMeta meta = info.getItemMeta();
            List<Component> lore = new ArrayList<>();
            String path = ready ? "prestige-system.gui.info-item.lore-ready" : "prestige-system.gui.info-item.lore-not-ready";

            for (String line : VeldoriaCore.getInstance().getConfig().getStringList(path)) {
                line = line.replace("%total_level%", String.valueOf(totalLvl))
                        .replace("%min_level%", String.valueOf(minLvl))
                        .replace("%reward%", String.valueOf(reward))
                        .replace("%needed%", String.valueOf(minLvl - totalLvl));
                lore.add(ColorUtils.format(line));
            }
            meta.lore(lore);
            info.setItemMeta(meta);
            gui.setItem(11, info);

            if (ready) {
                ItemStack btn = createItem(Material.EMERALD_BLOCK, "&a&lСДЕЛАТЬ ПРЕСТИЖ");
                gui.setItem(15, btn);
            } else {
                ItemStack btn = createItem(Material.RED_STAINED_GLASS, "&cНедостаточно уровней");
                gui.setItem(15, btn);
            }
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof PrestigeHolder)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.BLACK_STAINED_GLASS_PANE) return;

        int slot = event.getRawSlot();
        Inventory gui = event.getInventory();

        if (slot == 15 && clicked.getType() == Material.EMERALD_BLOCK) {
            setupGui(player, gui, true);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
            return;
        }

        if (slot == 11 && clicked.getType() == Material.LIME_STAINED_GLASS_PANE) {
            VeldoriaCore.getInstance().getPrestigeManager().doPrestige(player);
            return;
        }

        if (slot == 15 && clicked.getType() == Material.RED_STAINED_GLASS_PANE) {
            setupGui(player, gui, false);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
        }
    }

    private static ItemStack createItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (name != null) meta.displayName(ColorUtils.format(name));
        item.setItemMeta(meta);
        return item;
    }

    public static class PrestigeHolder implements InventoryHolder {
        @Override
        public @NotNull Inventory getInventory() { return Bukkit.createInventory(null, 9); }
    }
}