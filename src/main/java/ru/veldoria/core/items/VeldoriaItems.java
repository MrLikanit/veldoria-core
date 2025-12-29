package ru.veldoria.core.items;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import ru.veldoria.core.VeldoriaCore;
import ru.veldoria.core.utils.ColorUtils; // Импорт утилиты

import java.util.ArrayList;
import java.util.List;

public class VeldoriaItems {

    public static ItemStack getSpawnerPickaxe() {
        VeldoriaCore plugin = VeldoriaCore.getInstance();
        FileConfiguration config = plugin.getConfig();

        String matName = config.getString("items.spawner-pickaxe.material", "GOLDEN_PICKAXE");
        Material material = Material.getMaterial(matName);
        if (material == null) material = Material.GOLDEN_PICKAXE;

        ItemStack pickaxe = new ItemStack(material);
        ItemMeta meta = pickaxe.getItemMeta();

        String nameRaw = config.getString("items.spawner-pickaxe.name", "&6⛏ Экстрактор Спавнеров");
        meta.displayName(ColorUtils.format(nameRaw));

        List<String> loreRaw = config.getStringList("items.spawner-pickaxe.lore");
        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        for (String line : loreRaw) {
            lore.add(ColorUtils.format(line));
        }
        meta.lore(lore);

        int modelData = config.getInt("items.spawner-pickaxe.custom-model-data", 0);
        if (modelData != 0) meta.setCustomModelData(modelData);

        meta.getPersistentDataContainer().set(plugin.pickaxeKey, PersistentDataType.BOOLEAN, true);

        pickaxe.setItemMeta(meta);
        return pickaxe;
    }

    public static ItemStack getPvpTrap() {
        VeldoriaCore plugin = VeldoriaCore.getInstance();
        FileConfiguration config = plugin.getConfig();

        String matName = config.getString("items.pvp-trap.material", "PRISMARINE_SHARD");
        Material material = Material.getMaterial(matName);
        if (material == null) material = Material.PRISMARINE_SHARD;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String nameRaw = config.getString("items.pvp-trap.name", "&#FF0000☠ Якорь Бездны");
        meta.displayName(ColorUtils.format(nameRaw));

        List<String> loreRaw = config.getStringList("items.pvp-trap.lore");
        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        for (String line : loreRaw) {
            lore.add(ColorUtils.format(line));
        }
        meta.lore(lore);

        int modelData = config.getInt("items.pvp-trap.custom-model-data", 0);
        if (modelData != 0) meta.setCustomModelData(modelData);

        NamespacedKey key = new NamespacedKey(plugin, "pvp_trap_item");
        meta.getPersistentDataContainer().set(key, PersistentDataType.BOOLEAN, true);

        item.setItemMeta(meta);
        return item;
    }

    public static boolean isPvpTrap(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        NamespacedKey key = new NamespacedKey(VeldoriaCore.getInstance(), "pvp_trap_item");
        return item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BOOLEAN);
    }
}