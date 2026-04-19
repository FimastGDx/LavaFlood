package net.fimastgd.lavaflood.handler;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.player.PlayerEvent;

import net.fimastgd.lavaflood.network.LavaFloodSyncPacket;
import net.fimastgd.lavaflood.network.LavafloodModPackets;

import java.util.*;

@Mod.EventBusSubscriber(modid = "lavaflood")
public class LavaFloodHandler {

    // state
    private static boolean active = false;
    private static int centerX, centerZ;
    private static int intervalTicks; // ticks between layers
    private static int ticksUntilNext;
    private static int currentY; // next Y to fill
    private static int maxY;
    private static ServerLevel serverLevel;

    // all 16 concrete colours
    private static final Set<Block> CONCRETE = new HashSet<>(Arrays.asList(
            Blocks.WHITE_CONCRETE, Blocks.ORANGE_CONCRETE,
            Blocks.MAGENTA_CONCRETE, Blocks.LIGHT_BLUE_CONCRETE,
            Blocks.YELLOW_CONCRETE, Blocks.LIME_CONCRETE,
            Blocks.PINK_CONCRETE, Blocks.GRAY_CONCRETE,
            Blocks.LIGHT_GRAY_CONCRETE, Blocks.CYAN_CONCRETE,
            Blocks.PURPLE_CONCRETE, Blocks.BLUE_CONCRETE,
            Blocks.BROWN_CONCRETE, Blocks.GREEN_CONCRETE,
            Blocks.RED_CONCRETE, Blocks.BLACK_CONCRETE));

    // Public API

    /**
     * Called from {@link net.fimastgd.lavaflood.network.LavaFloodStartPacket}
     * on the server thread.
     */
    public static void start(ServerLevel level, ServerPlayer player, int seconds) {
        serverLevel = level;
        centerX = (int) player.getX();
        centerZ = (int) player.getZ();
        intervalTicks = seconds * 20; // 20 ticks = 1 second
        currentY = level.getMinBuildHeight(); // -64 in 1.18.2 overworld
        maxY = level.getMaxBuildHeight(); // 320
        ticksUntilNext = 1; // fill first layer next tick
        active = true;

        // Set world border: 250 blocks diameter centred on player
        WorldBorder border = level.getWorldBorder();
        border.setCenter(centerX + 0.5, centerZ + 0.5);
        border.setSize(250);

        broadcastSync();
    }

    public static void resume(ServerLevel level, int cx, int cz, int fromY, int seconds) {
        serverLevel = level;
        centerX = cx;
        centerZ = cz;
        intervalTicks = seconds * 20;
        currentY = fromY;
        maxY = level.getMaxBuildHeight();
        ticksUntilNext = intervalTicks;
        active = true;
        broadcastSync();
    }

    /** Полная остановка флуда */
    public static void stop() {
        active = false;
        if (serverLevel != null)
            broadcastSync();
    }

    // Server tick

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END)
            return;
        if (!active || serverLevel == null)
            return;

        // finished?
        if (currentY >= maxY) {
            active = false;
            broadcastSync();
            return;
        }

        if (ticksUntilNext <= 0) {
            fillLayer(currentY);
            currentY++;
            ticksUntilNext = intervalTicks;
        } else {
            ticksUntilNext--;
        }

        // sync HUD to clients every second
        if (ticksUntilNext % 20 == 0) {
            broadcastSync();
        }
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!active)
            return;
        if (!(event.getPlayer() instanceof ServerPlayer sp))
            return;

        // сразу шлём актуальное состояние только этому игроку
        int secsLeft = ticksUntilNext / 20;
        LavafloodModPackets.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp),
                new LavaFloodSyncPacket(active, currentY - 1, secsLeft));
    }

    // Layer fill

    /**
     * Fills one Y-layer with lava inside the 250×250 area, respecting:
     * <ul>
     * <li>Concrete blocks → never replaced</li>
     * <li>Space enclosed by a concrete structure not reachable from
     * outside → skipped automatically by the flood-fill</li>
     * </ul>
     */
    private static void fillLayer(int y) {
        // Берём актуальные параметры из границы мира, а не из захардкоженных полей
        WorldBorder border = serverLevel.getWorldBorder();
        int x1 = (int) border.getMinX();
        int z1 = (int) border.getMinZ();
        int sizeX = (int) (border.getMaxX() - border.getMinX());
        int sizeZ = (int) (border.getMaxZ() - border.getMinZ());

        boolean[][] reachable = floodFill(y, x1, z1, sizeX, sizeZ);

        for (int dx = 0; dx < sizeX; dx++) {
            for (int dz = 0; dz < sizeZ; dz++) {
                if (!reachable[dx][dz])
                    continue;
                BlockPos pos = new BlockPos(x1 + dx, y, z1 + dz);
                if (!serverLevel.isLoaded(pos))
                    continue;
                BlockState state = serverLevel.getBlockState(pos);
                if (isConcrete(state))
                    continue;
                serverLevel.setBlock(pos, Blocks.LAVA.defaultBlockState(), 3);
            }
        }
    }

    /**
     * BFS flood-fill on a 2-D grid seeded from every border cell.
     * Concrete blocks act as walls (impassable).
     *
     * @return boolean[w][d] – true means "reachable from outside"
     */
    private static boolean[][] floodFill(int y, int x1, int z1, int w, int d) {
        boolean[][] visited = new boolean[w][d];
        Deque<int[]> queue = new ArrayDeque<>();

        // seed all four edges
        for (int dx = 0; dx < w; dx++) {
            tryEnqueue(queue, visited, dx, 0, y, x1, z1, w, d);
            tryEnqueue(queue, visited, dx, d - 1, y, x1, z1, w, d);
        }
        for (int dz = 1; dz < d - 1; dz++) {
            tryEnqueue(queue, visited, 0, dz, y, x1, z1, w, d);
            tryEnqueue(queue, visited, w - 1, dz, y, x1, z1, w, d);
        }

        final int[] DDX = { 1, -1, 0, 0 };
        final int[] DDZ = { 0, 0, 1, -1 };

        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            for (int i = 0; i < 4; i++) {
                tryEnqueue(queue, visited,
                        cur[0] + DDX[i], cur[1] + DDZ[i],
                        y, x1, z1, w, d);
            }
        }
        return visited;
    }

    private static void tryEnqueue(Deque<int[]> queue, boolean[][] visited,
            int dx, int dz,
            int y, int x1, int z1, int w, int d) {
        if (dx < 0 || dx >= w || dz < 0 || dz >= d)
            return;
        if (visited[dx][dz])
            return;
        BlockPos pos = new BlockPos(x1 + dx, y, z1 + dz);
        if (serverLevel.isLoaded(pos) && isConcrete(serverLevel.getBlockState(pos)))
            return; // wall
        visited[dx][dz] = true;
        queue.add(new int[] { dx, dz });
    }

    // helpers

    public static boolean isConcrete(BlockState state) {
        return CONCRETE.contains(state.getBlock());
    }

    private static void broadcastSync() {
        if (serverLevel == null || serverLevel.getServer() == null)
            return;
        int secsLeft = ticksUntilNext / 20;
        LavaFloodSyncPacket pkt = new LavaFloodSyncPacket(active, currentY - 1, secsLeft);
        LavafloodModPackets.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.ALL.noArg(), pkt);
    }
}