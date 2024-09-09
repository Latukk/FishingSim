package org.latuk.fishingsim.handlers;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.latuk.fishingsim.Pet;
import org.latuk.fishingsim.utils.FishUtils;
import org.latuk.fishingsim.utils.InventoryUtils;
import org.latuk.fishingsim.utils.PetUtils;

import java.util.List;

public class Commands implements CommandExecutor {
    private final FishUtils fishUtils;
    private final InventoryUtils inventoryUtils;
    private final PetUtils petUtils;
    public Commands(FishUtils fishUtils, InventoryUtils inventoryUtils, PetUtils petUtils) {
        this.fishUtils = fishUtils;
        this.petUtils = petUtils;
        this.inventoryUtils = inventoryUtils;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("fishsim")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (args.length > 0) {
                    switch (args[0].toLowerCase()) {
                        case "sellfishes":
                            fishUtils.sellAllFishes(player);
                            break;
                        case "menu":
                            inventoryUtils.openMenu(player);
                            break;
                        case "upgraderod":
                            inventoryUtils.openFishingRodUpgrade(player);
                            break;
                        case "upgradelevel":
                            inventoryUtils.openLevelUpgrade(player);
                            break;
                        case "locations":
                            inventoryUtils.openLocationsMenu(player);
                            break;
                        case "pets":
                            inventoryUtils.openPetsMenu(player, 0);
                            break;
                        case "test":
                            break;
                    }
                }
            }
        }
        return true;
    }
}