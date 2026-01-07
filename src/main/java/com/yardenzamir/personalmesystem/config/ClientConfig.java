package com.yardenzamir.personalmesystem.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class ClientConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue SHOW_RECIPE_TOOLTIPS;
    public static final ForgeConfigSpec.IntValue MAX_TOOLTIP_RECIPES;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Communication Relay Tooltip Settings").push("tooltips");

        SHOW_RECIPE_TOOLTIPS = builder
                .comment("Show virtual recipes in item tooltips")
                .define("showRecipeTooltips", true);

        MAX_TOOLTIP_RECIPES = builder
                .comment("Maximum number of recipes to show in tooltips (0 = show all)")
                .defineInRange("maxTooltipRecipes", 3, 0, 20);

        builder.pop();

        SPEC = builder.build();
    }
}
