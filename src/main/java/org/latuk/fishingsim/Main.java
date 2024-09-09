package org.latuk.fishingsim;

import org.bukkit.plugin.java.JavaPlugin;
import org.latuk.fishingsim.NMS.NBTTags;
import org.latuk.fishingsim.handlers.Commands;
import org.latuk.fishingsim.handlers.Events;
import org.latuk.fishingsim.utils.*;

public final class Main extends JavaPlugin {

    @Override
    public void onEnable() {
        DataManager dataManager = new DataManager(this);
        NBTTags nbtTags = new NBTTags(this);
        PetUtils petUtils = new PetUtils(dataManager, nbtTags);
        FishUtils fishUtils = new FishUtils(dataManager, nbtTags, petUtils);
        ScoreboardManager scoreboardManager = new ScoreboardManager(this, dataManager, petUtils);
        InventoryUtils inventoryUtils = new InventoryUtils(dataManager, fishUtils, petUtils, nbtTags);
        getCommand("fishsim").setExecutor(new Commands(fishUtils, inventoryUtils, petUtils));
        getServer().getPluginManager().registerEvents(new Events(inventoryUtils, dataManager, fishUtils, petUtils), this);

        // Загружаем основные конфиги при старте плагина
        String[] cfgs = {"players", "fishes", "pets", "locations", "items", "menu", "levels", "scoreboard", "messages"};
        for (String cfg: cfgs) dataManager.setupConfig(cfg);
    }
    public static Main getInstance() {
        return JavaPlugin.getPlugin(Main.class);
    }

}
