package org.latuk.fishingsim.utils;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.latuk.fishingsim.Fish;
import org.latuk.fishingsim.NMS.NBTTags;
import org.latuk.fishingsim.Pet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class PetUtils {
    private DataManager dataManager;
    private NBTTags nbtTags;

    public PetUtils(DataManager dataManager, NBTTags nbtTags) {
        this.dataManager = dataManager;
        this.nbtTags = nbtTags;
    }
    public Pet getPetInfo(String petKey) {
        FileConfiguration config = dataManager.getCustomConfig("pets");
        String path = "pets." + petKey;

        String dataName = config.getString(path + ".dataName");
        String name = config.getString(path + ".name");
        List<String> lore = config.getStringList(path + ".lore");
        int level = config.getInt(path + ".level", 1);
        double multiplier = config.getDouble(path + ".multiplier");
        double chance = config.getDouble(path + ".chance");
        String headValue = config.getString(path + ".headValue");


        return new Pet(dataName, name, lore, level, multiplier, chance, headValue);
    }

    public Pet getRandomPet() {
        FileConfiguration config = dataManager.getCustomConfig("pets");
        Map<String, Pet> pets = new HashMap<>();

        // Загружаем всех питомцев в карту
        for (String petKey : config.getConfigurationSection("pets").getKeys(false)) {
            Pet pet = getPetInfo(petKey);
            pets.put(petKey, pet);
        }

        double totalChance = 0.0;

        // Суммируем все шансы питомцев
        for (Pet pet : pets.values()) {
            totalChance += pet.getChance();
        }

        // Выбираем случайное число в диапазоне от 0 до totalChance
        Random random = new Random();
        double randomValue = random.nextDouble() * totalChance; // Изменение здесь

        double currentChance = 0.0;

        // Проходим по всем питомцам, суммируя шансы, и проверяем, попадает ли случайное число в текущий диапазон
        for (Pet pet : pets.values()) {
            currentChance += pet.getChance();

            if (randomValue <= currentChance) {
                return pet; // Возвращаем питомца, если случайное значение попало в его диапазон шансов
            }
        }

        return null; // На случай, если что-то пойдет не так (хотя это не должно происходить)
    }


    public double getPlayerMultiplier(Player player) {
        double multiplier = 0.0;
        if (player == null) return 1.0;
        String playerName = player.getName();
        List<Map<String, Object>> equippedPets = dataManager.getEquippedPets(player);
        for (Map<String, Object> equippedPet : equippedPets) {
            Pet pet = getPetInfo((String) equippedPet.get("type"));
            double petMultiplier = pet.getMultiplier();
            multiplier += petMultiplier;
        }
        if (multiplier < 1) multiplier = 1.0;
        return multiplier;
    }

    private String replacePlaceholders(String text, String placeholder, double value) {
        return text.replace(placeholder, String.valueOf(value));
    }

    private String replacePlaceholders(String text, String placeholder, String value) {
        return text.replace(placeholder, value);
    }

    public void givePlayerRandomPetWithChance(Player player, int chance) {
        FileConfiguration messagesConfig = dataManager.getCustomConfig("messages");
        Random random = new Random();
        if(random.nextDouble() <= (double) chance / 100) {
            Pet pet = getRandomPet();
            dataManager.givePlayerPet(player, pet);
            String message = ChatColor.translateAlternateColorCodes('&', replacePlaceholders(messagesConfig.getString("messages.you-got-pet", "Ошибка"), "%pet%", pet.getName()));
            player.sendMessage(message);
        }
    }

}
