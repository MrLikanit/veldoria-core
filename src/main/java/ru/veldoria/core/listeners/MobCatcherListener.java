package ru.veldoria.core.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import ru.veldoria.core.VeldoriaCore;
import ru.veldoria.core.items.VeldoriaItems;
import ru.veldoria.core.utils.ColorUtils;
import ru.veldoria.core.utils.SoulPrismAnimation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class MobCatcherListener implements Listener {

    private final VeldoriaCore plugin;
    private final NamespacedKey DATA_KEY;
    private final Map<UUID, Long> bossSoundCooldown = new HashMap<>();

    public MobCatcherListener(VeldoriaCore plugin) {
        this.plugin = plugin;
        this.DATA_KEY = new NamespacedKey(plugin, "stored_entity_data");
    }

    @EventHandler
    public void onCatch(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!VeldoriaItems.isEmptyPrism(item)) return;
        if (!(event.getRightClicked() instanceof LivingEntity entity)) return;
        if (entity instanceof Player) return;

        event.setCancelled(true);

        if (!plugin.getProtectionHook().canInteract(player, entity.getLocation().getBlock())) {
            ColorUtils.sendActionBar(player, "errors.region-deny");
            return;
        }

        List<String> blacklist = plugin.getConfig().getStringList("items.soul-prism.blacklist");
        if (blacklist.contains(entity.getType().name())) {
            long now = System.currentTimeMillis();
            if (now - bossSoundCooldown.getOrDefault(player.getUniqueId(), 0L) > 2000) {
                ColorUtils.sendActionBar(player, "catcher.blacklisted");
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1, 0.5f);
                bossSoundCooldown.put(player.getUniqueId(), now);
            }
            return;
        }

        if (entity instanceof Tameable tameable && tameable.isTamed()) {
            if (tameable.getOwner() != null && !tameable.getOwner().getUniqueId().equals(player.getUniqueId()) && !player.isOp()) {
                ColorUtils.sendActionBar(player, "catcher.tamed-owner");
                return;
            }
        }

        ItemStack filledPrism = VeldoriaItems.getFilledPrism(entity);
        ItemMeta meta = filledPrism.getItemMeta();

        try {
            @SuppressWarnings("deprecation")
            byte[] data = Bukkit.getUnsafe().serializeEntity(entity);
            meta.getPersistentDataContainer().set(DATA_KEY, PersistentDataType.BYTE_ARRAY, data);
        } catch (Exception e) {
            player.sendMessage(ColorUtils.format("&cОшибка сохранения сущности!"));
            plugin.getLogger().log(Level.SEVERE, "Error serializing entity", e);
            return;
        }

        filledPrism.setItemMeta(meta);
        item.setAmount(item.getAmount() - 1);
        player.getInventory().addItem(filledPrism);

        SoulPrismAnimation.playCatchAnimation(entity);

        entity.remove();
        ColorUtils.sendActionBar(player, "catcher.success-catch");
    }

    @EventHandler
    public void onRelease(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!VeldoriaItems.isFilledPrism(item)) return;

        event.setCancelled(true);

        if (event.getClickedBlock() == null || !plugin.getProtectionHook().canInteract(player, event.getClickedBlock())) {
            ColorUtils.sendActionBar(player, "errors.region-deny");
            return;
        }

        Location loc = event.getClickedBlock().getLocation().add(0.5, 1.1, 0.5);
        if (loc.getWorld() == null) return;

        loc.setDirection(player.getLocation().getDirection().multiply(-1));

        try {
            byte[] data = item.getItemMeta().getPersistentDataContainer().get(DATA_KEY, PersistentDataType.BYTE_ARRAY);
            if (data == null) {
                player.sendMessage(ColorUtils.format("&cОшибка! Призма пуста."));
                return;
            }

            @SuppressWarnings("deprecation")
            var entity = Bukkit.getUnsafe().deserializeEntity(data, loc.getWorld());
            entity.spawnAt(loc);

        } catch (Exception e) {
            player.sendMessage(ColorUtils.format("&cСущность повреждена."));
            plugin.getLogger().log(Level.WARNING, "Error deserializing entity", e);
            return;
        }

        item.setAmount(item.getAmount() - 1);

        SoulPrismAnimation.playReleaseAnimation(loc);

        ColorUtils.sendActionBar(player, "catcher.success-release");
    }
}