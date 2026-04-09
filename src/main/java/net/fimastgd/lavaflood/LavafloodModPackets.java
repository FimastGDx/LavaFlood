package net.fimastgd.lavaflood.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public class LavafloodModPackets {

    private static final String VER = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("lavaflood", "main"),
            () -> VER, VER::equals, VER::equals
    );

    private static int id = 0;

    /** call this once from your main mod constructor / FMLCommonSetupEvent */
    public static void register() {
        // Client -> Server
        CHANNEL.registerMessage(id++,
                LavaFloodStartPacket.class,
                LavaFloodStartPacket::encode,
                LavaFloodStartPacket::decode,
                LavaFloodStartPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        // Server -> Client
        CHANNEL.registerMessage(id++,
                LavaFloodSyncPacket.class,
                LavaFloodSyncPacket::encode,
                LavaFloodSyncPacket::decode,
                LavaFloodSyncPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
}