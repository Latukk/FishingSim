package org.latuk.fishingsim.utils;


import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.latuk.fishingsim.Main;
import org.latuk.fishingsim.Pet;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;

public class DataManager {
    private final Main plugin;
    private final Map<String, FileConfiguration> configMap = new HashMap<>();
    private final Map<String, File> configFileMap = new HashMap<>();
    public DataManager(Main plugin) {
        this.plugin = plugin;
    }

    public void setupConfig(String fileName) {
        File configFile = new File(plugin.getDataFolder(), fileName + ".yml");
        configFileMap.put(fileName, configFile);

        if (!configFile.exists()) {
            plugin.saveResource(fileName + ".yml", false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        configMap.put(fileName, config);
    }

    public FileConfiguration getCustomConfig(String fileName) {
        return configMap.get(fileName);
    }

    public void saveCustomConfig(String fileName) {
        File configFile = configFileMap.get(fileName);
        FileConfiguration config = configMap.get(fileName);

        if (configFile != null && config != null) {
            try {
                config.save(configFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String getMessageFromConfig(String message, Player player) {
        String msg = getCustomConfig("messages").getString("messages." + message);
        String result = ChatColor.translateAlternateColorCodes('&', msg);


        // Замена плейсхолдеров
        result = result.replace("%player%", player.getName());

        return result != null ? result : "Произошла неизвестная ошибка!";
    }


    public List<String> getFishes() {
        List<String> fishes = new ArrayList<>();
        FileConfiguration config = getCustomConfig("fishes");
        ConfigurationSection fishesSection = config.getConfigurationSection("fishes");
        return fishes;
    }

    public void addNewPlayerToCFG(Player player) {
        if (player == null) return;
        String playerName = player.getName();
        FileConfiguration config = getCustomConfig("players");
        String path = "players." + playerName;
        if (!config.contains(path)) {
            config.set(path + ".items.FISHING_ROD", 1);
            config.set(path + ".fishes.total", 0);
            config.set(path + ".pets", new ArrayList<>());
            config.set(path + ".level", 1);
            config.set(path + ".money", 0);

            saveCustomConfig("players");
        }
    }

    public boolean isPlayerExistsInCFG(Player player) {
        if (player == null) return false;
        String playerName = player.getName();
        FileConfiguration config = getCustomConfig("players");
        String path = "players." + playerName;
        return config.contains(path);
    }

    public void givePlayerPet(Player player, Pet pet) {
        if (player == null) return;
        String playerName = player.getName();
        FileConfiguration config = getCustomConfig("players");
        String path = "players." + playerName;
        if (config.contains(path)) {
            List<Map<String, Object>> pets = (List<Map<String, Object>>) config.getList(path + ".pets", new ArrayList<>());


            Map<String, Object> newPet = new LinkedHashMap<>();
            newPet.put("uuid", UUID.randomUUID().toString());
            newPet.put("type", pet.getDataName());
            newPet.put("status", "notEquipped");

            pets.add(newPet);
            config.set(path + ".pets", pets);
            saveCustomConfig("players");
        } else {
            addNewPlayerToCFG(player);
            givePlayerPet(player, pet);
        }
    }

    public List<Map<String, Object>> getPlayerPets(Player player) {
        if (player == null) return new ArrayList<>();
        String playerName = player.getName();
        FileConfiguration config = getCustomConfig("players");
        String path = "players." + playerName;

        if (config.contains(path)) {
            List<Map<String, Object>> pets = (List<Map<String, Object>>) config.getList(path + ".pets", new ArrayList<>());
            return pets;
        }

        return new ArrayList<>();
    }

    public Map<String, Object> getPlayerPet(Player player, String uuid) {
        if (player == null) return null;
        String playerName = player.getName();
        FileConfiguration config = getCustomConfig("players");
        String path = "players." + playerName;

        if (config.contains(path)) {
            List<Map<String, Object>> pets = (List<Map<String, Object>>) config.getList(path + ".pets", new ArrayList<>());
            for (Map<String, Object> pet : pets) {
                if (uuid.equals(pet.get("uuid"))) return pet;
            }
        }

        return null;
    }

    public List<Map<String, Object>> getEquippedPets(Player player) {
        if (player == null) return new ArrayList<>();
        String playerName = player.getName();
        FileConfiguration config = getCustomConfig("players");
        String path = "players." + playerName;

        List<Map<String, Object>> equippedPets = new ArrayList<>();

        if (config.contains(path)) {
            List<Map<String, Object>> pets = (List<Map<String, Object>>) config.getList(path + ".pets", new ArrayList<>());
            for (Map<String, Object> pet : pets) {
                if (pet.get("status").equals("equipped")) {
                    equippedPets.add(pet);
                }
            }
        }

        return equippedPets;
    }

    public void equipPet(Player player, String uuid) {
        if (player == null) return;
        String playerName = player.getName();
        FileConfiguration config = getCustomConfig("players");
        String path = "players." + playerName;

        if (config.contains(path)) {
            List<Map<String, Object>> pets = (List<Map<String, Object>>) config.getList(path + ".pets", new ArrayList<>());

            // Проходим по списку всех питомцев и меняем статус на "equipped" только для нужного UUID
            for (Map<String, Object> pet : pets) {
                if (pet.get("uuid").equals(uuid)) {
                    pet.put("status", "equipped");
                }
            }
            config.set(path + ".pets", pets); // Сохраняем изменения в конфиге
            saveCustomConfig("players");
        }
    }


    public void unEquipPet(Player player, String uuid) {
        if (player == null) return;
        String playerName = player.getName();
        FileConfiguration config = getCustomConfig("players");
        String path = "players." + playerName;

        if (config.contains(path)) {
            List<Map<String, Object>> pets = (List<Map<String, Object>>) config.getList(path + ".pets", new ArrayList<>());

            // Проходим по списку всех питомцев и меняем статус на "notEquipped" только для нужного UUID
            for (Map<String, Object> pet : pets) {
                if (pet.get("uuid").equals(uuid)) {
                    pet.put("status", "notEquipped");
                }
            }
            config.set(path + ".pets", pets); // Сохраняем изменения в конфиге
            saveCustomConfig("players");
        }
    }

    public void givePlayerMoney(Player player, int amount) {
        if (player == null) return;
        String playerName = player.getName();
        FileConfiguration config = getCustomConfig("players");
        String path = "players." + playerName;
        if (config.contains(path)) {
            int currentMoney = config.getInt(path + ".money");
            config.set(path + ".money", currentMoney+amount);
            saveCustomConfig("players");
        } else {
            addNewPlayerToCFG(player);
            givePlayerMoney(player, amount);
        }
    }
    public void takePlayerMoney(Player player, int amount) {
        if (player == null) return;
        String playerName = player.getName();
        FileConfiguration config = getCustomConfig("players");
        String path = "players." + playerName;
        if (config.contains(path)) {
            int currentMoney = config.getInt(path + ".money");
            config.set(path + ".money", currentMoney-amount);
            saveCustomConfig("players");
        } else {
            addNewPlayerToCFG(player);
            takePlayerMoney(player, amount);
        }
    }

    public void givePlayerFish(Player player, String fishName, int amount) {
        if (player == null) return;
        String playerName = player.getName();
        FileConfiguration config = getCustomConfig("players");
        String path = "players." + playerName;
        if (config.contains(path)) {
            int currentFishes = config.getInt(path + ".fishes." + fishName);
            int currentTotalFishes = config.getInt(path + ".fishes.total");
            config.set(path + ".fishes.total", currentTotalFishes+amount);
            config.set(path + ".fishes." +fishName, currentFishes+amount);
            saveCustomConfig("players");
        } else {
            addNewPlayerToCFG(player);
            givePlayerFish(player, fishName, amount);
        }
    }

    public void givePlayerLevel(Player player, int amount) {
        if (player == null) return;
        String playerName = player.getName();
        FileConfiguration config = getCustomConfig("players");
        String path = "players." + playerName;
        if (config.contains(path)) {
            int currentLevel = config.getInt(path + ".level");
            config.set(path + ".level", currentLevel+amount);
            saveCustomConfig("players");
        } else {
            addNewPlayerToCFG(player);
            givePlayerLevel(player, amount);
        }
    }

    public int getPlayerMoney(Player player) {
        int money = 0;

        if (player == null) return 0;
        String playerName = player.getName();
        FileConfiguration config = getCustomConfig("players");
        String path = "players." + playerName;
        if (config.contains(path)) {
            money = config.getInt(path + ".money");
        } else {
            addNewPlayerToCFG(player);
            money = getPlayerMoney(player);
        }

        return money;
    }

    public int getPlayerFishes(Player player) {
        int fishes = 0;

        if (player == null) return 0;
        String playerName = player.getName();
        FileConfiguration config = getCustomConfig("players");
        String path = "players." + playerName;
        if (config.contains(path)) {
            fishes = config.getInt(path + ".fishes.total");
        } else {
            addNewPlayerToCFG(player);
            fishes = getPlayerFishes(player);
        }

        return fishes;
    }

    public int getPlayerLevel(Player player) {
        int level = 1;

        if (player == null) return 1;
        String playerName = player.getName();
        FileConfiguration config = getCustomConfig("players");
        String path = "players." + playerName;
        if (config.contains(path)) {
            level = config.getInt(path + ".level");
        } else {
            addNewPlayerToCFG(player);
            level = getPlayerLevel(player);
        }

        return level;
    }

    public int getFishingRodLevel(Player player) {
        int level = 1;

        if (player == null) return 1;
        String playerName = player.getName();
        FileConfiguration config = getCustomConfig("players");
        String path = "players." + playerName;
        if (config.contains(path)) {
            level = config.getInt(path + ".items.FISHING_ROD");
        } else {
            addNewPlayerToCFG(player);
            level = getFishingRodLevel(player);
        }

        return level;
    }

    public void giveRodLevel(Player player, int amount) {
        if (player == null) return;
        String playerName = player.getName();
        FileConfiguration config = getCustomConfig("players");
        String path = "players." + playerName;
        if (config.contains(path)) {
            int currentLevel = config.getInt(path + ".items.FISHING_ROD");
            config.set(path + ".items.FISHING_ROD", currentLevel+amount);
            saveCustomConfig("players");
        } else {
            addNewPlayerToCFG(player);
            giveRodLevel(player, amount);
        }
    }




    public String getLocationFromPlayerWorld(Player player) {
        World world = player.getWorld();
        String worldName = world.getName();
        FileConfiguration config = getCustomConfig("locations");
        for (String key : config.getConfigurationSection("locations").getKeys(false)) {
            String cfgWorldName = config.getString("locations." + key + ".world");
            if (worldName.equals(cfgWorldName)) return key;
        }
        return "loc_1";
    }

    public String formatNumber(double number) {
        DecimalFormat df = new DecimalFormat("#,##0.##");

        if (number >= 1_000_000_000_000_000.0) {
            return df.format(number / 1_000_000_000_000_000.0) + "Qi";
        } else if (number >= 1_000_000_000_000.0) {
            return df.format(number / 1_000_000_000_000.0) + "T";
        } else if (number >= 1_000_000_000.0) {
            return df.format(number / 1_000_000_000.0) + "B";
        } else if (number >= 1_000_000.0) {
            return df.format(number / 1_000_000.0) + "M";
        } else if (number >= 1_000.0) {
            return df.format(number / 1_000.0) + "K";
        } else {
            return df.format(number);
        }
    }

    public String formatNumber(int number) {
        DecimalFormat df = new DecimalFormat("#,##0.##");

        if (number >= 1_000_000_000_000_000.0) {
            return df.format(number / 1_000_000_000_000_000.0) + "Qi";
        } else if (number >= 1_000_000_000_000.0) {
            return df.format(number / 1_000_000_000_000.0) + "T";
        } else if (number >= 1_000_000_000.0) {
            return df.format(number / 1_000_000_000.0) + "B";
        } else if (number >= 1_000_000.0) {
            return df.format(number / 1_000_000.0) + "M";
        } else if (number >= 1_000.0) {
            return df.format(number / 1_000.0) + "K";
        } else {
            return df.format(number);
        }
    }

}
