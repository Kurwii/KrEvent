package kurwi.krevent2.commands;

import kurwi.krevent2.MainPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

public class StopGameCommand implements CommandExecutor {
    private final MainPlugin plugin;

    public StopGameCommand(MainPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("krevent.admin")) {
            sender.sendMessage(ChatColor.RED + "У вас нет прав на выполнение этой команды!");
            return true;
        }

        Bukkit.getScheduler().cancelTasks(plugin);

        SpawnCommand spawnCommand = plugin.getSpawnCommand();
        if (spawnCommand != null && spawnCommand.lootingBossBar != null) {
            spawnCommand.lootingBossBar.removeAll();
            spawnCommand.lootingBossBar = null;
        }

        World world = Bukkit.getWorld("world");
        if (world == null) {
            sender.sendMessage(ChatColor.RED + "Основной мир не найден!");
            return true;
        }
        Location stopLoc = new Location(world, -59, 29, -313);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.getInventory().clear();
            player.getActivePotionEffects().forEach(e -> player.removePotionEffect(e.getType()));
            player.teleport(stopLoc);
        }

        Bukkit.getWorlds().forEach(w -> {
            w.getEntitiesByClass(Item.class).forEach(Item::remove);
        });

        sender.sendMessage(ChatColor.GREEN + "Игра остановлена! Все игроки телепортированы на спавн.");
        return true;
    }
}
