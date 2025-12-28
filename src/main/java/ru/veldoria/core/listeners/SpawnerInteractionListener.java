package ru.veldoria.core.listeners;

import dev.aurelium.auraskills.api.stat.Stats;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import ru.veldoria.core.VeldoriaCore;

import java.security.SecureRandom;
import java.util.List;

public class SpawnerInteractionListener implements Listener {

    private final VeldoriaCore plugin;
    private final String GUI_TITLE = "Добыча спавнера";

    private final double BASE_CHANCE = 20.0;
    private final double CHANCE_PER_LUCK = 0.5;
    private final double FAIL_BONUS = 5.0;

    private final SecureRandom random = new SecureRandom();

    public SpawnerInteractionListener(VeldoriaCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (isSpawnerPickaxe(event.getPlayer().getInventory().getItemInMainHand())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.text("Этот предмет нельзя использовать как обычную кирку!", NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onSpawnerClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.SPAWNER) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!isSpawnerPickaxe(item)) return;

        event.setCancelled(true);

        // === ПРОВЕРКА ПРИВАТОВ ===
        if (!plugin.getProtectionHook().canInteract(player, block)) {
            player.sendMessage(Component.text("Вы не можете добывать спавнеры на чужой территории!", NamedTextColor.RED));
            return;
        }
        // =========================

        double chance = calculateTotalChance(player);
        openGui(player, block, chance);
    }

    private boolean isSpawnerPickaxe(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        if (!item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(plugin.pickaxeKey, PersistentDataType.BOOLEAN);
    }

    private double calculateTotalChance(Player player) {
        double total = BASE_CHANCE;
        if (plugin.getAuraSkills() != null) {
            var user = plugin.getAuraSkills().getUser(player.getUniqueId());
            if (user != null) {
                total += user.getStatLevel(Stats.LUCK) * CHANCE_PER_LUCK;
            }
        }
        Double pityBonus = player.getPersistentDataContainer().get(plugin.pityKey, PersistentDataType.DOUBLE);
        if (pityBonus != null) {
            total += pityBonus;
        }
        return Math.min(100.0, total);
    }

    private void openGui(Player player, Block spawnerBlock, double chance) {
        Inventory gui = Bukkit.createInventory(new SpawnerHolder(spawnerBlock, chance), 27, Component.text(GUI_TITLE));

        Double pity = player.getPersistentDataContainer().get(plugin.pityKey, PersistentDataType.DOUBLE);
        double pityVal = (pity == null) ? 0.0 : pity;

        ItemStack infoParams = new ItemStack(Material.PAPER);
        ItemMeta infoMeta = infoParams.getItemMeta();
        infoMeta.displayName(Component.text("Информация о шансе:", NamedTextColor.GOLD));
        infoMeta.lore(List.of(
                Component.text("Общий шанс: ", NamedTextColor.GRAY).append(Component.text(String.format("%.1f%%", chance), NamedTextColor.YELLOW)),
                Component.text("Бонус неудач: ", NamedTextColor.GRAY).append(Component.text(String.format("+%.1f%%", pityVal), NamedTextColor.AQUA))
        ));
        infoParams.setItemMeta(infoMeta);

        ItemStack confirmBtn = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta confirmMeta = confirmBtn.getItemMeta();
        confirmMeta.displayName(Component.text("ЗАБРАТЬ СПАВНЕР", NamedTextColor.GREEN, TextDecoration.BOLD));
        confirmMeta.lore(List.of(
                Component.text("Нажмите, чтобы начать ритуал.", NamedTextColor.GRAY),
                Component.text("Кирка будет поглощена сразу!", NamedTextColor.RED)
        ));
        confirmBtn.setItemMeta(confirmMeta);

        ItemStack cancelBtn = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta cancelMeta = cancelBtn.getItemMeta();
        cancelMeta.displayName(Component.text("Отмена", NamedTextColor.RED));
        cancelBtn.setItemMeta(cancelMeta);

        gui.setItem(11, infoParams);
        gui.setItem(13, confirmBtn);
        gui.setItem(15, cancelBtn);

        player.openInventory(gui);
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof SpawnerHolder holder)) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) return;

        if (clicked.getType() == Material.RED_STAINED_GLASS_PANE) {
            player.closeInventory();
            return;
        }

        if (clicked.getType() == Material.LIME_STAINED_GLASS_PANE) {
            startExtractionRitual(player, holder.block, holder.chance);
        }
    }

    private void startExtractionRitual(Player player, Block spawnerBlock, double chance) {
        ItemStack handItem = player.getInventory().getItemInMainHand();

        if (!isSpawnerPickaxe(handItem)) {
            player.sendMessage(Component.text("У вас нет необходимой кирки в руке!", NamedTextColor.RED));
            player.closeInventory();
            return;
        }

        if (spawnerBlock.getType() != Material.SPAWNER) {
            player.sendMessage(Component.text("Блок больше не является спавнером.", NamedTextColor.RED));
            player.closeInventory();
            return;
        }

        handItem.setAmount(handItem.getAmount() - 1);
        player.closeInventory();

        new BukkitRunnable() {
            int ticks = 0;
            final Location center = spawnerBlock.getLocation().add(0.5, 0.5, 0.5);

            @Override
            public void run() {
                if (ticks >= 40) {
                    finalizeMining(player, spawnerBlock, chance);
                    this.cancel();
                    return;
                }

                if (ticks % 2 == 0) {
                    double angle = ticks * 0.5;
                    double x = Math.cos(angle) * 1.2;
                    double z = Math.sin(angle) * 1.2;
                    center.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(x, Math.sin(ticks * 0.1) * 0.5, z), 1, 0, 0, 0, 0);
                    center.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(-x, -Math.sin(ticks * 0.1) * 0.5, -z), 1, 0, 0, 0, 0);
                }

                if (ticks % 5 == 0) {
                    float pitch = 0.5f + (ticks / 40.0f);
                    center.getWorld().playSound(center, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, pitch);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1L);
    }

    private void finalizeMining(Player player, Block spawnerBlock, double chance) {
        if (spawnerBlock.getType() != Material.SPAWNER) {
            player.sendMessage(Component.text("Ритуал прерван: Спавнер исчез!", NamedTextColor.RED));
            return;
        }

        double roll = random.nextDouble() * 100.0;
        Location loc = spawnerBlock.getLocation().add(0.5, 0.5, 0.5);

        if (roll <= chance) {
            if (spawnerBlock.getState() instanceof CreatureSpawner spawner) {
                giveSpawnerItem(player, spawner, loc);
            }
            spawnerBlock.setType(Material.AIR);
            player.getPersistentDataContainer().set(plugin.pityKey, PersistentDataType.DOUBLE, 0.0);
            player.sendMessage(Component.text("Успех! Спавнер добыт.", NamedTextColor.GREEN, TextDecoration.BOLD));
            loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
            loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, 30, 0.5, 0.5, 0.5, 0.1);
            loc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 50, 0.2, 0.5, 0.2, 0.1);
        } else {
            double currentPity = player.getPersistentDataContainer().getOrDefault(plugin.pityKey, PersistentDataType.DOUBLE, 0.0);
            double newPity = currentPity + FAIL_BONUS;
            player.getPersistentDataContainer().set(plugin.pityKey, PersistentDataType.DOUBLE, newPity);

            player.sendMessage(Component.text("Неудача! Энергия рассеялась.", NamedTextColor.RED, TextDecoration.BOLD));
            player.sendMessage(Component.text("Бонус неудачи накоплен: ", NamedTextColor.GRAY)
                    .append(Component.text("+" + FAIL_BONUS + "%", NamedTextColor.AQUA)));

            loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1, 1);
            loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 1);
            loc.getWorld().spawnParticle(Particle.SMOKE, loc, 20, 0.5, 0.5, 0.5, 0.1);
        }
    }

    private void giveSpawnerItem(Player player, CreatureSpawner spawnerState, Location dropLoc) {
        EntityType type = spawnerState.getSpawnedType();
        ItemStack item = new ItemStack(Material.SPAWNER);
        BlockStateMeta meta = (BlockStateMeta) item.getItemMeta();
        CreatureSpawner itemSpawnerState = (CreatureSpawner) meta.getBlockState();

        String mobName;
        if (type != null) {
            itemSpawnerState.setSpawnedType(type);
            mobName = type.name().toLowerCase().replace("_", " ");
        } else {
            mobName = "пустой";
        }

        meta.setBlockState(itemSpawnerState);
        meta.displayName(Component.text("Спавнер: " + mobName, NamedTextColor.GOLD));
        item.setItemMeta(meta);

        if (player.isOnline()) {
            if (!player.getInventory().addItem(item).isEmpty()) {
                player.getWorld().dropItem(player.getLocation(), item);
            }
        } else {
            dropLoc.getWorld().dropItem(dropLoc, item);
        }
    }

    private static class SpawnerHolder implements org.bukkit.inventory.InventoryHolder {
        final Block block;
        final double chance;

        public SpawnerHolder(Block block, double chance) {
            this.block = block;
            this.chance = chance;
        }

        @Override
        public @org.jetbrains.annotations.NotNull Inventory getInventory() {
            return null;
        }
    }
}