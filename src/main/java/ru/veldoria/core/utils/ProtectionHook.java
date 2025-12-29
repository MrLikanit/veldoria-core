package ru.veldoria.core.utils;

import com.palmergames.bukkit.towny.TownyAPI; // НОВЫЙ ИМПОРТ
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.utils.PlayerCacheUtil;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class ProtectionHook {

    private final boolean hasWorldGuard;
    private final boolean hasTowny;

    public static StateFlag TRAP_FLAG;
    public static StateFlag PICKAXE_FLAG;

    public ProtectionHook() {
        this.hasWorldGuard = Bukkit.getPluginManager().getPlugin("WorldGuard") != null;
        this.hasTowny = Bukkit.getPluginManager().getPlugin("Towny") != null;
    }

    public static void registerWgFlags() {
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") == null) return;

        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        try {
            StateFlag trapFlag = new StateFlag("veldoria-trap", true);
            registry.register(trapFlag);
            TRAP_FLAG = trapFlag;
        } catch (FlagConflictException e) {
            Flag<?> existing = registry.get("veldoria-trap");
            if (existing instanceof StateFlag) {
                TRAP_FLAG = (StateFlag) existing;
            }
        }

        try {
            StateFlag pickFlag = new StateFlag("veldoria-pickaxe", true);
            registry.register(pickFlag);
            PICKAXE_FLAG = pickFlag;
        } catch (FlagConflictException e) {
            Flag<?> existing = registry.get("veldoria-pickaxe");
            if (existing instanceof StateFlag) {
                PICKAXE_FLAG = (StateFlag) existing;
            }
        }
    }

    public boolean canInteract(Player player, Block block) {
        if (hasWorldGuard) {
            if (!checkCustomFlag(player, block.getLocation(), PICKAXE_FLAG)) return false;
            if (!canWorldGuardBuild(player, block)) return false;
        }
        if (hasTowny) {
            if (!canTownyBuild(player, block)) return false;
        }
        return true;
    }

    public boolean canPvp(Player player, Location loc) {
        if (hasWorldGuard) {
            if (!checkCustomFlag(player, loc, TRAP_FLAG)) return false;
            if (!canWorldGuardPvp(player, loc)) return false;
        }
        if (hasTowny) {
            if (!canTownyPvp(loc)) return false;
        }
        return true;
    }


    private boolean checkCustomFlag(Player player, Location bukkitLoc, StateFlag flag) {
        if (flag == null) return true;
        try {
            RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
            com.sk89q.worldedit.util.Location loc = BukkitAdapter.adapt(bukkitLoc);
            return query.testState(loc, WorldGuardPlugin.inst().wrapPlayer(player), flag);
        } catch (Throwable e) { return true; }
    }

    private boolean canWorldGuardBuild(Player player, Block block) {
        try {
            RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
            com.sk89q.worldedit.util.Location loc = BukkitAdapter.adapt(block.getLocation());
            return query.testState(loc, WorldGuardPlugin.inst().wrapPlayer(player), Flags.BUILD);
        } catch (Throwable e) { return true; }
    }

    private boolean canWorldGuardPvp(Player player, Location bukkitLoc) {
        try {
            RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
            com.sk89q.worldedit.util.Location loc = BukkitAdapter.adapt(bukkitLoc);
            return query.testState(loc, WorldGuardPlugin.inst().wrapPlayer(player), Flags.PVP);
        } catch (Throwable e) { return true; }
    }

    private boolean canTownyBuild(Player player, Block block) {
        try {
            return PlayerCacheUtil.getCachePermission(player, block.getLocation(), Material.SPAWNER, TownyPermission.ActionType.DESTROY);
        } catch (Throwable e) { return true; }
    }

    private boolean canTownyPvp(Location loc) {
        try {
            return TownyAPI.getInstance().isPVP(loc);
        } catch (Throwable e) { return true; }
    }
}