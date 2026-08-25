package com.supplymod;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.AffineTransformation;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages "crafting altars" created via the /crafting command: a spinning
 * hologram of the reward item, with a floating list of required ingredients
 * above it (red = missing, green = have enough). Right-clicking the item
 * when all ingredients are satisfied consumes them from the player's
 * inventory and gives the reward.
 */
public class CraftAltarManager {

    private static final List<Altar> activeAltars = new ArrayList<>();

    public static void onWorldTick(ServerWorld world) {
        List<Altar> toRemove = new ArrayList<>();
        for (Altar altar : activeAltars) {
            if (altar.itemDisplay.getEntityWorld() != world) continue;

            spinItem(altar);

            if (world.getTime() % 20 == 0) {
                updateColors(world, altar);
            }

            if (altar.itemDisplay.isRemoved()) {
                toRemove.add(altar);
            }
        }
        activeAltars.removeAll(toRemove);
    }

    private static void spinItem(Altar altar) {
        altar.spinAngle += 0.05f;
        AffineTransformation transform = new AffineTransformation(
                new Vector3f(0, 0, 0),
                new Quaternionf().rotateY(altar.spinAngle),
                new Vector3f(1, 1, 1),
                new Quaternionf()
        );
        altar.itemDisplay.setTransformation(transform);
    }

    private static void updateColors(ServerWorld world, Altar altar) {
        ServerPlayerEntity nearest = null;
        double bestDist = Double.MAX_VALUE;
        for (ServerPlayerEntity player : world.getPlayers()) {
            double dist = player.getPos().distanceTo(altar.center);
            if (dist < bestDist) {
                bestDist = dist;
                nearest = player;
            }
        }

        for (IngredientLine line : altar.ingredientLines) {
            int have = nearest == null ? 0 : countItem(nearest, line.ingredient.item);
            boolean satisfied = have >= line.ingredient.count;
            Formatting color = satisfied ? Formatting.GREEN : Formatting.RED;
            line.textDisplay.setText(Text.literal(
                    "x" + line.ingredient.count + " " + line.ingredient.item.getName().getString()
                            + " (" + have + "/" + line.ingredient.count + ")"
            ).formatted(color));
        }
    }

    private static int countItem(ServerPlayerEntity player, Item item) {
        int total = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isOf(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    public static void spawnAltar(ServerWorld world, CraftRecipe recipe, Vec3d position) {
        double baseY = position.y;

        DisplayEntity.ItemDisplayEntity itemDisplay =
                new DisplayEntity.ItemDisplayEntity(EntityType.ITEM_DISPLAY, world);
        itemDisplay.updatePosition(position.x, baseY + 1.0, position.z);
        itemDisplay.setStack(new ItemStack(recipe.resultItem, 1));
        world.spawnEntity(itemDisplay);

        DisplayEntity.TextDisplayEntity nameDisplay =
                new DisplayEntity.TextDisplayEntity(EntityType.TEXT_DISPLAY, world);
        nameDisplay.updatePosition(position.x, baseY + 1.6, position.z);
        nameDisplay.setText(Text.literal(recipe.displayName).formatted(Formatting.GOLD, Formatting.BOLD));
        nameDisplay.setBillboardMode(DisplayEntity.BillboardMode.CENTER);
        world.spawnEntity(nameDisplay);

        List<IngredientLine> lines = new ArrayList<>();
        double y = baseY + 2.0;
        // Build bottom-to-top so the list reads top-to-bottom as: header,
        // then each ingredient, then name, then the spinning item below.
        List<CraftRecipe.Ingredient> reversed = new ArrayList<>(recipe.ingredients);
        java.util.Collections.reverse(reversed);
        for (CraftRecipe.Ingredient ingredient : reversed) {
            DisplayEntity.TextDisplayEntity ingredientDisplay =
                    new DisplayEntity.TextDisplayEntity(EntityType.TEXT_DISPLAY, world);
            ingredientDisplay.updatePosition(position.x, y, position.z);
            ingredientDisplay.setText(Text.literal(
                    "x" + ingredient.count + " " + ingredient.item.getName().getString() + " (0/" + ingredient.count + ")"
            ).formatted(Formatting.RED));
            ingredientDisplay.setBillboardMode(DisplayEntity.BillboardMode.CENTER);
            world.spawnEntity(ingredientDisplay);

            lines.add(0, new IngredientLine(ingredient, ingredientDisplay));
            y += 0.3;
        }

        DisplayEntity.TextDisplayEntity headerDisplay =
                new DisplayEntity.TextDisplayEntity(EntityType.TEXT_DISPLAY, world);
        headerDisplay.updatePosition(position.x, y + 0.1, position.z);
        headerDisplay.setText(Text.literal("Wymagane przedmioty:").formatted(Formatting.GRAY, Formatting.BOLD));
        headerDisplay.setBillboardMode(DisplayEntity.BillboardMode.CENTER);
        world.spawnEntity(headerDisplay);

        Altar altar = new Altar(recipe, itemDisplay, nameDisplay, headerDisplay, lines, position);
        activeAltars.add(altar);
    }

    public static boolean tryCraft(ServerPlayerEntity player, DisplayEntity.ItemDisplayEntity clicked) {
        for (Altar altar : activeAltars) {
            if (altar.itemDisplay != clicked) continue;

            for (CraftRecipe.Ingredient ingredient : altar.recipe.ingredients) {
                if (countItem(player, ingredient.item) < ingredient.count) {
                    player.sendMessage(Text.literal("Brakuje wymaganych przedmiotow!").formatted(Formatting.RED), true);
                    return false;
                }
            }

            for (CraftRecipe.Ingredient ingredient : altar.recipe.ingredients) {
                int remaining = ingredient.count;
                for (int i = 0; i < player.getInventory().size() && remaining > 0; i++) {
                    ItemStack stack = player.getInventory().getStack(i);
                    if (stack.isOf(ingredient.item)) {
                        int take = Math.min(remaining, stack.getCount());
                        stack.decrement(take);
                        remaining -= take;
                    }
                }
            }

            player.getInventory().insertStack(new ItemStack(altar.recipe.resultItem, 1));

            ServerWorld world = (ServerWorld) player.getEntityWorld();
            world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_WITHER_SPAWN,
                    SoundCategory.HOSTILE, 1.0f, 1.0f);

            for (ServerPlayerEntity p : world.getPlayers()) {
                p.sendMessage(Text.literal("Legendarna Bron (" + altar.recipe.displayName + ") zostala stworzona!")
                        .formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD), false);
            }

            altar.itemDisplay.discard();
            altar.nameDisplay.discard();
            altar.headerDisplay.discard();
            for (IngredientLine line : altar.ingredientLines) {
                line.textDisplay.discard();
            }

            activeAltars.remove(altar);
            return true;
        }
        return false;
    }

    private static class IngredientLine {
        final CraftRecipe.Ingredient ingredient;
        final DisplayEntity.TextDisplayEntity textDisplay;

        IngredientLine(CraftRecipe.Ingredient ingredient, DisplayEntity.TextDisplayEntity textDisplay) {
            this.ingredient = ingredient;
            this.textDisplay = textDisplay;
        }
    }

    private static class Altar {
        final CraftRecipe recipe;
        final DisplayEntity.ItemDisplayEntity itemDisplay;
        final DisplayEntity.TextDisplayEntity nameDisplay;
        final DisplayEntity.TextDisplayEntity headerDisplay;
        final List<IngredientLine> ingredientLines;
        final Vec3d center;
        float spinAngle = 0f;

        Altar(CraftRecipe recipe, DisplayEntity.ItemDisplayEntity itemDisplay, DisplayEntity.TextDisplayEntity nameDisplay,
              DisplayEntity.TextDisplayEntity headerDisplay, List<IngredientLine> ingredientLines, Vec3d center) {
            this.recipe = recipe;
            this.itemDisplay = itemDisplay;
            this.nameDisplay = nameDisplay;
            this.headerDisplay = headerDisplay;
            this.ingredientLines = ingredientLines;
            this.center = center;
        }
    }
                                      }
