package dev.restonic4.hide_and_seek.manager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import java.util.ArrayList;
import java.util.List;

public class GameAreas {
    // CHANGE THESE COORDINATES AS NEEDED
    public static final String WORLD_NAME = "world";

    public static final int MIN_WORLD_Y = -51;
    public static final int MAX_WORLD_Y = 47;

    // Spawn location where players are teleported
    public static final double SPAWN_X = 80;
    public static final double SPAWN_Y = -12;
    public static final double SPAWN_Z = -128;
    public static final float SPAWN_YAW = -90.0f;
    public static final float SPAWN_PITCH = 0.0f;

    public static final double PARTICLE_STEP = 0.5; // Offset between dots in lines

    // Main play area (2 positions)
    public static final int PLAY_X1 = 134, PLAY_Y1 = MIN_WORLD_Y, PLAY_Z1 = -183;
    public static final int PLAY_X2 = 25, PLAY_Y2 = MAX_WORLD_Y, PLAY_Z2 = -39;

    // Subareas (name, x1, y1, z1, x2, y2, z2)
    public static final Object[][] SUB_AREAS_DATA = {
            { "spawn", 64, MIN_WORLD_Y, -144, 95, MAX_WORLD_Y, -113 },
            { "desert", 95, MIN_WORLD_Y, -148, 64, MAX_WORLD_Y, -179 },
            { "swamp", 60, MIN_WORLD_Y, -148, 29, MAX_WORLD_Y, -179 },
            { "watch_tower", 99, MIN_WORLD_Y, -148, 130, MAX_WORLD_Y, -179 },
            { "school", 99, MIN_WORLD_Y, -144, 130, MAX_WORLD_Y, -113 },
            { "lighthouse", 60, MIN_WORLD_Y, -144, 29, MAX_WORLD_Y, -113 },
            { "construction", 99, MIN_WORLD_Y, -109, 130, MAX_WORLD_Y, -78 },
            { "ruins", 95, MIN_WORLD_Y, -78, 64, MAX_WORLD_Y, -109 },
            { "backrooms", 60, MIN_WORLD_Y, -109, 29, MAX_WORLD_Y, -78 },
            { "beach", 29, MIN_WORLD_Y, -74, 95, MAX_WORLD_Y, -43 },
            { "pool", 99, MIN_WORLD_Y, -74, 130, MAX_WORLD_Y, -43 },
    };

    private static Location spawnCache;
    private static CuboidArea playAreaCache;
    private static List<CuboidArea> subAreasCache;

    public static Location getSpawn() {
        if (spawnCache == null) {
            World world = Bukkit.getWorld(WORLD_NAME);
            if (world == null)
                world = Bukkit.getWorlds().get(0);
            spawnCache = new Location(world, SPAWN_X, SPAWN_Y, SPAWN_Z, SPAWN_YAW, SPAWN_PITCH);
        }
        return spawnCache;
    }

    public static CuboidArea getPlayArea() {
        if (playAreaCache == null) {
            World world = Bukkit.getWorld(WORLD_NAME);
            if (world == null)
                world = Bukkit.getWorlds().get(0);
            playAreaCache = new CuboidArea("Play Area", world, PLAY_X1, PLAY_Y1, PLAY_Z1, PLAY_X2, PLAY_Y2, PLAY_Z2);
        }
        return playAreaCache;
    }

    public static List<CuboidArea> getSubAreas() {
        if (subAreasCache == null) {
            subAreasCache = new ArrayList<>();
            World world = Bukkit.getWorld(WORLD_NAME);
            if (world == null)
                world = Bukkit.getWorlds().get(0);
            for (Object[] data : SUB_AREAS_DATA) {
                subAreasCache.add(new CuboidArea(
                        (String) data[0],
                        world,
                        (int) data[1], (int) data[2], (int) data[3],
                        (int) data[4], (int) data[5], (int) data[6]));
            }
        }
        return subAreasCache;
    }
}
