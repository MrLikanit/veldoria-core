package ru.veldoria.core.managers;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import ru.veldoria.core.VeldoriaCore;
import ru.veldoria.core.utils.ColorUtils;

import java.util.*;

public class ArenaManager {

    private final VeldoriaCore plugin;
    private final List<ActiveArena> arenas = new ArrayList<>();
    private final BlockData BARRIER_BLOCK = Material.RED_STAINED_GLASS.createBlockData();
    private final Map<UUID, Long> lastPushTime = new HashMap<>();

    private final Map<UUID, Set<Location>> playerVisibleBarriers = new HashMap<>();

    public ArenaManager(VeldoriaCore plugin) {
        this.plugin = plugin;
        startTicker();
    }

    public void createArena(Location center) {
        double radius = plugin.getConfig().getDouble("items.pvp-trap.settings.radius", 8.0);
        int durationSec = plugin.getConfig().getInt("items.pvp-trap.settings.duration", 20);

        ActiveArena arena = new ActiveArena(center, radius, System.currentTimeMillis() + (durationSec * 1000L));
        calculateWallCircle(arena);
        arenas.add(arena);

        center.getWorld().playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 2f, 0.5f);
        center.getWorld().playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.5f, 0.5f);

        updateSmartWall(arena, false);
    }

    public void disable() {
        for (UUID uuid : playerVisibleBarriers.keySet()) {
            Player p = plugin.getServer().getPlayer(uuid);
            if (p != null) clearWallForPlayer(p);
        }
        playerVisibleBarriers.clear();
        arenas.clear();
    }

    public boolean isInsideArena(Player player) {
        for (ActiveArena arena : arenas) {
            if (!arena.center.getWorld().equals(player.getWorld())) continue;
            double distX = arena.center.getX() - player.getLocation().getX();
            double distZ = arena.center.getZ() - player.getLocation().getZ();
            if ((distX * distX) + (distZ * distZ) <= (arena.radius * arena.radius)) {
                return true;
            }
        }
        return false;
    }

    private void startTicker() {
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (arenas.isEmpty()) return;

                long now = System.currentTimeMillis();
                Iterator<ActiveArena> it = arenas.iterator();

                while (it.hasNext()) {
                    ActiveArena arena = it.next();

                    if (now > arena.expiryTime) {
                        arena.center.getWorld().playSound(arena.center, Sound.BLOCK_BEACON_DEACTIVATE, 1, 1);
                        cleanupArenaViewers(arena);
                        clearCooldowns(arena);
                        it.remove();
                        continue;
                    }

                    updateSmartWall(arena, false);

                    if (ticks % 5 == 0) {
                        drawArenaEffects(arena);
                    }

                    checkPlayers(arena, now);
                }

                if (ticks % 100 == 0) {
                    lastPushTime.entrySet().removeIf(entry -> now - entry.getValue() > 5000);
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 2L);
    }

    private void clearCooldowns(ActiveArena arena) {
        for (Player p : arena.center.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(arena.center) < 2500) {
                if (p.hasCooldown(Material.WIND_CHARGE)) p.setCooldown(Material.WIND_CHARGE, 0);
            }
        }
    }

    private void cleanupArenaViewers(ActiveArena arena) {
        for (Player p : arena.center.getWorld().getPlayers()) {
            if (playerVisibleBarriers.containsKey(p.getUniqueId())) {
                clearWallForPlayer(p);
            }
        }
    }

    private void calculateWallCircle(ActiveArena arena) {
        int steps = 72;
        for (int i = 0; i < steps; i++) {
            double angle = (2 * Math.PI / steps) * i;
            double x = arena.radius * Math.cos(angle);
            double z = arena.radius * Math.sin(angle);
            arena.cachedCircleOffsets.add(new Vector(x, 0, z));
        }
    }

    private void updateSmartWall(ActiveArena arena, boolean forceRemove) {
        if (forceRemove) return;

        for (Player p : arena.center.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(arena.center) > 2500) {
                if (playerVisibleBarriers.containsKey(p.getUniqueId())) clearWallForPlayer(p);
                continue;
            }

            Set<Location> newBlocks = calculateVisibleWallBlocks(p, arena);

            Set<Location> oldBlocks = playerVisibleBarriers.getOrDefault(p.getUniqueId(), new HashSet<>());

            for (Location loc : oldBlocks) {
                if (!newBlocks.contains(loc)) {
                    p.sendBlockChange(loc, loc.getBlock().getBlockData());
                }
            }

            for (Location loc : newBlocks) {
                if (!oldBlocks.contains(loc)) {
                    if (!loc.getBlock().getType().isSolid()) {
                        p.sendBlockChange(loc, BARRIER_BLOCK);
                    }
                }
            }

            if (newBlocks.isEmpty()) {
                playerVisibleBarriers.remove(p.getUniqueId());
            } else {
                playerVisibleBarriers.put(p.getUniqueId(), newBlocks);
            }
        }
    }

    private Set<Location> calculateVisibleWallBlocks(Player p, ActiveArena arena) {
        Set<Location> blocks = new HashSet<>();
        Location playerLoc = p.getLocation();
        int playerY = playerLoc.getBlockY();

        for (Vector offset : arena.cachedCircleOffsets) {
            double wallX = arena.center.getX() + offset.getX();
            double wallZ = arena.center.getZ() + offset.getZ();

            double dx = playerLoc.getX() - wallX;
            double dz = playerLoc.getZ() - wallZ;
            double distSq = (dx * dx) + (dz * dz);

            if (distSq <= 9) {
                for (int y = -2; y <= 4; y++) {
                    blocks.add(new Location(arena.center.getWorld(), wallX, playerY + y, wallZ));
                }
            }
        }
        return blocks;
    }

    private void clearWallForPlayer(Player p) {
        Set<Location> oldBlocks = playerVisibleBarriers.remove(p.getUniqueId());
        if (oldBlocks != null) {
            for (Location loc : oldBlocks) {
                p.sendBlockChange(loc, loc.getBlock().getBlockData());
            }
        }
    }

    private void checkPlayers(ActiveArena arena, long now) {
        double radiusSq = arena.radius * arena.radius;
        double warningRadiusSq = (arena.radius - 1.0) * (arena.radius - 1.0);

        for (Player player : arena.center.getWorld().getPlayers()) {
            double distX = player.getLocation().getX() - arena.center.getX();
            double distZ = player.getLocation().getZ() - arena.center.getZ();
            double dist2DSq = (distX * distX) + (distZ * distZ);

            if (dist2DSq > radiusSq) {
                teleportBack(player, arena.center);
                continue;
            }

            if (dist2DSq > warningRadiusSq) {
                pushPlayerBack(player, arena.center, now);
            }
        }
    }

    private void teleportBack(Player player, Location center) {
        Vector direction = player.getLocation().toVector().subtract(center.toVector()).normalize();
        Location safeCenter = center.clone();
        safeCenter.setY(player.getLocation().getY());
        safeCenter.setDirection(player.getLocation().getDirection());

        player.teleport(safeCenter);
        ColorUtils.sendActionBar(player, "trap.escape-deny");
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
    }

    private void pushPlayerBack(Player player, Location center, long now) {
        Vector dir = center.toVector().subtract(player.getLocation().toVector());
        dir.setY(0).normalize().multiply(0.8);
        dir.setY(0.2);

        player.setVelocity(new Vector(0,0,0));
        player.setVelocity(dir);

        if (now - lastPushTime.getOrDefault(player.getUniqueId(), 0L) > 500) {
            player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.3f, 0.5f);
            ColorUtils.sendActionBar(player, "trap.wall-hit");
            lastPushTime.put(player.getUniqueId(), now);
        }
    }

    private void drawArenaEffects(ActiveArena arena) {
        for (int i = 0; i < 360; i += 30) {
            double angle = Math.toRadians(i);
            double x = arena.radius * Math.cos(angle);
            double z = arena.radius * Math.sin(angle);
            Location loc = arena.center.clone().add(x, 0.5, z);
            arena.center.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 1, 0, 0, 0, 0);
        }
    }

    private static class ActiveArena {
        final Location center;
        final double radius;
        final long expiryTime;
        final List<Vector> cachedCircleOffsets = new ArrayList<>();

        public ActiveArena(Location center, double radius, long expiryTime) {
            this.center = center;
            this.radius = radius;
            this.expiryTime = expiryTime;
        }
    }
}