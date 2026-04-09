package net.fimastgd.lavaflood.client;

public class LavaFloodClientData {
    public static volatile boolean active           = false;
    public static volatile int     currentY         = 0;
    public static volatile int     secondsRemaining = 0;

    public static void reset() {
        active           = false;
        currentY         = 0;
        secondsRemaining = 0;
    }
}