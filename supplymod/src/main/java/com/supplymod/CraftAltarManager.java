package com.supplymod;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.decoration.InteractionEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.AffineTransformation;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Manages "crafting altars" created via the /crafting command: a small,
 * slightly tilted, spinning hologram of the reward item, with a floating
 * list of required ingredients above it (red = missing, green = have
 * enough). Since Display entities have no hitbox and can't be clicked
 * directly, an invisible InteractionEntity is layered on top of the item
 * display to actually catch the click. Right-clicking it when all
 * ingredients are satisfied consumes them from the player's inventory,
 * applies any configured enchantments and lore, sets the reward item's
 * display name to match the pedestal's color, gives the reward, plays
 * the Wither spawn sound, and announces the craft in chat with the
 * player's name.
 *
 * The altar's chunk is force-loaded for as long as it's active - otherwise
 * the chunk unloads when no player is nearby, which freezes every entity
 * in it. If any of an altar's entities were removed outside of this
 * manager (e.g. a manual /kill in-game instead of /crafting remove), that
 * altar is detected and cleaned up on the next tick instead of being
 * touched further. On player (re)join, every altar is fully torn down and
 * respawned with brand-new entity IDs, since a rejoining client can lose
 * track of previously-known invisible/display entities and never fixing
 * that is worse than a brief re-render flicker.
 */
public class CraftAltarManager {

    private static final List<Altar> activeAltars = new ArrayList<>();
    private static final float ITEM_SCALE = 0.5f;
    private static final float TILT_RADIANS = 0.3f; // ~17 degrees

    public static void onWorldTick(ServerWorld world) {
        Iterator<Altar> it = activeAltars.iterator();
        while (it.hasNext()) {
            Altar altar = it.next();

            if (altar.itemDisplay.getEntityWorld() != world) continue;

            if (altar.itemDisplay.isRemoved() || altar.hitbox.isRemoved()
                    || altar.nameDisplay.isRemoved() || altar.headerDisplay.isRemoved()
                    || anyLineRemoved(altar)) {
                cleanupAltar(world, altar);
                it.remove();
                continue;
            }

            try {
                spinItem(altar);

                if (world.getTime() % 20 == 0) {
                    updateColors(world, altar);
                }
            } catch (Exception e) {
                SupplyMod.LOGGER.warn("[SupplyMod] Oltarz '{}' napotkal blad i zostal usuniety: {}",
                        altar.recipe.displayName, e.getMessage());
                cleanupAltar(world, altar);
                it.remove();
            }
        }
    }

    public static void refreshTrackingOnJoin(ServerWorld world) {
        List<Altar> toRebuild = new ArrayList<>();
        for (Altar altar : activeAltars) {
            if (altar.itemDisplay.getEntityWorld() != world) continue;
            toRebuild.add(altar);
        }

        for (Altar altar : toRebuild) {
            Vec3d position = new Vec3d(altar.center.x, altar.center.y, altar.center.z);
            CraftRecipe recipe = altar.recipe;

            cleanupAltar(world, altar);
            activeAltars.remove(altar);

            spawnAltar(world, recipe, position);
        }
    }

    private static boolean anyLineRemoved(Altar altar) {
        for (IngredientLine line : altar.ingredientLines) {
            if (line.textDisplay.isRemoved()) return true;
        }
        return false;
    }

    private static void cleanupAltar(ServerWorld world, Altar altar) {
        if (!altar.itemDisplay.isRemoved()) altar.itemDisplay.discard();
        if (!altar.hitbox.isRemoved()) altar.hitbox.discard();
        if (!altar.nameDisplay.isRemoved()) altar.nameDisplay.discard();
        if (!altar.headerDisplay.isRemoved()) altar.headerDisplay.discard();
        for (IngredientLine line : altar.ingredientLines) {
            if (!line.textDisplay.isRemoved()) line.textDisplay.discard();
        }
        releaseChunk(world, altar);
    }

    private static void spinItem(Altar altar) {
        altar.spinAngle += 0.05f;
        AffineTransformation transform = new AffineTransformation(
                new Vector3f(0, 0, 0),
                new Quaternionf().rotateX(TILT_RADIANS).rotateY(altar.spinAngle),
                new Vector3f(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE),
                new Quaternionf()
        );
        altar.itemDisplay.setTransformation(transform);
    }

    private static void updateColors(ServerWorld world, Altar altar) {
        ServerPlayerEntity nearest = null;
        double bestDist = Double.MAX_VALUE;
        for (ServerPlayerEntity player : world.getPlayers()) {
            Vec3d playerPos = new Vec3d(player.getX(), player.getY(), player.getZ());
            double dist = playerPos.distanceTo(altar.center);
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

        ChunkPos chunkPos = new ChunkPos((int) Math.floor(position.x) >> 4, (int) Math.floor(position.z) >> 4);
        world.setChunkForced(chunkPos.x, chunkPos.z, true);

        DisplayEntity.ItemDisplayEntity itemDisplay =
                new DisplayEntity.ItemDisplayEntity(EntityType.ITEM_DISPLAY, world);
        itemDisplay.updatePosition(position.x, baseY + 1.0, position.z);
        itemDisplay.setItemStack(new ItemStack(recipe.resultItem, 1));
        itemDisplay.setInvulnerable(true);
        world.spawnEntity(itemDisplay);

        InteractionEntity hitbox = new InteractionEntity(EntityType.INTERACTION, world);
        hitbox.updatePosition(position.x, baseY + 1.0, position.z);
        hitbox.setInvulnerable(true);
        world.spawnEntity(hitbox);

        DisplayEntity.TextDisplayEntity nameDisplay =
                new DisplayEntity.TextDisplayEntity(EntityType.TEXT_DISPLAY, world);
        nameDisplay.updatePosition(position.x, baseY + 1.6, position.z);
        nameDisplay.setText(Text.literal(recipe.displayName).formatted(recipe.nameColor, Formatting.BOLD));
        nameDisplay.setBillboardMode(DisplayEntity.BillboardMode.CENTER);
        nameDisplay.setInvulnerable(true);
        world.spawnEntity(nameDisplay);

        List<IngredientLine> lines = new ArrayList<>();
        double y = baseY + 2.0;
        List<CraftRecipe.Ingredient> reversed = new ArrayList<>(recipe.ingredients);
        java.util.Collections.reverse(reversed);
        for (CraftRecipe.Ingredient ingredient : reversed) {
            DisplayEntity.TextDisplayEntity ingredientDisplay =
                    new DisplayEntity.TextDisplayEntity(EntityType.TEXT_DISPLAY, world);
            ingredientDisplay.updatePosition(position.x, y, position.z);
            ingredientDisplay.setText(Text.literal(
                    "x" + ingredient.count + " " + ingredient.item.getName().getString()
            ).formatted(Formatting.RED));
            ingredientDisplay.setBillboardMode(DisplayEntity.BillboardMode.CENTER);
            ingredientDisplay.setInvulnerable(true);
            world.spawnEntity(ingredientDisplay);

            lines.add(0, new IngredientLine(ingredient, ingredientDisplay));
            y += 0.3;
        }

        DisplayEntity.TextDisplayEntity headerDisplay =
                new DisplayEntity.TextDisplayEntity(EntityType.TEXT_DISPLAY, world);
        headerDisplay.updatePosition(position.x, y + 0.1, position.z);
        headerDisplay.setText(Text.literal("Wymagane przedmioty:").formatted(Formatting.GOLD, Formatting.BOLD));
        headerDisplay.setBillboardMode(DisplayEntity.BillboardMode.CENTER);
        headerDisplay.setInvulnerable(true);
        world.spawnEntity(headerDisplay);

        Altar altar = new Altar(recipe, itemDisplay, hitbox, nameDisplay, headerDisplay, lines, position, chunkPos);
        activeAltars.add(altar);
    }

    public static boolean tryCraft(ServerPlayerEntity player, InteractionEntity clickedHitbox) {
        for (Altar altar : activeAltars) {
            if (altar.hitbox != clickedHitbox) continue;

            for (CraftRecipe.Ingredient ingredient : altar.recipe.ingredients) {
                if (countItem(player, ingredient.item) < ingredient.count) {
                    player.sendMessage(Text.literal("Nie masz wystarczajacej ilosci przedmiotow!").formatted(Formatting.RED), false);
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

            ServerWorld world = (ServerWorld) player.getEntityWorld();

            ItemStack rewardStack = new ItemStack(altar.recipe.resultItem, 1);
            rewardStack.set(DataComponentTypes.CUSTOM_NAME,
                    Text.literal(altar.recipe.displayName).formatted(altar.recipe.nameColor, Formatting.BOLD));

            if (!altar.recipe.enchantments.isEmpty()) {
                var enchantRegistry = world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
                for (CraftRecipe.EnchantEntry entry : altar.recipe.enchantments) {
                    net.minecraft.enchantment.Enchantment enchantValue = enchantRegistry.get(entry.enchantment.getValue());
                    if (enchantValue != null) {
                        RegistryEntry<net.minecraft.enchantment.Enchantment> enchantEntry = enchantRegistry.getEntry(enchantValue);
                        EnchantmentHelper.apply(rewardStack, builder -> builder.add(enchantEntry, entry.level));
                    }
                }
            }

            if (!altar.recipe.lore.isEmpty()) {
                List<Text> loreLines = new ArrayList<>();
                for (String line : altar.recipe.lore) {
                    loreLines.add(Text.literal(line).formatted(Formatting.GRAY, Formatting.ITALIC));
                }
                rewardStack.set(DataComponentTypes.LORE, new LoreComponent(loreLines));
            }

            player.getInventory().insertStack(rewardStack);

            world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_WITHER_SPAWN,
                    SoundCategory.HOSTILE, 1.0f, 1.0f);

            for (ServerPlayerEntity p : world.getPlayers()) {
                p.sendMessage(Text.literal(player.getName().getString() + " stworzyl legendarna bron "
                        + altar.recipe.displayName).formatted(Formatting.GOLD), false);
            }

            cleanupAltar(world, altar);
            activeAltars.remove(altar);
            return true;
        }
        return false;
    }

    public static boolean removeAltarByName(String name) {
        Iterator<Altar> it = activeAltars.iterator();
        boolean removedAny = false;
        while (it.hasNext()) {
            Altar altar = it.next();
            if (!altar.recipe.displayName.equalsIgnoreCase(name)) continue;

            ServerWorld world = (ServerWorld) altar.itemDisplay.getEntityWorld();
            cleanupAltar(world, altar);
            it.remove();
            removedAny = true;
        }
        return removedAny;
    }

    private static void releaseChunk(ServerWorld world, Altar altar) {
        world.setChunkForced(altar.chunkPos.x, altar.chunkPos.z, false);
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
        final InteractionEntity hitbox;
        final DisplayEntity.TextDisplayEntity nameDisplay;
        final DisplayEntity.TextDisplayEntity headerDisplay;
        final List<IngredientLine> ingredientLines;
        final Vec3d center;
        final ChunkPos chunkPos;
        float spinAngle = 0f;

        Altar(CraftRecipe recipe, DisplayEntity.ItemDisplayEntity itemDisplay, InteractionEntity hitbox,
              DisplayEntity.TextDisplayEntity nameDisplay, DisplayEntity.TextDisplayEntity headerDisplay,
              List<IngredientLine> ingredientLines, Vec3d center, ChunkPos chunkPos) {
            this.recipe = recipe;
            this.itemDisplay = itemDisplay;
            this.hitbox = hitbox;
            this.nameDisplay = nameDisplay;
            this.headerDisplay = headerDisplay;
            this.ingredientLines = ingredientLines;
            this.center = center;
            this.chunkPos = chunkPos;
        }
    }
    }
