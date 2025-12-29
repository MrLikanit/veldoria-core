package ru.veldoria.core.listeners;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import ru.veldoria.core.VeldoriaCore;
import ru.veldoria.core.items.VeldoriaItems;
import ru.veldoria.core.utils.ColorUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class DisenchantListener implements Listener {

    private final VeldoriaCore plugin;

    private static final int SLOT_ITEM = 19;
    private static final int SLOT_SCROLL = 25;
    private static final int SLOT_BUTTON = 22;
    private static final int SLOT_ITEM_BG = 10;
    private static final int SLOT_SCROLL_BG = 16;

    public DisenchantListener(VeldoriaCore plugin) {
        this.plugin = plugin;
    }

    public static void openDisenchantMenu(Player player) {
        Inventory gui = Bukkit.createInventory(new DisenchantHolder(), 54, ColorUtils.getMsg("disenchant.gui-title"));
        setupBaseGui(gui);
        player.openInventory(gui);
    }

    private static void setupBaseGui(Inventory gui) {
        ItemStack glass = createDecoItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            gui.setItem(i, glass);
        }

        gui.setItem(SLOT_ITEM_BG, createDecoItem(Material.IRON_BARS, "&7⬇ Положите предмет сюда ⬇"));
        gui.setItem(SLOT_SCROLL_BG, createDecoItem(Material.IRON_BARS, "&7⬇ Положите свиток сюда ⬇"));

        gui.setItem(SLOT_ITEM, null);
        gui.setItem(SLOT_SCROLL, null);
        gui.setItem(SLOT_BUTTON, createDecoItem(Material.BARRIER, "&c&lОжидание предметов..."));
    }

    private static ItemStack createDecoItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ColorUtils.format(name));
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof DisenchantHolder)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory gui = event.getInventory();
        ItemStack clicked = event.getCurrentItem();
        int slot = event.getRawSlot();
        boolean isTopInv = slot < gui.getSize();

        if (event.getClick() == ClickType.NUMBER_KEY || event.getClick() == ClickType.DROP || event.getClick() == ClickType.CONTROL_DROP) {
            if (isTopInv || event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                event.setCancelled(true);
                return;
            }
        }

        if (isTopInv) {
            if (slot != SLOT_ITEM && slot != SLOT_SCROLL) {
                event.setCancelled(true);

                if (slot == SLOT_BUTTON) processRandomDisenchant(player, gui);
                if (slot >= 27) processSelectDisenchant(player, gui, clicked);
                return;
            }
        }

        else {
            if (event.isShiftClick()) {
                event.setCancelled(true);
                if (clicked == null || clicked.getType() == Material.AIR) return;

                if (VeldoriaItems.isRandomDisenchanter(clicked) || VeldoriaItems.isSelectDisenchanter(clicked)) {
                    if (gui.getItem(SLOT_SCROLL) == null) {
                        gui.setItem(SLOT_SCROLL, clicked.clone());
                        event.setCurrentItem(null);
                    }
                } else {
                    if (gui.getItem(SLOT_ITEM) == null && !clicked.getEnchantments().isEmpty()) {
                        gui.setItem(SLOT_ITEM, clicked.clone());
                        event.setCurrentItem(null);
                    }
                }
            }
        }

        Bukkit.getScheduler().runTask(plugin, () -> updateGuiState(gui));
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof DisenchantHolder) {
            for (int slot : event.getRawSlots()) {
                if (slot < 54 && slot != SLOT_ITEM && slot != SLOT_SCROLL) {
                    event.setCancelled(true);
                    return;
                }
            }
            Bukkit.getScheduler().runTask(plugin, () -> updateGuiState(event.getInventory()));
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof DisenchantHolder) {
            Inventory gui = event.getInventory();
            Player player = (Player) event.getPlayer();
            returnItem(player, gui, SLOT_ITEM);
            returnItem(player, gui, SLOT_SCROLL);
        }
    }

    private void returnItem(Player player, Inventory gui, int slot) {
        ItemStack item = gui.getItem(slot);
        if (item != null && item.getType() != Material.AIR) {
            if (!player.getInventory().addItem(item).isEmpty()) {
                player.getWorld().dropItem(player.getLocation(), item);
            }
        }
    }

    private void updateGuiState(Inventory gui) {
        ItemStack item = gui.getItem(SLOT_ITEM);
        ItemStack scroll = gui.getItem(SLOT_SCROLL);

        gui.setItem(SLOT_ITEM_BG, item != null && item.getType() != Material.AIR
                ? createDecoItem(Material.LIME_STAINED_GLASS_PANE, "&aПредмет установлен")
                : createDecoItem(Material.IRON_BARS, "&7⬇ Предмет сюда ⬇"));

        gui.setItem(SLOT_SCROLL_BG, scroll != null && scroll.getType() != Material.AIR
                ? createDecoItem(Material.LIME_STAINED_GLASS_PANE, "&aСвиток установлен")
                : createDecoItem(Material.IRON_BARS, "&7⬇ Свиток сюда ⬇"));

        ItemStack glass = createDecoItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 27; i < 54; i++) gui.setItem(i, glass);

        if (item == null || scroll == null || item.getType() == Material.AIR || scroll.getType() == Material.AIR) {
            gui.setItem(SLOT_BUTTON, createDecoItem(Material.BARRIER, "&c&lОжидание предметов..."));
            return;
        }

        Map<Enchantment, Integer> enchants = item.getEnchantments();
        if (enchants.isEmpty()) {
            gui.setItem(SLOT_BUTTON, createDecoItem(Material.BARRIER, "&cНа предмете нет чар!"));
            return;
        }

        if (VeldoriaItems.isRandomDisenchanter(scroll)) {
            ItemStack btn = new ItemStack(Material.KNOWLEDGE_BOOK);
            ItemMeta meta = btn.getItemMeta();
            meta.displayName(ColorUtils.format("&d&lСНЯТЬ СЛУЧАЙНЫЙ ЧАР"));
            meta.lore(List.of(ColorUtils.format("&7Нажмите, чтобы извлечь"), ColorUtils.format("&7одно случайное зачарование.")));
            btn.setItemMeta(meta);
            gui.setItem(SLOT_BUTTON, btn);
        }
        else if (VeldoriaItems.isSelectDisenchanter(scroll)) {
            gui.setItem(SLOT_BUTTON, createDecoItem(Material.BOOK, "&b&lВыберите чар снизу ⬇"));

            int index = 27;
            for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
                if (index >= 54) break;

                Enchantment ench = entry.getKey();
                int level = entry.getValue();

                ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
                EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
                meta.addStoredEnchant(ench, level, true);

                NamespacedKey key = ench.getKey();
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "ench_key"), PersistentDataType.STRING, key.toString());

                List<Component> lore = new ArrayList<>();
                for (String line : plugin.getConfig().getStringList("disenchant.gui-lore")) {
                    Component lineComp = ColorUtils.format(line)
                            .replaceText(b -> b.matchLiteral("%enchant%").replacement(ench.displayName(level)));
                    lore.add(lineComp);
                }
                meta.lore(lore);

                book.setItemMeta(meta);
                gui.setItem(index++, book);
            }
        }
        else {
            gui.setItem(SLOT_BUTTON, createDecoItem(Material.BARRIER, "&cНеверный свиток!"));
        }
    }

    private void processRandomDisenchant(Player player, Inventory gui) {
        ItemStack item = gui.getItem(SLOT_ITEM);
        ItemStack scroll = gui.getItem(SLOT_SCROLL);
        if (item == null || scroll == null) return;

        Map<Enchantment, Integer> enchants = item.getEnchantments();
        if (enchants.isEmpty()) return;

        List<Enchantment> keys = new ArrayList<>(enchants.keySet());
        Enchantment chosen = keys.get(ThreadLocalRandom.current().nextInt(keys.size()));

        doDisenchant(player, gui, item, scroll, chosen, enchants.get(chosen));
    }

    private void processSelectDisenchant(Player player, Inventory gui, ItemStack clickedBook) {
        if (clickedBook == null || !clickedBook.hasItemMeta()) return;

        String keyStr = clickedBook.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(plugin, "ench_key"), PersistentDataType.STRING);
        if (keyStr == null) return;

        ItemStack item = gui.getItem(SLOT_ITEM);
        ItemStack scroll = gui.getItem(SLOT_SCROLL);
        if (item == null || scroll == null) return;

        NamespacedKey key = NamespacedKey.fromString(keyStr);
        Enchantment enchant = org.bukkit.Registry.ENCHANTMENT.get(key);

        if (enchant == null || !item.containsEnchantment(enchant)) {
            player.sendMessage(ColorUtils.format("&cЭтот чар уже снят!"));
            updateGuiState(gui);
            return;
        }

        doDisenchant(player, gui, item, scroll, enchant, item.getEnchantmentLevel(enchant));
    }

    private void doDisenchant(Player player, Inventory gui, ItemStack item, ItemStack scroll, Enchantment enchant, int level) {
        ItemStack resultBook = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) resultBook.getItemMeta();
        meta.addStoredEnchant(enchant, level, true);
        resultBook.setItemMeta(meta);

        item.removeEnchantment(enchant);

        if (scroll.getAmount() > 1) {
            scroll.setAmount(scroll.getAmount() - 1);
        } else {
            gui.setItem(SLOT_SCROLL, null);
        }

        if (!player.getInventory().addItem(resultBook).isEmpty()) {
            player.getWorld().dropItem(player.getLocation(), resultBook);
            player.sendMessage(ColorUtils.getMsg("disenchant.inventory-full"));
        } else {
            Component msg = ColorUtils.getMsg("disenchant.received-book")
                    .replaceText(b -> b.matchLiteral("%enchant%").replacement(enchant.displayName(level)));
            player.sendMessage(msg);
        }

        player.playSound(player.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 1, 1);
        player.sendMessage(ColorUtils.getMsg("disenchant.success"));

        updateGuiState(gui);
    }

    public static class DisenchantHolder implements InventoryHolder {
        @Override
        public @NotNull Inventory getInventory() { return Bukkit.createInventory(null, 9); }
    }
}