package ru.veldoria.core.items;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import ru.veldoria.core.VeldoriaCore;

import java.util.List;

public class VeldoriaItems {

    public static ItemStack getSpawnerPickaxe() {
        VeldoriaCore plugin = VeldoriaCore.getInstance();
        ItemStack pickaxe = new ItemStack(Material.GOLDEN_PICKAXE);
        ItemMeta meta = pickaxe.getItemMeta();

        meta.displayName(Component.text("⛏ Экстрактор Спавнеров", NamedTextColor.GOLD));
        meta.lore(List.of(
                Component.text("Нажмите ПКМ по спавнеру,", NamedTextColor.GRAY),
                Component.text("чтобы попытаться добыть его.", NamedTextColor.GRAY),
                Component.text("Одноразовый предмет.", NamedTextColor.DARK_RED)
        ));

        meta.getPersistentDataContainer().set(plugin.pickaxeKey, PersistentDataType.BOOLEAN, true);

        pickaxe.setItemMeta(meta);
        return pickaxe;
    }
}