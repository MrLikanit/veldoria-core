package ru.veldoria.core.items;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import ru.veldoria.core.VeldoriaCore;

import java.util.ArrayList;
import java.util.List;

public class VeldoriaItems {

    public static ItemStack getSpawnerPickaxe() {
        VeldoriaCore plugin = VeldoriaCore.getInstance();
        FileConfiguration config = plugin.getConfig();
        var mm = MiniMessage.miniMessage();

        String matName = config.getString("items.spawner-pickaxe.material", "GOLDEN_PICKAXE");
        Material material = Material.getMaterial(matName);
        if (material == null) material = Material.GOLDEN_PICKAXE;

        ItemStack pickaxe = new ItemStack(material);
        ItemMeta meta = pickaxe.getItemMeta();

        String nameRaw = config.getString("items.spawner-pickaxe.name", "<gold>Extract Pickaxe");
        meta.displayName(mm.deserialize(nameRaw));

        List<String> loreRaw = config.getStringList("items.spawner-pickaxe.lore");
        List<Component> lore = new ArrayList<>();
        for (String line : loreRaw) {
            lore.add(mm.deserialize(line));
        }
        meta.lore(lore);

        // CustomModelData
        int modelData = config.getInt("items.spawner-pickaxe.custom-model-data", 0);
        if (modelData != 0) {
            meta.setCustomModelData(modelData);
        }

        meta.getPersistentDataContainer().set(plugin.pickaxeKey, PersistentDataType.BOOLEAN, true);

        pickaxe.setItemMeta(meta);
        return pickaxe;
    }
}