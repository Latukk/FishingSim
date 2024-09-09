package org.latuk.fishingsim;

import org.bukkit.Material;

import java.util.List;
import java.util.Map;

public class Fish {
    private String dataName;
    private String name;
    private List<String> lore;
    private double minWeight;
    private double maxWeight;
    private Material material;
    private int price;
    private Map<String, Integer> locationChances; // Шансы по локациям

    public Fish(String dataName, String name, List<String> lore, double minWeight, double maxWeight, Material material, int price, Map<String, Integer> locationChances) {
        this.dataName = dataName;
        this.name = name;
        this.lore = lore;
        this.minWeight = minWeight;
        this.maxWeight = maxWeight;
        this.material = material;
        this.price = price;
        this.locationChances = locationChances;
    }

    public String getDataName() {
        return dataName;
    }

    public String getName() {
        return name;
    }

    public List<String> getLore() {
        return lore;
    }

    public double getMinWeight() {
        return minWeight;
    }

    public double getMaxWeight() {
        return maxWeight;
    }

    public Material getMaterial() {
        return material;
    }

    public int getPrice() {
        return price;
    }

    public int getLocationChance(String location) {
        return locationChances.getOrDefault(location, 0);
    }
}
