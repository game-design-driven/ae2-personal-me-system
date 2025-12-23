package com.yardenzamir.personalmesystem.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Mixin plugin that only applies FancyMenu mixins when FancyMenu is loaded.
 */
public class FancyMenuMixinPlugin implements IMixinConfigPlugin {

    private static boolean fancyMenuLoaded = false;

    static {
        try {
            Class.forName("de.keksuccino.fancymenu.FancyMenu");
            fancyMenuLoaded = true;
        } catch (ClassNotFoundException e) {
            fancyMenuLoaded = false;
        }
    }

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        // Only apply FancyMenu mixins if FancyMenu is loaded
        if (mixinClassName.contains("FancyMenu")) {
            return fancyMenuLoaded;
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
