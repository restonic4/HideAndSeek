package dev.restonic4.hide_and_seek.manager;

import dev.restonic4.hide_and_seek.HideAndSeekPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class AreaManager {
    private final GameManager gameManager;

    public AreaManager(GameManager gameManager) {
        this.gameManager = gameManager;
        startParticleTask();
    }

    public void setSubAreaEnabled(CuboidArea area, boolean enabled) {
        area.setEnabled(enabled);
        if (!enabled) {
            Bukkit.broadcast(Component.text("Area " + area.getName() + " has been DISABLED!", NamedTextColor.RED));

            // Kill hiders inside
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (gameManager.isHider(player) && area.isInside(player.getLocation())) {
                    gameManager.catchHider(player);
                    player.sendMessage(
                            Component.text("You were caught because you were in a disabled area!", NamedTextColor.RED));
                }
            }
        } else {
            Bukkit.broadcast(Component.text("Area " + area.getName() + " has been ENABLED!", NamedTextColor.GREEN));
        }
    }

    private void startParticleTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    for (CuboidArea area : GameAreas.getSubAreas()) {
                        if (!area.isEnabled()) {
                            spawnParticlesForPlayer(player, area);
                        }
                    }
                }
            }
        }.runTaskTimer(HideAndSeekPlugin.instance, 0L, 5L);
    }

    private void spawnParticlesForPlayer(Player player, CuboidArea area) {
        Location pLoc = player.getLocation();
        double viewDist = 15.0; // Show particles within this radius

        // Check if player is near the area
        if (!area.getWorld().equals(pLoc.getWorld()))
            return;

        double minX = area.getMinX();
        double minY = area.getMinY();
        double minZ = area.getMinZ();
        double maxX = area.getMaxX() + 1;
        double maxY = area.getMaxY() + 1;
        double maxZ = area.getMaxZ() + 1;

        // Draw edges near player
        double step = GameAreas.PARTICLE_STEP;

        // Bottom edges
        drawDottedLineForPlayer(player, minX, minY, minZ, maxX, minY, minZ, step, viewDist);
        drawDottedLineForPlayer(player, maxX, minY, minZ, maxX, minY, maxZ, step, viewDist);
        drawDottedLineForPlayer(player, maxX, minY, maxZ, minX, minY, maxZ, step, viewDist);
        drawDottedLineForPlayer(player, minX, minY, maxZ, minX, minY, minZ, step, viewDist);

        // Top edges
        drawDottedLineForPlayer(player, minX, maxY, minZ, maxX, maxY, minZ, step, viewDist);
        drawDottedLineForPlayer(player, maxX, maxY, minZ, maxX, maxY, maxZ, step, viewDist);
        drawDottedLineForPlayer(player, maxX, maxY, maxZ, minX, maxY, maxZ, step, viewDist);
        drawDottedLineForPlayer(player, minX, maxY, maxZ, minX, maxY, minZ, step, viewDist);

        // Side edges
        drawDottedLineForPlayer(player, minX, minY, minZ, minX, maxY, minZ, step, viewDist);
        drawDottedLineForPlayer(player, maxX, minY, minZ, maxX, maxY, minZ, step, viewDist);
        drawDottedLineForPlayer(player, maxX, minY, maxZ, maxX, maxY, maxZ, step, viewDist);
        drawDottedLineForPlayer(player, minX, minY, maxZ, minX, maxY, maxZ, step, viewDist);

        // Draw Walls (Faces) near player
        drawFaceForPlayer(player, area, minX, minY, minZ, minX, maxY, maxZ, viewDist); // -X
        drawFaceForPlayer(player, area, maxX, minY, minZ, maxX, maxY, maxZ, viewDist); // +X
        drawFaceForPlayer(player, area, minX, minY, minZ, maxX, minY, maxZ, viewDist); // -Y
        drawFaceForPlayer(player, area, minX, maxY, minZ, maxX, maxY, maxZ, viewDist); // +Y
        drawFaceForPlayer(player, area, minX, minY, minZ, maxX, maxY, minZ, viewDist); // -Z
        drawFaceForPlayer(player, area, minX, minY, maxZ, maxX, maxY, maxZ, viewDist); // +Z

        // Random internal particles near player
        for (int i = 0; i < 3; i++) {
            double rx = clamp(pLoc.getX() + (Math.random() - 0.5) * viewDist, minX, maxX);
            double ry = clamp(pLoc.getY() + (Math.random() - 0.5) * viewDist, minY, maxY);
            double rz = clamp(pLoc.getZ() + (Math.random() - 0.5) * viewDist, minZ, maxZ);

            Location particleLoc = new Location(area.getWorld(), rx, ry, rz);
            if (particleLoc.distanceSquared(pLoc) <= viewDist * viewDist && area.isInside(particleLoc)) {
                player.spawnParticle(Particle.REDSTONE, rx, ry, rz, 1, 0, 0, 0, 0,
                        new Particle.DustOptions(org.bukkit.Color.RED, 0.5f));
            }
        }
    }

    private void drawFaceForPlayer(Player player, CuboidArea area, double x1, double y1, double z1, double x2,
            double y2, double z2, double viewDist) {
        Location pLoc = player.getLocation();

        // Define the bounds of the face area that is within view distance
        double minX = clamp(pLoc.getX() - viewDist, x1, x2);
        double maxX = clamp(pLoc.getX() + viewDist, x1, x2);
        double minY = clamp(pLoc.getY() - viewDist, y1, y2);
        double maxY = clamp(pLoc.getY() + viewDist, y1, y2);
        double minZ = clamp(pLoc.getZ() - viewDist, z1, z2);
        double maxZ = clamp(pLoc.getZ() + viewDist, z1, z2);

        // Density factor - how many particles per square block area
        double density = 1.0;
        double width = maxX - minX;
        double height = maxY - minY;
        double depth = maxZ - minZ;

        // Calculate surface area of this visible portion
        double surfaceArea = 0;
        if (x1 == x2)
            surfaceArea = height * depth;
        else if (y1 == y2)
            surfaceArea = width * depth;
        else if (z1 == z2)
            surfaceArea = width * height;

        int count = (int) (surfaceArea * density);
        if (count > 30)
            count = 30; // Cap per face per tick for performance

        for (int i = 0; i < count; i++) {
            double rx = minX + Math.random() * (maxX - minX);
            double ry = minY + Math.random() * (maxY - minY);
            double rz = minZ + Math.random() * (maxZ - minZ);

            Location particleLoc = new Location(area.getWorld(), rx, ry, rz);
            if (particleLoc.distanceSquared(pLoc) <= viewDist * viewDist) {
                player.spawnParticle(Particle.REDSTONE, rx, ry, rz, 1, 0, 0, 0, 0,
                        new Particle.DustOptions(org.bukkit.Color.RED, 0.6f));
            }
        }
    }

    private void drawDottedLineForPlayer(Player player, double x1, double y1, double z1, double x2, double y2,
            double z2, double step, double viewDist) {
        double distance = Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2) + Math.pow(z2 - z1, 2));
        Location pLoc = player.getLocation();

        for (double d = 0; d <= distance; d += step) {
            double ratio = d / distance;
            double x = x1 + (x2 - x1) * ratio;
            double y = y1 + (y2 - y1) * ratio;
            double z = z1 + (z2 - z1) * ratio;

            double distSq = Math.pow(x - pLoc.getX(), 2) + Math.pow(y - pLoc.getY(), 2) + Math.pow(z - pLoc.getZ(), 2);
            if (distSq <= viewDist * viewDist) {
                player.spawnParticle(Particle.REDSTONE, x, y, z, 1, 0, 0, 0, 0,
                        new Particle.DustOptions(org.bukkit.Color.RED, 1.0f));
            }
        }
    }

    private double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }
}
