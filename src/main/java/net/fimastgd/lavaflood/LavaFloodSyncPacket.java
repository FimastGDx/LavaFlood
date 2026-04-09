package net.fimastgd.lavaflood.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import net.fimastgd.lavaflood.client.LavaFloodClientData;

import java.util.function.Supplier;

public class LavaFloodSyncPacket {

    public final boolean active;
    public final int currentY;
    public final int secondsRemaining;

    public LavaFloodSyncPacket(boolean active, int currentY, int secondsRemaining) {
        this.active           = active;
        this.currentY         = currentY;
        this.secondsRemaining = secondsRemaining;
    }

    public static void encode(LavaFloodSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.active);
        buf.writeInt(msg.currentY);
        buf.writeInt(msg.secondsRemaining);
    }

    public static LavaFloodSyncPacket decode(FriendlyByteBuf buf) {
        return new LavaFloodSyncPacket(buf.readBoolean(), buf.readInt(), buf.readInt());
    }

    public static void handle(LavaFloodSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // runs on the client rendering thread
            LavaFloodClientData.active           = msg.active;
            LavaFloodClientData.currentY         = msg.currentY;
            LavaFloodClientData.secondsRemaining = msg.secondsRemaining;
        });
        ctx.get().setPacketHandled(true);
    }
}