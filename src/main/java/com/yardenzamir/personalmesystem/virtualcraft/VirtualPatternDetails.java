package com.yardenzamir.personalmesystem.virtualcraft;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Objects;

/**
 * IPatternDetails implementation for virtual autocraft recipes.
 * Virtual patterns execute instantly without requiring Crafting CPUs.
 */
public class VirtualPatternDetails implements IPatternDetails {
    private static final String NBT_RECIPE_ID = "virtual_recipe_id";

    private final VirtualRecipe recipe;
    private final AEItemKey definition;
    private final IInput[] inputs;
    private final GenericStack[] outputs;

    public VirtualPatternDetails(VirtualRecipe recipe) {
        this.recipe = Objects.requireNonNull(recipe);
        this.definition = createDefinitionKey(recipe.id());
        this.inputs = recipe.inputs().stream()
                .map(VirtualPatternInput::new)
                .toArray(IInput[]::new);
        this.outputs = recipe.outputs().toArray(GenericStack[]::new);
    }

    private static AEItemKey createDefinitionKey(ResourceLocation recipeId) {
        var stack = new ItemStack(Items.PAPER);
        var tag = new CompoundTag();
        tag.putString(NBT_RECIPE_ID, recipeId.toString());
        stack.setTag(tag);
        return AEItemKey.of(stack);
    }

    public VirtualRecipe getRecipe() {
        return recipe;
    }

    public ResourceLocation getRecipeId() {
        return recipe.id();
    }

    @Override
    public AEItemKey getDefinition() {
        return definition;
    }

    @Override
    public IInput[] getInputs() {
        return inputs;
    }

    @Override
    public GenericStack[] getOutputs() {
        return outputs;
    }

    @Override
    public GenericStack getPrimaryOutput() {
        return outputs.length > 0 ? outputs[0] : null;
    }

    @Override
    public boolean supportsPushInputsToExternalInventory() {
        return false;
    }

    @Override
    public int hashCode() {
        return recipe.id().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof VirtualPatternDetails other)) return false;
        return recipe.id().equals(other.recipe.id());
    }

    @Override
    public String toString() {
        return "VirtualPattern[" + recipe.id() + "]";
    }
}
