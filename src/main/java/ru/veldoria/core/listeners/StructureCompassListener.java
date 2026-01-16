package ru.veldoria.core.listeners;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.generator.structure.Structure;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.StructureSearchResult;
import org.jetbrains.annotations.NotNull;
import ru.veldoria.core.VeldoriaCore;
import ru.veldoria.core.items.VeldoriaItems;
import ru.veldoria.core.utils.ColorUtils;

import java.util.*;
import java.util.stream.Collectors;

public class StructureCompassListener implements Listener {

    private final VeldoriaCore plugin;
    private final NamespacedKey COOLDOWN_KEY;

    public StructureCompassListener(VeldoriaCore plugin) {
        this.plugin = plugin;
        this.COOLDOWN_KEY = new NamespacedKey(plugin, "compass_cooldown");
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (!VeldoriaItems.isStructureCompass(item)) return;

        Player player = event.getPlayer();

        long now = System.currentTimeMillis();
        long end = player.getPersistentDataContainer().getOrDefault(COOLDOWN_KEY, PersistentDataType.LONG, 0L);

        if (now < end) {
            long left = (end - now) / 1000;
            player.sendMessage(ColorUtils.getMsg("compass.cooldown", "%time%", String.valueOf(left)));
            return;
        }

        openStructureGui(player, 0);
    }

    private void openStructureGui(Player player, int page) {
        String title = plugin.getConfig().getString("compass-gui.title", "Structures").replace("%page%", String.valueOf(page + 1));
        Inventory gui = Bukkit.createInventory(new CompassHolder(page), 54, ColorUtils.format(title));

        @SuppressWarnings("deprecation")
        List<Structure> allStructures = Registry.STRUCTURE.stream()
                .sorted(Comparator.comparing(s -> s.key().value()))
                .toList();

        int start = page * 45;
        int end = Math.min(start + 45, allStructures.size());

        for (int i = start; i < end; i++) {
            Structure struct = allStructures.get(i);
            String name = struct.key().value().replace("_", " ");
            String format = plugin.getConfig().getString("compass-gui.structure-icon", "&b%structure%");

            ItemStack icon = new ItemStack(Material.MAP);
            ItemMeta meta = icon.getItemMeta();
            meta.displayName(ColorUtils.format(format.replace("%structure%", name)));
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "struct_key"), PersistentDataType.STRING, struct.key().toString());
            icon.setItemMeta(meta);

            gui.addItem(icon);
        }

        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta meta = prev.getItemMeta();
            meta.displayName(ColorUtils.format(plugin.getConfig().getString("compass-gui.prev-page", "<<<")));
            prev.setItemMeta(meta);
            gui.setItem(45, prev);
        }

        if (end < allStructures.size()) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta meta = next.getItemMeta();
            meta.displayName(ColorUtils.format(plugin.getConfig().getString("compass-gui.next-page", ">>>")));
            next.setItemMeta(meta);
            gui.setItem(53, next);
        }

        player.openInventory(gui);
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1, 1);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof CompassHolder holder)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        if (clicked.getType() == Material.ARROW) {
            if (event.getSlot() == 45) openStructureGui(player, holder.page - 1);
            if (event.getSlot() == 53) openStructureGui(player, holder.page + 1);
            return;
        }

        if (clicked.getType() == Material.MAP) {
            String keyStr = clicked.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(plugin, "struct_key"), PersistentDataType.STRING);
            if (keyStr == null) return;

            NamespacedKey key = NamespacedKey.fromString(keyStr);
            if (key == null) return;

            @SuppressWarnings("deprecation")
            Structure structure = Registry.STRUCTURE.get(key);

            if (structure != null) {
                startSearch(player, structure);
                player.closeInventory();
            }
        }
    }

    private void startSearch(Player player, Structure structure) {
        ColorUtils.sendActionBar(player, "compass.searching");

        int radius = plugin.getConfig().getInt("items.structure-compass.settings.search-radius", 1500);

        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                StructureSearchResult result = player.getWorld().locateNearestStructure(player.getLocation(), structure, radius, false);
                Location found = (result != null) ? result.getLocation() : null;

                if (found != null) {
                    updateCompass(player, found, structure);
                } else {
                    player.sendMessage(ColorUtils.getMsg("compass.not-found", "%radius%", String.valueOf(radius)));
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1, 0.5f);
                }
            } catch (Exception e) {
                player.sendMessage(ColorUtils.format("&cСтруктура не найдена в этом мире."));
            }
        });
    }

    private void updateCompass(Player player, Location target, Structure structure) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!VeldoriaItems.isStructureCompass(hand)) {
            hand = player.getInventory().getItemInOffHand();
            if (!VeldoriaItems.isStructureCompass(hand)) {
                player.sendMessage(ColorUtils.format("&cВозьмите компас в руку!"));
                return;
            }
        }

        int cdSec = plugin.getConfig().getInt("items.structure-compass.settings.cooldown", 300);
        long endTime = System.currentTimeMillis() + (cdSec * 1000L);

        player.getPersistentDataContainer().set(COOLDOWN_KEY, PersistentDataType.LONG, endTime);

        player.setCooldown(hand.getType(), cdSec * 20);

        ItemMeta meta = hand.getItemMeta();
        String structName = structure.key().value().replace("_", " ");

        if (meta instanceof CompassMeta compassMeta) {
            compassMeta.setLodestone(target);
            compassMeta.setLodestoneTracked(false);

            compassMeta.displayName(ColorUtils.format(
                    plugin.getConfig().getString("items.structure-compass.name") + " &7(" + structName + ")"
            ));
            hand.setItemMeta(compassMeta);

            player.sendMessage(ColorUtils.getMsg("compass.found", "%structure%", structName));
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1, 2);
        } else {
            player.sendMessage(ColorUtils.format("&aСтруктура " + structName + " найдена на: X=" + target.getBlockX() + ", Z=" + target.getBlockZ()));
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
        }
    }

    public static class CompassHolder implements InventoryHolder {
        final int page;
        public CompassHolder(int page) { this.page = page; }
        @Override
        public @NotNull Inventory getInventory() { return Bukkit.createInventory(null, 54); }
    }
}