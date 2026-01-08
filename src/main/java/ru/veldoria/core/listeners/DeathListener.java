package ru.veldoria.core.listeners;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import ru.veldoria.core.items.VeldoriaItems;
import ru.veldoria.core.utils.ColorUtils;

import java.util.ArrayList;
import java.util.List;

public class DeathListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        ItemStack clock = null;
        int clockSlot = -1;

        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (VeldoriaItems.isDeathClock(contents[i])) {
                clock = contents[i];
                clockSlot = i;
                break;
            }
        }

        if (clock != null) {
            event.setKeepInventory(true);
            event.setKeepLevel(true);
            event.getDrops().clear();
            event.setDroppedExp(0);

            clock.setAmount(clock.getAmount() - 1);
            player.getInventory().setItem(clockSlot, clock.getAmount() > 0 ? clock : null);

            List<ItemStack> toRemove = new ArrayList<>();
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.getType() != Material.AIR) {
                    if (item.containsEnchantment(Enchantment.VANISHING_CURSE)) {
                        toRemove.add(item);
                    }
                }
            }
            for (ItemStack item : toRemove) {
                player.getInventory().remove(item);
            }

            ColorUtils.sendActionBar(player, "&e⌛ Хроносфера разбилась, но спасла ваши вещи!");
            player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1, 0.5f);
            player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1, 2f);
        }
    }
}