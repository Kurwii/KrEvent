package kurwi.krevent2.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.ArrayList;
import java.util.List;

public class ConfigManager {
    private final JavaPlugin plugin;
    private FileConfiguration config;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void setupConfig() {
        plugin.saveDefaultConfig();
        config = plugin.getConfig();
        config.options().copyDefaults(true);
        plugin.saveConfig();
    }


    public void addSpawnPoint(Location location) {
        List<String> spawns = config.getStringList("spawn-points");
        String locString = String.format("%s,%.2f,%.2f,%.2f,%.2f,%.2f",
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch());

        // Проверка на дубликаты
        if (!spawns.contains(locString)) {
            spawns.add(locString);
            config.set("spawn-points", spawns);
            plugin.saveConfig();
        }
    }

    public List<Location> getSpawnPoints() {
        List<String> spawnStrings = config.getStringList("spawn-points");
        List<Location> locations = new ArrayList<>();

        for (String spawn : spawnStrings) {
            String[] parts = spawn.split(",");
            if (parts.length != 6) continue;

            try {
                World world = Bukkit.getWorld(parts[0]);
                double x = Double.parseDouble(parts[1]);
                double y = Double.parseDouble(parts[2]);
                double z = Double.parseDouble(parts[3]);
                float yaw = Float.parseFloat(parts[4]);
                float pitch = Float.parseFloat(parts[5]);

                locations.add(new Location(world, x, y, z, yaw, pitch));
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid spawn point: " + spawn);
            }
        }
        return locations;
    }
}
