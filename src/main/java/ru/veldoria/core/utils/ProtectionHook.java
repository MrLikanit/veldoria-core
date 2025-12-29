package ru.veldoria.core.utils;

import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.utils.PlayerCacheUtil;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class ProtectionHook {

    private final boolean hasWorldGuard;
    private final boolean hasTowny;

    public ProtectionHook() {
        this.hasWorldGuard = Bukkit.getPluginManager().getPlugin("WorldGuard") != null;
        this.hasTowny = Bukkit.getPluginManager().getPlugin("Towny") != null;
    }

    public boolean canInteract(Player player, Block block) {
        if (hasWorldGuard) {
            if (!canWorldGuard(player, block)) return false;
        }

        if (hasTowny) {
            if (!canTowny(player, block)) return false;
        }

        return true;
    }

    private boolean canWorldGuard(Player player, Block block) {
        try {
            RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
            com.sk89q.worldedit.util.Location loc = BukkitAdapter.adapt(block.getLocation());

            return query.testState(loc, WorldGuardPlugin.inst().wrapPlayer(player), Flags.BUILD);
        } catch (Throwable e) {
            return true;
        }
    }

    private boolean canTowny(Player player, Block block) {
        try {
            return PlayerCacheUtil.getCachePermission(player, block.getLocation(), Material.SPAWNER, TownyPermission.ActionType.DESTROY);
        } catch (Throwable e) {
            return true;
        }
    }
}