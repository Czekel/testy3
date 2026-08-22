package com.supplymod.mixin;

import com.supplymod.ModItems;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.Property;
import org.spongepowered.asm.mixin.Final;
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
 *
 * NOTE (MC 1.21.1): items no longer carry a raw NbtCompound directly -
 * custom data is stored via the DataComponentTypes.CUSTOM_DATA component
 * (wrapping an NbtComponent), which is why this differs from older-version
 * mixin examples you may see online.
 */
@Mixin(AnvilScreenHandler.class)
public abstract class AnvilScreenHandlerMixin {

    @Shadow
    private Inventory input;

    @Shadow
    @Final
    protected CraftingResultInventory output;

    @Shadow
    @Final
    private Property levelCost;

    @Inject(method = "updateResult", at = @At("RETURN"))
    private void supplymod$applyPardonBook(CallbackInfo ci) {
        ItemStack left = this.input.getStack(0);
        ItemStack right = this.input.getStack(1);

        if (left.isEmpty() || right.isEmpty()) {
            return;
        }
        if (!right.isOf(ModItems.BOOK_OF_PARDON)) {
            return;
        }

        ItemStack result = left.copy();
        NbtCompound nbt = new NbtCompound();
        nbt.putBoolean("supplymod_pardoned", true);
        result.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));

        this.output.setStack(0, result);
        this.levelCost.set(1);
    }
}
