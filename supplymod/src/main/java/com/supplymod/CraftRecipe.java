package com.supplymod;

import net.minecraft.item.Item;

import java.util.List;

public class CraftRecipe {
    public final Item resultItem;
    public final String displayName;
    public final List<Ingredient> ingredients;

    public CraftRecipe(Item resultItem, String displayName, List<Ingredient> ingredients) {
        this.resultItem = resultItem;
        this.displayName = displayName;
        this.ingredients = ingredients;
    }

    public static class Ingredient {
        public final Item item;
        public final int count;

        public Ingredient(Item item, int count) {
            this.item = item;
            this.count = count;
        }
    }
}
