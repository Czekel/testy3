package com.supplymod;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Formatting;

import java.util.List;

public class CraftRecipe {
    public final Item resultItem;
    public final String displayName;
    public final Formatting nameColor;
    public final List<Ingredient> ingredients;
    public final List<EnchantEntry> enchantments;
    public final List<String> lore;

    public CraftRecipe(Item resultItem, String displayName, Formatting nameColor,
                        List<Ingredient> ingredients, List<EnchantEntry> enchantments, List<String> lore) {
        this.resultItem = resultItem;
        this.displayName = displayName;
        this.nameColor = nameColor;
        this.ingredients = ingredients;
        this.enchantments = enchantments;
        this.lore = lore;
    }

    public static class Ingredient {
        public final Item item;
        public final int count;

        public Ingredient(Item item, int count) {
            this.item = item;
            this.count = count;
        }
    }

    public static class EnchantEntry {
        public final RegistryKey<Enchantment> enchantment;
        public final int level;

        public EnchantEntry(RegistryKey<Enchantment> enchantment, int level) {
            this.enchantment = enchantment;
            this.level = level;
        }
    }
}
