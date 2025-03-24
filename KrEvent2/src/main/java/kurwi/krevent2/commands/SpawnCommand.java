package kurwi.krevent2.commands;

import kurwi.krevent2.MainPlugin;
import kurwi.krevent2.utils.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Sound;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SpawnCommand implements CommandExecutor {
    private final MainPlugin plugin;
    private int countdownTaskId = -1;
    public BossBar lootingBossBar;

    public SpawnCommand(MainPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("addspawn")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Только игроки могут использовать эту команду!");
                return true;
            }
            Player player = (Player) sender;
            if (!player.hasPermission("krevent.admin")) {
                player.sendMessage(ChatColor.RED + "Нет прав!");
                return true;
            }
            ConfigManager configManager = plugin.getConfigManager();
            configManager.addSpawnPoint(player.getLocation());
            player.sendMessage(ChatColor.GREEN + "Точка спавна добавлена!");
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("startgame")) {
            if (!sender.hasPermission("krevent.admin")) {
                sender.sendMessage(ChatColor.RED + "Нет прав!");
                return true;
            }
            ConfigManager configManager = plugin.getConfigManager();
            List<Location> spawnPoints = configManager.getSpawnPoints();

            if (spawnPoints.isEmpty()) {
                sender.sendMessage(ChatColor.RED + "Нет точек спавна!");
                return true;
            }

            List<Player> survivalPlayers = Bukkit.getOnlinePlayers().stream()
                    .filter(p -> p.getGameMode() == GameMode.SURVIVAL)
                    .collect(Collectors.toList());

            Collections.shuffle(survivalPlayers); // Первое перемешивание игроков

            List<Player> playersToTeleport = new ArrayList<>();
            List<Player> spectators = new ArrayList<>();

            for (int i = 0; i < survivalPlayers.size(); i++) {
                if (i < spawnPoints.size()) {
                    playersToTeleport.add(survivalPlayers.get(i));
                } else {
                    spectators.add(survivalPlayers.get(i));
                }
            }

            // Обработка зрителей
            spectators.forEach(p -> {
                p.sendMessage(ChatColor.RED + "Недостаточно точек спавна. Вы в режиме наблюдателя.");
                p.setGameMode(GameMode.SPECTATOR);
            });

            plugin.getBarrelListener().resetBarrels();

            // Очистка инвентаря и эффектов
            Bukkit.getOnlinePlayers().forEach(p -> {
                p.getInventory().clear();
                p.getActivePotionEffects().forEach(effect -> p.removePotionEffect(effect.getType()));
            });

            // Удаление выброшенных предметов
            Bukkit.getWorlds().forEach(world ->
                    world.getEntitiesByClass(Item.class).forEach(Item::remove)
            );

            startCountdown(Bukkit.getOnlinePlayers(), playersToTeleport, spawnPoints);
            return true;
        }
        return false;
    }

    private void startCountdown(Iterable<? extends Player> allPlayersIterable,
                                List<Player> playersToTeleport,
                                List<Location> spawnPoints) {
        List<Player> allPlayers = new ArrayList<>();
        allPlayersIterable.forEach(allPlayers::add);

        // Наложение слепоты
        allPlayers.forEach(p -> p.addPotionEffect(
                new PotionEffect(PotionEffectType.BLINDNESS, 80, 1, true, true)));

        countdownTaskId = new BukkitRunnable() {
            int count = 3;

            @Override
            public void run() {
                if (count > 0) {
                    allPlayers.forEach(p -> {
                        p.sendTitle(ChatColor.GOLD + String.valueOf(count), "", 5, 20, 5);
                        playCountdownSound(p, count);
                    });
                    count--;
                } else {
                    String startMsg = ChatColor.translateAlternateColorCodes('&',
                            "&6&lИгра началась! &6Собирайте ресурсы в бочках!");
                    Bukkit.broadcastMessage(startMsg);
                    allPlayers.forEach(p -> {
                        p.sendTitle(ChatColor.GREEN + "START!", "", 5, 40, 10);
                        p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.5f);
                    });
                    performRandomTeleportation(playersToTeleport, spawnPoints);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L).getTaskId();
    }

    private void playCountdownSound(Player p, int count) {
        float pitch = 1.0f - (0.15f * (3 - count));
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, pitch);
    }

    private void performRandomTeleportation(List<Player> players, List<Location> spawnPoints) {
        List<Location> availableSpawns = new ArrayList<>(spawnPoints);
        Collections.shuffle(availableSpawns); // Перемешиваем точки спавна

        for (Player player : players) {
            if (availableSpawns.isEmpty()) {
                player.sendMessage(ChatColor.RED + "Ошибка телепортации: не осталось свободных точек спавна!");
                plugin.getLogger().warning("Недостаточно точек для игрока " + player.getName());
                continue;
            }

            Location spawn = availableSpawns.remove(0); // Берем первую доступную точку и удаляем ее из списка
            player.teleportAsync(spawn).thenRun(() -> {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        player.removePotionEffect(PotionEffectType.BLINDNESS);
                        player.sendMessage(ChatColor.GREEN + "Вы телепортированы на случайную точку!");
                    }
                }.runTaskLater(plugin, 20L);
            });
        }
        startLootingTimer();
    }

    private void startLootingTimer() {
        List<Player> allPlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        lootingBossBar = Bukkit.createBossBar(
                ChatColor.GREEN + "Сбор лута: 60 секунд",
                BarColor.GREEN,
                BarStyle.SEGMENTED_20
        );
        allPlayers.forEach(lootingBossBar::addPlayer);
        lootingBossBar.setVisible(true);
        lootingBossBar.setProgress(1.0);

        new BukkitRunnable() {
            int timePassed = 0;

            @Override
            public void run() {
                int remaining = 60 - timePassed;
                lootingBossBar.setTitle(ChatColor.GREEN + "Сбор лута: " + remaining + " сек");
                lootingBossBar.setProgress((double) remaining / 60);

                if (remaining == 30 || remaining == 10 || (remaining <= 5 && remaining >= 1)) {
                    String message = ChatColor.YELLOW + "Осталось " + remaining + " сек!";
                    allPlayers.forEach(p -> p.sendMessage(message));
                }

                if (remaining <= 0) {
                    lootingBossBar.removeAll();
                    cancel();
                    World world = Bukkit.getWorld("world");
                    if (world != null) {
                        Location arena = new Location(world, 6, 118, 1);
                        allPlayers.stream()
                                .filter(p -> p.getGameMode() == GameMode.SURVIVAL)
                                .forEach(p -> {
                                    p.teleport(arena);
                                    p.sendMessage(ChatColor.RED + "Время вышло!");
                                });
                    }
                }
                timePassed++;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
}