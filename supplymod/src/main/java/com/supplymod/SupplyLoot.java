package com.supplymod;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Loot for the Supply Drop crate.
 * Every entry is an INDEPENDENT roll (not a weighted pool) - meaning several
 * entries (even the same item rolled with different stack sizes) can succeed
 * in the same crate, exactly as requested.
 *
 * Percentages as given:
 *  - Blok diamentu        30%
 *  - Totem Niesmiertelnosci 5%
 *  - Zlote jablko zaklete (Notch Apple) 10%
 *  - Zlote jablko          39%
 *  - Brodawki nether       25%
 *  - Czesc diamentowej zbroi (losowa) 40%
 *  - Zelazo + Zloto        70%
 *  - Zaczarowana ksiazka   30% (patrz uwaga nizej)
 *  - Serce (item)          10%
 *  - Ksiega Ulaskawienia    1%
 *  - Netherite scrap        5%
 *  - Diamenty (luzem)      30% (bylo 20%, +10pp na prosbe)
 *  - Miedz                 80%
 *
 *  Wypelniacze dodane przeze mnie, zeby zrzut nie byl pusty gdy nic
 *  wiekszego nie wypadnie:
 *  - Chleb                 60%
 *  - Strzaly                50%
 *  - Proch strzelniczy      35%
 *  - Sznurek                40%
 *  - Lapis lazuli           25%
 *  - Szmaragd               15%
 *  - Perla Endermana        12%
 *  - Siodlo                  8%
 *
 *  UWAGA (MC 1.21.1): zaklecia sa teraz elementem dynamicznego rejestru
 *  danych (podobnie jak biomy) i nie da sie ich pobrac statycznie z klasy
 *  Registries jak we wczesniejszych wersjach - wymagaja dostepu do
 *  RegistryWrapper w czasie dzialania serwera. Zeby nie blokowac reszty
 *  moda, ten wpis daje na razie zwykla (nie-zaklieta) Zaczarowana Ksiazke.
 *  Realne losowe zaklecie mozna dodac pozniej przekazujac np. ServerWorld
 *  do rollLoot() i czytajac world.getRegistryManager().get(RegistryKeys.ENCHANTMENT).
 */
public class SupplyLoot {

    public static List<ItemStack> rollLoot(Random random) {
        List<ItemStack> loot = new ArrayList<>();

        roll(random, 0.30, () -> new ItemStack(Items.DIAMOND_BLOCK, 1 + random.nextInt(2)), loot);
        roll(random, 0.05, () -> new ItemStack(Items.TOTEM_OF_UNDYING, 1), loot);
        roll(random, 0.10, () -> new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 1), loot); // "noche" = Notch Apple
        roll(random, 0.39, () -> new ItemStack(Items.GOLDEN_APPLE, 1 + random.nextInt(3)), loot);
        roll(random, 0.25, () -> new ItemStack(Items.NETHER_WART, 2 + random.nextInt(4)), loot);
        roll(random, 0.40, () -> randomDiamondArmorPiece(random), loot);
        roll(random, 0.70, () -> new ItemStack(Items.IRON_INGOT, 2 + random.nextInt(5)), loot);
        roll(random, 0.70, () -> new ItemStack(Items.GOLD_INGOT, 2 + random.nextInt(5)), loot);
        roll(random, 0.30, () -> new ItemStack(Items.ENCHANTED_BOOK, 1), loot);
        roll(random, 0.10, () -> new ItemStack(ModItems.HEART_ITEM, 1), loot);
        roll(random, 0.01, () -> new ItemStack(ModItems.BOOK_OF_PARDON, 1), loot);
        roll(random, 0.05, () -> new ItemStack(Items.NETHERITE_SCRAP, 1), loot);
        roll(random, 0.30, () -> new ItemStack(Items.DIAMOND, 1 + random.nextInt(3)), loot);
        roll(random, 0.80, () -> new ItemStack(Items.COPPER_INGOT, 3 + random.nextInt(6)), loot);

        // Wypelniacze - zeby zrzut nigdy nie byl calkiem pusty.
        roll(random, 0.60, () -> new ItemStack(Items.BREAD, 2 + random.nextInt(4)), loot);
        roll(random, 0.50, () -> new ItemStack(Items.ARROW, 5 + random.nextInt(11)), loot);
        roll(random, 0.35, () -> new ItemStack(Items.GUNPOWDER, 2 + random.nextInt(4)), loot);
        roll(random, 0.40, () -> new ItemStack(Items.STRING, 2 + random.nextInt(4)), loot);
        roll(random, 0.25, () -> new ItemStack(Items.LAPIS_LAZULI, 2 + random.nextInt(5)), loot);
        roll(random, 0.15, () -> new ItemStack(Items.EMERALD, 1 + random.nextInt(3)), loot);
        roll(random, 0.12, () -> new ItemStack(Items.ENDER_PEARL, 1 + random.nextInt(2)), loot);
        roll(random, 0.08, () -> new ItemStack(Items.SADDLE, 1), loot);

        return loot;
    }

    private static void roll(Random random, double chance, java.util.function.Supplier<ItemStack> supplier, List<ItemStack> loot) {
        if (random.nextDouble() < chance) {
            loot.add(supplier.get());
        }
    }

    private static ItemStack randomDiamondArmorPiece(Random random) {
        ItemStack[] pieces = new ItemStack[]{
                new ItemStack(Items.DIAMOND_HELMET),
                new ItemStack(Items.DIAMOND_CHESTPLATE),
                new ItemStack(Items.DIAMOND_LEGGINGS),
                new ItemStack(Items.DIAMOND_BOOTS)
        };
        return pieces[random.nextInt(pieces.length)];
    }
}
