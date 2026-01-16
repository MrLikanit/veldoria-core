package ru.veldoria.core.listeners;

import dev.aurelium.auraskills.api.stat.Stats;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import ru.veldoria.core.VeldoriaCore;
import ru.veldoria.core.items.VeldoriaItems;
import ru.veldoria.core.utils.ColorUtils;

import java.security.SecureRandom;
import java.util.*;

public class SpawnerUpgradeListener implements Listener {

    private final VeldoriaCore plugin;
    private final int SLOT_PRISM = 13;
    private final int SLOT_CONFIRM = 22;
    private final int SLOT_INFO = 4;

    private final SecureRandom random = new SecureRandom();
    private final Map<UUID, Long> lastOpenCooldown = new HashMap<>();

    public SpawnerUpgradeListener(VeldoriaCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.SPAWNER) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (player.isSneaking()) return;

        if (item != null && item.getType().name().endsWith("_SPAWN_EGG")) return;

        if (VeldoriaItems.isSpawnerPickaxe(item)) return;

        long now = System.currentTimeMillis();
        if (now - lastOpenCooldown.getOrDefault(player.getUniqueId(), 0L) < 500) {
            return;
        }

        event.setCancelled(true);

        if (!plugin.getProtectionHook().canInteract(player, block)) {
            ColorUtils.sendActionBar(player, "errors.region-deny");
            lastOpenCooldown.put(player.getUniqueId(), now);
            return;
        }

        lastOpenCooldown.put(player.getUniqueId(), now);
        openSpawnerMenu(player, block);
    }

    private void openSpawnerMenu(Player player, Block spawnerBlock) {
        Inventory gui = Bukkit.createInventory(new SpawnerHolder(spawnerBlock), 27, ColorUtils.format(
                plugin.getConfig().getString("spawner-manipulation.gui.title", "Spawner Upgrade")
        ));

        ItemStack bg = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = bg.getItemMeta();
        meta.displayName(Component.empty());
        bg.setItemMeta(meta);
        for (int i = 0; i < 27; i++) gui.setItem(i, bg);

        gui.setItem(SLOT_PRISM, null);

        updateInfoItem(gui, player, null);

        player.openInventory(gui);
        player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 1, 1);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof SpawnerHolder holder)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        event.setCancelled(true);
        int slot = event.getRawSlot();
        Inventory gui = event.getInventory();

        if (slot > 26) {
            event.setCancelled(false);
            if (event.isShiftClick()) {
                event.setCancelled(true);
                ItemStack current = event.getCurrentItem();
                if (VeldoriaItems.isFilledPrism(current)) {
                    if (gui.getItem(SLOT_PRISM) == null) {
                        gui.setItem(SLOT_PRISM, current.clone());
                        event.setCurrentItem(null);
                        updateGuiState(gui, player);
                    }
                }
            }
            return;
        }

        if (slot == SLOT_PRISM) {
            event.setCancelled(false);
            Bukkit.getScheduler().runTask(plugin, () -> updateGuiState(gui, player));
            return;
        }

        if (slot == SLOT_CONFIRM && gui.getItem(SLOT_CONFIRM).getType() == Material.LIME_STAINED_GLASS_PANE) {
            processChange(player, gui, holder.block);
        }
    }

    private void updateGuiState(Inventory gui, Player player) {
        ItemStack prism = gui.getItem(SLOT_PRISM);
        EntityType type = null;

        if (VeldoriaItems.isFilledPrism(prism)) {
            String typeStr = prism.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(plugin, "stored_entity_type"), PersistentDataType.STRING);
            if (typeStr != null) {
                try {
                    type = EntityType.valueOf(typeStr);
                } catch (IllegalArgumentException ignored) {}
            }
        }

        updateInfoItem(gui, player, type);

        if (type != null) {
            ItemStack btn = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
            ItemMeta meta = btn.getItemMeta();
            meta.displayName(ColorUtils.format("&a&lПОДТВЕРДИТЬ ИЗМЕНЕНИЕ"));
            btn.setItemMeta(meta);
            gui.setItem(SLOT_CONFIRM, btn);
        } else {
            gui.setItem(SLOT_CONFIRM, new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
        }
    }

    private void updateInfoItem(Inventory gui, Player player, EntityType type) {
        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.displayName(ColorUtils.format("&eИнформация"));
        List<Component> lore = new ArrayList<>();

        double chance = calculateChance(player, type);

        for (String line : plugin.getConfig().getStringList("spawner-manipulation.gui.info-lore")) {
            String chanceStr = String.format("%.1f", chance);
            lore.add(ColorUtils.format(line.replace("%chance%", chanceStr)));
        }
        infoMeta.lore(lore);
        info.setItemMeta(infoMeta);
        gui.setItem(SLOT_INFO, info);
    }

    private double calculateChance(Player player, EntityType type) {
        double chance = plugin.getConfig().getDouble("spawner-manipulation.change-chance", 1.0);

        if (type != null) {
            String mobName = type.name().toUpperCase();
            if (plugin.getConfig().contains("spawner-manipulation.specific-chances." + mobName)) {
                chance = plugin.getConfig().getDouble("spawner-manipulation.specific-chances." + mobName);
            }
        }

        double luckBonus = 0;
        if (plugin.getAuraSkills() != null) {
            var user = plugin.getAuraSkills().getUser(player.getUniqueId());
            if (user != null) {
                double luckLevel = user.getStatLevel(Stats.LUCK);
                double multiplier = plugin.getConfig().getDouble("spawner-manipulation.luck-multiplier", 0.01);
                luckBonus = luckLevel * multiplier;
            }
        }

        return Math.min(100.0, chance + luckBonus);
    }

    private void processChange(Player player, Inventory gui, Block block) {
        if (block.getType() != Material.SPAWNER) {
            player.sendMessage(ColorUtils.format("&cБлок больше не является спавнером!"));
            player.closeInventory();
            return;
        }

        ItemStack prism = gui.getItem(SLOT_PRISM);
        if (!VeldoriaItems.isFilledPrism(prism)) return;

        String typeStr = prism.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(plugin, "stored_entity_type"), PersistentDataType.STRING);
        if (typeStr == null) return;
        EntityType type = EntityType.valueOf(typeStr);

        List<String> blacklist = plugin.getConfig().getStringList("spawner-manipulation.blacklist");
        if (blacklist.contains(type.name())) {
            player.sendMessage(ColorUtils.getMsg("spawner.blacklisted"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
            player.closeInventory();
            return;
        }

        double finalChance = calculateChance(player, type);

        double roll = random.nextDouble() * 100.0;

        gui.setItem(SLOT_PRISM, null);

        if (roll <= finalChance) {
            CreatureSpawner spawner = (CreatureSpawner) block.getState();
            spawner.setSpawnedType(type);
            spawner.update();

            player.sendMessage(ColorUtils.getMsg("spawner.change-success", "%mob%", type.name()));

            String chanceStr = String.format("%.1f", finalChance);
            player.sendActionBar(ColorUtils.format("&aУспех! Шанс: " + chanceStr + "%"));

            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1, 1);
            player.getWorld().spawnParticle(org.bukkit.Particle.END_ROD, block.getLocation().add(0.5, 0.5, 0.5), 20, 0.5, 0.5, 0.5, 0.1);
        } else {
            player.sendMessage(ColorUtils.getMsg("spawner.change-fail"));

            String chanceStr = String.format("%.1f", finalChance);
            player.sendActionBar(ColorUtils.format("&cНеудача... Шанс: " + chanceStr + "%"));

            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1, 1);
            player.getWorld().spawnParticle(org.bukkit.Particle.SMOKE, block.getLocation().add(0.5, 0.5, 0.5), 20, 0.5, 0.5, 0.5, 0.1);
        }

        player.closeInventory();
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof SpawnerHolder) {
            ItemStack item = event.getInventory().getItem(SLOT_PRISM);
            if (item != null) {
                if (!event.getPlayer().getInventory().addItem(item).isEmpty()) {
                    event.getPlayer().getWorld().dropItem(event.getPlayer().getLocation(), item);
                }
            }
        }
    }

    public static class SpawnerHolder implements InventoryHolder {
        final Block block;
        public SpawnerHolder(Block block) { this.block = block; }
        @Override
        public @NotNull Inventory getInventory() { return Bukkit.createInventory(null, 9); }
    }
}