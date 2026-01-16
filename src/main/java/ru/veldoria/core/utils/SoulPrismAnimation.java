package ru.veldoria.core.utils;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import ru.veldoria.core.VeldoriaCore;

public class SoulPrismAnimation {

    /**
     * Анимация всасывания души
     */
    public static void playCatchAnimation(Entity target) {
        new BukkitRunnable() {
            double radius = 1.5;
            double angle = 0;
            final Location center = target.getLocation().add(0, 1, 0);

            @Override
            public void run() {
                if (radius <= 0.1) {
                    center.getWorld().spawnParticle(Particle.SOUL, center, 10, 0.1, 0.1, 0.1, 0.05);
                    center.getWorld().playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1, 1.5f);
                    this.cancel();
                    return;
                }

                for (int i = 0; i < 2; i++) {
                    double currentAngle = angle + (i * Math.PI);
                    double x = radius * Math.cos(currentAngle);
                    double z = radius * Math.sin(currentAngle);

                    Location particleLoc = center.clone().add(x, (radius - 1.5) * 0.5, z);

                    center.getWorld().spawnParticle(Particle.SCULK_SOUL, particleLoc, 1, 0, 0, 0, 0);
                }

                radius -= 0.1;
                angle += 0.5;
            }
        }.runTaskTimer(VeldoriaCore.getInstance(), 0L, 1L);
    }

    /**
     * Анимация освобождения
     */
    public static void playReleaseAnimation(Location center) {
        center.getWorld().spawnParticle(Particle.END_ROD, center, 15, 0.2, 1, 0.2, 0.1);
        center.getWorld().playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1, 2f);

        new BukkitRunnable() {
            double radius = 0.5;

            @Override
            public void run() {
                if (radius > 2.0) {
                    this.cancel();
                    return;
                }

                for (int degree = 0; degree < 360; degree += 20) {
                    double rad = Math.toRadians(degree);
                    double x = radius * Math.cos(rad);
                    double z = radius * Math.sin(rad);

                    Location loc = center.clone().add(x, 0.5, z);
                    center.getWorld().spawnParticle(Particle.WITCH, loc, 1, 0, 0, 0, 0);
                }

                radius += 0.2;
            }
        }.runTaskTimer(VeldoriaCore.getInstance(), 0L, 1L);
    }
}