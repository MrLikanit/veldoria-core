package ru.veldoria.core.items;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import ru.veldoria.core.VeldoriaCore;
import ru.veldoria.core.utils.ColorUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
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

    public static ItemStack getEmptyPrism() {
        return createItem("items.soul-prism", "soul_prism_empty", false);
    }

    public static ItemStack getFilledPrism(LivingEntity entity) {
        VeldoriaCore plugin = VeldoriaCore.getInstance();
        FileConfiguration config = plugin.getConfig();

        String matName = config.getString("items.soul-prism.filled.material", "AMETHYST_CLUSTER");
        Material material = Material.getMaterial(matName);
        if (material == null) material = Material.AMETHYST_CLUSTER;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String mobKey = "entity." + entity.getType().getKey().getNamespace() + "." + entity.getType().getKey().getKey();
        String mobNameTag = entity.customName() != null
                ? ColorUtils.serialize(entity.customName())
                : "<translate:" + mobKey + ">";

        String nameRaw = config.getString("items.soul-prism.filled.name", "Prism")
                .replace("<mob>", mobNameTag);
        meta.displayName(ColorUtils.format(nameRaw));

        List<Component> lore = new ArrayList<>();
        double health = Math.round(entity.getHealth() * 10.0) / 10.0;

        for (String line : config.getStringList("items.soul-prism.filled.lore-header")) {
            lore.add(ColorUtils.format(line
                    .replace("<mob>", mobNameTag)
                    .replace("<health>", String.valueOf(health))
            ));
        }

        if (entity instanceof Villager villager) {
            String profKey = "entity.minecraft.villager." + villager.getProfession().getKey().getKey();
            lore.add(ColorUtils.format("&6Торговля (<translate:" + profKey + ">):"));

            String tradeFmt = plugin.getMessages().getString("catcher.villager-trade-format", "%buy1% -> %sell%");

            for (MerchantRecipe recipe : villager.getRecipes()) {
                String buy1 = getItemDescription(recipe.getIngredients().get(0));

                String buy2 = "";
                if (recipe.getIngredients().size() > 1 && recipe.getIngredients().get(1) != null && !recipe.getIngredients().get(1).getType().isAir()) {
                    buy2 = " + " + getItemDescription(recipe.getIngredients().get(1));
                }

                String sell = getItemDescription(recipe.getResult());

                lore.add(ColorUtils.format(tradeFmt
                        .replace("%buy1%", buy1)
                        .replace("%buy2%", buy2)
                        .replace("%sell%", sell)
                ));
            }
        }

        for (String line : config.getStringList("items.soul-prism.filled.lore-footer")) {
            lore.add(ColorUtils.format(line));
        }
        meta.lore(lore);

        int modelData = config.getInt("items.soul-prism.filled-model-data", 0);
        if (modelData != 0) meta.setCustomModelData(modelData);

        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "soul_prism_filled"), PersistentDataType.BOOLEAN, true);
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "stored_entity_type"), PersistentDataType.STRING, entity.getType().getKey().getKey());

        item.setItemMeta(meta);
        return item;
    }

    private static String getItemDescription(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return "";

        int amount = stack.getAmount();
        String prefix = stack.getType().isBlock() ? "block" : "item";
        String key = stack.getType().getKey().getKey();
        String namespace = stack.getType().getKey().getNamespace();

        String translatableItem = "<translate:" + prefix + "." + namespace + "." + key + ">";

        StringBuilder enchants = new StringBuilder();
        Map<Enchantment, Integer> enchMap = stack.getEnchantments();

        if (stack.getType() == Material.ENCHANTED_BOOK) {
            org.bukkit.inventory.meta.EnchantmentStorageMeta esm = (org.bukkit.inventory.meta.EnchantmentStorageMeta) stack.getItemMeta();
            if (esm != null) enchMap = esm.getStoredEnchants();
        }

        if (!enchMap.isEmpty()) {
            enchants.append(" <gray>(");
            int i = 0;
            for (Map.Entry<Enchantment, Integer> entry : enchMap.entrySet()) {
                String enchNs = entry.getKey().getKey().getNamespace();
                String enchId = entry.getKey().getKey().getKey();
                String enchTransKey = "enchantment." + enchNs + "." + enchId;

                String level = entry.getValue() > 0 ? " " + entry.getValue() : "";

                if (i > 0) enchants.append(", ");
                enchants.append("<translate:").append(enchTransKey).append(">").append(level);
                i++;
                if (i >= 2) {
                    enchants.append("...");
                    break;
                }
            }
            enchants.append(")");
        }

        return amount + "x " + translatableItem + enchants.toString();
    }


    public static boolean isSpawnerPickaxe(ItemStack item) { return checkNbt(item, VeldoriaCore.getInstance().pickaxeKey.getKey()); }
    public static boolean isPvpTrap(ItemStack item) { return checkNbt(item, "pvp_trap_item"); }
    public static boolean isRandomDisenchanter(ItemStack item) { return checkNbt(item, "random_disenchant_item"); }
    public static boolean isSelectDisenchanter(ItemStack item) { return checkNbt(item, "select_disenchant_item"); }
    public static boolean isDeathClock(ItemStack item) { return checkNbt(item, "death_clock_item"); }
    public static boolean isEmptyPrism(ItemStack item) { return checkNbt(item, "soul_prism_empty"); }
    public static boolean isFilledPrism(ItemStack item) { return checkNbt(item, "soul_prism_filled"); }

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
        for (String line : loreRaw) lore.add(ColorUtils.format(line));
        meta.lore(lore);
        int modelData = config.getInt(configPath + ".custom-model-data", 0);
        if (modelData != 0) meta.setCustomModelData(modelData);
        if (unstackable) meta.setMaxStackSize(1);
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