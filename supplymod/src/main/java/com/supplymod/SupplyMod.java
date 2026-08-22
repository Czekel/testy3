package com.supplymod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.item.ItemStack;
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
    // Populated by PlayerDropInventoryMixin when it rescues a tagged item
    // right before vanilla would drop it on death.
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
