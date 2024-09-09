package org.latuk.fishingsim;

import java.util.List;

public class Pet {
    private String dataName;
    private String name;
    private List<String> lore;
    private int level;
    private double multiplier;
    private double chance;
    private String headValue;

    public Pet(String dataName, String name, List<String> lore, int level, double multiplier, double chance, String headValue) {
        this.dataName = dataName;
        this.name = name;
        this.lore = lore;
        this.level = level;
        this.multiplier = multiplier;
        this.chance = chance;
        this.headValue = headValue;
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

    public int getLevel() {
        return level;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public double getChance() {
        return chance;
    }

    public String getHeadValue() {
        return headValue;
    }
}