package org.latuk.fishingsim.handlers;

import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.latuk.fishingsim.Pet;
import org.latuk.fishingsim.utils.DataManager;
import org.latuk.fishingsim.utils.FishUtils;
import org.latuk.fishingsim.utils.InventoryUtils;
import org.latuk.fishingsim.utils.PetUtils;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class Events implements Listener {
    private final InventoryUtils inventoryUtils;
    private final DataManager dataManager;
    private final FishUtils fishUtils;
    private final PetUtils petUtils;
    public Events(InventoryUtils inventoryUtils, DataManager dataManager, FishUtils fishUtils, PetUtils petUtils) {
        this.inventoryUtils = inventoryUtils;
        this.dataManager = dataManager;
        this.fishUtils = fishUtils;
        this.petUtils = petUtils;
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFishing(PlayerFishEvent event) {
        Player player = event.getPlayer();
        Entity caught = event.getCaught();
        if (caught != null) {
            caught.remove();
            inventoryUtils.givePlayerRandomFish(player);
            petUtils.givePlayerRandomPetWithChance(player, 25);
        } else {
            Inventory inventory = player.getInventory();
            if (!inventoryUtils.hasFreeSpace(inventory)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + dataManager.getMessageFromConfig("full-inventory", player));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getInventory();
        ItemStack item = event.getCurrentItem();
        if (item == null) return;

        String title = event.getView().getTitle();

        switch (title) {
            default:
                if (title.contains("Меню")) {
                    event.setCancelled(true);
                    player.updateInventory();
                    inventoryUtils.onMenuInteract(player, item);
                } else if (title.contains("Улучшение удочки")) {
                    event.setCancelled(true);
                    player.updateInventory();
                    inventoryUtils.onFishingRodUpgrade(player, item);
                } else if (title.contains("Повышение уровня")) {
                    event.setCancelled(true);
                    player.updateInventory();
                    inventoryUtils.onLevelUpgrade(player, item);
                } else if (title.contains("Локации")) {
                    event.setCancelled(true);
                    player.updateInventory();
                    inventoryUtils.onLocationsMenuInteract(player, item);
                } else if (title.contains("Питомцы")) {
                    event.setCancelled(true);
                    player.updateInventory();
                    inventoryUtils.onPetsInteract(player, item, event.getAction());
                }
                break;
        }
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!dataManager.isPlayerExistsInCFG(player)) dataManager.addNewPlayerToCFG(player);
        int rodLevel = dataManager.getFishingRodLevel(player);
        inventoryUtils.givePlayerFishingRod(player, rodLevel);
        inventoryUtils.givePlayerMenuItem(player);

        for (int i = 0; i < 50; i++) dataManager.givePlayerPet(player, petUtils.getRandomPet());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDrop(PlayerDropItemEvent event) {
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (event.getAction().name().contains("RIGHT_CLICK")) inventoryUtils.onPlayerInteract(player, item);
    }
}
