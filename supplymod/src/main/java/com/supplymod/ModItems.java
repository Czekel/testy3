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
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public class ModItems {

    public static final double MAX_HEALTH_CAP = 40.0;
    public static final double HEALTH_PER_HEART_ITEM = 2.0;

    public static final String PARDON_KEY = "supplymod_pardoned";

    public static Item BOOK_OF_PARDON;
    public static Item HEART_ITEM;

    public static void register() {
        RegistryKey<Item> bookKey = RegistryKey.of(RegistryKeys.ITEM,
                Identifier.of(SupplyMod.MOD_ID, "book_of_pardon"));
        BOOK_OF_PARDON = Registry.register(
                Registries.ITEM,
                bookKey,
                new PardonBookItem(new Item.Settings().maxCount(16).registryKey(bookKey)));

        RegistryKey<Item> heartKey = RegistryKey.of(RegistryKeys.ITEM,
                Identifier.of(SupplyMod.MOD_ID, "heart"));
        HEART_ITEM = Registry.register(
                Registries.ITEM,
                heartKey,
                new HeartItem(new Item.Settings().maxCount(16).registryKey(heartKey)));
    }

    public static boolean isPardoned(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
        return data != null && data.copyNbt().getBoolean(PARDON_KEY, false);
    }

    public static class PardonBookItem extends Item {
        public PardonBookItem(Settings settings) {
            super(settings);
        }

        @Override
        public boolean hasGlint(ItemStack stack) {
            return true;
        }

        @Override
        public ActionResult use(World world, PlayerEntity player, Hand hand) {
            ItemStack book = player.getStackInHand(hand);

            if (world.isClient()) {
                return ActionResult.SUCCESS;
            }

            Hand otherHand = (hand == Hand.MAIN_HAND) ? Hand.OFF_HAND : Hand.MAIN_HAND;
            ItemStack target = player.getStackInHand(otherHand);

            if (target.isEmpty()) {
                player.sendMessage(Text.literal("Trzymaj w drugiej rece przedmiot, ktory chcesz chronic."), true);
                return ActionResult.FAIL;
            }
            if (target.isOf(BOOK_OF_PARDON)) {
                player.sendMessage(Text.literal("Nie mozesz oznaczyc drugiej Ksiegi Ulaskawienia."), true);
                return ActionResult.FAIL;
            }

            NbtCompound nbt = new NbtCompound();
            nbt.putBoolean(PARDON_KEY, true);
            target.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));

            player.sendMessage(Text.literal("Przedmiot zostal oznaczony Ksiega Ulaskawienia - nie wypadnie po smierci!"), true);

            if (!player.getAbilities().creativeMode) {
                book.decrement(1);
            }
            return ActionResult.SUCCESS;
        }
    }

    public static class HeartItem extends Item {
        public HeartItem(Settings settings) {
            super(settings);
        }

        @Override
        public ActionResult use(World world, PlayerEntity player, Hand hand) {
            ItemStack stack = player.getStackInHand(hand);
            if (world.isClient()) {
                return ActionResult.SUCCESS;
            }

            EntityAttributeInstance attr = player.getAttributeInstance(EntityAttributes.MAX_HEALTH);
            if (attr == null) {
                return ActionResult.PASS;
            }

            double currentBase = attr.getBaseValue();
            if (currentBase >= MAX_HEALTH_CAP) {
                player.sendMessage(Text.literal("Masz juz maksymalna liczbe serc (20)."), true);
                return ActionResult.FAIL;
            }

            double newBase = Math.min(MAX_HEALTH_CAP, currentBase + HEALTH_PER_HEART_ITEM);
            attr.setBaseValue(newBase);
            player.setHealth((float) Math.min(player.getHealth() + HEALTH_PER_HEART_ITEM, (float) newBase));
            player.sendMessage(Text.literal("Zdobywasz dodatkowe serce! (" + (int) (newBase / 2) + "/20)"), true);

            world.sendEntityStatus(player, (byte) 35);

            if (!player.getAbilities().creativeMode) {
                stack.decrement(1);
            }
            return ActionResult.SUCCESS;
        }
    }
        }
