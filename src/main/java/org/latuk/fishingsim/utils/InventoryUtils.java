package org.latuk.fishingsim.utils;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.latuk.fishingsim.Fish;
import org.latuk.fishingsim.NMS.NBTTags;
import org.latuk.fishingsim.Pet;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class InventoryUtils {
    private final DataManager dataManager;
    private final FishUtils fishUtils;
    private final PetUtils petUtils;
    private final NBTTags nbtTags;

    public InventoryUtils(DataManager dataManager, FishUtils fishUtils, PetUtils petUtils, NBTTags nbtTags) {
        this.dataManager = dataManager;
        this.fishUtils = fishUtils;
        this.petUtils = petUtils;
        this.nbtTags = nbtTags;
    }

    private int getFreeSlot(Inventory inventory) {
        int count = 0;
        for (ItemStack i : inventory) {
            if (i == null) {
                count++;
            }
        }
        return count;
    }
    public boolean hasFreeSpace(Inventory inventory) {
        return getFreeSlot(inventory) > 5;
    }

    private String replacePlaceholders(String text, String placeholder, double value) {
        return text.replace(placeholder, String.valueOf(value));
    }

    private String replacePlaceholders(String text, String placeholder, String value) {
        return text.replace(placeholder, value);
    }

    public void givePlayerRandomFish(Player player) {
        if (player != null) {
            Fish randomFish = fishUtils.getRandomFish(dataManager.getLocationFromPlayerWorld(player));
            if (randomFish == null) {
                player.sendMessage(dataManager.getMessageFromConfig("error", player));
                return;
            }
            Inventory inventory = player.getInventory();
            if (!hasFreeSpace(inventory)) {
                player.sendMessage(dataManager.getMessageFromConfig("full-inventory", player));
                return;
            }
            ItemStack fishItem = new ItemStack(randomFish.getMaterial());
            ItemMeta meta = fishItem.getItemMeta();

            double fishWeight = fishUtils.getRandomFishWeight(randomFish);
            int fishPrice = (int) Math.round(fishWeight * randomFish.getPrice());

            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', randomFish.getName()));
                List<String> coloredLore = randomFish.getLore().stream()
                        .map(line -> replacePlaceholders(line, "%weight%", fishWeight)) // Заменяем плейсхолдеры
                        .map(line -> ChatColor.translateAlternateColorCodes('&', line)) // Применяем цвета
                        .collect(Collectors.toList());
                meta.setLore(coloredLore);
            }


            fishItem.setItemMeta(meta);
            nbtTags.setNBTTagBoolean(fishItem, "fishSim:isFish", true);                                       // Является ли предмет рыбой
            nbtTags.setNBTTagString(fishItem, "fishSim:fishName", randomFish.getDataName());                        // Название рыбы(для конфига)
            nbtTags.setNBTTagDouble(fishItem, "fishSim:fishWeight", fishWeight);                                    // Вес рыбы
            nbtTags.setNBTTagInt(fishItem, "fishSim:fishPrice", fishPrice);                                         // Цена рыбы (Вес * цена за кг)

            String msg = dataManager.getCustomConfig("messages").getString("messages.gived-fish");

            // Замена плейсхолдеров
            msg = msg.replace("%player%", player.getName());
            msg = msg.replace("%fish%", randomFish.getName());
            msg = msg.replace("%weight%", String.valueOf(fishWeight));
            msg = msg.replace("%price%", dataManager.formatNumber(randomFish.getPrice()));
            msg = msg.replace("%totalprice%", dataManager.formatNumber(fishPrice));

            inventory.addItem(fishItem);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
            dataManager.givePlayerFish(player, randomFish.getDataName(), 1);
        }
    }

    public void openMenu(Player player) {
        FileConfiguration menuConfig = dataManager.getCustomConfig("menu");
        int size = menuConfig.getInt("menu.menuSize");
        String menuName = "Меню";
        Inventory menu = Bukkit.createInventory(player, size, menuName);
        for (String key : menuConfig.getConfigurationSection("menu").getKeys(false)) {
            if (menuIsItemSection(key)) {
                String path = "menu." + key;

                // Получение нужных данных
                Material material = Material.getMaterial(menuConfig.getString(path + ".material", "STONE"));                                                           // Получение материала предмета из конфига
                int slot = menuConfig.getInt(path + ".slot", 0);                                                                                                       // Получение слота из конфига
                String itemName = ChatColor.translateAlternateColorCodes('&', menuConfig.getString(path + ".itemName", "&cОшибка"));                        // Получение названия предмета из конфига
                List<String> lore = menuConfig.getStringList(path + ".itemLore");                                                                                          // Получение описания предмета из конфига
                List<String> coloredLore = lore.stream()                                                                                                                        // Форматирование текста в описании(цвет)
                        .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                        .collect(Collectors.toList());
                String command = menuConfig.getString(path + ".command", "fishsim menu");                                                                             // Получение команды предмета из конфига

                // Создание предмета
                ItemStack item = new ItemStack(material);
                if (material.equals(Material.PLAYER_HEAD) && menuConfig.contains(path + ".headValue")) {                                                                        // Если надо выдать голову игрока со скином
                    String value = menuConfig.getString(path + ".headValue");
                    item = getSkull(value);
                }
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(itemName);
                    meta.setLore(coloredLore);
                }
                item.setItemMeta(meta);
                nbtTags.setNBTTagBoolean(item, "fishSim:isMenuItem", true);
                nbtTags.setNBTTagString(item, "fishSim:command", command);

                menu.setItem(slot, item);
            }
        }
        ItemStack gray_glass = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = gray_glass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
        }
        gray_glass.setItemMeta(meta);
        for (int i = 0; i < menu.getSize(); i++) {
            if (menu.getItem(i) == null) menu.setItem(i, gray_glass);
        }

        player.openInventory(menu);
    }

    public void openFishingRodUpgrade(Player player) {
        Inventory upraderodmenu = Bukkit.createInventory(player, 9, "Улучшение удочки");
        FileConfiguration playerConfig = dataManager.getCustomConfig("players");
        FileConfiguration itemsConfig = dataManager.getCustomConfig("items");
        String path = "items.FISHING_ROD.";
        int currentRodLevel = dataManager.getFishingRodLevel(player);
        int nextRodLevel = currentRodLevel + 1;
        ItemStack glass = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        if (glassMeta != null) glassMeta.setDisplayName(" ");
        glass.setItemMeta(glassMeta);
        ItemStack fishingRod = new ItemStack(Material.FISHING_ROD);
        ItemMeta meta = fishingRod.getItemMeta();
        int balance = dataManager.getPlayerMoney(player);
        int fishes = dataManager.getPlayerFishes(player);
        int price = 0;
        int fishesPrice = 0;
        if (meta != null) {
            String itemColor = "&c";
            String moneycolor = "&c";
            String fishescolor = "&c";
            String fishingRodName = ChatColor.translateAlternateColorCodes('&', "&cУдочка");
            List<String> lore = Arrays.asList(ChatColor.translateAlternateColorCodes('&', "&cМаксимальный уровень!"));
            if (itemsConfig.contains(path + ".level." + nextRodLevel)) {
                price = itemsConfig.getInt(path + ".level." + nextRodLevel + ".price");
                fishesPrice = itemsConfig.getInt(path + ".level." + nextRodLevel + ".fishes");
                if (balance >= price) moneycolor = "&a";
                if (fishes >= fishesPrice) fishescolor = "&a";
                if (balance > price && fishes > fishesPrice) {
                    itemColor = "&a";
                    glass.setType(Material.GREEN_STAINED_GLASS_PANE);
                }
                fishingRodName = ChatColor.translateAlternateColorCodes('&', replacePlaceholders(itemsConfig.getString(path + "shopName", "&cУдочка"), "%itemcolor%", itemColor));

                lore = itemsConfig.getStringList(path + "shopLore");                                                                                                       // Получение описания предмета из конфига
                final String finalFishingRodName = fishingRodName;
                final String finalMoneycolor = moneycolor;
                final String finalFishescolor = fishescolor;
                final int finalPrice = price;
                final int finalFishesPrice = fishesPrice;
                Map<String, Object> enchantments = itemsConfig.getConfigurationSection(path + ".level." + nextRodLevel + ".enchantments").getValues(false);
                for (Map.Entry<String, Object> entry : enchantments.entrySet()) {
                    String enchantmentName = entry.getKey();
                    int level = (int) entry.getValue();
                    Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(enchantmentName.toLowerCase()));
                    meta.addEnchant(enchantment, level, true);
                    if (enchantmentName.equalsIgnoreCase("LURE")) lore.add("&9Приманка " + level);
                }

                List<String> coloredLore = lore.stream()                                                                                                                        // Форматирование текста в описании
                        .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                        .map(line -> ChatColor.translateAlternateColorCodes('&', replacePlaceholders(line, "%fishing_rod_level%", nextRodLevel)))
                        .map(line -> ChatColor.translateAlternateColorCodes('&', replacePlaceholders(line, "%moneycolor%", finalMoneycolor)))
                        .map(line -> ChatColor.translateAlternateColorCodes('&', replacePlaceholders(line, "%fishescolor%", finalFishescolor)))
                        .map(line -> ChatColor.translateAlternateColorCodes('&', replacePlaceholders(line, "%balance%", dataManager.formatNumber(balance))))
                        .map(line -> ChatColor.translateAlternateColorCodes('&', replacePlaceholders(line, "%price%", dataManager.formatNumber(finalPrice))))
                        .map(line -> ChatColor.translateAlternateColorCodes('&', replacePlaceholders(line, "%fishes%", dataManager.formatNumber(fishes))))
                        .map(line -> ChatColor.translateAlternateColorCodes('&', replacePlaceholders(line, "%fishesprice%", dataManager.formatNumber(finalFishesPrice))))
                        .collect(Collectors.toList());

                meta.setDisplayName(finalFishingRodName);
                meta.setLore(coloredLore);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            } else {
                fishingRodName = ChatColor.translateAlternateColorCodes('&', "&cУдочка");
                lore = Arrays.asList(ChatColor.translateAlternateColorCodes('&', "&cМаксимальный уровень!"));
                meta.setDisplayName(fishingRodName);
                meta.setLore(lore);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
        }
        fishingRod.setItemMeta(meta);

        nbtTags.setNBTTagBoolean(fishingRod, "fishSim:isFishingRodFromMenu", true);
        nbtTags.setNBTTagInt(fishingRod, "fishSim:fishingRodLevel", nextRodLevel);
        nbtTags.setNBTTagInt(fishingRod, "fishSim:price", price);
        nbtTags.setNBTTagInt(fishingRod, "fishSim:fishesPrice", fishesPrice);

        upraderodmenu.setItem(4, fishingRod);

        for (int i = 0; i < upraderodmenu.getSize(); i++) {
            if (upraderodmenu.getItem(i) == null) upraderodmenu.setItem(i, glass);
        }

        player.openInventory(upraderodmenu);
    }

    public void openLevelUpgrade(Player player) {
        Inventory upradelevelmenu = Bukkit.createInventory(player, 9, "Повышение уровня");
        FileConfiguration playerConfig = dataManager.getCustomConfig("players");
        FileConfiguration levelsConfig = dataManager.getCustomConfig("levels");
        String path = "levels.";
        int currentLevel = dataManager.getPlayerLevel(player);
        int nextLevel = currentLevel + 1;
        ItemStack glass = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        if (glassMeta != null) glassMeta.setDisplayName(" ");
        glass.setItemMeta(glassMeta);
        ItemStack expBottle = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = expBottle.getItemMeta();
        int balance = dataManager.getPlayerMoney(player);
        int fishes = dataManager.getPlayerFishes(player);
        int price = 0;
        int fishesPrice = 0;
        if (meta != null) {
            String itemColor = "&c";
            String moneycolor = "&c";
            String fishescolor = "&c";
            String expBottleName = ChatColor.translateAlternateColorCodes('&', "&cПовышение уровня");
            List<String> lore = Arrays.asList(ChatColor.translateAlternateColorCodes('&', "&cМаксимальный уровень!"));
            if (levelsConfig.contains(path + nextLevel)) {
                price = levelsConfig.getInt(path + nextLevel + ".price");
                fishesPrice = levelsConfig.getInt(path + nextLevel + ".fishes");
                if (balance >= price) moneycolor = "&a";
                if (fishes >= fishesPrice) fishescolor = "&a";
                if (balance > price && fishes > fishesPrice) {
                    itemColor = "&a";
                    glass.setType(Material.GREEN_STAINED_GLASS_PANE);
                }
                expBottleName = ChatColor.translateAlternateColorCodes('&', replacePlaceholders(levelsConfig.getString(path + "itemName", "&cПовышение уровня"), "%namecolor%", itemColor));

                lore = levelsConfig.getStringList(path + "itemLore");                                                                                                       // Получение описания предмета из конфига
                final String finalExpBottleName = expBottleName;
                final String finalMoneycolor = moneycolor;
                final String finalFishescolor = fishescolor;
                final int finalPrice = price;
                final int finalFishesPrice = fishesPrice;

                List<String> coloredLore = lore.stream()                                                                                                                        // Форматирование текста в описании
                        .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                        .map(line -> ChatColor.translateAlternateColorCodes('&', replacePlaceholders(line, "%playerlevel%", String.valueOf(currentLevel))))
                        .map(line -> ChatColor.translateAlternateColorCodes('&', replacePlaceholders(line, "%nextlevel%", String.valueOf(nextLevel))))
                        .map(line -> ChatColor.translateAlternateColorCodes('&', replacePlaceholders(line, "%moneycolor%", finalMoneycolor)))
                        .map(line -> ChatColor.translateAlternateColorCodes('&', replacePlaceholders(line, "%fishescolor%", finalFishescolor)))
                        .map(line -> ChatColor.translateAlternateColorCodes('&', replacePlaceholders(line, "%balance%", dataManager.formatNumber(balance))))
                        .map(line -> ChatColor.translateAlternateColorCodes('&', replacePlaceholders(line, "%price%", dataManager.formatNumber(finalPrice))))
                        .map(line -> ChatColor.translateAlternateColorCodes('&', replacePlaceholders(line, "%fishes%", dataManager.formatNumber(fishes))))
                        .map(line -> ChatColor.translateAlternateColorCodes('&', replacePlaceholders(line, "%fishesprice%", dataManager.formatNumber(finalFishesPrice))))
                        .collect(Collectors.toList());

                meta.setDisplayName(finalExpBottleName);
                meta.setLore(coloredLore);
            } else {
                expBottleName = ChatColor.translateAlternateColorCodes('&', "&cПовышение уровня");
                lore = Arrays.asList(ChatColor.translateAlternateColorCodes('&', "&cМаксимальный уровень!"));
                meta.setDisplayName(expBottleName);
                meta.setLore(lore);
            }
        }
        expBottle.setItemMeta(meta);

        nbtTags.setNBTTagBoolean(expBottle, "fishSim:isExpBottleFromMenu", true);
        nbtTags.setNBTTagInt(expBottle, "fishSim:nextLevel", nextLevel);
        nbtTags.setNBTTagInt(expBottle, "fishSim:price", price);
        nbtTags.setNBTTagInt(expBottle, "fishSim:fishesPrice", fishesPrice);

        upradelevelmenu.setItem(4, expBottle);

        for (int i = 0; i < upradelevelmenu.getSize(); i++) {
            if (upradelevelmenu.getItem(i) == null) upradelevelmenu.setItem(i, glass);
        }

        player.openInventory(upradelevelmenu);
    }

    public void openLocationsMenu(Player player) {
        FileConfiguration locationsConfig = dataManager.getCustomConfig("locations");
        int size = 27;
        String menuName = "Локации";
        Inventory menu = Bukkit.createInventory(player, size, menuName);
        for (String key : locationsConfig.getConfigurationSection("locations").getKeys(false)) {
                String path = "locations." + key;
                // Получение нужных данных
            Material material = Material.getMaterial(locationsConfig.getString(path + ".material", "STONE"));                                                           // Получение материала предмета из конфига
            int slot = locationsConfig.getInt(path + ".slot", 0);                                                                                                       // Получение слота из конфига
            int playerlevel = dataManager.getPlayerLevel(player);
            int loclevel = locationsConfig.getInt(path + ".level", 1);
            String itemcolor = "&c";
            if (playerlevel >= loclevel) itemcolor = "&a";
            final String finalItemColor = itemcolor;
            String itemName = ChatColor.translateAlternateColorCodes('&', replacePlaceholders(locationsConfig.getString(path + ".name", "&cОшибка"), "%itemcolor%", itemcolor));                        // Получение названия предмета из конфига
            List<String> lore = locationsConfig.getStringList(path + ".lore");                                                                                          // Получение описания предмета из конфига
            List<String> coloredLore = lore.stream()                                                                                                                        // Форматирование текста в описании(цвет)
                    .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                    .map(line -> ChatColor.translateAlternateColorCodes('&', replacePlaceholders(line, "%playerlevel%", String.valueOf(playerlevel))))
                    .map(line -> ChatColor.translateAlternateColorCodes('&', replacePlaceholders(line, "%loclevel%", String.valueOf(loclevel))))
                    .map(line -> ChatColor.translateAlternateColorCodes('&', replacePlaceholders(line, "%itemcolor%", finalItemColor)))
                    .collect(Collectors.toList());

            // Создание предмета
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(itemName);
                meta.setLore(coloredLore);
            }
            item.setItemMeta(meta);
            nbtTags.setNBTTagBoolean(item, "fishSim:isLocationsMenuItem", true);
            nbtTags.setNBTTagString(item, "fishSim:locName", key);

            menu.setItem(slot, item);
        }
        ItemStack gray_glass = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = gray_glass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
        }
        gray_glass.setItemMeta(meta);
        for (int i = 0; i < menu.getSize(); i++) {
            if (menu.getItem(i) == null) menu.setItem(i, gray_glass);
        }

        player.openInventory(menu);
    }

    public void openPetsMenu(Player player, int page) {
        FileConfiguration petsConfig = dataManager.getCustomConfig("pets");
        FileConfiguration playersConfig = dataManager.getCustomConfig("players");
        int size = 54;
        String menuName = "Питомцы - Страница " + (page + 1);
        Inventory menu = Bukkit.createInventory(player, size, menuName);

        List<Map<String, Object>> playerPets = dataManager.getPlayerPets(player);
        int totalPets = playerPets.size();
        int petsPerPage = 21; // Мы используем 21 слот на странице (7 питомцев в строке x 3 строки)
        int totalPages = (int) Math.ceil((double) totalPets / petsPerPage);

        // Список слотов для размещения питомцев (10-16, 19-25, 28-34)
        int[] petSlots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};

        // Сортировка питомцев: экипированные в начале, остальные по убыванию множителя
        playerPets.sort((pet1, pet2) -> {
            boolean pet1Equipped = pet1.get("status").equals("equipped");
            boolean pet2Equipped = pet2.get("status").equals("equipped");

            // Если один питомец экипирован, он идет раньше
            if (pet1Equipped && !pet2Equipped) return -1;
            if (!pet1Equipped && pet2Equipped) return 1;

            // Если оба питомца имеют одинаковый статус, сортируем по множителю
            String type1 = (String) pet1.get("type");
            String type2 = (String) pet2.get("type");
            double multiplier1 = petsConfig.getDouble(".pets." + type1 + ".multiplier", 1.0);
            double multiplier2 = petsConfig.getDouble(".pets." + type2 + ".multiplier", 1.0);

            // Сортировка по убыванию множителя
            return Double.compare(multiplier2, multiplier1);
        });

        // Расчет, какие питомцы выводить на текущей странице
        int start = page * petsPerPage;
        int end = Math.min(start + petsPerPage, totalPets);

        // Добавление питомцев в инвентарь
        int shift = 0;
        for (int i = start; i < end; i++) {
            Map<String, Object> pet = playerPets.get(i);
            String type = (String) pet.get("type");
            String path = ".pets." + type;
            Pet currentPet = petUtils.getPetInfo(type);
            ItemStack item = getSkull(currentPet.getHeadValue());
            int slot = petSlots[shift];
            shift++;
            double multiplier = petsConfig.getDouble(path + ".multiplier", 1.0);
            double chance = petsConfig.getDouble(path + ".chance", 100.0);

            // Формируем имя и описание предмета
            String itemName = ChatColor.translateAlternateColorCodes('&', currentPet.getName());
            if (pet.get("status").equals("equipped")) {
                itemName = ChatColor.translateAlternateColorCodes('&', "&a[Экипировано] " + currentPet.getName());
            }
            List<String> lore = currentPet.getLore();
            List<String> coloredLore = lore.stream()
                    .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                    .map(line -> ChatColor.translateAlternateColorCodes('&', replacePlaceholders(line, "%multiplier%", String.valueOf(multiplier))))
                    .map(line -> ChatColor.translateAlternateColorCodes('&', replacePlaceholders(line, "%chance%", String.valueOf(chance))))
                    .collect(Collectors.toList());

            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(itemName);
                meta.setLore(coloredLore);
            }
            item.setItemMeta(meta);
            nbtTags.setNBTTagBoolean(item, "fishSim:isPetInMenu", true);
            nbtTags.setNBTTagString(item, "fishSim:petsMenuPetName", petUtils.getPetInfo((String) pet.get("type")).getDataName());
            nbtTags.setNBTTagString(item, "fishSim:petsMenuPetUUID", (String) pet.get("uuid"));
            nbtTags.setNBTTagInt(item, "fishSim:petsPage", page);
            menu.setItem(slot, item); // Устанавливаем предмет в слот
        }

        // Заполняем оставшиеся слоты серым стеклом
        ItemStack gray_glass = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = gray_glass.getItemMeta();
        if (meta != null) meta.setDisplayName(" ");
        gray_glass.setItemMeta(meta);
        for (int i = 0; i < menu.getSize(); i++) {
            if (menu.getItem(i) == null) menu.setItem(i, gray_glass);
        }

        // Кнопка следующей страницы
        if (page < totalPages - 1) {
            ItemStack nextPage = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = nextPage.getItemMeta();
            if (nextMeta != null) {
                nextMeta.setDisplayName(ChatColor.GREEN + "Следующая страница");
                nextPage.setItemMeta(nextMeta);
                nbtTags.setNBTTagBoolean(nextPage, "fishSim:isPetsNextPageItem", true);
                nbtTags.setNBTTagInt(nextPage, "fishSim:petsPage", page);
            }
            menu.setItem(51, nextPage);
        }

        // Кнопка предыдущей страницы
        if (page > 0) {
            ItemStack prevPage = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prevPage.getItemMeta();
            if (prevMeta != null) {
                prevMeta.setDisplayName(ChatColor.RED + "Предыдущая страница");
                prevPage.setItemMeta(prevMeta);
                nbtTags.setNBTTagBoolean(prevPage, "fishSim:isPetsPrevPageItem", true);
                nbtTags.setNBTTagInt(prevPage, "fishSim:petsPage", page);
            }
            menu.setItem(47, prevPage);
        }

        // Кнопка обновления
        ItemStack refreshItem = new ItemStack(Material.REDSTONE);
        ItemMeta refreshMeta = refreshItem.getItemMeta();
        if (refreshMeta != null) {
            refreshMeta.setDisplayName(ChatColor.YELLOW + "Обновить питомцев");
            refreshItem.setItemMeta(refreshMeta);
            nbtTags.setNBTTagBoolean(refreshItem, "fishSim:isPetsRefreshItem", true);
            nbtTags.setNBTTagInt(refreshItem, "fishSim:petsPage", page);
        }
        menu.setItem(49, refreshItem); // Центр инвентаря

        player.openInventory(menu);
    }

    public void onPetsInteract(Player player, ItemStack item, InventoryAction action) {
        FileConfiguration messagesConfig = dataManager.getCustomConfig("messages");
        if (nbtTags.getNBTTagBoolean(item, "fishSim:isPetsNextPageItem")) {
            int page = nbtTags.getNBTTagInt(item, "fishSim:petsPage");
            openPetsMenu(player, page + 1);
        }
        if (nbtTags.getNBTTagBoolean(item, "fishSim:isPetsPrevPageItem")) {
            int page = nbtTags.getNBTTagInt(item, "fishSim:petsPage");
            openPetsMenu(player, page - 1);
        }
        if (nbtTags.getNBTTagBoolean(item, "fishSim:isPetsRefreshItem")) {
            int page = nbtTags.getNBTTagInt(item, "fishSim:petsPage");
            openPetsMenu(player, page);
        }
        if (nbtTags.getNBTTagBoolean(item, "fishSim:isPetInMenu")) {
            String dataName = nbtTags.getNBTTagString(item, "fishSim:petsMenuPetName");
            String uuid = nbtTags.getNBTTagString(item, "fishSim:petsMenuPetUUID");
            int page = nbtTags.getNBTTagInt(item, "fishSim:petsPage");
            Map<String, Object> clickedPet = dataManager.getPlayerPet(player, uuid);
            if (clickedPet.get("status").equals("equipped")) {
                dataManager.unEquipPet(player, uuid);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', messagesConfig.getString("messages.unequip-pet")));
            } else if (dataManager.getEquippedPets(player).size() < 3) {
                dataManager.equipPet(player, uuid);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', messagesConfig.getString("messages.equip-pet")));
            } else if (dataManager.getEquippedPets(player).size() >= 3) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', messagesConfig.getString("messages.max-equipped-pets")));
            }
            openPetsMenu(player, page);
        }
    }

    public void onMenuInteract(Player player, ItemStack item) {
        if (nbtTags.getNBTTagBoolean(item, "fishSim:isMenuItem")) {
            String command = nbtTags.getNBTTagString(item, "fishSim:command");
            player.performCommand(command);
        }
    }

    public void onLocationsMenuInteract(Player player, ItemStack item) {
        if (!nbtTags.getNBTTagBoolean(item, "fishSim:isLocationsMenuItem")) return;

        String location_name = nbtTags.getNBTTagString(item, "fishSim:locName");
        FileConfiguration locationsConfig = dataManager.getCustomConfig("locations");
        FileConfiguration messagesConfig = dataManager.getCustomConfig("messages");
        String path = "locations." + location_name;

        if (!locationsConfig.contains(path)) return;

        int loc_level = locationsConfig.getInt(path + ".level");
        int player_level = dataManager.getPlayerLevel(player);

        if (player_level >= loc_level) {

            String locName = ChatColor.translateAlternateColorCodes('&', replacePlaceholders(locationsConfig.getString(path + ".name"), "%itemcolor%", "&a"));

            String worldName = locationsConfig.getString(path + ".world", "world");
            World world = Bukkit.getWorld(worldName);
            double x = locationsConfig.getDouble(path + ".x", 0);
            double y = locationsConfig.getDouble(path + ".y", 0);
            double z = locationsConfig.getDouble(path + ".z", 0);
            float yaw = (float) locationsConfig.getDouble(path + ".yaw", 0);
            float pitch = (float) locationsConfig.getDouble(path + ".pitch", 0);

            Location location = new Location(world, x, y, z);
            location.setYaw(yaw);
            location.setPitch(pitch);

            player.teleport(location);

            player.sendMessage(ChatColor.translateAlternateColorCodes('&', replacePlaceholders(messagesConfig.getString("messages.teleported-to-location"), "%location-name%", locName)));
        } else {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', messagesConfig.getString("messages.havent-level")));
        }
    }

    public void onFishingRodUpgrade(Player player, ItemStack item) {
        if (nbtTags.getNBTTagBoolean(item, "fishSim:isFishingRodFromMenu")) {
            int level = nbtTags.getNBTTagInt(item, "fishSim:fishingRodLevel");
            int price = nbtTags.getNBTTagInt(item, "fishSim:price");
            int fishesPrice = nbtTags.getNBTTagInt(item, "fishSim:fishesPrice");
            int balance = dataManager.getPlayerMoney(player);
            int fishes = dataManager.getPlayerFishes(player);
            if (balance >= price && fishes >= fishesPrice) {
                if (givePlayerFishingRod(player, level)) {
                    dataManager.takePlayerMoney(player, price);
                    dataManager.giveRodLevel(player, 1);
                }
            }
            if (player.getOpenInventory().getTitle().equalsIgnoreCase("Улучшение удочки")) {
                openFishingRodUpgrade(player);
            }
        }
    }

    public void onLevelUpgrade(Player player, ItemStack item) {
        FileConfiguration levelsConfig = dataManager.getCustomConfig("levels");
        if (nbtTags.getNBTTagBoolean(item, "fishSim:isExpBottleFromMenu")) {
            int level = nbtTags.getNBTTagInt(item, "fishSim:nextLevel");
            int price = nbtTags.getNBTTagInt(item, "fishSim:price");
            int fishesPrice = nbtTags.getNBTTagInt(item, "fishSim:fishesPrice");
            int balance = dataManager.getPlayerMoney(player);
            int fishes = dataManager.getPlayerFishes(player);
            if (balance >= price && fishes >= fishesPrice && levelsConfig.contains("levels." + level)) {
                dataManager.takePlayerMoney(player, price);
                dataManager.givePlayerLevel(player, 1);
            }
            if (player.getOpenInventory().getTitle().equalsIgnoreCase("Повышение уровня")) {
                openLevelUpgrade(player);
            }
        }
    }

    public boolean menuIsItemSection(String key) {
        FileConfiguration config = dataManager.getCustomConfig("menu");
        String path = "menu." + key;
        return config.contains(path + ".material") &&
               config.contains(path + ".slot");
    }

    public boolean hasItem(String itemType, Inventory inventory) {
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType().toString().equalsIgnoreCase(itemType)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasMenuItem(Inventory inventory) {
        for (ItemStack item : inventory.getContents()) {
            if (item != null && nbtTags.getNBTTagBoolean(item, "fishSim:isMenuItem")) {
                return true;
            }
        }
        return false;
    }

    public boolean hasFreeSlot(Inventory inventory) {
        for (ItemStack item : inventory.getContents()) {
            if (item == null) return true;
        }
        return false;
    }


    public boolean givePlayerFishingRod(Player player, int rodLevel) {
        Inventory inventory = player.getInventory();
        FileConfiguration playerConfig = dataManager.getCustomConfig("players");
        FileConfiguration itemsConfig = dataManager.getCustomConfig("items");
        String path = "items.FISHING_ROD.";

        if (itemsConfig.contains(path + "level." + rodLevel)) {

            if (hasItem("FISHING_ROD", inventory)) inventory.remove(Material.FISHING_ROD);

            ItemStack fishing_rod = new ItemStack(Material.FISHING_ROD);
            ItemMeta meta = fishing_rod.getItemMeta();

            String name = ChatColor.translateAlternateColorCodes('&', itemsConfig.getString(path + "name", "&bУдочка"));
            List<String> lore = itemsConfig.getStringList(path + "lore");

            ConfigurationSection enchantmentsSection = itemsConfig.getConfigurationSection(path + ".level." + rodLevel + ".enchantments");

            if (enchantmentsSection != null) {
                Map<String, Object> enchantments = enchantmentsSection.getValues(false);
                for (Map.Entry<String, Object> entry : enchantments.entrySet()) {
                    String enchantmentName = entry.getKey();
                    int level = (int) entry.getValue();
                    Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(enchantmentName.toLowerCase()));
                    if (enchantment != null) {
                        meta.addEnchant(enchantment, level, true);
                    }
                    if (enchantmentName.equalsIgnoreCase("LURE")) {
                        lore.add("&9Приманка " + level);
                    }
                }
            }


            List<String> coloredLore = lore.stream()                                                                                                                        // Форматирование текста в описании
                    .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                    .map(line -> ChatColor.translateAlternateColorCodes('&', replacePlaceholders(line, "%fishing_rod_level%", String.valueOf(rodLevel))))
                    .collect(Collectors.toList());

            if (meta != null) {
                meta.setDisplayName(name);
                meta.setLore(coloredLore);
                meta.setUnbreakable(true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
            }
            fishing_rod.setItemMeta(meta);

            if (hasFreeSlot(inventory)) {
                inventory.addItem(fishing_rod);
                return true;
            }
        }
        return false;
    }

    public void givePlayerMenuItem(Player player) {
        Inventory inventory = player.getInventory();
        if (!hasMenuItem(inventory)) {
            ItemStack menuItem = new ItemStack(Material.NETHER_STAR);
            ItemMeta meta = menuItem.getItemMeta();
            if (meta != null) meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&bМеню"));
            menuItem.setItemMeta(meta);
            nbtTags.setNBTTagBoolean(menuItem, "fishSim:isMenuItem", true);
            if (inventory.getItem(8) == null) {
                inventory.setItem(8, menuItem);
            } else {
                inventory.addItem(menuItem);
            }
        }
    }

    public void onPlayerInteract(Player player, ItemStack item) {
        if (nbtTags.getNBTTagBoolean(item, "fishSim:isMenuItem")) player.performCommand("fishsim menu");
    }


    public ItemStack getSkull(String value) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);

        if (value == null || value.isEmpty()) {
            return head;
        }

        SkullMeta headMeta = (SkullMeta) head.getItemMeta();
        GameProfile profile = new GameProfile(UUID.randomUUID(), null);

        // Устанавливаем текстуру, используя signature
        profile.getProperties().put("textures", new Property("textures", value));

        try {
            Field profileField = headMeta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(headMeta, profile);
        } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e1) {
            e1.printStackTrace();
        }

        head.setItemMeta(headMeta);
        return head;
    }


}
