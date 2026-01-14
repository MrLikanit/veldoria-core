package ru.veldoria.core.managers;

import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.registry.NamespacedId;
import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.api.user.SkillsUser;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import ru.veldoria.core.VeldoriaCore;
import ru.veldoria.core.utils.ColorUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class PrestigeManager {

    private final VeldoriaCore plugin;
    private final AuraSkillsApi auraSkills;

    public PrestigeManager(VeldoriaCore plugin) {
        this.plugin = plugin;
        this.auraSkills = plugin.getAuraSkills();
    }

    public int calculateReward(Player player) {
        SkillsUser user = auraSkills.getUser(player.getUniqueId());
        if (user == null) return 0;

        double totalShards = 0;
        ConfigurationSection mults = plugin.getConfig().getConfigurationSection("prestige-system.multipliers");
        if (mults == null) return 0;

        for (String skillName : mults.getKeys(false)) {
            NamespacedId id = NamespacedId.of("auraskills", skillName.toLowerCase(Locale.ROOT));
            Skill skill = auraSkills.getGlobalRegistry().getSkill(id);

            if (skill == null) {
                try {
                    String[] parts = skillName.split("/");
                    if (parts.length == 2) {
                        skill = auraSkills.getGlobalRegistry().getSkill(NamespacedId.of(parts[0], parts[1]));
                    }
                } catch (Exception ignored) {}
            }

            if (skill != null) {
                int level = user.getSkillLevel(skill);
                double multiplier = mults.getDouble(skillName, 0.0);
                totalShards += (level * multiplier);
            }
        }

        return (int) Math.round(totalShards);
    }

    public int getTotalLevel(Player player) {
        SkillsUser user = auraSkills.getUser(player.getUniqueId());
        if (user == null) return 0;
        return (int) user.getPowerLevel();
    }

    public void doPrestige(Player player) {
        if (!plugin.getConfig().getBoolean("prestige-system.enabled")) {
            player.sendMessage(ColorUtils.getMsg("prestige.disabled"));
            return;
        }

        int totalLevel = getTotalLevel(player);
        int minLevel = plugin.getConfig().getInt("prestige-system.min-total-level", 200);

        if (totalLevel < minLevel) {
            player.sendMessage(ColorUtils.getMsg("prestige.not-enough"));
            return;
        }

        int reward = calculateReward(player);

        plugin.getLogger().info("Prestige initiated for " + player.getName() + ". Reward: " + reward);

        SkillsUser user = auraSkills.getUser(player.getUniqueId());

        for (Skill skill : auraSkills.getGlobalRegistry().getSkills()) {
            try {
                if (user.getSkillLevel(skill) > 1) {
                    user.setSkillLevel(skill, 0);
                    user.setSkillXp(skill, 0);
                }
            } catch (Exception e) {
            }
        }

        if (reward > 0) {
            String command = String.format("points give %s %d -s", player.getName(), reward);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }

        logPrestige(player.getName(), totalLevel, reward);

        player.sendMessage(ColorUtils.getMsg("prestige.success", "%amount%", String.valueOf(reward)));

        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1, 1);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 0.5f);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 2f);

        player.closeInventory();
    }

    private void logPrestige(String playerName, int oldLevel, int shardsGiven) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            File logFile = new File(plugin.getDataFolder(), "prestige_logs.txt");
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();

            try (FileWriter fw = new FileWriter(logFile, true);
                 PrintWriter pw = new PrintWriter(fw)) {

                String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                pw.println(String.format("[%s] %s performed Prestige. Level: %d -> 0. Reward: %d shards.",
                        time, playerName, oldLevel, shardsGiven));

            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}