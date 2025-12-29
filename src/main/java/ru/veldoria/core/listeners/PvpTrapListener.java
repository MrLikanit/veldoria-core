package ru.veldoria.core.listeners;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import ru.veldoria.core.VeldoriaCore;
import ru.veldoria.core.items.VeldoriaItems;
import ru.veldoria.core.utils.ColorUtils;

public class PvpTrapListener implements Listener {

    private final VeldoriaCore plugin;

    public PvpTrapListener(VeldoriaCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onUseTrap(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!VeldoriaItems.isPvpTrap(item)) return;

        event.setCancelled(true);

        if (player.hasCooldown(item.getType())) {
            ColorUtils.sendActionBar(player, "errors.cooldown");
            return;
        }

        if (!plugin.getProtectionHook().canInteract(player, player.getLocation().getBlock())) {
            ColorUtils.sendActionBar(player, "errors.region-deny");
            return;
        }

        if (!plugin.getProtectionHook().canPvp(player, player.getLocation())) {
            ColorUtils.sendActionBar(player, "errors.pvp-deny");
            return;
        }

        item.setAmount(item.getAmount() - 1);

        plugin.getArenaManager().createArena(player.getLocation());
        player.sendMessage(ColorUtils.getMsg("trap.activate"));

        int cooldownSec = plugin.getConfig().getInt("items.pvp-trap.settings.cooldown", 60);
        player.setCooldown(item.getType(), cooldownSec * 20);
    }

    @EventHandler
    public void onUseItem(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) return;

        if (item.getType() == Material.CHORUS_FRUIT || item.getType() == Material.ENDER_PEARL) {
            if (plugin.getArenaManager().isInsideArena(player)) {
                event.setCancelled(true);
                ColorUtils.sendActionBar(player, "trap.magic-blocked");
                player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1, 2);
                player.updateInventory();
            }
        }

        if (item.getType() == Material.WIND_CHARGE) {
            if (plugin.getArenaManager().isInsideArena(player)) {
                if (!player.hasCooldown(Material.WIND_CHARGE)) {
                    int windCd = plugin.getConfig().getInt("items.pvp-trap.settings.wind-charge-cooldown", 10);
                    player.setCooldown(Material.WIND_CHARGE, windCd * 20);
                    ColorUtils.sendActionBar(player, "trap.wind-nerf");
                }
            }
        }
    }

    @EventHandler
    public void onGlide(EntityToggleGlideEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        if (event.isGliding()) {
            if (plugin.getArenaManager().isInsideArena(player)) {
                event.setCancelled(true);
                ColorUtils.sendActionBar(player, "trap.elytra-blocked");
            }
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL ||
                event.getCause() == PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT) {

            if (plugin.getArenaManager().isInsideArena(event.getPlayer())) {
                event.setCancelled(true);
                ColorUtils.sendActionBar(event.getPlayer(), "trap.escape-deny");
            }
        }
    }
}