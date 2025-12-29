package ru.veldoria.core;

import dev.aurelium.auraskills.api.AuraSkillsApi;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ru.veldoria.core.commands.VeldoriaCommand;
import ru.veldoria.core.listeners.PvpTrapListener;
import ru.veldoria.core.listeners.SpawnerInteractionListener;
import ru.veldoria.core.managers.ArenaManager;
import ru.veldoria.core.utils.ProtectionHook;

import java.io.File;

public final class VeldoriaCore extends JavaPlugin {

    private static VeldoriaCore instance;
    private AuraSkillsApi auraSkills;
    private ProtectionHook protectionHook;
    private ArenaManager arenaManager;
    private YamlConfiguration messagesConfig;

    public final NamespacedKey pickaxeKey = new NamespacedKey(this, "spawner_extractor");
    public final NamespacedKey pityKey = new NamespacedKey(this, "mining_pity_bonus");

    @Override
    public void onLoad() {
        try {
            if (Bukkit.getPluginManager().getPlugin("WorldGuard") != null) {
                ProtectionHook.registerWgFlags();
                getLogger().info("Custom WorldGuard flags registered.");
            }
        } catch (Throwable e) {
            getLogger().warning("WG flags error (ignore if WG not installed).");
        }
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        loadMessages(); // Загружаем сообщения

        if (Bukkit.getPluginManager().getPlugin("AuraSkills") != null) {
            auraSkills = AuraSkillsApi.get();
        }

        protectionHook = new ProtectionHook();
        arenaManager = new ArenaManager(this);

        getCommand("veldoriacore").setExecutor(new VeldoriaCommand());

        getServer().getPluginManager().registerEvents(new SpawnerInteractionListener(this), this);
        getServer().getPluginManager().registerEvents(new PvpTrapListener(this), this);
    }

    @Override
    public void onDisable() {
        if (arenaManager != null) arenaManager.disable();
    }

    public void loadMessages() {
        File file = new File(getDataFolder(), "messages.yml");
        if (!file.exists()) {
            saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(file);
    }

    public YamlConfiguration getMessages() {
        return messagesConfig;
    }

    public static VeldoriaCore getInstance() { return instance; }
    public AuraSkillsApi getAuraSkills() { return auraSkills; }
    public ProtectionHook getProtectionHook() { return protectionHook; }
    public ArenaManager getArenaManager() { return arenaManager; }
}