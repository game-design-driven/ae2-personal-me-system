package com.yardenzamir.personalmesystem.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class ServerConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.DoubleValue POWER_USAGE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Communication Relay Settings").push("relay");

        POWER_USAGE = builder
                .comment("Power (AE) consumed per crafting operation. Set to 0 to disable.")
                .defineInRange("powerUsage", 0.0, 0.0, 10000.0);

        builder.pop();

        SPEC = builder.build();
    }
}
