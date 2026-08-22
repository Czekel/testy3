package com.supplymod.mixin;

import com.supplymod.SupplyMod;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Ksiega Ulaskawienia effect: before the vanilla "drop everything on death"
 * logic runs, pull out any item tagged "supplymod_pardoned" from the
 * player's inventory (main, armor, offhand) so it never hits the ground.
 * The item is stashed in SupplyMod.PARDONED_ITEMS and handed back to the
 * player on respawn (see ServerPlayerEvents.COPY_FROM in SupplyMod), with
 * the pardon tag removed - the protection is single-use.
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerDropInventoryMixin {

    @Inject(method = "dropInventory", at = @At("HEAD"))
    private void supplymod$rescuePardonedItems(CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        PlayerInventory inv = player.getInventory();

        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (isPardoned(stack)) {
                ItemStack saved = stack.copy();
                saved.getOrCreateNbt().remove("supplymod_pardoned");
                SupplyMod.stashFor(player.getUuid()).add(saved);
                inv.setStack(i, ItemStack.EMPTY);
            }
        }
    }

    private static boolean isPardoned(ItemStack stack) {
        return !stack.isEmpty() && stack.hasNbt()
                && stack.getNbt().contains("supplymod_pardoned")
                && stack.getNbt().getBoolean("supplymod_pardoned");
    }
}

