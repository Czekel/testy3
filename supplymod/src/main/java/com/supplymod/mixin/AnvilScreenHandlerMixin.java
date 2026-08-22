package com.supplymod.mixin;

import com.supplymod.ModItems;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.AnvilScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds a recipe to the anvil: combine ANY item (left slot) with the
 * "Ksiega Ulaskawienia" (Book of Pardon, right slot) to tag the left item
 * so that it survives the player's death instead of dropping.
 *
 * We hook updateResult(), which vanilla calls whenever the anvil inputs
 * change, and if the right slot holds a Book of Pardon we override the
 * output with a tagged copy of the left item at a fixed, cheap level cost.
 */
@Mixin(AnvilScreenHandler.class)
public abstract class AnvilScreenHandlerMixin {

    @Shadow
    private Inventory input;

    @Shadow
    public abstract void setNewItemName(String name);

    @Inject(method = "updateResult", at = @At("RETURN"))
    private void supplymod$applyPardonBook(CallbackInfo ci) {
        AnvilScreenHandler self = (AnvilScreenHandler) (Object) this;
        ItemStack left = this.input.getStack(0);
        ItemStack right = this.input.getStack(1);

        if (left.isEmpty() || right.isEmpty()) {
            return;
        }
        if (!right.isOf(ModItems.BOOK_OF_PARDON)) {
            return;
        }

        ItemStack result = left.copy();
        NbtCompound nbt = result.getOrCreateNbt();
        nbt.putBoolean("supplymod_pardoned", true);

        self.getOutput().setStack(0, result);
        self.setNewLevelCost(1);
    }
}

