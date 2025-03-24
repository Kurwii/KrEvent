package kurwi.krevent2;

import kurwi.krevent2.commands.SpawnCommand;
import kurwi.krevent2.commands.StopGameCommand;
import kurwi.krevent2.listeners.BarrelListener;
import kurwi.krevent2.utils.ConfigManager;
import org.bukkit.plugin.java.JavaPlugin;

public class MainPlugin extends JavaPlugin {
    private ConfigManager configManager;
    private BarrelListener barrelListener;
    private SpawnCommand spawnCommand;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        configManager.setupConfig();

        barrelListener = new BarrelListener(this);
        getServer().getPluginManager().registerEvents(barrelListener, this);

        spawnCommand = new SpawnCommand(this);
        // Регистрируем команды
        getCommand("startgame").setExecutor(spawnCommand);
        getCommand("stopgame").setExecutor(new StopGameCommand(this));
        getCommand("addspawn").setExecutor(spawnCommand); // регистрация команды /addspawn

        getLogger().info("KrEvent2 запущен!");
    }

    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public BarrelListener getBarrelListener() {
        return barrelListener;
    }

    public SpawnCommand getSpawnCommand() {
        return spawnCommand;
    }
}
