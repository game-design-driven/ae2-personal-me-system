package com.yardenzamir.personalmesystem.virtualcraft;

import appeng.api.stacks.GenericStack;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Objects;

/**
 * Defines a virtual autocraft recipe that executes instantly without requiring
 * Crafting CPUs or coprocessors.
 */
public record VirtualRecipe(
        ResourceLocation id,
        List<VirtualPatternInput.InputSpec> inputs,
        List<GenericStack> outputs
) {
    public VirtualRecipe {
        Objects.requireNonNull(id, "Recipe ID cannot be null");
        Objects.requireNonNull(inputs, "Inputs cannot be null");
        Objects.requireNonNull(outputs, "Outputs cannot be null");
        if (inputs.isEmpty()) {
            throw new IllegalArgumentException("Recipe must have at least one input");
        }
        if (outputs.isEmpty()) {
            throw new IllegalArgumentException("Recipe must have at least one output");
        }
        inputs = List.copyOf(inputs);
        outputs = List.copyOf(outputs);
    }
}
