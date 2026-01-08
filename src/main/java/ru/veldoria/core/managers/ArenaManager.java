package ru.veldoria.core.managers;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
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

    public ArenaManager(VeldoriaCore plugin) {
        this.plugin = plugin;
        startTicker();
    }

    public boolean canPlaceArena(Location center, double newRadius) {
        for (ActiveArena arena : arenas) {
            if (!arena.center.getWorld().equals(center.getWorld())) continue;

            double distanceSq = arena.center.distanceSquared(center);
            double minDistance = arena.radius + newRadius;

            if (distanceSq < minDistance * minDistance) {
                return false;
            }
        }
        return true;
    }

    public void createArena(Location center) {
        double radius = plugin.getConfig().getDouble("items.pvp-trap.settings.radius", 8.0);
        int durationSec = plugin.getConfig().getInt("items.pvp-trap.settings.duration", 20);

        ActiveArena arena = new ActiveArena(center, radius, System.currentTimeMillis() + (durationSec * 1000L));
        calculateWallCircle(arena);
        arenas.add(arena);

        center.getWorld().playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 2f, 0.5f);
        center.getWorld().playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.5f, 0.5f);

        scanForNewVictims(arena);
        updateDynamicWall(arena, false);
    }

    public void disable() {
        for (ActiveArena arena : arenas) {
            updateDynamicWall(arena, true);
        }
        arenas.clear();
    }

    public boolean isInsideArena(Player player) {
        for (ActiveArena arena : arenas) {
            if (!arena.center.getWorld().equals(player.getWorld())) continue;
            if (arena.participants.contains(player.getUniqueId())) {
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
                        updateDynamicWall(arena, true);
                        clearCooldowns(arena);
                        it.remove();
                        continue;
                    }

                    if (ticks % 5 == 0) {
                        updateDynamicWall(arena, false);
                        drawArenaEffects(arena);
                        scanForNewVictims(arena);
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

    private void scanForNewVictims(ActiveArena arena) {
        double radiusSq = arena.radius * arena.radius;

        for (Player p : arena.center.getWorld().getPlayers()) {
            if (arena.participants.contains(p.getUniqueId())) continue;
            if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) continue;
            if (p.isDead()) continue;

            double distX = p.getLocation().getX() - arena.center.getX();
            double distZ = p.getLocation().getZ() - arena.center.getZ();

            if ((distX * distX) + (distZ * distZ) <= radiusSq) {
                if (!plugin.getProtectionHook().canPvp(p, p.getLocation())) {
                    continue;
                }

                arena.participants.add(p.getUniqueId());
                ColorUtils.sendActionBar(p, "&cВы попали в ловушку!");
            }
        }
    }

    private void checkPlayers(ActiveArena arena, long now) {
        double radiusSq = arena.radius * arena.radius;
        double warningRadiusSq = (arena.radius - 1.0) * (arena.radius - 1.0);

        Iterator<UUID> it = arena.participants.iterator();
        while (it.hasNext()) {
            UUID uid = it.next();
            Player player = Bukkit.getPlayer(uid);

            if (player == null || !player.isOnline()) {
                it.remove();
                continue;
            }

            if (player.isDead()) {
                it.remove();
                continue;
            }

            if (!player.getWorld().equals(arena.center.getWorld())) {
                it.remove();
                continue;
            }

            if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
                it.remove();
                clearWallForPlayer(player, arena);
                continue;
            }

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

    private void clearCooldowns(ActiveArena arena) {
        for (Player p : arena.center.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(arena.center) < 2500) {
                if (p.hasCooldown(Material.WIND_CHARGE)) p.setCooldown(Material.WIND_CHARGE, 0);
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

    private void updateDynamicWall(ActiveArena arena, boolean forceRemove) {
        double showThreshold = arena.radius - 4.0;
        double showThresholdSq = showThreshold * showThreshold;

        for (Player p : arena.center.getWorld().getPlayers()) {
            double distX = p.getLocation().getX() - arena.center.getX();
            double distZ = p.getLocation().getZ() - arena.center.getZ();
            double distSq = (distX * distX) + (distZ * distZ);

            if (distSq > 2500) continue;

            UUID uid = p.getUniqueId();
            boolean wasSeeing = arena.viewers.contains(uid);

            if (forceRemove) {
                if (wasSeeing) {
                    sendWallPackets(p, arena, false);
                    arena.viewers.remove(uid);
                }
                continue;
            }

            boolean nearEdge = distSq > showThresholdSq && distSq < (arena.radius + 15) * (arena.radius + 15);

            if (nearEdge) {
                sendWallPackets(p, arena, true);
                arena.viewers.add(uid);
            } else {
                if (wasSeeing) {
                    sendWallPackets(p, arena, false);
                    arena.viewers.remove(uid);
                }
            }
        }
    }

    private void clearWallForPlayer(Player p, ActiveArena arena) {
        if (arena.viewers.contains(p.getUniqueId())) {
            sendWallPackets(p, arena, false);
            arena.viewers.remove(p.getUniqueId());
        }
    }

    private void sendWallPackets(Player p, ActiveArena arena, boolean show) {
        int playerY = p.getLocation().getBlockY();
        int down = show ? 3 : 10;
        int up = show ? 4 : 10;

        for (Vector offset : arena.cachedCircleOffsets) {
            double x = arena.center.getX() + offset.getX();
            double z = arena.center.getZ() + offset.getZ();

            double dx = p.getLocation().getX() - x;
            double dz = p.getLocation().getZ() - z;

            if ((dx * dx) + (dz * dz) > 36) {
                if (show) continue;
            }

            for (int yOffset = -down; yOffset <= up; yOffset++) {
                Location loc = new Location(arena.center.getWorld(), x, playerY + yOffset, z);
                if (show) {
                    if (!loc.getBlock().getType().isSolid()) p.sendBlockChange(loc, BARRIER_BLOCK);
                    else p.sendBlockChange(loc, loc.getBlock().getBlockData());
                } else {
                    p.sendBlockChange(loc, loc.getBlock().getBlockData());
                }
            }
        }
    }

    private void teleportBack(Player player, Location center) {
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
        final Set<UUID> viewers = new HashSet<>();
        final Set<UUID> participants = new HashSet<>();

        public ActiveArena(Location center, double radius, long expiryTime) {
            this.center = center;
            this.radius = radius;
            this.expiryTime = expiryTime;
        }
    }
}