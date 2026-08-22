package com.supplymod.mixin;

import com.supplymod.SupplyMod;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
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
 *
 * NOTE (MC 1.21.1): custom data lives in the DataComponentTypes.CUSTOM_DATA
 * component (an NbtComponent), not a raw NbtCompound on the stack directly.
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
                saved.remove(DataComponentTypes.CUSTOM_DATA);
                SupplyMod.stashFor(player.getUuid()).add(saved);
                inv.setStack(i, ItemStack.EMPTY);
            }
        }
    }

    private static boolean isPardoned(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
        return data != null && data.copyNbt().getBoolean("supplymod_pardoned");
    }
}
