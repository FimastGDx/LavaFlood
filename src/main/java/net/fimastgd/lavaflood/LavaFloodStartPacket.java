package net.fimastgd.lavaflood.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import net.fimastgd.lavaflood.handler.LavaFloodHandler;

import java.util.function.Supplier;

public class LavaFloodStartPacket {

    private final int seconds;

    public LavaFloodStartPacket(int seconds) {
        this.seconds = seconds;
    }

    public static void encode(LavaFloodStartPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.seconds);
    }

    public static LavaFloodStartPacket decode(FriendlyByteBuf buf) {
        return new LavaFloodStartPacket(buf.readInt());
    }

    public static void handle(LavaFloodStartPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            int seconds = Math.max(1, Math.min(1800, msg.seconds));
            LavaFloodHandler.start((ServerLevel) player.getLevel(), player, seconds);
        });
        ctx.get().setPacketHandled(true);
    }
}