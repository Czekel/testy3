package com.supplymod;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.text.Text;
import net.minecraft.util.TypedActionResult;

public class ModItems {

    // Max health player can reach with heart items: 20 hearts = 40.0 HP.
    public static final double MAX_HEALTH_CAP = 40.0;
    // Each Heart item grants 2.0 HP (1 heart).
    public static final double HEALTH_PER_HEART_ITEM = 2.0;
    public static final Identifier HEART_BONUS_MODIFIER_ID =
            Identifier.of(SupplyMod.MOD_ID, "extra_heart_bonus");

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

    /**
     * Ksiega Ulaskawienia - zawsze ma wbudowany wanilowy efekt polysku
     * (ten sam "enchantment glint" co przedmioty zaklete), zeby migotala
     * w rece tak jak na zdjeciu referencyjnym - bez potrzeby kopiowania
     * jakiejkolwiek tekstury.
     */
    public static class PardonBookItem extends Item {
        public PardonBookItem(Settings settings) {
            super(settings);
        }

        @Override
        public boolean hasGlint(ItemStack stack) {
            return true;
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
        public TypedActionResult<ItemStack> use(net.minecraft.world.World world, net.minecraft.entity.player.PlayerEntity player, Hand hand) {
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

            // Efekt "totemowy" - dokladnie ten sam blysk na ekranie + dzwiek,
            // co przy zuzyciu Totemu Niesmiertelnosci. Status 35 to wanilowy
            // packet EntityStatus obslugiwany po stronie klienta.
            world.sendEntityStatus(player, (byte) 35);

            if (!player.getAbilities().creativeMode) {
                stack.decrement(1);
            }
            return TypedActionResult.success(stack);
        }
    }
}

