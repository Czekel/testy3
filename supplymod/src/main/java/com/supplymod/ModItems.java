package com.supplymod;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class ModItems {

    // Max health player can reach with heart items: 20 hearts = 40.0 HP.
    public static final double MAX_HEALTH_CAP = 40.0;
    // Each Heart item grants 2.0 HP (1 heart).
    public static final double HEALTH_PER_HEART_ITEM = 2.0;

    public static final String PARDON_KEY = "supplymod_pardoned";

    public static Item BOOK_OF_PARDON;
    public static Item HEART_ITEM;

    public static void register() {
        BOOK_OF_PARDON = Registry.register(
                Registries.ITEM,
                Identifier.of(SupplyMod.MOD_ID, "book_of_pardon"),
                new PardonBookItem(new Item.Settings().maxCount(16)));

        HEART_ITEM = Registry.register(
                Registries.ITEM,
                Identifier.of(SupplyMod.MOD_ID, "heart"),
                new HeartItem(new Item.Settings().maxCount(16)));
    }

    /** Checks whether a stack is protected by the Ksiega Ulaskawienia effect. */
    public static boolean isPardoned(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
        return data != null && data.copyNbt().getBoolean(PARDON_KEY);
    }

    /**
     * Ksiega Ulaskawienia - no anvil needed. Hold the item you want to
     * protect in one hand and the book in the other, then right-click with
     * the book: it tags the OTHER hand's item and is consumed.
     * Always shows the vanilla "enchantment glint" shimmer.
     */
    public static class PardonBookItem extends Item {
        public PardonBookItem(Settings settings) {
            super(settings);
        }

        @Override
        public boolean hasGlint(ItemStack stack) {
            return true;
        }

        @Override
        public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
            ItemStack book = player.getStackInHand(hand);

            if (world.isClient) {
                return TypedActionResult.success(book);
            }

            Hand otherHand = (hand == Hand.MAIN_HAND) ? Hand.OFF_HAND : Hand.MAIN_HAND;
            ItemStack target = player.getStackInHand(otherHand);

            if (target.isEmpty()) {
                player.sendMessage(Text.literal("Trzymaj w drugiej rece przedmiot, ktory chcesz chronic."), true);
                return TypedActionResult.fail(book);
            }
            if (target.isOf(BOOK_OF_PARDON)) {
                player.sendMessage(Text.literal("Nie mozesz oznaczyc drugiej Ksiegi Ulaskawienia."), true);
                return TypedActionResult.fail(book);
            }

            NbtCompound nbt = new NbtCompound();
            nbt.putBoolean(PARDON_KEY, true);
            target.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));

            player.sendMessage(Text.literal("Przedmiot zostal oznaczony Ksiega Ulaskawienia - nie wypadnie po smierci!"), true);

            if (!player.getAbilities().creativeMode) {
                book.decrement(1);
            }
            return TypedActionResult.success(book);
        }
    }

    /**
     * Custom item: consuming it permanently raises the player's max health by
     * one heart (2.0 HP), up to the 20-heart (40 HP) cap.
     */
    public static class HeartItem extends Item {
        public HeartItem(Settings settings) {
            super(settings);
        }

        @Override
        public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
            ItemStack stack = player.getStackInHand(hand);
            if (world.isClient) {
                return TypedActionResult.success(stack);
            }

            EntityAttributeInstance attr = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
            if (attr == null) {
                return TypedActionResult.pass(stack);
            }

            double currentBase = attr.getBaseValue();
            if (currentBase >= MAX_HEALTH_CAP) {
                player.sendMessage(Text.literal("Masz juz maksymalna liczbe serc (20)."), true);
                return TypedActionResult.fail(stack);
            }

            double newBase = Math.min(MAX_HEALTH_CAP, currentBase + HEALTH_PER_HEART_ITEM);
            attr.setBaseValue(newBase);
            player.setHealth((float) Math.min(player.getHealth() + HEALTH_PER_HEART_ITEM, (float) newBase));
            player.sendMessage(Text.literal("Zdobywasz dodatkowe serce! (" + (int) (newBase / 2) + "/20)"), true);

            world.sendEntityStatus(player, (byte) 35);

            if (!player.getAbilities().creativeMode) {
                stack.decrement(1);
            }
            return TypedActionResult.success(stack);
        }
    }
}
