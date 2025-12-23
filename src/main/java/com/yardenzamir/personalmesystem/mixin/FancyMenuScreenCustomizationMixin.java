package com.yardenzamir.personalmesystem.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Removes AE2 screens from FancyMenu's blacklist so they can be customized.
 */
@Mixin(targets = "de.keksuccino.fancymenu.customization.ScreenCustomization", remap = false)
public class FancyMenuScreenCustomizationMixin {

    @Inject(method = "isScreenBlacklisted(Ljava/lang/String;)Z", at = @At("HEAD"), cancellable = true)
    private static void allowAE2Screens(String screenClassPath, CallbackInfoReturnable<Boolean> cir) {
        if (screenClassPath != null && screenClassPath.startsWith("appeng.")) {
            cir.setReturnValue(false);
        }
    }
}
