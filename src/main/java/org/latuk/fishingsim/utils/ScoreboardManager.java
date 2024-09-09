package org.latuk.fishingsim.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.latuk.fishingsim.Main;

import java.util.List;

public class ScoreboardManager {
    private final Main plugin;
    private final DataManager dataManager;
    private final PetUtils petUtils;

    public ScoreboardManager(Main plugin, DataManager dataManager, PetUtils petUtils) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.petUtils = petUtils;
        startUpdatingScoreboards();
    }

    // Запускаем обновление Scoreboard для всех игроков
    private void startUpdatingScoreboards() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    updateScoreboard(player);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // Обновляем Scoreboard для игрока
    private void updateScoreboard(Player player) {
        FileConfiguration scoreboardConfig = dataManager.getCustomConfig("scoreboard");
        String serverName = scoreboardConfig.getString("server-name");
        List<String> scoreboardLines = scoreboardConfig.getStringList("scoreboard");

        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("customScoreboard", "dummy",
                ChatColor.translateAlternateColorCodes('&', "&a" + serverName));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        int scorePosition = scoreboardLines.size() - 1; // Начинаем с конца

        for (String line : scoreboardLines) {
            line = replacePlaceholders(line, player);

            if (line.isEmpty()) {
                // Используем невидимый символ для пустой строки
                line = ChatColor.RESET + " "; // Можно использовать пробел или другой невидимый символ
            }

            Score score = objective.getScore(line);
            score.setScore(scorePosition--);
        }

        player.setScoreboard(scoreboard);
    }

    private String replacePlaceholders(String line, Player player) {
        line = ChatColor.translateAlternateColorCodes('&', line);
        line = line.replace("%server-name%", dataManager.getCustomConfig("scoreboard").getString("server-name"));
        line = line.replace("%fishes%", dataManager.formatNumber(dataManager.getPlayerFishes(player)));
        line = line.replace("%money%", dataManager.formatNumber(dataManager.getPlayerMoney(player)));
        line = line.replace("%level%", dataManager.formatNumber(dataManager.getPlayerLevel(player)));
        line = line.replace("%multiplier%", dataManager.formatNumber(petUtils.getPlayerMultiplier(player)));
        line = line.replace("%player-name%", player.getName());
        line = line.replace("%server-online%", String.valueOf(Bukkit.getOnlinePlayers().size()));
        return line;
    }
}
