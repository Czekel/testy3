package com.supplymod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
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

    // Items saved here after death (Ksiega Ulaskawienia effect) waiting to be
    // returned to the player on respawn. Keyed by player UUID.
    public static final Map<UUID, List<ItemStack>> PARDONED_ITEMS = new ConcurrentHashMap<>();

    @Override
    public void onInitialize() {
        LOGGER.info("[SupplyMod] Startuje...");

        ModItems.register();
        SupplyDropManager.init();

        // Daily 20% roll for a supply drop.
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            server.getWorlds().forEach(SupplyDropManager::onWorldTick);
        });

        // Ksiega Ulaskawienia: right before a fatal hit actually kills the
        // player, pull any tagged item out of their inventory so vanilla's
        // drop-on-death logic never sees it (it's simply not in the
        // inventory anymore by the time death processing runs). This uses
        // only a standard, supported Fabric API event - no mixin needed.
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
            }
            return true; // never cancel death itself, only rescue items
        });

        // Restore items protected by Ksiega Ulaskawienia after respawn.
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
    }

    public static List<ItemStack> stashFor(UUID playerId) {
        return PARDONED_ITEMS.computeIfAbsent(playerId, k -> new ArrayList<>());
    }
}
