package com.supplymod;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SupplyMod implements ModInitializer {
    public static final String MOD_ID = "supplymod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final Map<UUID, List<ItemStack>> PARDONED_ITEMS = new ConcurrentHashMap<>();

    private static final double HEARTS_LOST_ON_DEATH = ModItems.HEALTH_PER_HEART_ITEM;

    @Override
    public void onInitialize() {
        LOGGER.info("[SupplyMod] Startuje...");

        ModItems.register();
        SupplyDropManager.init();

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            server.getWorlds().forEach(SupplyDropManager::onWorldTick);
            server.getWorlds().forEach(CraftAltarManager::onWorldTick);
        });

        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, damageSource, damageAmount) -> {
            if (entity instanceof ServerPlayerEntity player) {
                PlayerInventory inv = player.getInventory();
                for (int i = 0; i < inv.size(); i++) {
                    ItemStack stack = inv.getStack(i);
                    if (ModItems.isPardoned(stack)) {
                        ItemStack saved = stack.copy();
                        saved.remove(DataComponentTypes.CUSTOM_DATA);
                        stashFor(player.getUuid()).add(saved);
                        inv.setStack(i, ItemStack.EMPTY);
                    }
                }

                EntityAttributeInstance attr = player.getAttributeInstance(EntityAttributes.MAX_HEALTH);
                if (attr != null) {
                    double currentBase = attr.getBaseValue();
                    double newBase = Math.max(20.0, currentBase - HEARTS_LOST_ON_DEATH);
                    if (newBase < currentBase) {
                        attr.setBaseValue(newBase);
                        LOGGER.info("[SupplyMod] {} stracil serce po smierci (nowe maksimum: {}/20)",
                                player.getName().getString(), (int) (newBase / 2));
                    }
                }
            }
            return true;
        });

        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            UUID id = oldPlayer.getUuid();
            List<ItemStack> saved = PARDONED_ITEMS.remove(id);
            if (saved != null) {
                for (ItemStack stack : saved) {
                    if (!newPlayer.getInventory().insertStack(stack)) {
                        newPlayer.dropItem(stack, false);
                    }
                }
                LOGGER.info("[SupplyMod] Zwrocono {} przedmiot(y) chronione Ksiega Ulaskawienia dla {}",
                        saved.size(), newPlayer.getName().getString());
            }
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    CommandManager.literal("crafting")
                            .then(CommandManager.argument("name", StringArgumentType.string())
                                    .then(CommandManager.argument("result", StringArgumentType.string())
                                            .then(CommandManager.argument("ingredients", StringArgumentType.greedyString())
                                                    .executes(ctx -> {
                                                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                                                        if (player == null) return 0;

                                                        String name = StringArgumentType.getString(ctx, "name");
                                                        String resultId = StringArgumentType.getString(ctx, "result");
                                                        String ingredientsRaw = StringArgumentType.getString(ctx, "ingredients");

                                                        Item resultItem = Registries.ITEM.get(Identifier.of(resultId));
                                                        if (resultItem == net.minecraft.item.Items.AIR) {
                                                            ctx.getSource().sendError(Text.literal("Nieznany przedmiot: " + resultId));
                                                            return 0;
                                                        }

                                                        List<CraftRecipe.Ingredient> ingredients = new ArrayList<>();
                                                        for (String token : ingredientsRaw.trim().split("\\s+")) {
                                                            String[] parts = token.split(":");
                                                            if (parts.length != 3) {
                                                                ctx.getSource().sendError(Text.literal("Zly format skladnika: " + token
                                                                        + " (oczekiwano namespace:item:ilosc)"));
                                                                return 0;
                                                            }
                                                            Identifier itemId = Identifier.of(parts[0], parts[1]);
                                                            Item ingredientItem = Registries.ITEM.get(itemId);
                                                            if (ingredientItem == net.minecraft.item.Items.AIR) {
                                                                ctx.getSource().sendError(Text.literal("Nieznany skladnik: " + parts[0] + ":" + parts[1]));
                                                                return 0;
                                                            }
                                                            int count;
                                                            try {
                                                                count = Integer.parseInt(parts[2]);
                                                            } catch (NumberFormatException e) {
                                                                ctx.getSource().sendError(Text.literal("Zla ilosc dla: " + token));
                                                                return 0;
                                                            }
                                                            ingredients.add(new CraftRecipe.Ingredient(ingredientItem, count));
                                                        }

                                                        CraftRecipe recipe = new CraftRecipe(resultItem, name, ingredients);
                                                        CraftAltarManager.spawnAltar(
                                                                (ServerWorld) player.getEntityWorld(),
                                                                recipe,
                                                                new Vec3d(player.getX(), player.getY(), player.getZ()));

                                                        ctx.getSource().sendFeedback(() -> Text.literal("Utworzono oltarz craftingowy: " + name), false);
                                                        return 1;
                                                    })
                                            )
                                    )
                            )
            );
        });

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (entity instanceof DisplayEntity.ItemDisplayEntity itemDisplay && player instanceof ServerPlayerEntity serverPlayer) {
                boolean crafted = CraftAltarManager.tryCraft(serverPlayer, itemDisplay);
                return crafted ? ActionResult.SUCCESS : ActionResult.PASS;
            }
            return ActionResult.PASS;
        });
    }

    public static List<ItemStack> stashFor(UUID playerId) {
        return PARDONED_ITEMS.computeIfAbsent(playerId, k -> new ArrayList<>());
    }
                                                            }
