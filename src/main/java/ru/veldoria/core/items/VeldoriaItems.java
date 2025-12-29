package ru.veldoria.core.items;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import ru.veldoria.core.VeldoriaCore;
import ru.veldoria.core.utils.ColorUtils;

import java.util.ArrayList;
import java.util.List;

public class VeldoriaItems {


    public static ItemStack getSpawnerPickaxe() {
        return createItem("items.spawner-pickaxe", VeldoriaCore.getInstance().pickaxeKey.getKey(), false);
    }

    public static ItemStack getPvpTrap() {
        return createItem("items.pvp-trap", "pvp_trap_item", false);
    }

    public static ItemStack getRandomDisenchanter() {
        return createItem("items.random-disenchanter", "random_disenchant_item", false);
    }

    public static ItemStack getSelectDisenchanter() {
        return createItem("items.select-disenchanter", "select_disenchant_item", false);
    }

    public static ItemStack getDeathClock() {
        return createItem("items.death-clock", "death_clock_item", true);
    }


    public static boolean isSpawnerPickaxe(ItemStack item) {
        return checkNbt(item, VeldoriaCore.getInstance().pickaxeKey.getKey());
    }

    public static boolean isPvpTrap(ItemStack item) {
        return checkNbt(item, "pvp_trap_item");
    }

    public static boolean isRandomDisenchanter(ItemStack item) {
        return checkNbt(item, "random_disenchant_item");
    }

    public static boolean isSelectDisenchanter(ItemStack item) {
        return checkNbt(item, "select_disenchant_item");
    }

    public static boolean isDeathClock(ItemStack item) {
        return checkNbt(item, "death_clock_item");
    }


    private static ItemStack createItem(String configPath, String nbtKey, boolean unstackable) {
        VeldoriaCore plugin = VeldoriaCore.getInstance();
        FileConfiguration config = plugin.getConfig();

        String matName = config.getString(configPath + ".material", "PAPER");
        Material material = Material.getMaterial(matName);
        if (material == null) material = Material.PAPER;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String nameRaw = config.getString(configPath + ".name", "Item");
        meta.displayName(ColorUtils.format(nameRaw));

        List<String> loreRaw = config.getStringList(configPath + ".lore");
        List<Component> lore = new ArrayList<>();
        for (String line : loreRaw) {
            lore.add(ColorUtils.format(line));
        }
        meta.lore(lore);

        int modelData = config.getInt(configPath + ".custom-model-data", 0);
        if (modelData != 0) meta.setCustomModelData(modelData);

        if (unstackable) {
            meta.setMaxStackSize(1);
        }

        NamespacedKey key = new NamespacedKey(plugin, nbtKey);
        meta.getPersistentDataContainer().set(key, PersistentDataType.BOOLEAN, true);

        item.setItemMeta(meta);
        return item;
    }

    private static boolean checkNbt(ItemStack item, String keyName) {
        if (item == null || !item.hasItemMeta()) return false;
        NamespacedKey key = new NamespacedKey(VeldoriaCore.getInstance(), keyName);
        return item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BOOLEAN);
    }
}