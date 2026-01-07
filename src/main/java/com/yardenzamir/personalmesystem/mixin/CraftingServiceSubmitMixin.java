package com.yardenzamir.personalmesystem.mixin;

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.crafting.ICraftingSubmitResult;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.crafting.execution.CraftingSubmitResult;
import appeng.me.service.CraftingService;
import com.yardenzamir.personalmesystem.virtualcraft.VirtualPatternDetails;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;

/**
 * Mixin to handle virtual-pattern-only crafting jobs without requiring a CPU.
 * When a crafting job consists entirely of virtual patterns, this mixin
 * executes them immediately without needing a Crafting CPU.
 */
@Mixin(value = CraftingService.class, remap = false)
public class CraftingServiceSubmitMixin {

    @Shadow
    @Final
    private IGrid grid;

    /**
     * Intercept job submission to handle virtual-only jobs without CPU.
     */
    @Inject(method = "submitJob", at = @At("HEAD"), cancellable = true)
    private void handleVirtualOnlyJob(ICraftingPlan job, @Nullable ICraftingRequester requestingMachine,
                                      @Nullable ICraftingCPU target, boolean prioritizePower,
                                      IActionSource src, CallbackInfoReturnable<ICraftingSubmitResult> cir) {
        // Check if this is a simulation or has missing items
        if (job.simulation() || !job.missingItems().isEmpty()) {
            return; // Let normal flow handle it
        }

        // Check if all patterns in the job are virtual
        if (!isVirtualOnlyJob(job)) {
            return; // Let normal flow handle it
        }

        // Execute the virtual crafting immediately without CPU
        var storage = grid.getStorageService().getInventory();

        // Extract all required inputs from the network
        Map<AEKey, Long> extractedItems = new HashMap<>();
        for (var entry : job.usedItems()) {
            var key = entry.getKey();
            var amount = entry.getLongValue();
            var extracted = storage.extract(key, amount, Actionable.MODULATE, src);
            if (extracted < amount) {
                // Failed to extract enough - rollback and fail
                for (var rollback : extractedItems.entrySet()) {
                    storage.insert(rollback.getKey(), rollback.getValue(), Actionable.MODULATE, src);
                }
                cir.setReturnValue(CraftingSubmitResult.missingIngredient(new GenericStack(key, amount - extracted)));
                return;
            }
            extractedItems.put(key, extracted);
        }

        // Calculate all outputs from pattern executions
        KeyCounter outputs = new KeyCounter();
        for (var entry : job.patternTimes().entrySet()) {
            var pattern = entry.getKey();
            var times = entry.getValue();
            for (var output : pattern.getOutputs()) {
                outputs.add(output.what(), output.amount() * times);
            }
        }

        // Insert all crafted outputs into the network
        for (var entry : outputs) {
            storage.insert(entry.getKey(), entry.getLongValue(), Actionable.MODULATE, src);
        }

        // Success! No CPU needed.
        cir.setReturnValue(CraftingSubmitResult.successful(null));
    }

    /**
     * Check if a crafting job only uses virtual patterns.
     */
    private boolean isVirtualOnlyJob(ICraftingPlan job) {
        var patternTimes = job.patternTimes();
        if (patternTimes.isEmpty()) {
            return false; // No patterns to check
        }
        for (var entry : patternTimes.entrySet()) {
            IPatternDetails pattern = entry.getKey();
            if (!(pattern instanceof VirtualPatternDetails)) {
                return false;
            }
        }
        return true;
    }
}
