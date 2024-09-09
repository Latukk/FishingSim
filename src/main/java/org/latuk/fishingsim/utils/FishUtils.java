package org.latuk.fishingsim.utils;

import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.latuk.fishingsim.Fish;
import org.latuk.fishingsim.NMS.NBTTags;

import java.text.DecimalFormat;
import java.util.*;

public class FishUtils {
    private DataManager dataManager;
    private NBTTags nbtTags;
    private PetUtils petUtils;

    public FishUtils(DataManager dataManager, NBTTags nbtTags, PetUtils petUtils) {
        this.dataManager = dataManager;
        this.nbtTags = nbtTags;
        this.petUtils = petUtils;
    }
    public Fish getFishInfo(String fishKey, FileConfiguration config) {
        String path = "fishes." + fishKey;

        String dataName = config.getString(path + ".dataName");
        String name = config.getString(path + ".name");
        List<String> lore = config.getStringList(path + ".lore");
        double minWeight = config.getDouble(path + ".weight.min");
        double maxWeight = config.getDouble(path + ".weight.max");
        Material material = Material.valueOf(config.getString(path + ".material"));
        int price = config.getInt(path + ".price");

        // Получаем шансы для каждой локации
        Map<String, Integer> locationChances = new HashMap<>();
        if (config.isConfigurationSection(path + ".chances")) {
            for (String location : config.getConfigurationSection(path + ".chances").getKeys(false)) {
                int chance = config.getInt(path + ".chances." + location);
                locationChances.put(location, chance);
            }
        }

        return new Fish(dataName, name, lore, minWeight, maxWeight, material, price, locationChances);
    }

    public Fish getRandomFish(String loc) {
        FileConfiguration config = dataManager.getCustomConfig("fishes");
        Map<String, Fish> fishes = new HashMap<>();

        for (String fishKey : config.getConfigurationSection("fishes").getKeys(false)) {
            Fish fish = getFishInfo(fishKey, config);
            fishes.put(fishKey, fish);
        }

        int totalChance = 0;

        // Суммируем все шансы для указанной локации
        for (Fish fish : fishes.values()) {
            totalChance += fish.getLocationChance(loc);
        }

        // Выбираем случайное число в диапазоне от 0 до totalChance
        Random random = new Random();
        int randomValue = random.nextInt(totalChance);

        int currentChance = 0;

        // Проходим по рыбам и проверяем, в какой диапазон попадает случайное число
        for (Fish fish : fishes.values()) {
            currentChance += fish.getLocationChance(loc);

            if (randomValue < currentChance) {
                return fish;
            }
        }

        return null; // В случае, если что-то пойдет не так
    }

    public double getRandomFishWeight(Fish fish) {
        double minWeight = fish.getMinWeight();
        double maxWeight = fish.getMaxWeight();

        Random random = new Random();
        double weight = minWeight + (maxWeight - minWeight) * random.nextDouble();

        // Округляем число до двух знаков после запятой
        weight = Math.round(weight * 100.0) / 100.0;

        return weight;
    }

    public double getFishWeight(ItemStack fish) {
        double fishWeight = 0.0;
        fishWeight = nbtTags.getNBTTagDouble(fish, "fishSim:fishWeight");
        return fishWeight;
    }

    public int getFishPrice(ItemStack fish) {
        int fishPrice = 0;
        fishPrice = nbtTags.getNBTTagInt(fish, "fishSim:fishPrice");
        return fishPrice;
    }

    public void sellAllFishes(Player player) {
        FileConfiguration msgConfig = dataManager.getCustomConfig("messages");
        Inventory playerInventory = player.getInventory();
        int fishes = 0;
        int money = 0;
        double multiplier = petUtils.getPlayerMultiplier(player);
        Map<String, Map<String, Map<Integer, Integer>>> selledFishes = new HashMap<>();
        for (ItemStack item : playerInventory.getContents()) {
            if (nbtTags.getNBTTagBoolean(item, "fishSim:isFish")) {
                String fishType = nbtTags.getNBTTagString(item, "fishSim:fishName");
                String fishName = item.getItemMeta().getDisplayName();
                int price = nbtTags.getNBTTagInt(item, "fishSim:fishPrice");
                int amount = item.getAmount();

                playerInventory.removeItem(item);
                fishes += amount;
                money += (int) ((price * amount) * multiplier);

                selledFishes.putIfAbsent(fishType, new HashMap<>());
                selledFishes.get(fishType).putIfAbsent(fishName, new HashMap<>());
                selledFishes.get(fishType).get(fishName).put(price * amount, selledFishes.get(fishType).get(fishName).getOrDefault(price * amount, 0) + amount);
            }
        }
        if (fishes > 0) {
            dataManager.givePlayerMoney(player, money);
            String msg = msgConfig.getString("messages.sells-fish");
            msg = msg.replace("%player%", player.getName());
            msg = msg.replace("%fishes%", String.valueOf(fishes));
            msg = msg.replace("%money%", dataManager.formatNumber(money));

            TextComponent message = new TextComponent(ChatColor.translateAlternateColorCodes('&', msg));
            StringBuilder hoverText = new StringBuilder(ChatColor.translateAlternateColorCodes('&', "&7Проданные рыбы:"));

            for (Map.Entry<String, Map<String, Map<Integer, Integer>>> entry : selledFishes.entrySet()) {
                String fishType = entry.getKey();

                for (Map.Entry<String, Map<Integer, Integer>> fishEntry : entry.getValue().entrySet()) {
                    String fishName = fishEntry.getKey();

                    for (Map.Entry<Integer, Integer> priceEntry : fishEntry.getValue().entrySet()) {
                        int price = priceEntry.getKey();
                        int quantity = priceEntry.getValue();
                        hoverText.append(ChatColor.translateAlternateColorCodes('&', "\n&7x" +
                                quantity + " " + fishName + " &7за &a" + dataManager.formatNumber(price * multiplier) +
                                "&7$ (&ax" + dataManager.formatNumber(multiplier) + "&7)"));
                    }
                }
            }
            message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hoverText.toString())));
            player.spigot().sendMessage(message);

        } else {
            String msg = msgConfig.getString("messages.dont-have-fish");
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
        }
    }
}
