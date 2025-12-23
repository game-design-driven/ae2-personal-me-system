package com.example.personalmesystem.network;

import com.example.personalmesystem.PersonalMESystemMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(PersonalMESystemMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void init() {
        CHANNEL.registerMessage(
                packetId++,
                OpenPersonalMEPacket.class,
                OpenPersonalMEPacket::encode,
                OpenPersonalMEPacket::decode,
                OpenPersonalMEPacket::handle
        );
        CHANNEL.registerMessage(
                packetId++,
                BindPersonalMEPacket.class,
                BindPersonalMEPacket::encode,
                BindPersonalMEPacket::decode,
                BindPersonalMEPacket::handle
        );
    }

    public static void sendToServer(Object msg) {
        CHANNEL.sendToServer(msg);
    }
}
