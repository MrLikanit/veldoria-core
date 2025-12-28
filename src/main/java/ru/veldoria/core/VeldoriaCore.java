package ru.veldoria.core;

import dev.aurelium.auraskills.api.AuraSkillsApi;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import ru.veldoria.core.commands.VeldoriaCommand;
import ru.veldoria.core.listeners.SpawnerInteractionListener;
import ru.veldoria.core.utils.ProtectionHook;

public final class VeldoriaCore extends JavaPlugin {

    private static VeldoriaCore instance;
    private AuraSkillsApi auraSkills;
    private ProtectionHook protectionHook;

    public final NamespacedKey pickaxeKey = new NamespacedKey(this, "spawner_extractor");
    public final NamespacedKey pityKey = new NamespacedKey(this, "mining_pity_bonus");

    @Override
    public void onEnable() {
        instance = this;

        if (Bukkit.getPluginManager().getPlugin("AuraSkills") != null) {
            auraSkills = AuraSkillsApi.get();
            getLogger().info("AuraSkills найден.");
        }

        protectionHook = new ProtectionHook();
        getLogger().info("Защита регионов (WG/Towny) инициализирована.");

        getCommand("veldoriacore").setExecutor(new VeldoriaCommand());

        getServer().getPluginManager().registerEvents(new SpawnerInteractionListener(this), this);
    }

    public static VeldoriaCore getInstance() {
        return instance;
    }

    public AuraSkillsApi getAuraSkills() {
        return auraSkills;
    }

    public ProtectionHook getProtectionHook() {
        return protectionHook;
    }
}