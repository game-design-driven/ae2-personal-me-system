package com.yardenzamir.personalmesystem.mixin;

import appeng.client.gui.me.crafting.CraftConfirmScreen;
import appeng.menu.me.crafting.CraftConfirmMenu;
import appeng.menu.me.crafting.CraftingPlanSummary;
import net.minecraft.client.gui.components.Button;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to allow starting crafting jobs even without CPUs when virtual patterns are involved.
 * The server-side CraftingServiceSubmitMixin handles virtual-only jobs without needing a CPU.
 * For non-virtual jobs, the server will properly return an error if no CPU is available.
 */
@Mixin(value = CraftConfirmScreen.class, remap = false)
public class CraftConfirmScreenMixin {

    @Shadow
    private Button start;

    /**
     * After the original updateBeforeRender, re-enable the start button if we have a valid plan,
     * even if there's no CPU available. Virtual crafting doesn't need CPUs.
     */
    @Inject(method = "updateBeforeRender", at = @At("RETURN"))
    private void allowStartWithoutCpu(CallbackInfo ci) {
        // Get the menu to check the plan
        var screen = (CraftConfirmScreen) (Object) this;
        var menu = screen.getMenu();
        CraftingPlanSummary plan = menu.getPlan();

        // If we have a valid, non-simulation plan, allow starting even without CPU
        // Server-side mixin will handle virtual-only jobs, others will get proper error
        if (plan != null && !plan.isSimulation()) {
            this.start.active = true;
        }
    }
}
