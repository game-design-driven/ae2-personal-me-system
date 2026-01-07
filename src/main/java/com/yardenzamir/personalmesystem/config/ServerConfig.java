package com.yardenzamir.personalmesystem.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class ServerConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue REQUIRE_CHANNEL;
    public static final ForgeConfigSpec.DoubleValue POWER_USAGE;
    public static final ForgeConfigSpec.IntValue MAX_RECIPES_PER_RELAY;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Communication Relay Settings").push("relay");

        REQUIRE_CHANNEL = builder
                .comment("Whether the Communication Relay requires an ME channel to function")
                .define("requireChannel", true);

        POWER_USAGE = builder
                .comment("Power (AE) consumed per crafting operation. Set to 0 to disable.")
                .defineInRange("powerUsage", 0.0, 0.0, 10000.0);

        MAX_RECIPES_PER_RELAY = builder
                .comment("Maximum number of recipes a single relay can hold. Set to 0 for unlimited.")
                .defineInRange("maxRecipesPerRelay", 0, 0, 1000);

        builder.pop();

        SPEC = builder.build();
    }
}
