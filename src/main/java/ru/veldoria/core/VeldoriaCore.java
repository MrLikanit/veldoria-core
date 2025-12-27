package ru.veldoria.core;

import dev.aurelium.auraskills.api.AuraSkillsApi;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import ru.veldoria.core.commands.VeldoriaCommand;
import ru.veldoria.core.listeners.SpawnerInteractionListener;

public final class VeldoriaCore extends JavaPlugin {

    private static VeldoriaCore instance;
    private AuraSkillsApi auraSkills;

    // Ключ для проверки кирки
    public final NamespacedKey pickaxeKey = new NamespacedKey(this, "spawner_extractor");
    // Ключ для накопления бонуса неудач (Bad Luck Protection)
    public final NamespacedKey pityKey = new NamespacedKey(this, "mining_pity_bonus");

    @Override
    public void onEnable() {
        instance = this;

        if (Bukkit.getPluginManager().getPlugin("AuraSkills") != null) {
            auraSkills = AuraSkillsApi.get();
            getLogger().info("AuraSkills подключен.");
        }

        // Регистрируем новую единую команду
        getCommand("veldoria").setExecutor(new VeldoriaCommand(this));

        getServer().getPluginManager().registerEvents(new SpawnerInteractionListener(this), this);
    }

    public static VeldoriaCore getInstance() {
        return instance;
    }

    public AuraSkillsApi getAuraSkills() {
        return auraSkills;
    }
}